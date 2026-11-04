package ganymedes01.etfuturum.client.gui.options;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** The title screen's accessibility button (spec §54.10): a 20 x 20 button with 26.3's 15 x 15 icon in its middle. */
public class GuiAccessibilityButton extends GuiButton {

	private static final ResourceLocation ICON = new ResourceLocation("etfuturum", "textures/gui/sprites/icon/accessibility.png");

	public GuiAccessibilityButton(int id, int x, int y) {
		super(id, x, y, 20, 20, "");
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		super.drawButton(mc, mouseX, mouseY);
		if (visible) {
			mc.getTextureManager().bindTexture(ICON);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glEnable(GL11.GL_BLEND);
			func_146110_a(xPosition + 2, yPosition + 2, 0.0F, 0.0F, 15, 15, 15.0F, 15.0F);
		}
	}

	/** Whether the mouse is over the button, for its hover text. */
	public boolean hovered(int mouseX, int mouseY) {
		return visible && mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
	}
}
