package io.github.asgerjon.mcfly.wood.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntitySign;
import org.lwjgl.opengl.GL11;

/**
 * 26.3's hanging sign editor (SPEC-modern-movement.md 91.9; `HangingSignEditScreen`): the sign's picture
 * (`gui/hanging_signs/<wood>.png`, 16 by 16) times 4.5, 13 above y 125; the text at scale 1, 60-pixel lines 9 apart.
 */
public class HangingSignEditScreen extends SignEditScreen {

	private static final float BACKGROUND_SCALE = 4.5F;

	public HangingSignEditScreen(TileEntitySign sign, boolean front) {
		super(sign, front);
	}

	@Override
	protected String title() {
		return I18n.format("gui.etfuturum.hanging_sign.edit");
	}

	@Override
	protected float signYOffset() {
		return 125.0F;
	}

	@Override
	protected float textScale() {
		return 1.0F;
	}

	@Override
	protected void drawBackground() {
		GL11.glTranslatef(0.0F, -13.0F, 0.0F);
		GL11.glScalef(BACKGROUND_SCALE, BACKGROUND_SCALE, 1.0F);
		Gui.func_146110_a(-8, -8, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
	}
}
