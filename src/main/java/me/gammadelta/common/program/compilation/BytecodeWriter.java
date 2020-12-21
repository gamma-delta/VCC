package me.gammadelta.common.program.compilation;

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

    private byte[] writeLiteral(Instruction.Arg arg) throws BytecodeWriteException {
        if (arg.token.type == Token.Type.HEXADECIMAL) {
            // Save these from least -> most significant.
            ArrayList<Byte> value = new ArrayList<>();
            byte wip = 0;
            int seenNibbles = 0;

            char[] toParse = arg.token.value.substring(2).toCharArray();
            for (int idx = toParse.length - 1; idx >= 0; idx--) {
                char c = toParse[idx];
                if (c == '_') {
                    continue;
                }
                byte thisNibble = Byte.parseByte(String.valueOf(c), 16);
                seenNibbles++;
                if (seenNibbles % 2 == 1) {
                    // we're halfway through a nibble
                    wip = thisNibble;
                } else {
                    // we are in the middle of a nibble
                    // reunite with its fellow nibble.
                    wip <<= 4;
                    wip |= thisNibble;
                    value.add(wip);
                    wip = 0;
                }
            }
            // Get a byte for the size
            // intellij why do you think this is 0?
            int size = value.size();
            if (seenNibbles % 2 == 1) {
                // we're halfway thru a nibble
                size++;
            }
            if (size > 63) {
                // too many bytes would need to be written ;-;
                throw new BytecodeWriteException.LiteralTooLong(arg.token);
            }

            ArrayList<Byte> out = new ArrayList<>(size);
            // Put our size in
            int futzedSize = size & 0b00111111 | 0b01000000;
            out.add((byte) futzedSize);
            // If we have an odd number of nibbles, put it on the top.
            // 0xF1122 -> [22 11] + F -> [0F 11 22]
            if (seenNibbles % 2 == 1) {
                out.add(wip);
            }
            // and pop bytes off
            for (int c = value.size() - 1; c >= 0; c--) {
                out.add(value.get(c));
            }

        }
    }
}
