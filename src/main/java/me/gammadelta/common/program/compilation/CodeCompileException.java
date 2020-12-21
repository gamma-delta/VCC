package me.gammadelta.common.program.compilation;

public class CodeCompileException extends Exception {
	private static final long serialVersionUID = 8604300245570679461L;

	public CodeCompileException(String message, Object... args) {
		super(String.format(message, args));
	}
}
