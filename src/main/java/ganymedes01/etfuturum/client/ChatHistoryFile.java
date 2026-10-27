package ganymedes01.etfuturum.client;

import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The chat's typed-message history (the up arrow) kept across game restarts, beyond modern: one message per line in
 * etfuturum_chat_history.txt next to options.txt, the last hundred, as modern's in-memory history is capped at.
 */
public final class ChatHistoryFile {

	private static final int KEPT = 100;

	private ChatHistoryFile() {
	}

	private static File file() {
		return new File(Minecraft.getMinecraft().mcDataDir, "etfuturum_chat_history.txt");
	}

	/** The saved history, oldest first; empty when there is none. */
	public static List<String> load() {
		File file = file();
		if (!file.isFile()) {
			return new ArrayList<>();
		}
		try {
			List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			lines.removeIf(String::isEmpty);
			return lines.size() > KEPT ? new ArrayList<>(lines.subList(lines.size() - KEPT, lines.size())) : new ArrayList<>(lines);
		} catch (IOException e) {
			Logger.warn("Could not read the chat history from " + file + ": " + e);
			return new ArrayList<>();
		}
	}

	public static void save(List<String> history) {
		List<String> kept = history.size() > KEPT ? history.subList(history.size() - KEPT, history.size()) : history;
		try {
			Files.write(file().toPath(), kept, StandardCharsets.UTF_8);
		} catch (IOException e) {
			Logger.warn("Could not save the chat history to " + file() + ": " + e);
		}
	}
}
