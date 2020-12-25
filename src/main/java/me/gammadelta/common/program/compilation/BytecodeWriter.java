package me.gammadelta.common.program.compilation;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
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
    private List<Instruction> program;
    private ByteList wipProgram = new ByteArrayList();

    /**
     * Maps instruction indexes to the byte indexes they start at.
     */
    private IntList instructionStarts = new IntArrayList();

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
    public ByteList writeProgramToBytecode() throws BytecodeWriteException {
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
                    ByteList iv = writeIV(arg);
                    wipProgram.addAll(iv);
                } else {
                    // external location arg
                    ByteList location = writeLocation(arg);
                    wipProgram.addAll(location);
                }
            }
        }

        // Do our second pass and fill in the labels expected.
        ByteArrayList out = new ByteArrayList(this.wipProgram.size());
        // As we fill in labels, bytes *after* that space need to be shifted forwards.
        // Store here a map of [label indices -> extra bytes written]
        // and only insert the values *up to* the currently writing index
        // TODO: is an AVL or RB tree map better?
        Int2IntSortedMap accumulatedExtraBytes = new Int2IntAVLTreeMap();
        for (int byteIdx = 0; byteIdx < this.wipProgram.size(); byteIdx++) {
            if (!labelIVsRequiringFills.containsKey(byteIdx)) {
                // totally normal write
                out.add(this.wipProgram.getByte(byteIdx));
            } else {
                // we must add our byte index instead
                int labelInstructionIdx = labelIVsRequiringFills.get(byteIdx);
                // get all the extra bytes we inserted *before* this label
                int extraBytes = accumulatedExtraBytes.headMap(labelInstructionIdx)
                        .values()
                        .stream()
                        .reduce(0, Integer::sum);
                int labelByteIdx = instructionStarts.getInt(labelInstructionIdx) + extraBytes;

                // If the byte index we are referring to happens *after* this, we need to insert extra bytes
                // to account for the bytes we are about to write.
                if (labelByteIdx > byteIdx) {
                    // To do this, we find the position of the highest 1 bit of the number, divide by 8, and add 1.
                    // i got this code off stackoverflow
                    int highestBit = 0;
                    for (int c = 31; c >= 0; c--) {
                        int mask = 1 << c;
                        if ((labelByteIdx & mask) != 0) {
                            highestBit = c + 1;
                            break;
                        }
                    }
                    int extraBytesDueToLongSize = highestBit / 8 + 1;
                    labelByteIdx += extraBytesDueToLongSize;
                }

                // write this as a literal
                ByteList labelValue =
                        writeLiteral(new Token(Token.Type.DECIMAL, String.valueOf(labelByteIdx), -1, -1));
                // we already had one byte in the size, so subtract one
                accumulatedExtraBytes.put(labelByteIdx, labelValue.size() - 1);
                out.addAll(labelValue);
            }
        }

        return out;
    }

    // region Writers for the three argument types

    private static byte writeRegister(Arg arg) {
        return writeRegister(arg.token);
    }

    private ByteList writeIV(Arg arg) throws BytecodeWriteException {
        if (arg.token.type != Token.Type.STACKVALUE) {
            return writeIVThatsNotAStackvalue(arg.token);
        } else {
            return writeStackvalue(arg.token, arg.stackvaluePosition);
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
            ByteList address = writeIV(arg);
            ByteList out = new ByteArrayList(address.size() + 1);
            out.add((byte) 0);
            out.addAll(address);
            return out;
        }
    }

    // endregion

    // region Writers for all the concrete things the argument types can be

    private ByteList writeIVThatsNotAStackvalue(Token token) throws BytecodeWriteException {
        if (token.alias != null && token.alias.type == Token.Type.LABEL_USAGE) {
            // this is a label and the original is the *instruction index*.
            return writeLabelIV(token);
        } else if (token.type == Token.Type.DECIMAL || token.type == Token.Type.HEXADECIMAL || token.type == Token.Type.BINARY) {
            return writeLiteral(token);
        } else if (token.type == Token.Type.REGISTER) {
            return new ByteArrayList(Collections.singletonList(writeRegister(token)));
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
                // 0x0 -> [0], not [0, 0]
                // 0x00 -> [0, 0], not [0, 0, 0]
                leadingZeroBytes--;
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
            byte[] value = imLazy.toByteArray();
            int size = value.length;
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

    private ByteList writeLabelIV(Token labelTok) {
        // Haha sike we don't write jack shit
        // Save this byte index as requiring a label
        this.labelIVsRequiringFills.put(this.wipProgram.size(), Utils.parseInt(labelTok.value));
        return new ByteArrayList(Collections.singletonList((byte) 0));
    }

    private ByteList writeStackvalue(Token svTok, Token sizeTok) throws BytecodeWriteException {
        // Token has the length; stackvaluePosition has the position.
        int size = Integer.parseInt(svTok.meat());
        if (size < 0 || size > 128) {
            throw new BytecodeWriteException.StackvalueSizeOutOfBounds(svTok);
        }
        int header = 0b00100000 | size;

        ByteList position = writeIVThatsNotAStackvalue(sizeTok);
        ByteList out = new ByteArrayList(position.size() + 1);
        out.add((byte) header);
        out.addAll(position);
        return out;
    }

    // endregion
}
