package me.gammadelta.common.program.compilation;

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
	private final int maxExpandDepth = 32;

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
					throw new CodeCompileException("Found preprocessor token in middle of line: %s", next);
				}
				int expandDepth = 0;
				Token original = next;
				while (next.type == Token.Type.NAME && defLookup.containsKey(next.meat())) {
					if (++expandDepth > maxExpandDepth) {
						throw new CodeCompileException("Definition expansion recursion limit of %d exceeded while preprocessing token %s", expandDepth, original);
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
			throw new CodeCompileException("Unknown directive %s", directive);
		}
	}
	private void handleDef(Token directive, List<Token> arguments) throws CodeCompileException {
		if (arguments.size() != 2) {
			throw new CodeCompileException("Directive requires exactly two arguments (got %d): %s", arguments.size(), directive);
		}

		Token identifier = arguments.get(0);
		if (identifier.type != Token.Type.NAME) {
			throw new CodeCompileException("Expected type %s, got %s in argument to directive %s", Token.Type.NAME, identifier.type, directive);
		}
		String key = identifier.meat();
		if (defLookup.containsKey(key)) {
			throw new CodeCompileException("Redefinition of %s in directive %s", key, directive);
		}
		defLookup.put(key, arguments.get(1));
	}
}
