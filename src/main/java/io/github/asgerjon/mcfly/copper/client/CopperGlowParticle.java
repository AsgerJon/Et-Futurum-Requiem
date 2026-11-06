package io.github.asgerjon.mcfly.copper.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.copper.Weathering;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.world.World;

/**
 * 26.3's `wax_on`, `wax_off` and `scrape` (SPEC-modern-movement.md §87): its `GlowParticle` (the glow sprite, 0.75 of a
 * particle's size, a friction of 0.96, its own light rising over its life) as `WaxOnProvider`, `WaxOffProvider` and
 * `ScrapeProvider` set it: wax on orange (0.91, 0.55, 0.08), wax off near white (1, 0.9, 1), scrape one of two greens
 * (0.29, 0.58, 0.51 or 0.43, 0.77, 0.62); the speed given times 0.005 across and 0.01 up (the scrape's 0.01 all ways);
 * a life of 10 to 39 ticks.
 */
@SideOnly(Side.CLIENT)
public class CopperGlowParticle extends McFlyParticle {

	public CopperGlowParticle(World world, double x, double y, double z, double xAux, double yAux, double zAux, String kind) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, particleRand.nextInt(30) + 10, (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F * 0.75F,
			colour(kind), "textures/particle/glow.png", 1);
		boolean scrape = Weathering.SCRAPE.equals(kind);
		double across = scrape ? 0.01D : 0.005D;
		motionX = xAux * across;
		motionY = yAux * 0.01D;
		motionZ = zAux * across;
		particleGravity = 0.0F;
	}

	private static int colour(String kind) {
		if (Weathering.WAX_ON.equals(kind)) {
			return 0xFFE88C14;
		}
		if (Weathering.WAX_OFF.equals(kind)) {
			return 0xFFFFE5FF;
		}
		return particleRand.nextBoolean() ? 0xFF4A9482 : 0xFF6EC49E;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// 1.7.10's particles keep 0.98 a tick, 26.3's glow 0.96
		motionX *= 0.96D / 0.98D;
		motionY *= 0.96D / 0.98D;
		motionZ *= 0.96D / 0.98D;
	}

	/** 26.3's `addSmoothBlockEmission`: its own light rising to full over its life. */
	@Override
	public int getBrightnessForRender(float partialTicks) {
		int lit = super.getBrightnessForRender(partialTicks);
		int own = Math.min(15, (int) ((particleAge + partialTicks) / particleMaxAge * 15.0F));
		return (lit & ~0xFFFF) | Math.max(lit >> 4 & 15, own) << 4;
	}
}
