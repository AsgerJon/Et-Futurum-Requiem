package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import cpw.mods.fml.common.eventhandler.Event;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.gamerule.DoInsomnia;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Phantoms over the sleepless (SPEC-modern-movement.md §68.3): 26.3's `PhantomSpawner` (the overworld's), its local
 * difficulty (`DifficultyInstance`, which 1.7.10 lacks), and the player's `time_since_rest` statistic. Everything is
 * off until {@link #registerStat()} (enablePhantoms and enablePhantomSpawns); ServerEventHandler calls
 * {@link #worldTick} at the end of each server world's tick and {@link #onDeath} when a player dies.
 */
public final class PhantomSpawning {

	/** 26.3's `time_since_rest`: ticks awake since the player last slept or died ("Time Since Last Rest"). */
	public static StatBase timeSinceRestStat;

	/** 26.3's `nextTick`, one for each overworld (its spawner belongs to the world). */
	private static final Map<World, int[]> NEXT_TRY = new WeakHashMap<>();

	private PhantomSpawning() {
	}

	/** At startup, before any player's statistics are read (so a saved value isn't dropped as unknown). */
	public static void registerStat() {
		if (timeSinceRestStat == null) {
			timeSinceRestStat = new StatBasic("stat.timeSinceRest", new ChatComponentTranslation("stat.timeSinceRest"),
				StatBase.timeStatType).initIndependentStat().registerStat();
		}
	}

	public static boolean isOn() {
		return timeSinceRestStat != null;
	}

	/** The player's ticks since they last slept (26.3's `time_since_rest`). */
	public static int timeSinceRest(EntityPlayerMP player) {
		return isOn() ? player.func_147099_x().writeStat(timeSinceRestStat) : 0;
	}

	public static void setTimeSinceRest(EntityPlayerMP player, int ticks) {
		if (isOn()) {
			player.func_147099_x().func_150873_a(player, timeSinceRestStat, ticks);
		}
	}

	/**
	 * The end of a server world's tick. The statistic, for each player in it: 26.3's `Player.tick` counts a tick when
	 * not asleep, `startSleeping` puts it back to 0 (here: 0 each tick asleep, the same). Then the spawner, which 26.3
	 * gives only the overworld and ticks only with `spawn_mobs` (1.7.10's doMobSpawning) on.
	 */
	public static void worldTick(WorldServer world) {
		if (!isOn()) {
			return;
		}
		for (Object entity : world.playerEntities) {
			if (entity instanceof EntityPlayerMP && !(entity instanceof FakePlayer)) {
				EntityPlayerMP player = (EntityPlayerMP) entity;
				if (!player.isPlayerSleeping()) {
					player.addStat(timeSinceRestStat, 1);
				} else if (timeSinceRest(player) != 0) {
					setTimeSinceRest(player, 0);
				}
			}
		}
		if (world.provider.dimensionId == 0 && world.getGameRules().getGameRuleBooleanValue("doMobSpawning")) {
			tick(world);
		}
	}

	/** 26.3's `die` puts the statistic back to 0. */
	public static void onDeath(EntityLivingBase entity) {
		if (entity instanceof EntityPlayerMP && !(entity instanceof FakePlayer)) {
			setTimeSinceRest((EntityPlayerMP) entity, 0);
		}
	}

	/** 26.3's `PhantomSpawner.tick`: a try every 60 to 119 s, the wait counting only while phantoms may spawn. */
	private static void tick(WorldServer world) {
		if (!maySpawn(world)) {
			return;
		}
		int[] next = NEXT_TRY.get(world);
		if (next == null) {
			NEXT_TRY.put(world, next = new int[1]);
		}
		if (--next[0] > 0) {
			return;
		}
		next[0] += (60 + world.rand.nextInt(60)) * 20;
		tryEveryPlayer(world);
	}

	/** One try for every player (26.3's spawner past its 60 to 119 s wait): how many phantoms came. For tests. */
	public static int tryOnce(WorldServer world) {
		return isOn() && maySpawn(world) ? tryEveryPlayer(world) : 0;
	}

	/** 26.3's `spawnEnemies` (not Peaceful) and the `spawn_phantoms` game rule (`doInsomnia`; a world without it: on). */
	private static boolean maySpawn(World world) {
		return world.difficultySetting != EnumDifficulty.PEACEFUL && DoInsomnia.isOn(world);
	}

	private static int tryEveryPlayer(WorldServer world) {
		boolean sky = !world.provider.hasNoSky;
		// 26.3: the sky darkened by 5 or more (night, or a storm); 1.7.10's skylightSubtracted is the same number
		if (sky && world.skylightSubtracted < 5) {
			return 0;
		}
		// 26.3's sea level is the generator's; 1.7.10's is one below its average ground level (63, a flat world's 3)
		int seaLevel = world.provider.getAverageGroundLevel() - 1;
		Random random = world.rand;
		int came = 0;
		for (Object entity : new ArrayList<Object>(world.playerEntities)) {
			if (!(entity instanceof EntityPlayerMP) || entity instanceof FakePlayer) {
				continue;
			}
			EntityPlayerMP player = (EntityPlayerMP) entity;
			int x = MathHelper.floor_double(player.posX);
			int y = MathHelper.floor_double(player.posY);
			int z = MathHelper.floor_double(player.posZ);
			if (sky && (y < seaLevel || !world.canBlockSeeTheSky(x, y, z))) {
				continue;
			}
			if (!(localDifficulty(world, x, z) > random.nextFloat() * 3.0F)) {
				continue;
			}
			int awake = MathHelper.clamp_int(timeSinceRest(player), 1, Integer.MAX_VALUE);
			if (random.nextInt(awake) < 72000) {
				continue;
			}
			int spawnY = y + 20 + random.nextInt(15);
			int spawnX = x - 10 + random.nextInt(21);
			int spawnZ = z - 10 + random.nextInt(21);
			if (!isEmptySpot(world, spawnX, spawnY, spawnZ)) {
				continue;
			}
			IEntityLivingData data = null;
			int group = 1 + random.nextInt(world.difficultySetting.getDifficultyId() + 1);
			for (int i = 0; i < group; i++) {
				EntityPhantom phantom = new EntityPhantom(world);
				phantom.setLocationAndAngles(spawnX + 0.5D, spawnY, spawnZ + 0.5D, 0.0F, 0.0F);
				float fx = (float) phantom.posX, fy = (float) phantom.posY, fz = (float) phantom.posZ;
				// 26.3 asks no spawn rule of a phantom; Forge's events still let other mods say no, as for 1.7.10's spawns
				if (ForgeEventFactory.canEntitySpawn(phantom, world, fx, fy, fz) == Event.Result.DENY) {
					continue;
				}
				if (!ForgeEventFactory.doSpecialSpawn(phantom, world, fx, fy, fz)) {
					data = phantom.onSpawnWithEgg(data);
				}
				if (world.spawnEntityInWorld(phantom)) {
					came++;
				}
			}
		}
		return came;
	}

	/** 26.3's `getCurrentDifficultyAt`: the world's day time, the chunk's inhabited time and the moon (none if unloaded). */
	public static float localDifficulty(World world, int x, int z) {
		long inhabited = 0L;
		float moon = 0.0F;
		if (world.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
			inhabited = world.getChunkFromChunkCoords(x >> 4, z >> 4).inhabitedTime;
			moon = world.getCurrentMoonPhaseFactor();
		}
		return effectiveDifficulty(world.difficultySetting, world.getWorldTime(), inhabited, moon);
	}

	/** 26.3's `DifficultyInstance.calculateDifficulty`. */
	public static float effectiveDifficulty(EnumDifficulty base, long dayTime, long inhabitedTime, float moonBrightness) {
		if (base == EnumDifficulty.PEACEFUL) {
			return 0.0F;
		}
		boolean hard = base == EnumDifficulty.HARD;
		float scale = 0.75F;
		float worldPart = MathHelper.clamp_float((dayTime - 72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
		scale += worldPart;
		float localPart = MathHelper.clamp_float(inhabitedTime / 3600000.0F, 0.0F, 1.0F) * (hard ? 1.0F : 0.75F);
		localPart += MathHelper.clamp_float(moonBrightness * 0.25F, 0.0F, worldPart);
		if (base == EnumDifficulty.EASY) {
			localPart *= 0.5F;
		}
		scale += localPart;
		return base.getDifficultyId() * scale;
	}

	/**
	 * 26.3's `NaturalSpawner.isValidEmptySpawnBlock` for a phantom: no full collision box, no power source, no fluid,
	 * no rail (`prevent_mob_spawning_inside`), nothing that burns or hurts (`isBlockDangerous`).
	 */
	private static boolean isEmptySpot(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block.getMaterial().isLiquid() || block.canProvidePower() || BlockRailBase.func_150051_a(block)) {
			return false;
		}
		AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
		if (box != null && box.minX <= x && box.minY <= y && box.minZ <= z && box.maxX >= x + 1 && box.maxY >= y + 1 && box.maxZ >= z + 1) {
			return false;
		}
		return block.getMaterial() != Material.fire && block != Blocks.cactus && block != ModBlocks.MAGMA.get()
			&& block != ModBlocks.LAVA_CAULDRON.get() && block != ModBlocks.WITHER_ROSE.get()
			&& block != ModBlocks.SWEET_BERRY_BUSH.get() && block != McFlyBlocks.POWDER_SNOW.get();
	}
}
