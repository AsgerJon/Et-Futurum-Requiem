package io.github.asgerjon.mcfly.wood.client;

import io.github.asgerjon.mcfly.wood.SignSide;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.block.Block;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * A sign's text, both sides (SPEC-modern-movement.md 91.8; 26.3's `StandingSignRenderer`, `AbstractSignRenderer`), for
 * every `TileEntitySign`: its body is a block model in the chunk. From the block's middle, turned by the sign's turn
 * (a wall sign then moved to its board), the back turned half a turn; 1/96 a pixel at (0, 1/3, 0.0467); each line the
 * part that fits 90 pixels, centred, 10 apart, drawn by `SignTextDrawing`.
 */
public class SignRenderer extends TileEntitySpecialRenderer {

	private static final float SCALE = 0.010416667F;
	/** 26.3's `HangingSignRenderer`: 0.9 / 64. */
	private static final float HANGING_SCALE = 0.0140625F;

	/** On the client in init: for `TileEntitySign` (vanilla's renderer's place) and so every subclass without its own. */
	public static void register() {
		cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySign.class, new SignRenderer());
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
		if (!(tile instanceof TileEntitySign) || !(tile instanceof Signs.Sided sided)) {
			return;
		}
		Block block = tile.getBlockType();
		int meta = tile.getBlockMetadata();
		boolean standing = Signs.standing(block);
		boolean hanging = Signs.hanging(tile);
		float angle = Signs.yRot(block, meta);
		FontRenderer font = func_147498_b();
		for (boolean front : new boolean[]{true, false}) {
			SignSide side = sided.mcfly$side(front);
			if (!side.hasMessage()) {
				continue;
			}
			GL11.glPushMatrix();
			float scale;
			if (hanging) {
				// 26.3's `HangingSignRenderer`
				GL11.glTranslated(x + 0.5D, y + 0.9375D, z + 0.5D);
				GL11.glRotatef(-angle, 0.0F, 1.0F, 0.0F);
				GL11.glTranslatef(0.0F, -0.3125F, 0.0F);
				if (!front) {
					GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
				}
				GL11.glTranslatef(0.0F, -0.32F, 0.073F);
				scale = HANGING_SCALE;
			} else {
				GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
				GL11.glRotatef(-angle, 0.0F, 1.0F, 0.0F);
				if (!standing) {
					GL11.glTranslatef(0.0F, -0.3125F, -0.4375F);
				}
				if (!front) {
					GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
				}
				GL11.glTranslatef(0.0F, 0.33333334F, 0.046666667F);
				scale = SCALE;
			}
			GL11.glScalef(scale, -scale, scale);
			GL11.glNormal3f(0.0F, 0.0F, -scale);
			GL11.glDepthMask(false);
			int lineHeight = Signs.lineHeight(tile), maxWidth = Signs.maxWidth(tile);
			int middle = Signs.LINES * lineHeight / 2;
			for (int i = 0; i < Signs.LINES; i++) {
				String line = firstFitting(font, side.line(i), maxWidth);
				SignTextDrawing.draw(font, line, -font.getStringWidth(line) / 2, i * lineHeight - middle, side.color, side.glowing, tile);
			}
			GL11.glDepthMask(true);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glPopMatrix();
		}
	}

	/** 26.3's `font.split(line, width)`'s first part: what fits, broken at a space where it can be. */
	@SuppressWarnings("unchecked")
	private static String firstFitting(FontRenderer font, String line, int width) {
		if (line.isEmpty() || font.getStringWidth(line) <= width) {
			return line;
		}
		List<String> parts = font.listFormattedStringToWidth(line, width);
		return parts.isEmpty() ? "" : parts.get(0);
	}
}
