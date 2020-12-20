package me.gammadelta.common.program;

import java.util.HashMap;
import java.util.Optional;

/**
 * A list of all the opcodes!
 */
public enum Opcode {
    NOP,

    // === Moving stuff
    MOV,
    MOVK,
    SWP,
    READ,
    WRITE,
    COPY,
    PUSH,
    POP,

    // === Arithmetic
    // Normal
    ADD,
    SUB,
    // Bit twiddling
    INC,
    DEC,
    NEG,
    NOT,
    SHL,
    SHR,
    SHRU,

    // Jumps
    JMP,
    JZ,
    JNZ,
    JGZ,
    JLZ,
    CALL,
    RET,

    // Misc
    QUERY,
    EMERGENCY,

    // and an invalid one
    ILLEGAL;

    private static HashMap<String, Opcode> NAMES_TO_OPCODES;
    static {
        for (Opcode op : Opcode.values()) {
            NAMES_TO_OPCODES.put(op.toString(), op);
        }
    }

    /** Try and get an opcode from the string, or Null otherwise */
    public static Opcode matchStr(String i) {
        String upper = i.toUpperCase();
        if (upper == "ILLEGAL") {
            // nope
            return null;
        }
        return NAMES_TO_OPCODES.get(i.toUpperCase());
    }

    /**
     * Contains what types of arguments opcodes can consume.
     * Note that this does *N O T* actually hold any data in any way.
     *
     * For example, Register doesn't store which register it is.
     */
    public enum ArgumentType {
        REGISTER,
        IMMEDIATE,
        EXTERNAL;
        // remember jump locations == immediate values
    }


    /** Return the arguments this opcode wants. */
    public ArgumentType[] argTypes() {
        switch (this) {
            case NOP:
                return new ArgumentType[]{};

            case MOV:
            case MOVK:
                return new ArgumentType[]{ArgumentType.IMMEDIATE, ArgumentType.REGISTER};
            case SWP:
                return new ArgumentType[]{ArgumentType.REGISTER, ArgumentType.REGISTER};
            case READ:
                return new ArgumentType[]{ArgumentType.IMMEDIATE, ArgumentType.EXTERNAL, ArgumentType.REGISTER};
            case WRITE:
                return new ArgumentType[]{ArgumentType.IMMEDIATE, ArgumentType.IMMEDIATE, ArgumentType.EXTERNAL};
            case COPY:
                return new ArgumentType[]{ArgumentType.IMMEDIATE, ArgumentType.EXTERNAL, ArgumentType.EXTERNAL};
            case PUSH:
                return new ArgumentType[]{ArgumentType.IMMEDIATE};
            case POP:
                return new ArgumentType[]{ArgumentType.REGISTER};

            case ADD:
            case SUB:
                return new ArgumentType[]{ArgumentType.REGISTER, ArgumentType.IMMEDIATE};

            case INC:
            case DEC:
            case NEG:
            case NOT:
                return new ArgumentType[]{ArgumentType.REGISTER};
            case SHL:
            case SHR:
            case SHRU:
                return new ArgumentType[]{ArgumentType.REGISTER, ArgumentType.IMMEDIATE};

            case JMP:
                return new ArgumentType[]{ArgumentType.IMMEDIATE};
            case JZ:
            case JNZ:
            case JGZ:
            case JLZ:
                return new ArgumentType[]{ArgumentType.IMMEDIATE, ArgumentType.IMMEDIATE};
            case CALL:
                return new ArgumentType[]{ArgumentType.IMMEDIATE};
            case RET:
                return new ArgumentType[]{};

            case QUERY:
                return new ArgumentType[]{ArgumentType.IMMEDIATE, ArgumentType.REGISTER};
            case EMERGENCY:
            case ILLEGAL:
                return new ArgumentType[]{};
        }

        throw new IllegalStateException("unreachable");
    }
}
