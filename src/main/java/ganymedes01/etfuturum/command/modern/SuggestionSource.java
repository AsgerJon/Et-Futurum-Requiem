package ganymedes01.etfuturum.command.modern;

import java.util.Collection;

/** What argument types ask of a command source when parsing and suggesting, on the server or the client. */
public interface SuggestionSource {

	Collection<String> onlinePlayerNames();

	/** Entity selectors need permission level 2; a 1.7.10 client doesn't know its level and allows them. */
	boolean allowsSelectors();

	/** Coordinates to suggest for a position (spec §47.1): the looked-at block on the client, else ~ ~ ~. */
	default String[] absoluteCoordinates() {
		return new String[]{"~", "~", "~"};
	}
}
