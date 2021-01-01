package me.gammadelta.common.program.compilation;

import java.util.LinkedList;

/**
 * Lex an input string into tokens.
 * This implementation places a newline after every logical "statement";
 * multiple newlines are elided.
 */
public class CodeLexer {
	public static final String WHITESPACE = " \t\r,";
	public static final String TERMINATORS = "\n.";

	private final char[] input;
	private int offset = 0, row = 1, col = 0;
	private boolean lastNewline = true;
	// NOTE:
	// Stackvalues are implemented as TWO TOKENS
	// In `@4'R0`, this will push Stackvalue(@4') and Register(R0).
	// We do this by, when we find a stackvalue, we save the remainder (`R0`)
	// and return the stackvalue.
	// Then next time we check out the remainder to see if it's an OK token.
	private String stackvalueRemainder = null;

	public CodeLexer(String input) {
		this.input = input.toCharArray();
	}

	/** Test if the rest of the string is nonempty and [0-9_]. */
	private static boolean isDec(String str, int offset) {
		if (str.length() <= offset) {
			return false;
		}
		for (int i = offset; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (i == offset) {
				if (ch == '_') {
					// no underscores first
					return false;
				}
				if (ch == '-') {
					// negative signs in the front are OK
					continue;
				}
			}
			if ((ch < '0' || ch > '9') && ch != '_') {
				return false;
			}
		}
		return true;
	}
	/** Test if the rest of the string is nonempty and [0-9a-fA-F_]. */
	private static boolean isHex(String str, int offset) {
		if (str.length() <= offset) {
			return false;
		}
		for (int i = offset; i < str.length(); i++) {
			char ch = Character.toUpperCase(str.charAt(i));
			if (i == offset && ch == '_') {
				// no underscores first
				return false;
			}
			if ((ch < '0' || ch > '9') && (ch < 'A' || ch > 'F') && ch != '_') {
				return false;
			}
		}
		return true;
	}
	/** Test if the rest of the string is nonempty and [01_] */
	private static boolean isBin(String str, int offset) {
		if (str.length() <= offset) {
			return false;
		}
		for (int i = offset; i < str.length(); i++) {
			char ch = Character.toUpperCase(str.charAt(i));
			if (i == offset && ch == '_') {
				// no underscores first
				return false;
			}
			if (ch != '0' && ch != '1' && ch != '_') {
				return false;
			}
		}
		return true;
	}

	/** Scan past whitespace. */
	private void skipWhitespace() {
		while (offset < input.length && WHITESPACE.indexOf(input[offset]) != -1) {
			offset++;
			col++;
		}
	}

	/** Scan non-whitespace characters and return them as a string. */
	private String nextNonWhitespace() {
		int startOffset = offset;
		while (offset < input.length && TERMINATORS.indexOf(input[offset]) == -1 && WHITESPACE.indexOf(input[offset]) == -1) {
			offset++;
			col++;
		}
		return new String(input, startOffset, offset - startOffset);
	}

	/** Determine the type of the body of a token. */
	private Token.Type findType(String body) {
		if (body.charAt(0) == '#') {
			return Token.Type.PREPROCESSOR;
		}
		if (
				(Character.toUpperCase(body.charAt(0)) == 'R' && isDec(body, 1))
				|| body.equalsIgnoreCase("NIL")
				|| body.equalsIgnoreCase("IP")
				|| body.equalsIgnoreCase("SP")
				|| body.equalsIgnoreCase("FLAGS")
		) {
			return Token.Type.REGISTER;
		}
		if (Character.toUpperCase(body.charAt(0)) == 'D' && isDec	(body, 1)) {
			return Token.Type.DATAFACE;
		}
		if (findStackvalue(body) != null) {
			return Token.Type.STACKVALUE;
		}
		if (isDec(body, 0)) {
			return Token.Type.DECIMAL;
		}
		if (body.toLowerCase().startsWith("0x") && isHex(body, 2)) {
			return Token.Type.HEXADECIMAL;
		}
		if (body.toLowerCase().startsWith("0b") && isBin(body, 2)) {
			return Token.Type.BINARY;
		}
		if (body.endsWith(":")) {
			return Token.Type.LABEL_DECLARATION;
		}
		return Token.Type.NAME;
	}
	/** Get the index at which the stackvalue can be split into the size and location, or null if parsing fails. */
	private static Integer findStackvalue(String input) {
		if (input.charAt(0) != '@') {
			return null;
		}
		// Go from the @ to the '
		int quotePos = input.indexOf('\'');
		if (quotePos == -1) {
			return null;
		}
		return quotePos + 1;
	}

	/** Parse the next token from input, or null if none were found. */
	public Token nextToken() throws CodeCompileException {
		String tokenBody;
		boolean previousWasStackvalue = false;
		if (this.stackvalueRemainder != null) {
			// the previous token was `@4'R0` or something
			// and this has `R0`.
			tokenBody = stackvalueRemainder;
			this.stackvalueRemainder = null;
			previousWasStackvalue = true;
		} else {
			skipWhitespace();
			if (offset < input.length && input[offset] == ';') {
				// this is a comment
				// we use \n here and not TERMINATORS because otherwise
				// periods would end comments.
				while (offset < input.length && input[offset] != '\n') offset++;
			}
			if (offset < input.length && TERMINATORS.indexOf(input[offset]) != -1) {
				// this is a newline
				offset++;
				row++;
				col = 0;
				if (lastNewline) {
					return nextToken();
				}
				lastNewline = true;
				if (offset >= input.length) {
					return new Token(Token.Type.NEWLINE, "<EOF>", row, col);
				}
				return new Token(Token.Type.NEWLINE, input[offset] == '.' ? "<period>" : "<newline>", row, col);
			}

			tokenBody = nextNonWhitespace();
		}

		int startCol = col;
		if (tokenBody.length() == 0) {
			// out of items
			return null;
		}
		lastNewline = false;
		Token.Type type = findType(tokenBody);
		if (type == Token.Type.STACKVALUE) {
			// ok we have to do some special processing
			if (previousWasStackvalue) {
				throw new CodeCompileException.LexException.TwoStackvalues(row, col, this.stackvalueRemainder, tokenBody);
			}
			// this will not be null; we just checked if this is a stackvalue.
			// shut up intellij
			// more like, dumbj
			int svSplit = findStackvalue(tokenBody);
			String svSize = tokenBody.substring(0, svSplit);
			String svPos = tokenBody.substring(svSplit);
			// save the remainder
			this.stackvalueRemainder = svPos;
			return new Token(Token.Type.STACKVALUE, svSize, row, startCol);
		}
		return new Token(type, tokenBody, row, startCol);
	}

	/** Parse the rest of the tokens from the input. */
	public LinkedList<Token> slurp() throws CodeCompileException {
		LinkedList<Token> tokens = new LinkedList<>();
		Token next;
		while ((next = nextToken()) != null) {
			tokens.add(next);
		}
		if (!lastNewline) {
			tokens.add(new Token(Token.Type.NEWLINE, "<EOF>", row, col));
		}
		return tokens;
	}
}
