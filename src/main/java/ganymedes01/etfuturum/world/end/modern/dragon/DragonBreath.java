package ganymedes01.etfuturum.world.end.modern.dragon;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import ganymedes01.etfuturum.entities.EntityDragonFireball;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Random;

/**
 * The dragon's breath as 26.3's (SPEC-modern-movement.md §76.8): the strafe's fireball, the perched breath's cloud, and
 * the breath particles of landing and breathing.
 */
final class DragonBreath {

	private DragonBreath() {
	}

	/** 26.3's shot: from 1 ahead of the head (its view, pitch 0) and 1 up, at the player's middle; the shoot sound. */
	static void fireball(ModernDragon modern, EntityPlayer target) {
		EntityDragon dragon = modern.dragon;
		float yaw = dragon.rotationYaw * (float) (Math.PI / 180.0);
		double viewX = -MathHelper.sin(yaw);
		double viewZ = MathHelper.cos(yaw);
		double sx = dragon.dragonPartHead.posX - viewX;
		double sy = dragon.dragonPartHead.boundingBox.minY + dragon.dragonPartHead.height * 0.5 + 0.5;
		double sz = dragon.dragonPartHead.posZ - viewZ;
		double dx = target.posX - sx;
		double dy = target.boundingBox.minY + target.height * 0.5 - sy;
		double dz = target.posZ - sz;
		dragon.worldObj.playSoundEffect(dragon.posX, dragon.posY, dragon.posZ, McFly.MC_ASSETS + ":entity.ender_dragon.shoot", 10.0F,
			1.0F + (dragon.getRNG().nextFloat() - dragon.getRNG().nextFloat()) * 0.2F);
		dragon.worldObj.spawnEntityInWorld(new EntityDragonFireball(dragon.worldObj, dragon, sx, sy, sz, dx, dy, dz));
	}

	/**
	 * 26.3's perched breath: a cloud on the ground 2.5 in front of the head (radius 5, 200 ticks, instant damage, breath
	 * particles, a quarter of an effect's time).
	 */
	static Object flameCloud(ModernDragon modern) {
		EntityDragon dragon = modern.dragon;
		double lx = dragon.dragonPartHead.posX - dragon.posX;
		double lz = dragon.dragonPartHead.posZ - dragon.posZ;
		double length = Math.sqrt(lx * lx + lz * lz);
		if (length > 0.0) {
			lx /= length;
			lz /= length;
		}
		double cx = dragon.dragonPartHead.posX + lx * 5.0 / 2.0;
		double cz = dragon.dragonPartHead.posZ + lz * 5.0 / 2.0;
		double start = dragon.dragonPartHead.boundingBox.minY + dragon.dragonPartHead.height * 0.5;
		double y = start;
		int bx = MathHelper.floor_double(cx);
		int bz = MathHelper.floor_double(cz);
		while (dragon.worldObj.isAirBlock(bx, MathHelper.floor_double(y), bz)) {
			y -= 1.0;
			if (y < 0.0) {
				y = start;
				break;
			}
		}
		y = MathHelper.floor_double(y) + 1;
		EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(dragon.worldObj, cx, y, cz);
		cloud.setOwner(dragon);
		cloud.setRadius(5.0F);
		cloud.setDuration(200);
		cloud.setParticle(EntityAreaEffectCloud.DRAGON_BREATH);
		cloud.setPotionDurationScale(0.25F);
		cloud.addEffect(new PotionEffect(Potion.harm.id, 1, 0));
		dragon.worldObj.spawnEntityInWorld(cloud);
		return cloud;
	}

	static void removeCloud(Object cloud) {
		if (cloud instanceof EntityAreaEffectCloud area) {
			area.setDead();
		}
	}

	/** Landing (client): 8 breath particles from the head each tick, along where it looks (26.3's fan is a no-op). */
	static void landingParticles(ModernDragon modern) {
		EntityDragon dragon = modern.dragon;
		Vec3 look = modern.headLookVector().normalize();
		Random random = dragon.getRNG();
		for (int i = 0; i < 8; i++) {
			double x = dragon.dragonPartHead.posX + random.nextGaussian() / 2.0;
			double y = dragon.dragonPartHead.boundingBox.minY + dragon.dragonPartHead.height * 0.5 + random.nextGaussian() / 2.0;
			double z = dragon.dragonPartHead.posZ + random.nextGaussian() / 2.0;
			particle(dragon, x, y, z, -look.xCoord * 0.08F + dragon.motionX, -look.yCoord * 0.3F + dragon.motionY, -look.zCoord * 0.08F + dragon.motionZ);
		}
	}

	/** Perched breathing (client, ticks 2 to 8): streams of breath from the head where it looks down. */
	static void flameParticles(ModernDragon modern) {
		EntityDragon dragon = modern.dragon;
		Vec3 look = modern.headLookVector().normalize();
		Random random = dragon.getRNG();
		for (int i = 0; i < 8; i++) {
			double x = dragon.dragonPartHead.posX + random.nextGaussian() / 2.0;
			double y = dragon.dragonPartHead.boundingBox.minY + dragon.dragonPartHead.height * 0.5 + random.nextGaussian() / 2.0;
			double z = dragon.dragonPartHead.posZ + random.nextGaussian() / 2.0;
			for (int j = 0; j < 6; j++) {
				particle(dragon, x, y, z, -look.xCoord * 0.08F * j, -look.yCoord * 0.6F, -look.zCoord * 0.08F * j);
			}
		}
	}

	private static void particle(EntityDragon dragon, double x, double y, double z, double vx, double vy, double vz) {
		if (dragon.worldObj.isRemote) {
			ganymedes01.etfuturum.client.particle.DragonBreathFX.spawn(dragon.worldObj, x, y, z, vx, vy, vz);
		}
	}
}
