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

    public static class LiteralMismatchedSize extends BytecodeWriteException {
        public final Token literal;
        public LiteralMismatchedSize(Token literal) {
            super("Somehow had a different size than expected when writing the literal %s (this should never happen)", literal.canonicalize());
            this.literal = literal;
        }
    }
}
