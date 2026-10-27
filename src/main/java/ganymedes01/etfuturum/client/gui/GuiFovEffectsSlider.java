package ganymedes01.etfuturum.client.gui;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Modern's "FOV Effects" slider (options.fovEffectScale): how much the field of view follows speed, flying, bow
 * drawing and water. The knob runs over the square root of the value, as modern's does, so the low end is fine:
 * the middle is 25%, 10% sits a third of the way. 0 shows as OFF. Saved to functions.cfg when the knob is released.
 */
public class GuiFovEffectsSlider extends GuiButton {

	private float position;
	private boolean dragging;

	public GuiFovEffectsSlider(int id, int x, int y) {
		super(id, x, y, 150, 20, "");
		position = (float) Math.sqrt(ConfigFunctions.fovEffectScale);
		displayString = label();
	}

	private static String label() {
		int percent = (int) (ConfigFunctions.fovEffectScale * 100.0F);
		return I18n.format("options.etfuturum.fovEffects") + ": " + (percent == 0 ? I18n.format("options.off") : percent + "%");
	}

	/** The slider look: the dark track, as vanilla's option sliders. */
	@Override
	public int getHoverState(boolean hovered) {
		return 0;
	}

	private void moveTo(int mouseX) {
		position = MathHelper.clamp_float((mouseX - (xPosition + 4)) / (float) (width - 8), 0.0F, 1.0F);
		ConfigFunctions.fovEffectScale = position * position;
		displayString = label();
	}

	@Override
	protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
		if (visible) {
			if (dragging) {
				moveTo(mouseX);
			}
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			int knobX = xPosition + (int) (position * (width - 8));
			drawTexturedModalRect(knobX, yPosition, 0, 66, 4, 20);
			drawTexturedModalRect(knobX + 4, yPosition, 196, 66, 4, 20);
		}
	}

	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (super.mousePressed(mc, mouseX, mouseY)) {
			moveTo(mouseX);
			dragging = true;
			return true;
		}
		return false;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY) {
		if (dragging) {
			dragging = false;
			ConfigFunctions.saveFovEffectScale(ConfigFunctions.fovEffectScale);
		}
	}
}
