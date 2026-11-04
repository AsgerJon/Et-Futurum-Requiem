package ganymedes01.etfuturum.client.gui;

import io.github.asgerjon.mcfly.config.McFlyConfig;
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
		String value = McFlyConfig.attackIndicator;
		String shown = "off".equals(value) ? I18n.format("options.off") : I18n.format("options.etfuturum.attackIndicator." + value);
		return I18n.format("options.etfuturum.attackIndicator") + ": " + shown;
	}

	/**
	 * In the options list a click reaches the button here; it moves on to the next setting. Silent, as 1.7.10's option
	 * buttons in that list are; the modern options list plays the click for every button.
	 */
	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (!super.mousePressed(mc, mouseX, mouseY)) {
			return false;
		}
		int current = 0;
		for (int i = 0; i < VALUES.length; i++) {
			if (VALUES[i].equals(McFlyConfig.attackIndicator)) {
				current = i;
			}
		}
		McFlyConfig.saveAttackIndicator(VALUES[(current + 1) % VALUES.length]);
		displayString = label();
		return true;
	}
}
