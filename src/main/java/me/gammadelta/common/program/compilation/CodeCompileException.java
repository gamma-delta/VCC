package me.gammadelta.common.program.compilation;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import java.util.List;

import static me.gammadelta.common.item.ItemCoupon.*;

// The hope is eventually to not have any string messages anywhere here;
// this way we can present the problem to the player in a localization-dependent way.
// We just store the type of problem and the specifics.
public abstract class CodeCompileException extends Exception {
    private static final long serialVersionUID = 8604300245570679461L;
    public final String message;

    public final int row;
    public final int col;

    private CodeCompileException(int row, int col, String message, Object... args) {
        this(row, col, String.format(message, args));
    }

    private CodeCompileException(int row, int col, String message) {
        super(message);
        this.row = row;
        this.col = col;
        this.message = message;
    }

    /**
     * Serialize this problem to NBT so the coupon can read it out.
     */
    public ListNBT serializeAll() {
        ListNBT out = new ListNBT();
        out.add(this.serialize());
        return out;
    }

    /**
     * Serialize this problem to a single NBT entry.
     * You should implement this unless your error is a bunch of errors in one
     */
    protected abstract CompoundNBT serialize();

    /**
     * Helper method to put the row and column in a tag.
     * The LINE key will be put in later by the compiler
     * (because these errors don't know about the entire program)
     */
    protected CompoundNBT boilerplateRowCol() {
        CompoundNBT out = new CompoundNBT();
        out.putInt(ERROR_ROW_KEY, this.row);
        out.putInt(ERROR_COL_KEY, this.col);
        return out;
    }

    /**
     * Helper method to put the key and values in.
     */
    protected void addKeyVals(CompoundNBT error, String key, String... vals) {
        error.putString(ERROR_KEY_KEY, key);
        ListNBT valsNBT = new ListNBT();
        for (String val : vals) {
            valsNBT.add(StringNBT.valueOf(val));
        }
        error.put(ERROR_VALUES_KEY, valsNBT);
    }

    protected void addKeyVals(CompoundNBT error, String key, Object... vals) {
        String[] stringedVals = new String[vals.length];
        for (int i = 0; i < vals.length; i++) {
            stringedVals[i] = vals[i].toString();
        }
        // hopefully, java will use the *more* specific `String...` overload
        addKeyVals(error, key, stringedVals);
    }

    /**
     * Put it all together! This method calculates the required key,
     * adds boilerplate row/col, and formats it all together
     */
    protected CompoundNBT boilerplate(Object... vals) {
        String errorType;
        if (this instanceof LexException) {
            errorType = LEX_KEY;
        } else if (this instanceof PreprocessException) {
            errorType = PREPROCESS_KEY;
        } else if (this instanceof ParseException) {
            errorType = PARSE_KEY;
        } else if (this instanceof BytecodeWriteException) {
            errorType = BYTECODE_KEY;
        } else {
            // uh-oh
            CompoundNBT iGoofedUp = boilerplateRowCol();
            addKeyVals(iGoofedUp, "error.unknown", this.getClass().getCanonicalName());
            return iGoofedUp;
        }
        String errorKey = String.format("error.%s.%s", errorType, this.getClass().getSimpleName());

        CompoundNBT out = boilerplateRowCol();
        addKeyVals(out, errorKey, vals);
        return out;
    }


    public abstract static class LexException extends CodeCompileException {
        public LexException(int row, int col, String message, Object... args) {
            super(row, col, String.format("%d:%d : %s", row, col, String.format(message, args)));
        }

        public static class TwoStackvalues extends LexException {
            public final String sv1;
            public final String sv2;

            public TwoStackvalues(int row, int col, String sv1, String sv2) {
                super(row, col, "Tried to put two stackvalues together (%s and %s)", sv1, sv2);
                this.sv1 = sv1;
                this.sv2 = sv2;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(this.sv1, this.sv2);
            }
        }
    }

    public abstract static class PreprocessException extends CodeCompileException {
        public PreprocessException(int row, int col, String message, Object... args) {
            super(row, col, String.format("%d:%d : %s", row, col, String.format(message, args)));
        }

        public static class DirectiveInMiddleOfLine extends PreprocessException {
            public final Token problem;

            public DirectiveInMiddleOfLine(Token problem) {
                super(problem.row, problem.col, "Directive '%s' in middle of line", problem.value);
                this.problem = problem;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(this.problem.value);
            }
        }

        public static class DefinitionExpansionTooThicc extends PreprocessException {
            public final Token problem;

            public DefinitionExpansionTooThicc(Token problem) {
                super(problem.row, problem.col,
                        "Exceeded definition expansion recursion limit of %d when preprocessing '%s'",
                        CodePreprocessor.MAX_EXPAND_DEPTH, problem.value);
                this.problem = problem;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(CodePreprocessor.MAX_EXPAND_DEPTH, problem.value);
            }
        }

        public static class UnknownDirective extends PreprocessException {
            public final Token problem;

            public UnknownDirective(Token problem) {
                super(problem.row, problem.col, "Unknown directive '%s'", problem.meat());
                this.problem = problem;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(problem.value);
            }
        }

        public static final class BadArity extends PreprocessException {
            public final Token problem;
            public final int argcWanted;
            public final int argcGot;

            public BadArity(Token problem, int argcWanted, int argcGot) {
                super(problem.row, problem.col, "Directive '%s' wants %d arguments but got %d", problem.meat(),
                        argcWanted, argcGot);
                this.problem = problem;
                this.argcWanted = argcWanted;
                this.argcGot = argcGot;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(problem.value, argcWanted, argcGot);
            }
        }

        public static final class BadArgType extends PreprocessException {
            public final Token directive;
            public final Token problem;
            public final Token.Type wanted;
            public final int index;

            public BadArgType(Token directive, Token problem, Token.Type wanted, int index) {
                super(problem.row, problem.col, "Directive '%s' wants token type %s for index %d but got %s",
                        directive.meat(), wanted, index, problem.type);
                this.directive = directive;
                this.problem = problem;
                this.wanted = wanted;
                this.index = index;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(directive.value, wanted, index, problem.type);
            }
        }

        public static final class Redefinition extends PreprocessException {
            public final Token problem;

            public Redefinition(Token problem) {
                super(problem.row, problem.col, "Directive redefines '%s'", problem.meat());
                this.problem = problem;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(problem.value);
            }
        }
    }

    /**
     * An exception that happens while parsing.
     */
    public abstract static class ParseException extends CodeCompileException {
        public ParseException(int row, int col, String message, Object... args) {
            // java, why do you suck so much
            super(row, col, String.format("%d:%d : %s", row, col, String.format(message, args)));
        }

        /**
         * A bunch of ParseExceptions.
         */
        public static class Bunch extends CodeCompileException {
            public final List<ParseException> problems;

            public Bunch(List<ParseException> problems) {
                super(-1, -1, "The following problems occured while parsing:\n%s", prettyPrintProblems(problems));
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

            @Override
            public ListNBT serializeAll() {
                ListNBT out = new ListNBT();
                for (ParseException problem : this.problems) {
                    out.add(problem.serialize());
                }
                return out;
            }

            @Override
            protected CompoundNBT serialize() {
                throw new IllegalStateException("You should never be individually serializing this");
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

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(notAnOpcode.value, notAnOpcode.type);
            }
        }

        public static class UnknownOpcode extends ParseException {
            public final Token unknown;

            public UnknownOpcode(Token unknown) {
                super(unknown.row, unknown.col, "Unknown opcode %s", unknown.value);
                this.unknown = unknown;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(this.unknown);
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

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(opcode, arityWanted, arityGot);
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

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(opcode, argType, argIndex, offendingArg.canonicalize(), offendingArg.type);
            }
        }

        // TODO: can we somehow get the *token* position of the original label?
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

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(usurperLabel.canonicalize(), originalPos);
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

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(labelUsage);
            }
        }

        public static class StackvalueWithNothingFollowing extends ParseException {
            public final Token stackvalue;

            public StackvalueWithNothingFollowing(Token stackvalue) {
                super(stackvalue.row, stackvalue.col,
                        "Stackvalue %s had nothing following it (this shouldn't be possible?)",
                        stackvalue.canonicalize());
                this.stackvalue = stackvalue;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(stackvalue);
            }
        }
    }


    public abstract static class BytecodeWriteException extends CodeCompileException {
        private BytecodeWriteException(int row, int col, String message, Object... args) {
            super(row, col, String.format(message, args));
        }

        public static class LiteralTooLong extends BytecodeWriteException {
            public final Token literal;

            public LiteralTooLong(Token literal) {
                super(literal.row, literal.col, "Literal %s was too large (maximum is 9,223,372,036,854,775,807 or 2^63 - 1)",
                        literal.canonicalize());
                this.literal = literal;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(literal.canonicalize());
            }
        }

        public static class StackvalueSizeOutOfBounds extends BytecodeWriteException {
            public final Token stackvalueSize;

            public StackvalueSizeOutOfBounds(Token stackvalueSize) {
                super(stackvalueSize.row, stackvalueSize.col, "Stackvalue %s's size was out of bounds (0-256 inclusive allowed)",
                        stackvalueSize.canonicalize());
                this.stackvalueSize = stackvalueSize;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(stackvalueSize.canonicalize());
            }
        }
    }
}
