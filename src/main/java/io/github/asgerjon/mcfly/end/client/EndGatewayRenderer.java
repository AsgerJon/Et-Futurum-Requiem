package io.github.asgerjon.mcfly.end.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.end.EndGatewayBlockEntity;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * 26.3's end gateway look (`TheEndGatewayRenderer`, `BeaconRenderer.submitBeaconBeam`, the `rendertype_end_portal`
 * shader with 16 layers; SPEC-modern-movement.md §76.10, §84). The beam while it appears or after a use: a turning
 * diamond 0.15 out and a faint square 0.175 out, as far down as up. The cube: each side that shows, the shader's sum
 * drawn in fixed-function passes: the sky texture times its first colour, then each layer's portal texture times its
 * colour added on, all in screen space (1.7.10's texgen with the shader's projection, then each layer's turn, scale and
 * drift in the texture matrix).
 */
@SideOnly(Side.CLIENT)
public class EndGatewayRenderer extends TileEntitySpecialRenderer {

	private static final ResourceLocation BEAM = new ResourceLocation("etfuturum", "textures/entity/end_portal/end_gateway_beam.png");
	private static final ResourceLocation SKY = new ResourceLocation("textures/environment/end_sky.png");
	private static final ResourceLocation PORTAL = new ResourceLocation("textures/entity/end_portal.png");
	/** 26.3's DyeColor.MAGENTA (appearing) and PURPLE (after a use) texture colours. */
	private static final int SPAWN_COLOUR = 0xC74EBD;
	private static final int COOLDOWN_COLOUR = 0x8932B8;
	/** The gateway's `PORTAL_LAYERS`. */
	private static final int LAYERS = 16;
	/** The shader's `COLORS`. */
	private static final float[][] COLOURS = {
		{0.022087F, 0.098399F, 0.110818F}, {0.011892F, 0.095924F, 0.089485F}, {0.027636F, 0.101689F, 0.100326F},
		{0.046564F, 0.109883F, 0.114838F}, {0.064901F, 0.117696F, 0.097189F}, {0.063761F, 0.086895F, 0.123646F},
		{0.084817F, 0.111994F, 0.166380F}, {0.097489F, 0.154120F, 0.091064F}, {0.106152F, 0.131144F, 0.195191F},
		{0.097721F, 0.110188F, 0.187229F}, {0.133516F, 0.138278F, 0.148582F}, {0.070006F, 0.243332F, 0.235792F},
		{0.196766F, 0.142899F, 0.214696F}, {0.047281F, 0.315338F, 0.321970F}, {0.204675F, 0.390010F, 0.302066F},
		{0.080955F, 0.314821F, 0.661491F}};

	private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
	private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
	private static final FloatBuffer PLANE = GLAllocation.createDirectFloatBuffer(4);

	/** On the client in init (called from McFly: a static call, so a server never loads this class). */
	public static void register() {
		ClientRegistry.bindTileEntitySpecialRenderer(EndGatewayBlockEntity.class, new EndGatewayRenderer());
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
		EndGatewayBlockEntity gateway = (EndGatewayBlockEntity) tile;
		GL11.glDisable(GL11.GL_FOG);
		if (gateway.isSpawning() || gateway.isCoolingDown()) {
			float scale = gateway.isSpawning() ? gateway.getSpawnPercent(partialTicks) : gateway.getCooldownPercent(partialTicks);
			double distance = gateway.isSpawning() ? gateway.getWorldObj().getHeight() - 1 : 50.0;
			scale = MathHelper.sin(scale * (float) Math.PI);
			int height = MathHelper.floor_double(scale * distance);
			if (height > 0) {
				float time = Math.floorMod(gateway.getWorldObj().getTotalWorldTime(), 40L) + partialTicks;
				beam(x, y, z, scale, time, -height, height * 2, gateway.isSpawning() ? SPAWN_COLOUR : COOLDOWN_COLOUR, 0.15F, 0.175F);
			}
		}
		cube(gateway, x, y, z, partialTicks);
		GL11.glEnable(GL11.GL_FOG);
	}

	// ---------------------------------------------------------------- the beam (26.3's submitBeaconBeam)

	private void beam(double x, double y, double z, float scale, float time, int start, int height, int colour, float solid, float glow) {
		int end = start + height;
		float scroll = height < 0 ? time : -time;
		float vOffset = frac(scroll * 0.2F - MathHelper.floor_float(scroll * 0.1F));
		bindTexture(BEAM);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		Tessellator tessellator = Tessellator.instance;
		// the solid diamond, turning
		GL11.glPushMatrix();
		GL11.glRotatef(time * 2.25F - 45.0F, 0.0F, 1.0F, 0.0F);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
		float v2 = -1.0F + vOffset;
		float v1 = height * scale * (0.5F / solid) + v2;
		tessellator.startDrawingQuads();
		tessellator.setBrightness(0xF000F0);
		tessellator.setColorRGBA_I(colour, 255);
		part(tessellator, start, end, 0.0F, solid, solid, 0.0F, -solid, 0.0F, 0.0F, -solid, v1, v2);
		tessellator.draw();
		GL11.glPopMatrix();
		// the glow, a square, a quarter-ish see-through (alpha 32)
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthMask(false);
		v2 = -1.0F + vOffset;
		v1 = height * scale + v2;
		tessellator.startDrawingQuads();
		tessellator.setBrightness(0xF000F0);
		tessellator.setColorRGBA_I(colour, 32);
		part(tessellator, start, end, -glow, -glow, glow, -glow, -glow, glow, glow, glow, v1, v2);
		tessellator.draw();
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	/** 26.3's `renderPart`: four quads between the corners wn, en, ws, es. */
	private static void part(Tessellator t, int start, int end, float wnx, float wnz, float enx, float enz, float wsx, float wsz, float esx, float esz,
		float v1, float v2) {
		quad(t, start, end, wnx, wnz, enx, enz, v1, v2);
		quad(t, start, end, esx, esz, wsx, wsz, v1, v2);
		quad(t, start, end, enx, enz, esx, esz, v1, v2);
		quad(t, start, end, wsx, wsz, wnx, wnz, v1, v2);
	}

	private static void quad(Tessellator t, int start, int end, float ax, float az, float bx, float bz, float v1, float v2) {
		t.addVertexWithUV(ax, end, az, 1.0, v1);
		t.addVertexWithUV(ax, start, az, 1.0, v2);
		t.addVertexWithUV(bx, start, bz, 0.0, v2);
		t.addVertexWithUV(bx, end, bz, 0.0, v1);
	}

	private static float frac(float value) {
		return value - MathHelper.floor_float(value);
	}

	// ---------------------------------------------------------------- the cube (26.3's end portal shader)

	private void cube(EndGatewayBlockEntity gateway, double x, double y, double z, float partialTicks) {
		boolean[] shown = new boolean[6];
		boolean any = false;
		for (int side = 0; side < 6; side++) {
			shown[side] = gateway.shouldRenderFace(side);
			any |= shown[side];
		}
		if (!any) {
			return;
		}
		// 26.3's GameTime: the day's time as a share of the day
		float gameTime = (gateway.getWorldObj().getTotalWorldTime() % 24000L + partialTicks) / 24000.0F;
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
		// the generated coordinates are the block's own (planes given under this model view), carried to clip space by
		// the texture matrix below
		GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
		GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
		GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
		GL11.glTexGeni(GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
		GL11.glTexGen(GL11.GL_S, GL11.GL_EYE_PLANE, plane(1, 0, 0, 0));
		GL11.glTexGen(GL11.GL_T, GL11.GL_EYE_PLANE, plane(0, 1, 0, 0));
		GL11.glTexGen(GL11.GL_R, GL11.GL_EYE_PLANE, plane(0, 0, 1, 0));
		GL11.glTexGen(GL11.GL_Q, GL11.GL_EYE_PLANE, plane(0, 0, 0, 1));
		GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
		GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
		GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
		GL11.glEnable(GL11.GL_TEXTURE_GEN_Q);
		for (int pass = 0; pass <= LAYERS; pass++) {
			GL11.glMatrixMode(GL11.GL_TEXTURE);
			GL11.glPushMatrix();
			GL11.glLoadIdentity();
			float[] colour;
			if (pass == 0) {
				// the sky, written as it is (the sum starts here)
				bindTexture(SKY);
				GL11.glDisable(GL11.GL_BLEND);
				colour = COLOURS[0];
			} else {
				bindTexture(PORTAL);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
				colour = COLOURS[pass - 1];
				// the layer's matrix, read right to left: 26.3's SCALE_TRANSLATE, its drift, its turn and scale
				float layer = pass;
				GL11.glTranslatef(0.25F, 0.25F, 0.0F);
				GL11.glScalef(0.5F, 0.5F, 1.0F);
				GL11.glTranslatef(17.0F / layer, (2.0F + layer / 1.5F) * (gameTime * 1.5F), 0.0F);
				GL11.glRotatef((layer * layer * 4321.0F + layer * 9.0F) * 2.0F, 0.0F, 0.0F, 1.0F);
				float size = (4.5F - layer / 4.0F) * 2.0F;
				GL11.glScalef(size, size, 1.0F);
			}
			// the shader's projection_from_position: clip space to the screen's 0 to 1, still projective
			GL11.glTranslatef(0.5F, 0.5F, 0.0F);
			GL11.glScalef(0.5F, 0.5F, 1.0F);
			GL11.glMultMatrix(PROJECTION);
			GL11.glMultMatrix(MODELVIEW);
			Tessellator tessellator = Tessellator.instance;
			tessellator.startDrawingQuads();
			tessellator.setColorOpaque_F(colour[0], colour[1], colour[2]);
			faces(tessellator, shown, x, y, z);
			tessellator.draw();
			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
		}
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
		GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
		GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
		GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	private static FloatBuffer plane(float a, float b, float c, float d) {
		PLANE.clear();
		PLANE.put(a).put(b).put(c).put(d);
		PLANE.flip();
		return PLANE;
	}

	/** The block's sides that show, each wound to face out (1.7.10 culls back faces). */
	private static void faces(Tessellator t, boolean[] shown, double x, double y, double z) {
		double x1 = x + 1.0;
		double y1 = y + 1.0;
		double z1 = z + 1.0;
		if (shown[0]) {
			t.addVertex(x, y, z1);
			t.addVertex(x, y, z);
			t.addVertex(x1, y, z);
			t.addVertex(x1, y, z1);
		}
		if (shown[1]) {
			t.addVertex(x, y1, z);
			t.addVertex(x, y1, z1);
			t.addVertex(x1, y1, z1);
			t.addVertex(x1, y1, z);
		}
		if (shown[2]) {
			t.addVertex(x1, y1, z);
			t.addVertex(x1, y, z);
			t.addVertex(x, y, z);
			t.addVertex(x, y1, z);
		}
		if (shown[3]) {
			t.addVertex(x, y1, z1);
			t.addVertex(x, y, z1);
			t.addVertex(x1, y, z1);
			t.addVertex(x1, y1, z1);
		}
		if (shown[4]) {
			t.addVertex(x, y1, z);
			t.addVertex(x, y, z);
			t.addVertex(x, y, z1);
			t.addVertex(x, y1, z1);
		}
		if (shown[5]) {
			t.addVertex(x1, y1, z1);
			t.addVertex(x1, y, z1);
			t.addVertex(x1, y, z);
			t.addVertex(x1, y1, z);
		}
	}
}
