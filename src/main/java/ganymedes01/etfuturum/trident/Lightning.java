package ganymedes01.etfuturum.trident;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.ModEnchantments;
import io.github.asgerjon.mcfly.copper.LightningRodBlock;
import io.github.asgerjon.mcfly.copper.WeatheringCopper;
import ganymedes01.etfuturum.world.LightningRodRecord;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lightning as 26.3 has it (SPEC-modern-movement.md §58.3): Channeling's bolt, what a bolt does to the block it lands on
 * (a lightning rod powered, copper cleaned), where natural lightning strikes, and the skeleton horse trap it can leave.
 */
public final class Lightning {

	private static final String THUNDER_SOUND = McFly.MC_ASSETS + ":item.trident.thunder";
	/** 26.3's level event for electric sparks: data an axis (0 x, 1 y, 2 z) along a block, or -1 on all its faces. */
	public static final int SPARKS_EVENT = 3002;
	/** How far natural lightning looks for a lightning rod. */
	public static final int ROD_RANGE = 128;

	/** Set while a visual-only bolt is made, whose constructor would otherwise light fire (1.7.10's does, first thing). */
	private static boolean makingAVisualBolt;

	private Lightning() {
	}

	/** A thrown trident damaged the entity: with Channeling, in a thunderstorm, the entity under the open sky: a bolt. */
	public static void channel(ItemStack trident, Entity hit) {
		World world = hit.worldObj;
		if (hasChanneling(trident) && world.isThundering()
			&& world.canBlockSeeTheSky(MathHelper.floor_double(hit.posX), MathHelper.floor_double(hit.posY), MathHelper.floor_double(hit.posZ))) {
			strike(world, hit.posX, hit.posY, hit.posZ);
		}
	}

	/** A thrown trident hit this block: with Channeling, a lightning rod under the open sky, in a thunderstorm: a bolt. */
	public static void channelAtBlock(ItemStack trident, World world, int x, int y, int z, double hitX, double hitY, double hitZ) {
		if (hasChanneling(trident) && world.isThundering() && world.getBlock(x, y, z) instanceof LightningRodBlock
			&& world.canBlockSeeTheSky(x, y + 1, z)) {
			strike(world, hitX, hitY, hitZ);
		}
	}

	private static boolean hasChanneling(ItemStack trident) {
		return ModEnchantments.channeling != null && trident != null
			&& EnchantmentHelper.getEnchantmentLevel(ModEnchantments.channeling.effectId, trident) > 0;
	}

	/** Channeling's bolt, with its thunder (volume 5) where it strikes. */
	private static void strike(World world, double x, double y, double z) {
		world.addWeatherEffect(new EntityLightningBolt(world, x, y, z));
		world.playSoundEffect(x, y, z, THUNDER_SOUND, 5.0F, 1.0F);
	}

	/**
	 * The bolt's first tick on the server, on the block it lands on (the one just under it), visual-only bolts too (as
	 * 26.3's): a lightning rod is powered; weathering copper (a rod too) goes back to unweathered, then 3 to 5 random
	 * walks of 1 to 8 steps each clean a stage off the copper they reach, with sparks (waxed copper isn't cleaned but
	 * still starts the walks).
	 */
	public static void landed(EntityLightningBolt bolt) {
		World world = bolt.worldObj;
		int x = MathHelper.floor_double(bolt.posX);
		int y = MathHelper.floor_double(bolt.posY - 1.0E-6D);
		int z = MathHelper.floor_double(bolt.posZ);
		Block block = world.getBlock(x, y, z);
		if (block instanceof LightningRodBlock rod) {
			rod.struck(world, x, y, z);
		}
		if (!(block instanceof WeatheringCopper copper) || copper.stage(world, x, y, z) < 0) {
			return;
		}
		if (!copper.waxed(world, x, y, z) && copper.stage(world, x, y, z) != 0) {
			copper.become(world, x, y, z, 0, false);
		}
		Random random = world.rand;
		int walks = random.nextInt(3) + 3;
		for (int walk = 0; walk < walks; walk++) {
			int[] at = {x, y, z};
			int steps = random.nextInt(8) + 1;
			for (int step = 0; step < steps; step++) {
				if (!cleanAStep(world, at, random)) {
					break;
				}
			}
		}
	}

	/** One step of a walk: 10 tries at a block in the 3 x 3 x 3 around; the first weathering copper loses a stage. */
	private static boolean cleanAStep(World world, int[] at, Random random) {
		for (int attempt = 0; attempt < 10; attempt++) {
			int x = at[0] + random.nextInt(3) - 1;
			int y = at[1] + random.nextInt(3) - 1;
			int z = at[2] + random.nextInt(3) - 1;
			Block block = world.getBlock(x, y, z);
			if (block instanceof WeatheringCopper copper && copper.stage(world, x, y, z) >= 0) {
				if (!copper.waxed(world, x, y, z)) {
					int stage = copper.stage(world, x, y, z);
					if (stage != 0) {
						copper.become(world, x, y, z, stage - 1, false);
					}
					world.playAuxSFX(SPARKS_EVENT, x, y, z, -1);
					at[0] = x;
					at[1] = y;
					at[2] = z;
					return true;
				}
			}
		}
		return false;
	}

	/** Whether there is nothing but air above x y z: the block is the highest of its column (26.3's world-surface height). */
	public static boolean isHighestInColumn(World world, int x, int y, int z) {
		Chunk chunk = world.getChunkFromBlockCoords(x, z);
		int top = chunk.getTopFilledSegment() + 16;
		for (int above = y + 1; above < top; above++) {
			if (!world.isAirBlock(x, above, z)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Where natural lightning aimed at this column strikes (26.3's findLightningTargetAround): x, y, z of the block it
	 * lands in. Above the closest lightning rod within 128 blocks that is the highest of its column; else at a random
	 * living thing that sees the sky within 3 blocks of the column; else on the surface (what stops rain).
	 */
	public static int[] naturalTarget(WorldServer world, int x, int z) {
		int y = world.getPrecipitationHeight(x, z);
		ChunkCoordinates rod = LightningRodRecord.get(world).closestOnTop(world, x, y, z, ROD_RANGE);
		if (rod != null) {
			return new int[] {rod.posX, rod.posY + 1, rod.posZ};
		}
		AxisAlignedBB around = AxisAlignedBB.getBoundingBox(x - 3, y - 3, z - 3, x + 4, world.getHeight() + 4, z + 4);
		List<EntityLivingBase> living = new ArrayList<>();
		for (Object found : world.getEntitiesWithinAABB(EntityLivingBase.class, around)) {
			EntityLivingBase entity = (EntityLivingBase) found;
			if (entity.isEntityAlive() && world.canBlockSeeTheSky(MathHelper.floor_double(entity.posX),
				MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ))) {
				living.add(entity);
			}
		}
		if (!living.isEmpty()) {
			EntityLivingBase entity = living.get(world.rand.nextInt(living.size()));
			return new int[] {MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ)};
		}
		// a column with nothing in it
		return new int[] {x, y == -1 ? 1 : y, z};
	}

	/**
	 * Natural lightning aimed at this column, after 1.7.10's 1 in 100 000 chance (26.3's tickThunder): only where it
	 * rains at the target. Now and then (1 % times the local difficulty, never on a lightning rod, and only where mobs
	 * spawn) it is harmless and leaves a skeleton horse trap.
	 */
	public static void natural(WorldServer world, int x, int z) {
		int[] at = naturalTarget(world, x, z);
		if (!world.canLightningStrikeAt(at[0], at[1], at[2])) {
			return;
		}
		boolean trap = world.getGameRules().getGameRuleBooleanValue("doMobSpawning")
			&& world.rand.nextDouble() < effectiveDifficulty(world, at[0], at[1], at[2]) * 0.01D
			&& !(world.getBlock(at[0], at[1] - 1, at[2]) instanceof LightningRodBlock);
		if (trap) {
			EntityHorse horse = new EntityHorse(world);
			horse.setHorseType(4);
			SkeletonTrap.set(horse, true);
			horse.setGrowingAge(0);
			horse.setPosition(at[0], at[1], at[2]);
			world.spawnEntityInWorld(horse);
		}
		world.addWeatherEffect(trap ? visualBolt(world, at[0] + 0.5D, at[1], at[2] + 0.5D) : new EntityLightningBolt(world, at[0] + 0.5D, at[1], at[2] + 0.5D));
	}

	/** A bolt that only shows (and sounds): it lights no fire and strikes no one, though it still lands (26.3's visualOnly). */
	public static EntityLightningBolt visualBolt(World world, double x, double y, double z) {
		makingAVisualBolt = true;
		try {
			return new EntityLightningBolt(world, x, y, z);
		} finally {
			makingAVisualBolt = false;
		}
	}

	public static boolean isMakingAVisualBolt() {
		return makingAVisualBolt;
	}

	public static boolean isVisualOnly(EntityLightningBolt bolt) {
		return ((IVisualBolt) bolt).etfu$isVisualOnly();
	}

	/**
	 * 26.3's local difficulty (DifficultyInstance's effective difficulty): 0 on Peaceful, else the difficulty's number
	 * times 0.75 and up, more as the world ages (the day count), the chunk is lived in and the moon is full.
	 */
	public static float effectiveDifficulty(World world, int x, int y, int z) {
		EnumDifficulty difficulty = world.difficultySetting;
		if (difficulty == EnumDifficulty.PEACEFUL) {
			return 0.0F;
		}
		long inhabited = 0L;
		float moon = 0.0F;
		if (world.blockExists(x, y, z)) {
			inhabited = world.getChunkFromBlockCoords(x, z).inhabitedTime;
			moon = world.getCurrentMoonPhaseFactor();
		}
		float global = MathHelper.clamp_float((world.getWorldTime() - 72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
		float local = MathHelper.clamp_float(inhabited / 3600000.0F, 0.0F, 1.0F) * (difficulty == EnumDifficulty.HARD ? 1.0F : 0.75F);
		local += MathHelper.clamp_float(moon * 0.25F, 0.0F, global);
		if (difficulty == EnumDifficulty.EASY) {
			local *= 0.5F;
		}
		return difficulty.getDifficultyId() * (0.75F + global + local);
	}
}
