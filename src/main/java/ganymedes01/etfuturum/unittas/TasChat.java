package ganymedes01.etfuturum.unittas;

import ganymedes01.etfuturum.client.chat.ChatSuggestions;
import ganymedes01.etfuturum.client.chat.IChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.List;

/** The chat screen as a test typed into it: what it shows, more keys, and Enter. */
public final class TasChat {

	/** Ticks after a key before the screen is read: suggestions for 1.7.10's commands come from the server. */
	private static final int SETTLE_TICKS = 4;

	private final TasGame game;

	TasChat(TasGame game) {
		this.game = game;
	}

	/** Types more, key by key. */
	public TasChat typeMore(String text) {
		game.client(() -> {
			GuiScreen screen = openChat();
			for (char character : text.toCharArray()) {
				TasTestCase.keyTyped(screen, character, 0);
			}
			return null;
		});
		game.clientTicks(SETTLE_TICKS);
		return this;
	}

	/** Presses a key such as Keyboard.KEY_TAB, KEY_UP, KEY_BACK. */
	public TasChat key(int keyCode) {
		game.client(() -> {
			TasTestCase.keyTyped(openChat(), '\0', keyCode);
			return null;
		});
		game.clientTicks(SETTLE_TICKS);
		return this;
	}

	/** The text in the input. */
	public String text() {
		return game.client(() -> ((IChatScreen) openChat()).etfu$input().getText());
	}

	/** The suggestion list as shown, empty while it is closed. */
	public List<String> suggestions() {
		return game.client(() -> {
			ChatSuggestions suggestions = ((IChatScreen) openChat()).etfu$suggestions();
			return suggestions == null ? Collections.<String>emptyList() : suggestions.shownSuggestions();
		});
	}

	/** The grey usage or the red error above the input. */
	public List<String> usage() {
		return game.client(() -> {
			ChatSuggestions suggestions = ((IChatScreen) openChat()).etfu$suggestions();
			return suggestions == null ? Collections.<String>emptyList() : suggestions.usageLines();
		});
	}

	/** Presses Enter: the chat sends the text and closes; returns what came back. */
	public TasCommand enter() {
		String text = text();
		int mark = game.chatMark();
		game.client(() -> {
			TasTestCase.keyTyped(openChat(), '\r', Keyboard.KEY_RETURN);
			return null;
		});
		game.clientTicks(SETTLE_TICKS);
		return new TasCommand(text, -1, game.chatSince(mark));
	}

	public void close() {
		game.client(() -> {
			if (Minecraft.getMinecraft().currentScreen instanceof GuiChat) {
				Minecraft.getMinecraft().displayGuiScreen(null);
			}
			return null;
		});
	}

	private static GuiScreen openChat() {
		GuiScreen screen = Minecraft.getMinecraft().currentScreen;
		if (!(screen instanceof GuiChat)) {
			throw new AssertionError("the chat isn't open (the screen is " + (screen == null ? "the game" : screen.getClass().getSimpleName()) + ")");
		}
		return screen;
	}
}
