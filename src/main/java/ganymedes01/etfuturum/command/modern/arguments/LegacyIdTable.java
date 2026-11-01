package ganymedes01.etfuturum.command.modern.arguments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The table of modern ids for 1.7.10's items and block states (spec §48), read once from the mod's
 * `assets/etfuturum/commands/legacy_ids.txt`: one section per `[name]` line, `#` lines are comments.
 */
final class LegacyIdTable {

	private static final String PATH = "/assets/etfuturum/commands/legacy_ids.txt";

	private static Map<String, List<String>> sections;

	private LegacyIdTable() {
	}

	/** The section's lines, in the file's order. */
	static synchronized List<String> lines(String section) {
		if (sections == null) {
			sections = read();
		}
		List<String> lines = sections.get(section);
		return lines == null ? Collections.emptyList() : lines;
	}

	/** The section's words, for the sections that list ids. */
	static Set<String> words(String section) {
		Set<String> words = new HashSet<>();
		for (String line : lines(section)) {
			Collections.addAll(words, line.split(" "));
		}
		return words;
	}

	/** The section's `key=value` lines. */
	static Map<String, String> pairs(String section) {
		Map<String, String> pairs = new HashMap<>();
		for (String line : lines(section)) {
			int equals = line.indexOf('=');
			pairs.put(line.substring(0, equals), line.substring(equals + 1));
		}
		return pairs;
	}

	private static Map<String, List<String>> read() {
		Map<String, List<String>> read = new HashMap<>();
		InputStream stream = LegacyIdTable.class.getResourceAsStream(PATH);
		if (stream == null) {
			throw new IllegalStateException("Missing " + PATH);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			List<String> current = null;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("[")) {
					current = new ArrayList<>();
					read.put(line.substring(1, line.length() - 1), current);
				} else if (current != null) {
					current.add(line);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Can't read " + PATH, e);
		}
		return read;
	}
}
