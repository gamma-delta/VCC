package me.gammadelta.vcc.common.program.compilation;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * Preprocess input token sequences.
 * This implementation cleans out preprocessor tokens,
 * and replaces matched defines with the corresponding tokens.
 */
public class CodePreprocessor {
	private final Map<String, Token> defLookup = new HashMap<>();
	public static final int MAX_EXPAND_DEPTH = 32;

	public void preprocessTokens(List<Token> tokens) throws CodeCompileException {
		for (ListIterator<Token> it = tokens.listIterator(); it.hasNext();) {
			Token next = it.next();
			if (next.type == Token.Type.PREPROCESSOR) {
				it.remove();

				// save macro arguments
				Token argument;
				List<Token> arguments = new LinkedList<>();
				while ((argument = it.next()).type != Token.Type.NEWLINE) {
					it.remove();
					arguments.add(argument);
				}
				it.remove();
				handleDirective(next, arguments);
				continue;
			}
			// else, scan line for preprocessor macros
			do {
				if (next.type == Token.Type.PREPROCESSOR) {
					throw new CodeCompileException.PreprocessException.DirectiveInMiddleOfLine(next);
				}
				int expandDepth = 0;
				Token original = next;
				while (next.type == Token.Type.NAME && defLookup.containsKey(next.meat())) {
					if (++expandDepth > MAX_EXPAND_DEPTH) {
						throw new CodeCompileException.PreprocessException.DefinitionExpansionTooThicc(original);
					}
					next = next.rewrite(defLookup.get(next.meat()));
					it.set(next);
				}
			} while ((next = it.next()).type != Token.Type.NEWLINE);
		}
	}	
	private void handleDirective(Token directive, List<Token> arguments) throws CodeCompileException {
		if (directive.meat().equalsIgnoreCase("def")) {
			handleDef(directive, arguments);
		} else {
			throw new CodeCompileException.PreprocessException.UnknownDirective(directive);
		}
	}
	private void handleDef(Token directive, List<Token> arguments) throws CodeCompileException {
		if (arguments.size() != 2) {
			throw new CodeCompileException.PreprocessException.BadArity(directive, 2, arguments.size());
		}

		Token identifier = arguments.get(0);
		if (identifier.type != Token.Type.NAME) {
			throw new CodeCompileException.PreprocessException.BadArgType(directive, identifier, Token.Type.NAME, 0);
		}
		String key = identifier.meat();
		if (defLookup.containsKey(key)) {
			throw new CodeCompileException.PreprocessException.Redefinition(identifier);
		}
		defLookup.put(key, arguments.get(1));
	}
}
