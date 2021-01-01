package me.gammadelta.common.program.compilation;

import it.unimi.dsi.fastutil.bytes.ByteList;

import java.util.List;

/**
 * Helpful wrapper class to lex and parse a VCC-ASM program.
 */
public class ASMCompiler {
    public static List<Instruction> lexAndParse(String input) throws CodeCompileException {
        List<Token> tokens = new CodeLexer(input).slurp();
        // apparently this mutates the original tokens
        new CodePreprocessor().preprocessTokens(tokens);
        // and parse them
        List<Instruction> instrs = new CodeParser().parseInstructions(tokens);
        return instrs;
    }

    public static ByteList lexParseAndCompile(String input) throws CodeCompileException, CodeCompileException.BytecodeWriteException {
        List<Instruction> instructions = lexAndParse(input);
        return new BytecodeWriter(instructions).writeProgramToBytecode();
    }

    /** Pretty-print a list of instructions */
    public static String prettyPrintInstructions(List<Instruction> instrs) {
        StringBuilder bob = new StringBuilder();
        for (int j = 0; j < instrs.size(); j++) {
            Instruction i = instrs.get(j);
            bob.append(i.position);
            bob.append(": ");
            bob.append(i.opcode.toString());
            bob.append(' ');
            for (int c = 0; c < i.args.length; c++) {
                Instruction.Arg arg = i.args[c];
                bob.append(String.format("[%s]", arg.canonicalize()));
                if (c != i.args.length - 1) {
                    bob.append(' ');
                }
            }
            if (j != instrs.size() - 1) {
                bob.append('\n');
            }
        }
        return bob.toString();
    }
}
