package me.gammadelta.common.program.compilation;

public abstract class BytecodeWriteException extends Exception {
    private BytecodeWriteException(String message, Object... args) {
        super(String.format(message, args));
    }

    public static class LiteralTooLong extends BytecodeWriteException {
        public final Token literal;
        public LiteralTooLong(Token literal) {
            super("Literal %s was too large (maximum is 9,223,372,036,854,775,807 or 2^63 - 1)", literal.canonicalize());
            this.literal = literal;
        }
    }

    public static class StackvalueSizeOutOfBounds extends BytecodeWriteException {
        public final Token stackvalueSize;
        public StackvalueSizeOutOfBounds(Token stackvalueSize) {
            super("Stackvalue %s's size was out of bounds (0-256 inclusive allowed)", stackvalueSize.canonicalize());
            this.stackvalueSize = stackvalueSize;
        }
    }
}
