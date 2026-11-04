package ganymedes01.etfuturum.client.gui.options;

import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;

/**
 * 26.3's Mouse Settings (spec §54.4, §54.10, §54.12): Sensitivity, Scroll Sensitivity, Discrete Scrolling, Invert Mouse
 * X, Invert Mouse Y (1.7.10's Invert Mouse) and Touchscreen Mode, which 26.3 doesn't have, at the end. 26.3's Allow
 * Cursor Changes is left out.
 */
public class GuiMouseSettings extends GuiOptionsListScreen {

	public GuiMouseSettings(GuiScreen parent) {
		super(parent, "options.mouse_settings.title");
	}

	@Override
	protected void addOptions(OptionsRowList list) {
		list.add(OptionWidgets.of(GameSettings.Options.SENSITIVITY), OptionWidgets.scrollSensitivity(4120),
			new OptionWidgets.OnOff(4121, "options.discrete_mouse_scroll", () -> ControlOptions.discreteScrolling, on -> ControlOptions.discreteScrolling = on),
			new OptionWidgets.OnOff(4122, "options.invertMouseX", () -> ControlOptions.invertMouseX, on -> ControlOptions.invertMouseX = on),
			OptionWidgets.of(GameSettings.Options.INVERT_MOUSE, "options.invertMouseY"), OptionWidgets.of(GameSettings.Options.TOUCHSCREEN));
	}
}
