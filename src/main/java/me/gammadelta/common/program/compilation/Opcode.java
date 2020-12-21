package me.gammadelta.common.program.compilation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import javax.annotation.Nullable;

public enum Opcode {
    NOP(),

    // === Moving stuff
    MOV(Instruction.Arg.Type.IV, Instruction.Arg.Type.REGISTER),
    MOVK(Instruction.Arg.Type.IV, Instruction.Arg.Type.REGISTER),
    SWP(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.REGISTER),
    READ(Instruction.Arg.Type.IV, Instruction.Arg.Type.EXTERNAL, Instruction.Arg.Type.REGISTER),
    WRITE(Instruction.Arg.Type.IV, Instruction.Arg.Type.IV, Instruction.Arg.Type.EXTERNAL),
    COPY(Instruction.Arg.Type.IV, Instruction.Arg.Type.EXTERNAL, Instruction.Arg.Type.EXTERNAL),
    PUSH(Instruction.Arg.Type.IV),
    POP(Instruction.Arg.Type.REGISTER),

    // === Arithmetic
    // Normal
    ADD(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    SUB(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    // Bit twiddling
    INC(Instruction.Arg.Type.REGISTER),
    DEC(Instruction.Arg.Type.REGISTER),
    NEG(Instruction.Arg.Type.REGISTER),
    INV(Instruction.Arg.Type.REGISTER),
    AND(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    OR(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    XOR(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    NOT(Instruction.Arg.Type.REGISTER),
    SHL(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    SHR(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),
    SHRU(Instruction.Arg.Type.REGISTER, Instruction.Arg.Type.IV),

    // Jumps
    JMP(Instruction.Arg.Type.IV),
    JZ(Instruction.Arg.Type.IV, Instruction.Arg.Type.IV),
    JNZ(Instruction.Arg.Type.IV, Instruction.Arg.Type.IV),
    JGZ(Instruction.Arg.Type.IV, Instruction.Arg.Type.IV),
    JLZ(Instruction.Arg.Type.IV, Instruction.Arg.Type.IV),
    CALL(Instruction.Arg.Type.IV),
    RET(),

    // Misc
    QUERY(Instruction.Arg.Type.IV, Instruction.Arg.Type.REGISTER),
    EMERGENCY(),

    // and an invalid one
    ILLEGAL();

    public final Instruction.Arg.Type[] operands;

    private Opcode(Instruction.Arg.Type... operands) {
        this.operands = operands;
    }

    /**
     * Get an opcode from a string, or null if it was invalid/ILLEGAL
     */
    @Nullable
    public static Opcode getOpcodeFromString(String s) {
        String caps = s.toUpperCase();
        if (caps.equals("ILLEGAL")) {
            return null;
        }
        return Opcode.valueOf(caps);
    }

    /**
     * This is a Short map because java bad
     */
    public static final BiMap<Opcode, Short> OPCODES_TO_BYTECODE = HashBiMap.create();
    static {
        OPCODES_TO_BYTECODE.forcePut(Opcode.NOP, (short) 0x00);

        OPCODES_TO_BYTECODE.forcePut(Opcode.MOV, (short) 0x10);
        OPCODES_TO_BYTECODE.forcePut(Opcode.MOVK, (short) 0x11);
        OPCODES_TO_BYTECODE.forcePut(Opcode.SWP, (short) 0x12);
        OPCODES_TO_BYTECODE.forcePut(Opcode.READ, (short) 0x18);
        OPCODES_TO_BYTECODE.forcePut(Opcode.WRITE, (short) 0x19);
        OPCODES_TO_BYTECODE.forcePut(Opcode.COPY, (short) 0x1A);
        OPCODES_TO_BYTECODE.forcePut(Opcode.PUSH, (short) 0x1B);
        OPCODES_TO_BYTECODE.forcePut(Opcode.POP, (short) 0x1C);

        OPCODES_TO_BYTECODE.forcePut(Opcode.ADD, (short) 0x30);
        OPCODES_TO_BYTECODE.forcePut(Opcode.SUB, (short) 0x31);
        OPCODES_TO_BYTECODE.forcePut(Opcode.INC, (short) 0x40);
        OPCODES_TO_BYTECODE.forcePut(Opcode.DEC, (short) 0x41);
        OPCODES_TO_BYTECODE.forcePut(Opcode.NEG, (short) 0x42);
        OPCODES_TO_BYTECODE.forcePut(Opcode.INV, (short) 0x43);
        OPCODES_TO_BYTECODE.forcePut(Opcode.AND, (short) 0x44);
        OPCODES_TO_BYTECODE.forcePut(Opcode.OR, (short) 0x45);
        OPCODES_TO_BYTECODE.forcePut(Opcode.XOR, (short) 0x46);
        OPCODES_TO_BYTECODE.forcePut(Opcode.NOT, (short) 0x47);
        OPCODES_TO_BYTECODE.forcePut(Opcode.SHL, (short) 0x48);
        OPCODES_TO_BYTECODE.forcePut(Opcode.SHR, (short) 0x49);
        OPCODES_TO_BYTECODE.forcePut(Opcode.SHRU, (short) 0x4A);

        OPCODES_TO_BYTECODE.forcePut(Opcode.JMP, (short) 0x50);
        OPCODES_TO_BYTECODE.forcePut(Opcode.JZ, (short) 0x51);
        OPCODES_TO_BYTECODE.forcePut(Opcode.JNZ, (short) 0x52);
        OPCODES_TO_BYTECODE.forcePut(Opcode.JGZ, (short) 0x53);
        OPCODES_TO_BYTECODE.forcePut(Opcode.JLZ, (short) 0x54);
        OPCODES_TO_BYTECODE.forcePut(Opcode.CALL, (short) 0x58);
        OPCODES_TO_BYTECODE.forcePut(Opcode.RET, (short) 0x59);

        OPCODES_TO_BYTECODE.forcePut(Opcode.QUERY, (short) 0xF0);
        OPCODES_TO_BYTECODE.forcePut(Opcode.EMERGENCY, (short) 0xFF);
    }
}
