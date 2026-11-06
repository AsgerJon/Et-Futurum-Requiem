package ganymedes01.etfuturum.unittas;

import ganymedes01.etfuturum.client.chat.ChatSuggestions;
import ganymedes01.etfuturum.client.chat.IChatScreen;
import ganymedes01.etfuturum.client.options.ControlOptions;
import ganymedes01.etfuturum.client.options.ToggleKeys;
import ganymedes01.etfuturum.offhand.Offhand;
import io.github.asgerjon.unittas.TasHooks;
import io.github.asgerjon.unittas.TasPlugin;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Collections;
import java.util.List;

/**
 * The fork's part in a unitTAS run (unittas-to-efr.md §4): our options set as the tests expect them for the run and put
 * back after, the off-hand emptied between tests, and our modern chat read for `TasChat`. In the tests' source set, so
 * in no jar.
 */
@TasPlugin
public class EfrTasPlugin implements TasHooks {

	private boolean[] toggleKeys;
	private Object[] controlOptions;

	public EfrTasPlugin() {
	}

	/**
	 * Hold, not Toggle, for every key; 26.3's defaults for Controls and Mouse Settings (spec §54). The window's title
	 * "unitTAS EFR" (the user, 2026-11-05): unitTAS's own runs, in its repository, are "unitTAS" too.
	 */
	@Override
	public void beforeRun() {
		net.minecraft.client.Minecraft.getMinecraft().func_152344_a(() -> {
			String title = org.lwjgl.opengl.Display.getTitle();
			if (!title.endsWith(" EFR")) {
				org.lwjgl.opengl.Display.setTitle(title + " EFR");
			}
		});
		toggleKeys = new boolean[]{ToggleKeys.crouch, ToggleKeys.sprint, ToggleKeys.attack, ToggleKeys.use};
		ToggleKeys.crouch = ToggleKeys.sprint = ToggleKeys.attack = ToggleKeys.use = false;
		controlOptions = new Object[]{ControlOptions.autoJump, ControlOptions.sprintWindow, ControlOptions.operatorItemsTab,
			ControlOptions.invertMouseX, ControlOptions.scrollSensitivity, ControlOptions.discreteScrolling};
		ControlOptions.autoJump = false;
		ControlOptions.sprintWindow = 7;
		ControlOptions.operatorItemsTab = false;
		ControlOptions.invertMouseX = false;
		ControlOptions.scrollSensitivity = 1.0D;
		ControlOptions.discreteScrolling = false;
	}

	@Override
	public void afterRun() {
		if (toggleKeys != null) {
			ToggleKeys.crouch = toggleKeys[0];
			ToggleKeys.sprint = toggleKeys[1];
			ToggleKeys.attack = toggleKeys[2];
			ToggleKeys.use = toggleKeys[3];
		}
		if (controlOptions != null) {
			ControlOptions.autoJump = (Boolean) controlOptions[0];
			ControlOptions.sprintWindow = (Integer) controlOptions[1];
			ControlOptions.operatorItemsTab = (Boolean) controlOptions[2];
			ControlOptions.invertMouseX = (Boolean) controlOptions[3];
			ControlOptions.scrollSensitivity = (Double) controlOptions[4];
			ControlOptions.discreteScrolling = (Boolean) controlOptions[5];
		}
	}

	/** The off-hand slot emptied, as the main inventory is (unitTAS's reset closes the player's container itself since dev4). */
	@Override
	public void resetPlayer(EntityPlayerMP player) {
		if (Offhand.isEnabled()) {
			Offhand.setOffhand(player, null);
		}
	}

	/** What's typed in our modern chat; null leaves it to 1.7.10's chat. */
	@Override
	public String chatText(GuiScreen chat) {
		return chat instanceof IChatScreen modern ? modern.etfu$input().getText() : null;
	}

	@Override
	public List<String> chatSuggestions(GuiScreen chat) {
		if (!(chat instanceof IChatScreen modern)) {
			return null;
		}
		ChatSuggestions suggestions = modern.etfu$suggestions();
		return suggestions == null ? Collections.<String>emptyList() : suggestions.shownSuggestions();
	}

	@Override
	public List<String> chatUsage(GuiScreen chat) {
		if (!(chat instanceof IChatScreen modern)) {
			return null;
		}
		ChatSuggestions suggestions = modern.etfu$suggestions();
		return suggestions == null ? Collections.<String>emptyList() : suggestions.usageLines();
	}
}
