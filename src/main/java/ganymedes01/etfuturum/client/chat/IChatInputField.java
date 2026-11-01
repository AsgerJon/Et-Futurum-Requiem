package ganymedes01.etfuturum.client.chat;

import java.util.function.BiFunction;

/** What the chat's text field gains (modernchat.client.MixinGuiTextField). */
public interface IChatInputField {

	/** Colours for a drawn part of the text: (part, its offset in the text) to the part with formatting codes, or null. */
	void etfu$setFormatter(BiFunction<String, Integer, String> formatter);

	/** Grey text drawn after the cursor at the end of the text: the rest of the selected suggestion, or null. */
	void etfu$setSuggestion(String suggestion);
}
