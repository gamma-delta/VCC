package me.gammadelta.common.program.compilation;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.Utils;
import me.gammadelta.common.program.compilation.Instruction.Arg;

import java.math.BigInteger;
import java.util.*;

/**
 * Writes the given program to bytes
 */
public class BytecodeWriter {
    private static final int LABEL_USAGE_PLACEHOLDER_SIZE = 3;
    private static final byte LABEL_USAGE_HEADER = (byte) 0b01000000 | (0xff & LABEL_USAGE_PLACEHOLDER_SIZE);

    private List<Instruction> program;
    private ByteList wipProgram = new ByteArrayList();

    /**
     * Maps instruction indexes to the byte indexes they start at.
     */
    private IntList instructionStarts = new IntArrayList();

    /**
     * Cache of byte indexes of label IVs we need to fill in.
     * In the initial pass we write [header 0 0 0] for any label IVs as a placeholder,
     * and save (byte index of fill required => instruction index needed) here.
     * In the final pass we use `instructionStarts` to plug in the data.
     */
    private Map<Integer, Integer> labelIVsRequiringFills = new HashMap<>();

    public BytecodeWriter(List<Instruction> program) {
        this.program = program;
    }

    /**
     * Write the program to bytecode.
     */
    public ByteList writeProgramToBytecode() throws BytecodeWriteException {
        for (Instruction line : this.program) {
            this.instructionStarts.add(wipProgram.size());

            // Write opcode
            byte opcode = Opcode.OPCODES_TO_BYTECODE.get(line.opcode);
            wipProgram.add(opcode);

            // Write arguments
            for (Arg arg : line.args) {
                String reg = arg.token.canonicalize();
                if (arg.type == Arg.Type.REGISTER) {
                    byte regi = writeRegister(arg);
                    wipProgram.add(regi);
                } else if (arg.type == Arg.Type.IV) {
                    ByteList iv = writeIV(arg, 0);
                    wipProgram.addAll(iv);
                } else {
                    // external location arg
                    ByteList location = writeLocation(arg);
                    wipProgram.addAll(location);
                }
            }
        }

        // Do our second pass and fill in the labels expected.
        for (int byteIdx = 0; byteIdx < this.wipProgram.size(); byteIdx++) {
            if (labelIVsRequiringFills.containsKey(byteIdx)) {
                // we must add our byte index instead
                int labelInstructionIdx = labelIVsRequiringFills.get(byteIdx);
                int labelByteIdx = instructionStarts.getInt(labelInstructionIdx);
                // Turn this into 4 bytes
                byte[] idxBytes = new byte[LABEL_USAGE_PLACEHOLDER_SIZE];
                // we only need to write an int, but java will extend it for us
                Utils.writeLong(labelByteIdx, idxBytes);
                // write the header
                this.wipProgram.set(byteIdx++, LABEL_USAGE_HEADER);
                // and write the label destination to the output
                for (int i = 0; i < LABEL_USAGE_PLACEHOLDER_SIZE; i++) {
                    this.wipProgram.set(byteIdx++, idxBytes[i]);
                }
            }
        }

        return this.wipProgram;
    }

    // region Writers for the three argument types

    private static byte writeRegister(Arg arg) {
        return writeRegister(arg.token);
    }

    private ByteList writeIV(Arg arg, int labelOffset) throws BytecodeWriteException {
        if (arg.token.type != Token.Type.STACKVALUE) {
            return writeIVThatsNotAStackvalue(arg.token, labelOffset);
        } else {
            return writeStackvalue(arg.token, arg.stackvaluePosition, labelOffset);
        }
    }

    private ByteList writeLocation(Arg arg) throws BytecodeWriteException {
        if (arg.token.type == Token.Type.DATAFACE) {
            int index = Integer.parseInt(arg.token.meat());
            if (index > 127 || index < 0) {
                throw new IllegalStateException(String.format("Dataface index was out of bounds %s", arg.token));
            }
            byte dataface = (byte) (0b10000000 | index);
            return new ByteArrayList(Collections.singletonList(dataface));
        } else {
            // Write [0] here and append a literal
            // we do a label offset of 1 so it doesn't clobber the header.
            ByteList address = writeIV(arg, 1);
            ByteList out = new ByteArrayList(address.size() + 1);
            out.add((byte) 0);
            out.addAll(address);
            return out;
        }
    }

    // endregion

    // region Writers for all the concrete things the argument types can be

    private ByteList writeIVThatsNotAStackvalue(Token token, int labelOffset) throws BytecodeWriteException {
        if (token.alias != null && token.alias.type == Token.Type.LABEL_USAGE) {
            // this is a label and the original is the *instruction index*.
            return writeLabelIV(token, labelOffset);
        } else if (token.type == Token.Type.DECIMAL || token.type == Token.Type.HEXADECIMAL || token.type == Token.Type.BINARY) {
            return writeLiteral(token);
        } else if (token.type == Token.Type.REGISTER) {
            return new ByteArrayList(Collections.singletonList(writeRegister(token)));
        } else {
            throw new IllegalStateException(String.format("bad type when writing non-stackvalue IV %s to bytecode", token));
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

    private static ByteList writeLiteral(Token literalTok) throws BytecodeWriteException {
        if (literalTok.type == Token.Type.HEXADECIMAL || literalTok.type == Token.Type.BINARY) {
            // Don't worry about negatives here
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
            int leadingZeroBytes = leadingZeroes / charsPerByte;

            // Shell out to BigInt
            BigInteger imLazy = Utils.parseBigInt(literalTok.value);
            if (imLazy.equals(BigInteger.ZERO)) {
                // oop just return the header with zero bytes
                return ByteLists.singleton((byte) 0b01000000);
            }
            byte[] bigintBytesRaw = imLazy.toByteArray();
            // Slice away leading zero bytes
            int firstNonZeroByteIdx = 0;
            for (byte b : bigintBytesRaw) {
                if (b != 0) {
                    break;
                } else {
                    firstNonZeroByteIdx++;
                }
            }
            ByteList bigintBytes = new ByteArrayList(bigintBytesRaw, firstNonZeroByteIdx, bigintBytesRaw.length - firstNonZeroByteIdx);

            // We may need to add leading zeroes!
            int totalValueSize = leadingZeroBytes + bigintBytes.size();
            if (totalValueSize > 63) {
                throw new BytecodeWriteException.LiteralTooLong(literalTok);
            }
            ByteList out = new ByteArrayList(totalValueSize + 1); // add 1 for the size itself
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
            if (imLazy.equals(BigInteger.ZERO)) {
                // oop just return the header with zero bytes
                return ByteLists.singleton((byte) 0b01000000);
            }
            byte[] value = imLazy.toByteArray();
            // Calculate size WITHOUT leading zeroes
            int size = value.length;
            for (byte b : value) {
                if (b != 0) {
                    break;
                } else {
                    size--;
                }
            }
            if (size > 63) {
                throw new BytecodeWriteException.LiteralTooLong(literalTok);
            }
            ByteList out = new ByteArrayList(size + 1); // have space for our length
            int futzedSize = size & 0b00111111 | 0b01000000;
            out.add((byte) futzedSize);
            if (imLazy.equals(BigInteger.ZERO)) {
                // just write a zero
                out.add((byte) 0);
            } else {
                boolean seenANonZero = false;
                for (byte valByte : value) {
                    if (seenANonZero || valByte != 0) {
                        seenANonZero = true;
                        out.add(valByte);
                    }
                }
            }
            return out;
        }
    }

    private ByteList writeLabelIV(Token labelTok, int labelOffset) {
        int instrIdx = Utils.parseInt(labelTok.value);
        if (instrIdx == 0) {
            // save some space and write a 0.
            return ByteLists.singleton(0b01000000);
        }
        // Haha sike we don't write jack shit
        // Save this byte index as requiring a label
        this.labelIVsRequiringFills.put(this.wipProgram.size() + labelOffset, Utils.parseInt(labelTok.value));
        // Add 1 so we can add the header
        return new ByteArrayList(new byte[LABEL_USAGE_PLACEHOLDER_SIZE + 1]);
    }

    private ByteList writeStackvalue(Token svTok, Token sizeTok, int labelOffset) throws BytecodeWriteException {
        // Token has the length; stackvaluePosition has the position.
        int size = Integer.parseInt(svTok.meat());
        if (size < 0 || size > 128) {
            throw new BytecodeWriteException.StackvalueSizeOutOfBounds(svTok);
        }
        int header = 0b00100000 | size;

        ByteList position = writeIVThatsNotAStackvalue(sizeTok, labelOffset);
        ByteList out = new ByteArrayList(position.size() + 1);
        out.add((byte) header);
        out.addAll(position);
        return out;
    }

    // endregion
}
