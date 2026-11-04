package ganymedes01.etfuturum.client.gui.options;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.gui.GuiFovEffectsSlider;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;

/**
 * 26.3's Accessibility Settings (spec §54.4, §54.10), with the options that exist so far: the Controls... link (26.3
 * has it beside the narrator, which isn't ported), Chat Text Opacity (also on Chat Settings, one setting), View Bobbing
 * and FOV Effects.
 */
public class GuiAccessibilitySettings extends GuiOptionsListScreen {

	private static final int CONTROLS = 4101;

	public GuiAccessibilitySettings(GuiScreen parent) {
		super(parent, "options.accessibility.title");
	}

	@Override
	protected void addOptions(OptionsRowList list) {
		list.add(OptionWidgets.link(CONTROLS, "options.controls"), OptionWidgets.of(GameSettings.Options.CHAT_OPACITY),
			OptionWidgets.of(GameSettings.Options.VIEW_BOBBING), McFlyConfig.fovEffects ? new GuiFovEffectsSlider(1000, 0, 0) : null);
	}

	@Override
	protected void pressed(GuiButton button) {
		if (button.id == CONTROLS) {
			mc.displayGuiScreen(new GuiModernControls(this));
		}
	}
}
