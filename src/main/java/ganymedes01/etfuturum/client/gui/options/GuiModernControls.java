package ganymedes01.etfuturum.client.gui.options;

import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * 26.3's Controls (spec §54.4, §54.10), in 1.7.10's place: Mouse Settings... and Key Binds..., then Hold or Toggle for
 * Sneak, Sprint, Attack/Destroy and Use Item/Place Block (§54.11), then Auto-Jump, Sprint Window and Operator Items Tab
 * (§54.12; 26.3's Quit Shortcuts between the last two is left out).
 */
public class GuiModernControls extends GuiOptionsListScreen {

	private static final int MOUSE_SETTINGS = 4102;
	private static final int KEY_BINDS = 4103;

	public GuiModernControls(GuiScreen parent) {
		super(parent, "controls.title");
	}

	@Override
	protected void addOptions(OptionsRowList list) {
		list.add(OptionWidgets.link(MOUSE_SETTINGS, "options.mouse_settings"), OptionWidgets.link(KEY_BINDS, "controls.keybinds"));
		for (int key = 0; key < 4; key++) {
			list.add(new OptionWidgets.HoldOrToggle(4110 + key, key));
		}
		list.add(new OptionWidgets.OnOff(4114, "options.autoJump", () -> ControlOptions.autoJump, on -> ControlOptions.autoJump = on),
			OptionWidgets.sprintWindow(4115),
			new OptionWidgets.OnOff(4116, "options.operatorItemsTab", () -> ControlOptions.operatorItemsTab, on -> ControlOptions.operatorItemsTab = on));
	}

	@Override
	protected void pressed(GuiButton button) {
		if (button.id == MOUSE_SETTINGS) {
			mc.displayGuiScreen(new GuiMouseSettings(this));
		} else if (button.id == KEY_BINDS) {
			mc.displayGuiScreen(new GuiKeyBinds(this, mc.gameSettings));
		}
	}
}
