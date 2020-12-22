package me.gammadelta.common.program.compilation;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;

/**
 * Writes the given program to bytes
 */
public class BytecodeWriter {
    List<Instruction> program;

    public BytecodeWriter(List<Instruction> program) {
        this.program = program;
    }

    /**
     * Write the program to bytecode.
     */
    public byte[] writeProgramToBytecode() throws BytecodeWriteException {
        // yes this is HORRENDOUSLY ineffecient,
        // but by the end I will need O(1) popping.
        LinkedList<Byte> out = new LinkedList<>();
        // Maps labels to the byte index they point at
        Map<String, Integer> labelPositions = new HashMap<>();
        // Cache of indexes of IVs we need to fill in with labels.
        // We start by filling them with 8-byte literals;
        // if we need less precision after all is said and done,
        // we can pop things off.
        // if we need more precision I question why you
        // are writing a 18,446,744,073,709,551,616-byte long program.

        for (Instruction line : this.program) {
            // Write opcode
            short opcodeShort = Opcode.OPCODES_TO_BYTECODE.get(line.opcode);
            out.add((byte) opcodeShort); // this cuts off the top, i checked.

            // Write arguments
            for (Instruction.Arg arg : line.args) {
                String reg = arg.token.canonicalize();
                if (arg.type == Instruction.Arg.Type.REGISTER) {
                    byte regi = writeRegister(arg);
                    out.add(regi);
                } else if (arg.type == Instruction.Arg.Type.IV) {

                }
            }
        }

        // TODO
        return null;
    }

    private byte writeRegister(Instruction.Arg arg) {
        String reg = arg.token.canonicalize();
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
                throw new IllegalStateException(String.format("Register %s cannot be parsed", arg.token));
            }
            byte regIndex;
            try {
                regIndex = Byte.parseByte(reg.substring(1));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(String.format("Register %s had bad index", arg.token), e);
            }
            if ((regIndex & 0b10000000) != 0) {
                throw new IllegalStateException(String.format("Register %s had an index too large", arg.token));
            }
            return (byte) (0b10000000 & regIndex);
        }
    }

    public static byte[] writeLiteral(Instruction.Arg arg) throws BytecodeWriteException {
        if (arg.token.type == Token.Type.HEXADECIMAL || arg.token.type == Token.Type.BINARY) {
            // Don't worry about negatives here
            int radix = arg.token.type == Token.Type.HEXADECIMAL ? 16 : 2;
            int charsPerByte = arg.token.type == Token.Type.HEXADECIMAL ? 2 : 8;

            String strValue = arg.token.meat();
            int leadingZeroes = 0;
            for (char c : strValue.toCharArray()) {
                if (c == '0') {
                    leadingZeroes++;
                } else {
                    break;
                }
            }
            // Shell out to BigInt
            BigInteger imLazy = new BigInteger(strValue, radix);
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
                throw new BytecodeWriteException.LiteralTooLong(arg.token);
            }
            byte[] out = new byte[totalValueSize + 1]; // add 1 for the size itself
            // Put our size in
            int futzedSize = totalValueSize & 0b00111111 | 0b01000000;
            out[0] = (byte) futzedSize;
            // Put in leading zeroes
            for (int c = 0; c < leadingZeroBytes; c++) {
                out[c + 1] = 0;
            }
            // And put in the value
            for (int c = leadingZeroBytes; c < totalValueSize; c++) {
                int bigintIdx = c - leadingZeroBytes;
                out[c + 1] = bigintBytes[bigintIdx];
            }
            return out;
        } else {
            // decimal digit
            // just shell out to BigInteger, it'll be fine
            BigInteger imLazy = new BigInteger(arg.token.value);
            byte[] value = imLazy.toByteArray();
            int size = value.length;
            if (size > 63) {
                throw new BytecodeWriteException.LiteralTooLong(arg.token);
            }
            byte[] out = new byte[size + 1]; // have space for our length
            int futzedSize = size & 0b00111111 | 0b01000000;
            out[0] = (byte) futzedSize;
            for (int c = 0; c < size; c++) {
                out[c+1] = value[c];
            }
            return out;
        }
    }
}
