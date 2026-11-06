package io.github.asgerjon.mcfly.sculk.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.sculk.SculkShriekerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * 26.3's shriek (`ShriekParticle`; SPEC-modern-movement.md §90.9, step H4): after its delay, a ring rising 0.1 a tick
 * for 30 ticks, growing to 0.85 x 3/4 of its life and fading; two quads tilted 60 degrees about the x axis, back to back
 * (26.3's `rotationX(-1.0472)` and `rotationYXZ(-pi, 1.0472, 0)`), lit as by block light 15. Level event 3007 (the
 * shriek) makes 10 of them, 5 ticks apart, with `block.sculk_shrieker.shriek`.
 */
@SideOnly(Side.CLIENT)
public final class ShriekParticle extends EntityFX {

	private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft:textures/particle/shriek.png");
	private static final float SIZE = 0.85F;
	private int delay;

	private ShriekParticle(World world, double x, double y, double z, int delay) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.delay = delay;
		particleMaxAge = 30;
		particleGravity = 0.0F;
		motionX = 0.0D;
		motionY = 0.1D;
		motionZ = 0.0D;
		noClip = true;
		particleAlpha = 1.0F;
	}

	/** Level event 3007. */
	public static void levelEvent(World world, Random random, int x, int y, int z) {
		for (int i = 0; i < 10; i++) {
			Minecraft.getMinecraft().effectRenderer.addEffect(new ShriekParticle(world, x + 0.5D, y + SculkShriekerBlock.TOP_Y, z + 0.5D, i * 5));
		}
		world.playSound(x + 0.5D, y + SculkShriekerBlock.TOP_Y, z + 0.5D, McFly.MC_ASSETS + ":block.sculk_shrieker.shriek", 2.0F, 0.6F + random.nextFloat() * 0.4F,
			false);
	}

	/** 26.3's `tick`: the delay, then a particle's tick (no gravity, 0.98 slowing, through blocks). */
	@Override
	public void onUpdate() {
		if (delay > 0) {
			delay--;
			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;
			return;
		}
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (particleAge++ >= particleMaxAge) {
			setDead();
			return;
		}
		posX += motionX;
		posY += motionY;
		posZ += motionZ;
		motionX *= 0.98D;
		motionY *= 0.98D;
		motionZ *= 0.98D;
	}

	@Override
	public int getFXLayer() {
		return 3;
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return super.getBrightnessForRender(partialTicks) & 0xFFFF0000 | 15 << 4;
	}

	@Override
	public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
		if (delay > 0) {
			return;
		}
		float age = (particleAge + partialTicks) / particleMaxAge;
		float alpha = 1.0F - Math.max(0.0F, Math.min(1.0F, age));
		double size = SIZE * Math.max(0.0F, Math.min(1.0F, age * 0.75F));
		double x = prevPosX + (posX - prevPosX) * partialTicks - interpPosX;
		double y = prevPosY + (posY - prevPosY) * partialTicks - interpPosY;
		double z = prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ;
		int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		tessellator.startDrawingQuads();
		tessellator.setBrightness(getBrightnessForRender(partialTicks));
		tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha);
		// 26.3's two quads: (x, y, 0) turned -60 degrees about x is (x, y cos, -y sin); turned +60 about x then half round
		// about y it's (-x, y cos, -y sin): the same plane, its back, so it's seen from both sides
		double cos = 0.5D, sin = Math.sin(1.0472D);
		quad(tessellator, x, y, z, size, 1.0D, cos, sin);
		quad(tessellator, x, y, z, size, -1.0D, cos, sin);
		tessellator.draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, bound);
	}

	/** One quad of corners (+-1, +-1, 0) x size, tilted: x by the flip, y to (y cos, -y sin). */
	private static void quad(Tessellator tessellator, double x, double y, double z, double size, double flip, double cos, double sin) {
		double[][] corners = {{1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}, {-1, -1, 0, 1}};
		for (double[] c : corners) {
			double cx = c[0] * size * flip, cy = c[1] * size;
			tessellator.addVertexWithUV(x + cx, y + cy * cos, z - cy * sin, c[2], c[3]);
		}
	}
}
