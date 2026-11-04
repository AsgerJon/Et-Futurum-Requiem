package ganymedes01.etfuturum.client.particle;

import io.github.asgerjon.mcfly.particle.McFlyParticles;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Modern's particle source for a used totem (spec §55.2): for 30 ticks, wherever the entity is, 16 tries a tick at a
 * direction in the unit ball; each kept one puts a totem particle a quarter of the way along it from the entity's middle
 * (scaled by its size), moving along it with 0.2 more upwards. Draws nothing itself.
 */
public class TotemEmitterFX extends EntityFX {

	private static final int LIFE = 30;

	private final Entity entity;
	private int life;

	public TotemEmitterFX(World world, Entity entity) {
		super(world, entity.posX, entity.boundingBox.minY + entity.height * 0.5D, entity.posZ, 0.0D, 0.0D, 0.0D);
		this.entity = entity;
		onUpdate();
	}

	@Override
	public void onUpdate() {
		for (int i = 0; i < 16; i++) {
			double xa = rand.nextFloat() * 2.0F - 1.0F;
			double ya = rand.nextFloat() * 2.0F - 1.0F;
			double za = rand.nextFloat() * 2.0F - 1.0F;
			if (xa * xa + ya * ya + za * za > 1.0D) {
				continue;
			}
			double x = entity.posX + entity.width * xa / 4.0D;
			double y = entity.boundingBox.minY + entity.height * (0.5D + ya / 4.0D);
			double z = entity.posZ + entity.width * za / 4.0D;
			McFlyParticlesClient.add(McFlyParticles.TOTEM_OF_UNDYING, x, y, z, xa, ya + 0.2D, za);
		}
		if (++life >= LIFE) {
			setDead();
		}
	}

	@Override
	public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
	}

	@Override
	public int getFXLayer() {
		return 3;
	}
}
