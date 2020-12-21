package me.gammadelta.common.program.compilation;

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
}
