package me.gammadelta.common.program.compilation;

/**
 * A single token of the assembly language.
 * Exposes convenience functions for your convenience.
 */
public class Token {
	public final Type type;
	private final String value;
	public final Token alias;
	public final int row, col;

	public Token(Type type, String value, int row, int col) {
		this(type, value, null, row, col);
	}
	private Token(Type type, String value, Token alias, int row, int col) {
		this.type = type;
		this.value = value;
		this.alias = alias;
		this.row = row;
		this.col = col;
	}

	/** Remove the fluff of the token and return the meat. */
	public String meat() {
		switch (type) {
			case PREPROCESSOR:
			case REGISTER:
			case DATAFACE:
				return value.substring(1);
			case DECIMAL:
			case NAME:
			case NEWLINE:
				return value;
			case HEXADECIMAL:
				return value.substring(2).toUpperCase();
			case LABEL:
				return value.substring(0, value.length() - 1);
			case STACKVALUE:
				return value.substring(1, value.length() - 1);
			default:
				return null;
		}
	}

	/** Get a canonical version of the token for printing. */
	public String canonicalize() {
		switch (type) {
			case PREPROCESSOR:
			case LABEL:
			case DECIMAL:
			case NAME:
			case NEWLINE:
				return this.value;
			case REGISTER:
				return "R" + this.meat();
			case DATAFACE:
				return "D" + this.meat();

			case HEXADECIMAL:
				return "0x" + this.meat();

			case STACKVALUE:
				return "@" + this.meat() + "'";
			default:
				return "<unknown (this shouldn't appear)>";
		}
	}

	/** Rewrite this token as an alias of the given one. */
	public Token rewrite(Token clone) {
		return new Token(clone.type, clone.value, this, clone.row, clone.col);
	}
	/** Rewrite this token with a different type and value. */
	public Token rewrite(Type type, String value) {
		return new Token(type, value, this, row, col);
	}
	/** Get the original token before any calls to rewrite(). */
	public Token original() {
		return alias == null ? this : alias.original();
	}

	@Override
	public String toString() {
		String me = String.format("%s at %s:%s", value, row, col);
		return alias == null ? me : String.format("%s (aka %s)", original(), me);
	}

	public enum Type {
		PREPROCESSOR,

		REGISTER,
		DATAFACE,
		DECIMAL,
		HEXADECIMAL,
		STACKVALUE,

		LABEL,
		NAME,
		NEWLINE;

		/** Is this token a literal value? */
		public boolean isLiteral() {
			return this == Type.DECIMAL || this == Type.HEXADECIMAL;
		}
		/** Is this token valid as an IV? */
		public boolean isIV() {
			return isLiteral() || this == Type.REGISTER || this == Type.STACKVALUE;
		}
		/** Is this token valid as an external location? */
		public boolean isExternal() {
			return this.isIV() || this == Type.DATAFACE;
		}
	};
}
