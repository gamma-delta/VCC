package me.gammadelta.common.program.compilation;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * Parse a list of tokens into instructions.
 * This could probably not be a god class. Too bad!
 *
 * The above comment was written by Alwinfy.
 * What's a god class?
 */
public class CodeParser {
	public List<Instruction> parseInstructions(List<Token> tokens) throws CodeCompileException {
		Map<String, Integer> offsets = new HashMap<>();
		List<Instruction> instructions = new ArrayList<>();
		for (ListIterator<Token> it = tokens.listIterator(); it.hasNext(); ) {
			Token next;

			while ((next = it.next()).type == Token.Type.LABEL) {
				String meat = next.meat();
				if (offsets.containsKey(meat)) {
					throw new CodeCompileException("Label already used: %s", next);
				}
				offsets.put(meat, instructions.size());
			}
			if (next.type == Token.Type.NEWLINE) {
				// cope with single lines with label(s)
				continue;
			}
			if (next.type != Token.Type.NAME) {
				throw new CodeCompileException("Expected opcode, got %s %s", next.type, next);
			}
			Opcode op;
			try {
				op = Opcode.valueOf(next.meat().toUpperCase());
			} catch (IllegalArgumentException $) {
				throw new CodeCompileException("Unknown opcode %s", next);
			}

			Token argument;
			Instruction.Arg[] args = new Instruction.Arg[op.operands.length];
			int i;
			for (i = 0; (argument = it.next()).type != Token.Type.NEWLINE; i++) {
				if (i < args.length) {
					Instruction.Arg.Type type = op.operands[i];
					if (!type.matchesType(argument.type)) {
						throw new CodeCompileException("Instruction %s wants %s, found %s", op, type, argument);
					}
					// Check for stackvalues
					if (argument.type == Token.Type.STACKVALUE) {
						// ok we need to take the next value and use it as the position
						if (!it.hasNext()) {
							throw new CodeCompileException("Somehow, this stackvalue %s did not have any tokens after it. This shouldn't be possible?", argument);
						}
						Token svPos = it.next();
						args[i] = new Instruction.Arg(argument, svPos, type);
					} else {
						// nope, business as usual
						args[i] = new Instruction.Arg(argument, type);
					}
				}
			}
			if (i != args.length) {
				throw new CodeCompileException("Mismatched arity (want %d, got %d) to %s", args.length, i, op);
			}
			instructions.add(new Instruction(instructions.size(), next, op, args));
		}
		for (Instruction insn : instructions) {
			insn.reify(offsets);
		}
		return instructions;
	}
}
