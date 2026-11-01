package ganymedes01.etfuturum.client.gui;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

/**
 * Modern's "Attack Indicator" option in Video Settings (spec §46.11): Crosshair, Hotbar, OFF, one click to the next,
 * saved to attackIndicator in functions.cfg.
 */
public class GuiAttackIndicatorButton extends GuiButton {

	private static final String[] VALUES = {"crosshair", "hotbar", "off"};

	public GuiAttackIndicatorButton(int id, int x, int y) {
		super(id, x, y, 150, 20, "");
		displayString = label();
	}

	private static String label() {
		String value = ConfigFunctions.attackIndicator;
		String shown = "off".equals(value) ? I18n.format("options.off") : I18n.format("options.etfuturum.attackIndicator." + value);
		return I18n.format("options.etfuturum.attackIndicator") + ": " + shown;
	}

	/** In the options list a click reaches the button here; it moves on to the next setting. */
	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (!super.mousePressed(mc, mouseX, mouseY)) {
			return false;
		}
		func_146113_a(mc.getSoundHandler());
		int current = 0;
		for (int i = 0; i < VALUES.length; i++) {
			if (VALUES[i].equals(ConfigFunctions.attackIndicator)) {
				current = i;
			}
		}
		ConfigFunctions.saveAttackIndicator(VALUES[(current + 1) % VALUES.length]);
		displayString = label();
		return true;
	}
}
