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
		List<CodeCompileException.ParseException> problems = new ArrayList<>();

		// Maps tokens representing labels to the index of the instruction
		Map<String, Integer> labelOffsets = new HashMap<>();
		List<Instruction> instructions = new ArrayList<>();
		consumeTokens: for (ListIterator<Token> it = tokens.listIterator(); it.hasNext(); ) {
			Token next;

			while ((next = it.next()).type == Token.Type.LABEL_DECLARATION) {
				if (labelOffsets.containsKey(next.meat())) {
					problems.add(new CodeCompileException.ParseException.ReusedLabel(next, labelOffsets.get(next.meat())));
				}
				labelOffsets.put(next.meat(), instructions.size());
			}
			if (next.type == Token.Type.NEWLINE) {
				// cope with single lines with label(s)
				continue;
			}
			if (next.type != Token.Type.NAME) {
				// hey we wanted an opcode!
				problems.add(new CodeCompileException.ParseException.ExpectedOpcode(next));
				break;
			}
			Opcode op = Opcode.getOpcodeFromString(next.meat());
			if (op == null) {
				problems.add(
						new CodeCompileException.ParseException.UnknownOpcode(next)
				);
				break;
			}

			Token argument;
			Instruction.Arg[] args = new Instruction.Arg[op.operands.length];
			int i;
			getArgs: for (i = 0; (argument = it.next()).type != Token.Type.NEWLINE; i++) {
				if (i < args.length) {
					Instruction.Arg.Type type = op.operands[i];
					if (!type.matchesType(argument.type)) {
						problems.add(new CodeCompileException.ParseException.BadArgMatchup(
								op, type, i, argument
						));
						// Advance if it's a stackvalue to prevent getting out of sync
						if (argument.type == Token.Type.STACKVALUE) {
							it.next();
						}
						// We gotta put *something* here lest NPEs so just put the thing in
						args[i] = new Instruction.Arg(argument, type);
						continue getArgs;
					}
					// Check for stackvalues
					if (argument.type == Token.Type.STACKVALUE) {
						// ok we need to take the next value and use it as the position
						if (!it.hasNext()) {
							problems.add(new CodeCompileException.ParseException.StackvalueWithNothingFollowing(
									argument
							));
							break consumeTokens;
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
				problems.add(
						new CodeCompileException.ParseException.BadArity(
								argument.row, argument.col, op, args.length, i
						)
				);
				// Keep consuming tokens to the end of line if it's too few
				if (i < args.length) {
					while (it.next().type != Token.Type.NEWLINE) {}
				}
				continue;
			}
			instructions.add(new Instruction(instructions.size(), next, op, args));
		}

		for (Instruction insn : instructions) {
			try {
				insn.reify(labelOffsets);
			} catch (CodeCompileException.ParseException e) {
				problems.add(e);
			}
		}

		if (problems.size() != 0) {
			// oops
			throw new CodeCompileException.ParseException.Bunch(problems);
		}

		return instructions;
	}
}
