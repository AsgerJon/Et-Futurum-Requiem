package ganymedes01.etfuturum.command.modern;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Modern's rules for which values a suggestion list offers for what is typed (spec §40, §41.3). */
public final class Suggest {

	private Suggest() {
	}

	/** Values whose start, or a part after `.`, `_` or `/`, begins with what is typed. */
	public static CompletableFuture<Suggestions> matching(Iterable<String> values, SuggestionsBuilder builder) {
		String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (String value : values) {
			if (matchesSubString(typed, value.toLowerCase(Locale.ROOT))) {
				builder.suggest(value);
			}
		}
		return builder.buildFuture();
	}

	/**
	 * Resource ids ("namespace:path"): with a `:` typed, ids matching the whole text; else ids whose namespace or
	 * path matches.
	 */
	public static CompletableFuture<Suggestions> resources(Iterable<String> ids, SuggestionsBuilder builder) {
		return resources(ids, builder, "");
	}

	/**
	 * The same, each id behind a mark such as `!` (spec §43.5): all of them before anything is typed; else only once
	 * the typed text starts like the mark, matching what follows it.
	 */
	public static CompletableFuture<Suggestions> resources(Iterable<String> ids, SuggestionsBuilder builder, String mark) {
		return resources(ids, builder, mark, "");
	}

	/** The same, each id also followed by a suffix, such as `=` after an item component's name (spec §45.2). */
	public static CompletableFuture<Suggestions> resources(Iterable<String> ids, SuggestionsBuilder builder, String mark, String suffix) {
		String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
		if (!typed.isEmpty() && !mark.isEmpty()) {
			int common = 0;
			while (common < typed.length() && common < mark.length() && typed.charAt(common) == mark.charAt(common)) {
				common++;
			}
			if (common == 0) {
				return builder.buildFuture();
			}
			typed = typed.substring(common);
		}
		boolean hasNamespace = typed.indexOf(':') >= 0;
		for (String id : ids) {
			int colon = id.indexOf(':');
			if (hasNamespace ? matchesSubString(typed, id)
				: matchesSubString(typed, id.substring(0, colon)) || matchesSubString(typed, id.substring(colon + 1))) {
				builder.suggest(mark + id + suffix);
			}
		}
		return builder.buildFuture();
	}

	public static boolean matchesSubString(String typed, String value) {
		int index = 0;
		while (!value.startsWith(typed, index)) {
			int splitter = -1;
			for (int i = index; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == '.' || c == '_' || c == '/') {
					splitter = i;
					break;
				}
			}
			if (splitter < 0) {
				return false;
			}
			index = splitter + 1;
		}
		return true;
	}
}
