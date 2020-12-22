package me.gammadelta.common.program.compilation;

import me.gammadelta.Utils;
import me.gammadelta.common.program.compilation.Instruction.Arg;

import java.math.BigInteger;
import java.util.*;

/**
 * Writes the given program to bytes
 */
public class BytecodeWriter {
    private List<Instruction> program;
    private List<Byte> wipProgram = new ArrayList<>();

    /** Maps instruction indexes to the byte indexes they start at. */
    private ArrayList<Integer> instructionStarts = new ArrayList<>();

    /**
     * Cache of byte indexes of label IVs we need to fill in.
     * In the initial pass we write [0xCACA] for any label IVs as a placeholder,
     * and save (byte index => instruction index) here.
     * In the final pass we use `instructionStarts` to plug in the data.
     */
    private Map<Integer, Integer> labelIVsRequiringFills = new HashMap<>();

    public BytecodeWriter(List<Instruction> program) {
        this.program = program;
    }

    /**
     * Write the program to bytecode.
     */
    public ArrayList<Byte> writeProgramToBytecode() throws BytecodeWriteException {
        for (Instruction line : this.program) {
            this.instructionStarts.add(wipProgram.size());

            // Write opcode
            short opcodeShort = Opcode.OPCODES_TO_BYTECODE.get(line.opcode);
            wipProgram.add((byte) opcodeShort); // this cuts off the top, i checked.

            // Write arguments
            for (Arg arg : line.args) {
                String reg = arg.token.canonicalize();
                if (arg.type == Arg.Type.REGISTER) {
                    byte regi = writeRegister(arg);
                    wipProgram.add(regi);
                } else if (arg.type == Arg.Type.IV) {
                    ArrayList<Byte> iv = writeIV(arg);
                    wipProgram.addAll(iv);
                } else {
                    // external location arg
                    ArrayList<Byte> location = writeLocation(arg);
                    wipProgram.addAll(location);
                }
            }
        }

        // Do our second pass and fill in the labels expected.
        ArrayList<Byte> out = new ArrayList<>(this.wipProgram.size());
        int accumulatedExtraBytes = 0;
        for (int byteIdx = 0; byteIdx < this.wipProgram.size(); byteIdx++) {
            if (!labelIVsRequiringFills.containsKey(byteIdx)) {
                // totally normal write
                out.add(this.wipProgram.get(byteIdx));
            } else {
                // we must add our byte index instead
                int labelInstructionIdx = labelIVsRequiringFills.get(byteIdx);
                int labelByteIdx = instructionStarts.get(labelInstructionIdx) + accumulatedExtraBytes;
                // We might need to add extra bytes if labelByteIndex itself requires more than one byte to write.
                // To do this, we find the position of the highest 1 bit of the number, divide by 8, and add 1.
                // i got this code off stackoverflow
                int highestBit = labelByteIdx;
                highestBit |= highestBit >> 1;
                highestBit |= highestBit >> 2;
                highestBit |= highestBit >> 4;
                highestBit |= highestBit >> 8;
                highestBit |= highestBit >> 16;
                highestBit -= highestBit >> 1;
                int extraBytesNeeded = highestBit / 8 + 1;
                labelByteIdx += extraBytesNeeded;

                // write this as a literal
                ArrayList<Byte> labelValue =
                        writeLiteral(new Token(Token.Type.DECIMAL, String.valueOf(labelByteIdx), -1, -1));
                accumulatedExtraBytes += labelValue.size();
                out.addAll(labelValue);
            }
        }

        return out;
    }

    // region Writers for the three argument types

    private static byte writeRegister(Arg arg) {
        return writeRegister(arg.token);
    }

    private ArrayList<Byte> writeIV(Arg arg) throws BytecodeWriteException {
        if (arg.token.type != Token.Type.STACKVALUE) {
            return writeIVThatsNotAStackvalue(arg.token);
        } else {
            return writeStackvalue(arg.token, arg.stackvaluePosition);
        }
    }

    private ArrayList<Byte> writeLocation(Arg arg) throws BytecodeWriteException {
        if (arg.token.type == Token.Type.DATAFACE) {
            int index = Integer.parseInt(arg.token.meat());
            if (index > 127 || index < 0) {
                throw new IllegalStateException(String.format("Dataface index was out of bounds %s", arg.token));
            }
            byte dataface = (byte) (0b10000000 | index);
            return new ArrayList<>(Collections.singletonList(dataface));
        } else {
            // Write [0] here and append a literal
            ArrayList<Byte> address = writeIV(arg);
            ArrayList<Byte> out = new ArrayList<>(address.size() + 1);
            out.add((byte) 0);
            out.addAll(address);
            return out;
        }
    }

    // endregion

    // region Writers for all the concrete things the argument types can be

    private ArrayList<Byte> writeIVThatsNotAStackvalue(Token token) throws BytecodeWriteException {
        if (token.alias != null && token.alias.type == Token.Type.LABEL_USAGE) {
            // this is a label and the original is the *instruction index*.
            return writeLabelIV(token);
        } else if (token.type == Token.Type.DECIMAL || token.type == Token.Type.HEXADECIMAL || token.type == Token.Type.BINARY) {
            return writeLiteral(token);
        } else if (token.type == Token.Type.REGISTER) {
            return new ArrayList<>(Collections.singletonList(writeRegister(token)));
        } else {
            throw new IllegalStateException(String.format("bad type %s", token.type));
        }
    }

    private static byte writeRegister(Token regTok) {
        String reg = regTok.canonicalize();
        if (reg.equals("NIL")) {
            return 0;
        } else if (reg.equals("IP")) {
            return 1;
        } else if (reg.equals("SP")) {
            return 2;
        } else if (reg.equals("FLAGS")) {
            return 3;
        } else {
            if (reg.charAt(0) != 'R') {
                throw new IllegalStateException(String.format("Register %s cannot be parsed", regTok));
            }
            byte regIndex;
            try {
                regIndex = Byte.parseByte(reg.substring(1));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(String.format("Register %s had bad index", regTok), e);
            }
            if ((regIndex & 0b10000000) != 0) {
                throw new IllegalStateException(String.format("Register %s had an index too large", regTok));
            }
            return (byte) (0b10000000 | regIndex);
        }
    }

    private static ArrayList<Byte> writeLiteral(Token literalTok) throws BytecodeWriteException {
        if (literalTok.type == Token.Type.HEXADECIMAL || literalTok.type == Token.Type.BINARY) {
            // Don't worry about negatives here
            int radix = literalTok.type == Token.Type.HEXADECIMAL ? 16 : 2;
            int charsPerByte = literalTok.type == Token.Type.HEXADECIMAL ? 2 : 8;

            String strValue = literalTok.meat();
            int leadingZeroes = 0;
            for (char c : strValue.toCharArray()) {
                if (c == '0') {
                    leadingZeroes++;
                } else {
                    break;
                }
            }
            // Shell out to BigInt
            BigInteger imLazy = Utils.parseBigInt(literalTok.value);
            if (imLazy.equals(BigInteger.ZERO)) {
                // 0x0 -> [0], not [0, 0]
                // 0x00 -> [0, 0], not [0, 0, 0]
                leadingZeroes--;
            }
            byte[] bigintBytes = imLazy.toByteArray();


            // We may need to add leading zeroes!
            int leadingZeroBytes = leadingZeroes == 0 ? 0 : (leadingZeroes / charsPerByte + 1);
            int totalValueSize = leadingZeroBytes + bigintBytes.length;
            if (totalValueSize > 63) {
                throw new BytecodeWriteException.LiteralTooLong(literalTok);
            }
            ArrayList<Byte> out = new ArrayList<>(totalValueSize + 1); // add 1 for the size itself
            // Put our size in
            int futzedSize = totalValueSize & 0b00111111 | 0b01000000;
            out.add((byte) futzedSize);
            // Put in leading zeroes
            for (int c = 0; c < leadingZeroBytes; c++) {
                out.add((byte) 0);
            }
            // And put in the value
            // yes this is the best way to do it, boxing issues otherwise
            boolean seenANonZero = false;
            for (byte bigintByte : bigintBytes) {
                // big int sometimes puts leading zeroes
                if (seenANonZero || bigintByte != 0) {
                    seenANonZero = true;
                    out.add(bigintByte);
                }
            }
            return out;
        } else {
            // decimal digit
            // just shell out to BigInteger, it'll be fine
            BigInteger imLazy = Utils.parseBigInt(literalTok.value);
            byte[] value = imLazy.toByteArray();
            int size = value.length;
            if (size > 63) {
                throw new BytecodeWriteException.LiteralTooLong(literalTok);
            }
            ArrayList<Byte> out = new ArrayList<>(size + 1); // have space for our length
            int futzedSize = size & 0b00111111 | 0b01000000;
            out.add((byte) futzedSize);
            boolean seenANonZero = false;
            for (byte valByte : value) {
                if (seenANonZero || valByte != 0) {
                    seenANonZero = true;
                    out.add(valByte);
                }
            }
            return out;
        }
    }

    private ArrayList<Byte> writeLabelIV(Token labelTok) {
        // Haha sike we don't write jack shit
        // Save this byte index as requiring a label
        this.labelIVsRequiringFills.put(this.wipProgram.size(), Utils.parseInt(labelTok.value));
        return new ArrayList<>(Collections.singletonList((byte) 0));
    }

    private ArrayList<Byte> writeStackvalue(Token svTok, Token sizeTok) throws BytecodeWriteException {
        // Token has the length; stackvaluePosition has the position.
        int size = Integer.parseInt(svTok.meat());
        if (size < 0 || size > 128) {
            throw new BytecodeWriteException.StackvalueSizeOutOfBounds(svTok);
        }
        int header = 0b00100000 | size;

        ArrayList<Byte> position = writeIVThatsNotAStackvalue(sizeTok);
        ArrayList<Byte> out = new ArrayList<>(position.size() + 1);
        out.add((byte) header);
        out.addAll(position);
        return out;
    }

    // endregion
}
