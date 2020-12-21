package me.gammadelta.common.program.compilation;

import java.util.List;

// The hope is eventually to not have any string messages anywhere here;
// this way we can present the problem to the player in a localization-dependent way.
// We just store the type of problem and the specifics.
public class CodeCompileException extends Exception {
	private static final long serialVersionUID = 8604300245570679461L;
	public final String message;

	public CodeCompileException(String message, Object... args) {
		this(String.format(message, args));
	}

	public CodeCompileException(String message) {
		super(message);
		this.message = message;
	}

	/**
	 * An exception that happens while parsing.
	 */
	public abstract static class ParseException extends CodeCompileException {
		public final int row;
		public final int col;

		public ParseException(int row, int col, String message, Object... args) {
			// java, why do you suck so much
			super(String.format("%d:%d : %s", row, col, String.format(message, args)));
			this.row = row;
			this.col = col;
		}

		/**
		 * A bunch of ParseExceptions.
		 */
		public static class Bunch extends CodeCompileException {
			public final List<ParseException> problems;

			public Bunch(List<ParseException> problems) {
				super("The following problems occured while parsing:\n%s", prettyPrintProblems(problems));
				this.problems = problems;
			}

			// java bad
			private static String prettyPrintProblems(List<ParseException> problems) {
				StringBuilder bob = new StringBuilder();
				for (int c = 0; c < problems.size(); c++) {
					bob.append("- ");
					ParseException e = problems.get(c);
					bob.append(e.getClass().getSimpleName());
					bob.append(": ");
					bob.append(problems.get(c).message);
					if (c != problems.size() - 1) {
						bob.append('\n');
					}
				}
				return bob.toString();
			}
		}

		public static class ExpectedOpcode extends ParseException {
			public final Token notAnOpcode;
			public ExpectedOpcode(Token notAnOpcode) {
				super(
						notAnOpcode.row, notAnOpcode.col,
						"Expected an opcode (i.e. NAME) but found %s of type",
						notAnOpcode.value, notAnOpcode.type
				);
				this.notAnOpcode = notAnOpcode;
			}
		}

		public static class UnknownOpcode extends ParseException {
			public UnknownOpcode(Token unknown) {
				super(unknown.row, unknown.col, "Unknown opcode %s", unknown.value);
			}
		}

		public static class BadArity extends ParseException {
			public final int arityWanted;
			public final int arityGot;
			public final Opcode opcode;

			public BadArity(int row, int col, Opcode op, int arityWanted, int arityGot) {
				super(row, col, "Opcode %s wants %d arguments but got %d", op, arityWanted, arityGot);
				this.arityWanted = arityWanted;
				this.arityGot = arityGot;
				this.opcode = op;
			}
		}

		public static class BadArgMatchup extends ParseException {
			public final Opcode opcode;
			public final Instruction.Arg.Type argType;
			public final int argIndex;
			public final Token offendingArg;


			public BadArgMatchup(Opcode op, Instruction.Arg.Type argType, int argIndex, Token offendingArg) {
				super(offendingArg.row, offendingArg.col,
						"Opcode %s wants arg type %s for index %d, but argument %s is token type %s which does not match",
						op, argType, argIndex, offendingArg.canonicalize(), offendingArg.type);
				this.opcode = op;
				this.argType = argType;
				this.argIndex = argIndex;
				this.offendingArg = offendingArg;
			}
		}

		public static class ReusedLabel extends ParseException {
			public final int originalPos;
			public final Token usurperLabel;

			public ReusedLabel(Token usurper, int originalPos) {
				super(
						usurper.row, usurper.col,
						"Label %s overwrote the original label before instruction #%d",
						usurper.canonicalize(), originalPos
				);
				this.originalPos = originalPos;
				this.usurperLabel = usurper;
			}
		}

		public static class UnknownLabel extends ParseException {
			public final Token labelUsage;
			public UnknownLabel(Token labelUsage) {
				super(
						labelUsage.row, labelUsage.col,
						"Tried to use label %s but could not find it",
						labelUsage
				);
				this.labelUsage = labelUsage;
			}
		}

		public static class StackvalueWithNothingFollowing extends ParseException {
			public final Token stackvalue;

			public StackvalueWithNothingFollowing(Token stackvalue) {
				super(stackvalue.row, stackvalue.col, "Stackvalue %s had nothing following it (this shouldn't be possible?)", stackvalue.canonicalize());
				this.stackvalue = stackvalue;
			}
		}
	}


}
