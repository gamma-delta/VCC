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

    public final int page, row, col;


    private CodeCompileException(int page, int row, int col) {
        this.page = page;
        this.row = row;
        this.col = col;
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
     */
    protected CompoundNBT boilerplateRowCol() {
        CompoundNBT out = new CompoundNBT();
        out.putInt(ERROR_PAGE_KEY, this.page);
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
        public LexException(int page, int row, int col) {
            super(page, row, col);
        }

        public static class TwoStackvalues extends LexException {
            public final String sv1;
            public final String sv2;

            public TwoStackvalues(int page, int row, int col, String sv1, String sv2) {
                super(page, row, col);
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
        public PreprocessException(int page, int row, int col) {
            super(page, row, col);
        }

        public static class DirectiveInMiddleOfLine extends PreprocessException {
            public final Token problem;

            public DirectiveInMiddleOfLine(Token problem) {
                super(problem.page, problem.row, problem.col);
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
                super(problem.page, problem.row, problem.col);
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
                super(problem.page, problem.row, problem.col);
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
                super(problem.page, problem.row, problem.col);
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
                super(problem.page, problem.row, problem.col);
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
                super(problem.page, problem.row, problem.col);
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
        public ParseException(int page, int row, int col) {
            // java, why do you suck so much
            super(page, row, col);
        }

        /**
         * A bunch of ParseExceptions.
         */
        public static class Bunch extends CodeCompileException {
            public final List<ParseException> problems;

            public Bunch(List<ParseException> problems) {
                super(-1, -1, -1);
                this.problems = problems;
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
                super(notAnOpcode.page, notAnOpcode.row, notAnOpcode.col);
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
                super(unknown.page, unknown.row, unknown.col);
                this.unknown = unknown;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(this.unknown.value);
            }
        }

        public static class BadArity extends ParseException {
            public final int arityWanted;
            public final int arityGot;
            public final Opcode opcode;

            public BadArity(Token token, Opcode op, int arityWanted, int arityGot) {
                super(token.page, token.row, token.col);
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
                super(offendingArg.page, offendingArg.row, offendingArg.col);
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
                super(usurper.page, usurper.row, usurper.col);
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
                super(labelUsage.page, labelUsage.row, labelUsage.col);
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
                super(stackvalue.page, stackvalue.row, stackvalue.col);
                this.stackvalue = stackvalue;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(stackvalue);
            }
        }
    }


    public abstract static class BytecodeWriteException extends CodeCompileException {
        private BytecodeWriteException(int page, int row, int col) {
            super(page, row, col);
        }

        public static class LiteralTooLong extends BytecodeWriteException {
            public final Token literal;

            public LiteralTooLong(Token literal) {
                super(literal.page, literal.row, literal.col);
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
                super(stackvalueSize.page, stackvalueSize.row, stackvalueSize.col);
                this.stackvalueSize = stackvalueSize;
            }

            @Override
            protected CompoundNBT serialize() {
                return boilerplate(stackvalueSize.canonicalize());
            }
        }
    }
}
