package me.gammadelta.common.program.compilation;

import java.util.List;

/**
 * Helpful wrapper class to lex and parse a string into VCC-ASM.
 * All the work is in CodeLexer and CodeParser afaik
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

    /** Pretty-print a list of instructions */
    public static String prettyPrintInstructions(List<Instruction> instrs) {
        StringBuilder bob = new StringBuilder();
        for (Instruction i : instrs) {
            bob.append(i.position); bob.append(": ");
            bob.append(i.opcode.toString()); bob.append(' ');
            for (int c = 0; c < i.args.length; c++) {
                Instruction.Arg arg = i.args[c];
                bob.append(String.format("[%s]", arg.canonicalize()));
                if (c != i.args.length - 1) {
                    bob.append(' ');
                }
            }
            bob.append('\n');
        }
        return bob.toString();
    }
}
