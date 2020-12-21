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

	// hack: we hack the type of a LABEL arg from a string name to an int offset.
	// this whole block could be better written by having two Arg types,
	// but I'm too damn lazy to do that, so `reified` it is.
	public void reify(Map<String, Integer> offsets) throws CodeCompileException.ParseException {
		if (reified) {
			return;
		}
		for (int i = 0; i < args.length; i++) {
			Arg a = args[i];
			if (a.type == Arg.Type.LABEL) {
				if (!offsets.containsKey(a.token.meat())) {
					throw new CodeCompileException.ParseException.UnknownLabel(a.token);
				}
				args[i] = new Arg(a.token.rewrite(Token.Type.DECIMAL, String.valueOf(offsets.get(a.token.meat()) - position)), a.type);
			}
		}
		reified = true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append(opcode.name());
		for (Arg a : args) {
			sb.append(' ').append(a.token);
		}
		return sb.toString();
	}

	public static class Arg {
		public final Token token;
		// This will only be Some if this argument is a stackvalue
		@Nullable
		public final Token stackvaluePosition;
		public final Type type;

		public Arg(Token token, Type type) {
			this.token = token;
			this.stackvaluePosition = null;
			this.type = type;
		}
		public Arg(Token token, Token stackvaluePosition, Type type) {
			this.token = token;
			this.stackvaluePosition = stackvaluePosition;
			this.type = type;
		}

		/** Canonicalize a string version of this argument, for printing. */
		public String canonicalize() {
			String tokenStr = this.token.canonicalize();
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
			EXTERNAL,
			LABEL;

			/**
			 * Check if the token's type is valid for this argument type.
			 */
			public boolean matchesType(Token.Type type) {
				switch (this) {
					case IV: return type.isIV();
					case REGISTER: return type == Token.Type.REGISTER;
					case EXTERNAL: return type.isExternal();
					case LABEL: return type == Token.Type.NAME;
				}
				return false;
			}
		};
	};
}
