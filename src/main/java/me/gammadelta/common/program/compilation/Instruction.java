package me.gammadelta.common.program.compilation;

import javax.annotation.Nullable;
import java.util.Map;

public class Instruction {
	public final int position;
	public final Token optoken;
	public final Opcode opcode;
	public final Arg[] args;
	private boolean reified = false;

	public Instruction(int position, Token optoken, Opcode opcode, Arg[] args) {
		this.position = position;
		this.optoken = optoken;
		this.opcode = opcode;
		this.args = args;
	}

	/** Write this instruction as bytes */
	public byte[] write() {
		if (!reified) {
			throw new IllegalStateException("Reify an instruction before you write it out!");
		}
		// TODO implement to specifications
		return null;
	}

	// hack: we hack the type of a LABEL arg from a string name to an int offset.
	// this whole block could be better written by having two Arg types,
	// but I'm too damn lazy to do that, so `reified` it is.
	public void reify(Map<String, Integer> offsets) throws CodeCompileException {
		if (reified) {
			return;
		}
		for (int i = 0; i < args.length; i++) {
			Arg a = args[i];
			if (a.type == Arg.Type.LABEL) {
				if (!offsets.containsKey(a.value.meat())) {
					throw new CodeCompileException("Failed to find label %s", a.value);
				}
				args[i] = new Arg(a.value.rewrite(Token.Type.DECIMAL, String.valueOf(offsets.get(a.value.meat()) - position)), a.type);
			}
		}
		reified = true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append(opcode.name());
		for (Arg a : args) {
			sb.append(' ').append(a.value);
		}
		return sb.toString();
	}

	public static class Arg {
		public final Token value;
		// This will only be Some if this argument is a stackvalue
		@Nullable
		public final Token stackvaluePosition;
		public final Type type;

		public Arg(Token value, Type type) throws CodeCompileException {
			this.value = value;
			this.stackvaluePosition = null;
			this.type = type;
		}
		public Arg(Token value, Token stackvaluePosition, Type type) throws CodeCompileException {
			this.value = value;
			this.stackvaluePosition = stackvaluePosition;
			this.type = type;
		}

		/** Canonicalize a string version of this argument, for printing. */
		public String canonicalize() {
			String tokenStr = this.value.canonicalize();
			if (this.stackvaluePosition != null) {
				// we got an SV
				tokenStr += this.stackvaluePosition.canonicalize();
			}
			return String.format("%s %s", this.type.toString(), tokenStr);
		}

		public byte[] write() {
			// TODO implement to specifications
			return null;
		}

		public enum Type {
			IV,
			REGISTER,
			LITERAL,
			EXTERNAL,
			LABEL;

			/**
			 * Check if the token's type is valid for this argument type.
			 */
			public boolean matchesType(Token.Type type) {
				switch (this) {
					case IV: return type.isIV();
					case REGISTER: return type == Token.Type.REGISTER;
					case LITERAL: return type.isLiteral();
					case EXTERNAL: return type.isExternal();
					case LABEL: return type == Token.Type.NAME;
				}
				return false;
			}
		};
	};
}
