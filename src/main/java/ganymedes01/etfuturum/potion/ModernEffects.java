package ganymedes01.etfuturum.potion;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.mixins.early.moderneffects.AccessorEntitySlime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The work of 26.3's effects 1.7.10 lacks (SPEC-modern-movement.md §69.11; 26.3's `MobEffects`,
 * `WindChargedMobEffect`, `WeavingMobEffect`, `OozingMobEffect`, `InfestedMobEffect`, `WebBlock`, `FishingHook`).
 * The "killed" ones run at the end of the death animation, where 26.3 removes a killed mob (`onKilledRemoved`, from
 * `MixinEntityLivingBase`); infested's when hurt (`onHurt`, from ServerEventHandler).
 */
public final class ModernEffects {

	private ModernEffects() {
	}

	// ---------------------------------------------------------------- luck

	/** The luck an angler's effects add to fishing (26.3's luck attribute: +1 a level of luck, -1 of bad luck). */
	public static int fishingLuck(EntityPlayer angler) {
		return level(angler, ModPotions.luck) - level(angler, ModPotions.unluck);
	}

	private static int level(EntityLivingBase entity, Potion potion) {
		if (potion == null || entity == null) {
			return 0;
		}
		PotionEffect effect = entity.getActivePotionEffect(potion);
		return effect == null ? 0 : effect.getAmplifier() + 1;
	}

	// ---------------------------------------------------------------- weaving

	/** How much of its motion a cobweb lets through: x, y, z (26.3's `WebBlock`: weaving halves its hold). */
	public static double[] webFactors(Entity entity) {
		boolean woven = entity instanceof EntityLivingBase living && ModPotions.weaving != null && living.isPotionActive(ModPotions.weaving);
		return woven ? new double[]{0.5D, 0.25D, 0.5D} : new double[]{0.25D, 0.05D, 0.25D};
	}

	// ---------------------------------------------------------------- killed

	/** A killed mob or player at the end of its death animation (26.3's `onMobRemoved` with `KILLED`). */
	public static void onKilledRemoved(EntityLivingBase entity) {
		World world = entity.worldObj;
		if (world.isRemote) {
			return;
		}
		if (ModPotions.windCharged != null && entity.isPotionActive(ModPotions.windCharged)) {
			windBurst(world, entity);
		}
		if (ModPotions.weaving != null && entity.isPotionActive(ModPotions.weaving)
			&& (entity instanceof EntityPlayer || world.getGameRules().getGameRuleBooleanValue("mobGriefing"))) {
			cobwebs(world, entity);
		}
		if (ModPotions.oozing != null && entity.isPotionActive(ModPotions.oozing)) {
			// 26.3 caps them by max entity cramming, which 1.7.10 doesn't have: always 2
			for (int i = 0; i < 2; i++) {
				EntitySlime slime = new EntitySlime(world);
				((AccessorEntitySlime) slime).etfu$setSlimeSize(2);
				slime.setLocationAndAngles(entity.posX, entity.posY + 0.5D, entity.posZ, world.rand.nextFloat() * 360.0F, 0.0F);
				world.spawnEntityInWorld(slime);
			}
		}
	}

	/**
	 * 26.3's wind charged burst: an explosion of 3 to 5 at the mob's middle that harms no block or entity, knocks back
	 * 1.22 times as hard as one (less blast protection), and triggers what its rays reach.
	 */
	private static void windBurst(World world, EntityLivingBase source) {
		Random random = source.getRNG();
		double x = source.posX;
		double y = source.posY + source.height / 2.0F;
		double z = source.posZ;
		float size = 3.0F + random.nextFloat() * 2.0F;
		double reach = size * 2.0D;
		Vec3 centre = Vec3.createVectorHelper(x, y, z);
		@SuppressWarnings("unchecked")
		List<Entity> near = world.getEntitiesWithinAABBExcludingEntity(source,
			AxisAlignedBB.getBoundingBox(x - reach - 1.0D, y - reach - 1.0D, z - reach - 1.0D, x + reach + 1.0D, y + reach + 1.0D, z + reach + 1.0D));
		for (Entity entity : near) {
			double distance = entity.getDistance(x, y, z) / reach;
			if (distance > 1.0D) {
				continue;
			}
			double dx = entity.posX - x;
			double dy = entity.posY + entity.getEyeHeight() - y;
			double dz = entity.posZ - z;
			double length = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
			if (length == 0.0D) {
				continue;
			}
			double push = (1.0D - distance) * world.getBlockDensity(centre, entity.boundingBox) * 1.22D;
			if (entity instanceof EntityLivingBase) {
				push = EnchantmentProtection.func_92092_a(entity, push);
			}
			entity.motionX += dx / length * push;
			entity.motionY += dy / length * push;
			entity.motionZ += dz / length * push;
			entity.velocityChanged = true;
		}
		for (ChunkPosition pos : reached(world, source, x, y, z, size, random)) {
			trigger(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
		}
		world.playSoundEffect(x, y, z, McFly.MC_ASSETS + ":entity.breeze.wind_burst", 4.0F,
			(1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
		if (world instanceof WorldServer server) {
			server.func_147487_a(size < 2.0F ? "explode" : "largeexplode", x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			server.func_147487_a("cloud", x, y, z, 12, size / 4.0D, size / 4.0D, size / 4.0D, 0.05D);
		}
	}

	/** The blocks an explosion's rays reach, as 1.7.10's `Explosion.doExplosionA` casts them. */
	private static Set<ChunkPosition> reached(World world, Entity source, double x, double y, double z, float size, Random random) {
		Set<ChunkPosition> reached = new HashSet<>();
		float step = 0.3F;
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {
					if (i != 0 && i != 15 && j != 0 && j != 15 && k != 0 && k != 15) {
						continue;
					}
					double rx = i / 15.0F * 2.0F - 1.0F;
					double ry = j / 15.0F * 2.0F - 1.0F;
					double rz = k / 15.0F * 2.0F - 1.0F;
					double length = Math.sqrt(rx * rx + ry * ry + rz * rz);
					rx /= length;
					ry /= length;
					rz /= length;
					float strength = size * (0.7F + random.nextFloat() * 0.6F);
					double px = x;
					double py = y;
					double pz = z;
					for (; strength > 0.0F; strength -= step * 0.75F) {
						int bx = MathHelper.floor_double(px);
						int by = MathHelper.floor_double(py);
						int bz = MathHelper.floor_double(pz);
						Block block = world.getBlock(bx, by, bz);
						if (block.getMaterial() != Material.air) {
							strength -= (block.getExplosionResistance(source, world, bx, by, bz, x, y, z) + 0.3F) * step;
						}
						if (strength > 0.0F) {
							reached.add(new ChunkPosition(bx, by, bz));
						}
						px += rx * step;
						py += ry * step;
						pz += rz * step;
					}
				}
			}
		}
		return reached;
	}

	/**
	 * 26.3's `ExplosionInteraction.TRIGGER`: wooden doors, trapdoors and fence gates toggle; levers flip; buttons press.
	 * Still to come with their blocks (spec §73): a bell rings (`BellBlock.attemptToRing`), lit candles and candle cakes
	 * go out (`AbstractCandleBlock.extinguish`). EFR has neither yet.
	 */
	private static void trigger(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block instanceof BlockDoor door && block.getMaterial() != Material.iron) {
			int meta = world.getBlockMetadata(x, y, z);
			// one toggle a door: from its lower half only
			if ((meta & 8) == 0) {
				door.func_150014_a(world, x, y, z, (door.func_150012_g(world, x, y, z) & 4) == 0);
			}
		} else if (block instanceof BlockTrapDoor trapdoor && block.getMaterial() != Material.iron) {
			trapdoor.func_150120_a(world, x, y, z, (world.getBlockMetadata(x, y, z) & 4) == 0);
		} else if (block instanceof BlockFenceGate) {
			world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) ^ 4, 2);
			world.playAuxSFX(1003, x, y, z, 0);
		} else if (block instanceof BlockLever || block instanceof BlockButton) {
			// neither reads the player
			block.onBlockActivated(world, x, y, z, null, 0, 0.0F, 0.0F, 0.0F);
		}
	}

	/** 26.3's weaving: 2 or 3 cobwebs, of 15 random spots within a block, replaceable with a sturdy top under them. */
	private static void cobwebs(World world, EntityLivingBase entity) {
		Random random = entity.getRNG();
		int count = 2 + random.nextInt(2);
		int bx = MathHelper.floor_double(entity.posX);
		int by = MathHelper.floor_double(entity.posY);
		int bz = MathHelper.floor_double(entity.posZ);
		Set<ChunkPosition> chosen = new HashSet<>();
		for (int i = 0; i < 15 && chosen.size() < count; i++) {
			int x = bx + random.nextInt(3) - 1;
			int y = by + random.nextInt(3) - 1;
			int z = bz + random.nextInt(3) - 1;
			ChunkPosition pos = new ChunkPosition(x, y, z);
			if (!chosen.contains(pos) && world.getBlock(x, y, z).isReplaceable(world, x, y, z) && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) {
				chosen.add(pos);
			}
		}
		for (ChunkPosition pos : chosen) {
			world.setBlock(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, Blocks.web);
			if (world instanceof WorldServer server) {
				server.func_147487_a("blockcrack_" + Block.getIdFromBlock(Blocks.web) + "_0", pos.chunkPosX + 0.5D, pos.chunkPosY + 0.5D,
					pos.chunkPosZ + 0.5D, 8, 0.3D, 0.3D, 0.3D, 0.05D);
			}
		}
	}

	// ---------------------------------------------------------------- hurt

	/** 26.3's infested: hurt, a 1 in 10 chance of 1 or 2 silverfish thrown out along its look. */
	public static void onHurt(EntityLivingBase entity) {
		World world = entity.worldObj;
		if (world.isRemote || ModPotions.infested == null || !entity.isPotionActive(ModPotions.infested)) {
			return;
		}
		Random random = entity.getRNG();
		if (random.nextFloat() > 0.1F) {
			return;
		}
		int count = 1 + random.nextInt(2);
		Vec3 look = entity.getLookVec();
		for (int i = 0; i < count; i++) {
			EntitySilverfish silverfish = new EntitySilverfish(world);
			double angle = -Math.PI / 2.0D + random.nextDouble() * Math.PI;
			double vx = look.xCoord * 0.3D;
			double vy = look.yCoord * 0.3D * 1.5D;
			double vz = look.zCoord * 0.3D;
			silverfish.setLocationAndAngles(entity.posX, entity.posY + entity.height / 2.0F, entity.posZ, world.rand.nextFloat() * 360.0F, 0.0F);
			silverfish.motionX = vx * Math.cos(angle) + vz * Math.sin(angle);
			silverfish.motionY = vy;
			silverfish.motionZ = -vx * Math.sin(angle) + vz * Math.cos(angle);
			world.spawnEntityInWorld(silverfish);
			world.playSoundAtEntity(silverfish, "mob.silverfish.hit", 1.0F, 1.0F);
		}
	}
}
