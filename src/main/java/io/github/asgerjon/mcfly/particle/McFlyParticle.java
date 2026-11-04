package io.github.asgerjon.mcfly.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Our particles' base (SPEC-modern-movement.md §79.4, B9): a camera-facing square, drawn by itself in 1.7.10's lit
 * layer, from one picture, a strip of frames (`name_0.png` to `name_<n-1>.png`, the one shown chosen by
 * {@link #currentTexture}), or 1.7.10's particle sheet (`minecraft:textures/particle/particles.png`, a cell chosen by
 * {@link #setParticleTextureIndex}). Its colour is given as ARGB; with {@link #fadeAway} its alpha falls over the second
 * half of its life.
 */
@SideOnly(Side.CLIENT)
public class McFlyParticle extends EntityFX {

	private static final String SHEET = "minecraft:textures/particle/particles.png";

	protected static final Random particleRand = new Random();

	/** How many frames its picture has (1: one picture). */
	protected final int textures;
	/** The frame shown, of a strip. */
	protected int currentTexture;
	/** Whether its alpha falls over the second half of its life. */
	protected boolean fadeAway;
	/** The light where it is, as of its last tick. */
	protected float lightLevel;

	private final boolean sheet;
	private final ResourceLocation[] frames;

	public McFlyParticle(World world, double x, double y, double z, double mx, double my, double mz, int maxAge, float scale, int argb,
		String texture, int textures) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.textures = textures;
		this.sheet = SHEET.equals(texture);
		this.frames = new ResourceLocation[sheet ? 1 : textures];
		if (sheet || textures == 1) {
			frames[0] = new ResourceLocation(texture);
		} else {
			String base = texture.substring(0, texture.length() - ".png".length());
			for (int i = 0; i < textures; i++) {
				frames[i] = new ResourceLocation(base + "_" + i + ".png");
			}
		}
		// the little push 1.7.10 gives every particle, a hundredth of it, then the one it's given
		motionX = motionX * 0.01D + mx;
		motionY = motionY * 0.01D + my;
		motionZ = motionZ * 0.01D + mz;
		setPosition(x, y, z);
		prevPosX = x;
		prevPosY = y;
		prevPosZ = z;
		particleMaxAge = maxAge;
		particleScale = scale;
		noClip = true;
		particleAlpha = (argb >>> 24 & 0xFF) / 255.0F;
		particleRed = (argb >> 16 & 0xFF) / 255.0F;
		particleGreen = (argb >> 8 & 0xFF) / 255.0F;
		particleBlue = (argb & 0xFF) / 255.0F;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		lightLevel = worldObj.getLightBrightness((int) posX, (int) posY, (int) posZ);
		int half = particleMaxAge / 2;
		if (fadeAway && particleAge > half) {
			setAlphaF(1.0F - (particleAge - half) / (float) particleMaxAge);
		}
	}

	/** A cell of 1.7.10's particle sheet (16 by 16); only for a particle on the sheet. */
	@Override
	public void setParticleTextureIndex(int index) {
		if (!sheet) {
			throw new IllegalStateException("not a particle on the sheet");
		}
		particleTextureIndexX = index % 16;
		particleTextureIndexY = index / 16;
	}

	/** 1.7.10's lit layer: each particle there binds its own picture and draws itself. */
	@Override
	public int getFXLayer() {
		return 3;
	}

	@Override
	public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
		int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		Minecraft.getMinecraft().getTextureManager().bindTexture(frames[sheet ? 0 : Math.floorMod(currentTexture, textures)]);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		float x = (float) (prevPosX + (posX - prevPosX) * partialTicks - interpPosX);
		float y = (float) (prevPosY + (posY - prevPosY) * partialTicks - interpPosY);
		float z = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ);
		double u0 = 0.0D, u1 = 1.0D, v0 = 0.0D, v1 = 1.0D;
		if (sheet) {
			u0 = particleTextureIndexX / 16.0D;
			u1 = u0 + 0.0624375D;
			v0 = particleTextureIndexY / 16.0D;
			v1 = v0 + 0.0624375D;
		}
		double size = 0.1D * particleScale;
		tessellator.startDrawingQuads();
		tessellator.setBrightness(getBrightnessForRender(lightLevel));
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, particleAlpha);
		tessellator.addVertexWithUV(x - rx * size - ryz * size, y - rxz * size, z - rz * size - rxy * size, u1, v1);
		tessellator.addVertexWithUV(x - rx * size + ryz * size, y + rxz * size, z - rz * size + rxy * size, u1, v0);
		tessellator.addVertexWithUV(x + rx * size + ryz * size, y + rxz * size, z + rz * size + rxy * size, u0, v0);
		tessellator.addVertexWithUV(x + rx * size - ryz * size, y - rxz * size, z + rz * size - rxy * size, u0, v1);
		tessellator.draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, bound);
	}
}
