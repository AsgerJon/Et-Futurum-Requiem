package ganymedes01.etfuturum.client.chat;

import net.minecraft.client.gui.GuiTextField;

/** What the chat screen gains (modernchat.client.MixinGuiChat), read by unitTAS. */
public interface IChatScreen {

	/** The screen's suggestions, or null when the server sent no command tree. */
	ChatSuggestions etfu$suggestions();

	GuiTextField etfu$input();
}
