package ganymedes01.etfuturum.client.gui.options;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.client.settings.GameSettings;

import java.util.List;

import static ganymedes01.etfuturum.client.gui.options.OptionWidgets.of;

/**
 * 26.3's Chat Settings (spec §54.4, §54.10), with 1.7.10's chat options in 26.3's order, EFR's Command Suggestions, and
 * 1.7.10's Show Cape at the end (26.3 has the cape in Skin Customization). 1.7.10's screen keeps its Done.
 */
public class GuiModernChatSettings extends ScreenChatOptions implements IOptionsLayout {

	private OptionsRowList list;
	private GuiButton done;

	public GuiModernChatSettings(GuiScreen parent, GameSettings settings) {
		super(parent, settings);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		buttonList.clear();
		done = new GuiButton(200, width / 2 - 100, height - 27, I18n.format("gui.done"));
		buttonList.add(done);
		list = new OptionsRowList(mc, width, height, 32, height - 32, button -> {
		});
		list.add(of(Options.CHAT_VISIBILITY), of(Options.CHAT_COLOR), of(Options.CHAT_LINKS), of(Options.CHAT_LINKS_PROMPT), of(Options.CHAT_OPACITY),
			of(Options.CHAT_SCALE), of(Options.CHAT_WIDTH), of(Options.CHAT_HEIGHT_FOCUSED), of(Options.CHAT_HEIGHT_UNFOCUSED),
			McFlyConfig.modernChat ? new OptionWidgets.CommandSuggestions(4041) : null, of(Options.SHOW_CAPE));
	}

	@Override
	public List<Object> etfu$entries() {
		List<Object> entries = list.entries();
		entries.add(done);
		return entries;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		list.func_148179_a(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
		super.mouseMovedOrUp(mouseX, mouseY, state);
		list.func_148181_b(mouseX, mouseY, state);
	}

	/** The list and one title; 1.7.10's screen drew two sections of its own. */
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		list.drawScreen(mouseX, mouseY, partialTicks);
		drawCenteredString(fontRendererObj, I18n.format("options.chat.title"), width / 2, 5, 0xFFFFFF);
		for (Object button : buttonList) {
			((GuiButton) button).drawButton(mc, mouseX, mouseY);
		}
	}
}
