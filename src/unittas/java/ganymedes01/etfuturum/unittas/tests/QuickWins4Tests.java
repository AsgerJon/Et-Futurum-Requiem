package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockMagma;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.Freezing;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Quick wins batch 4 (SPEC-modern-movement.md §63): buckets and cauldrons, filling in creative, strays and the sky,
 * the weather's pace, animals panicking from harm, the cauldron's shape. The looks are the comparison's views. Each
 * test on its own spot (x 100 on, z 240 on), cleared after.
 */
@TasSuite("quick4")
public class QuickWins4Tests extends TasTestCase {

	private static final int GROUND = 4;

	private final List<int[]> boxes = new ArrayList<>();
	private final List<int[]> chunksChanged = new ArrayList<>();

	@Override
	protected int timeoutSeconds() {
		return 150;
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setWorldTime(6000);
			for (int[] box : boxes) {
				for (int x = box[0]; x <= box[2]; x++) {
					for (int z = box[1]; z <= box[3]; z++) {
						for (int y = GROUND + 8; y >= GROUND; y--) {
							world().setBlockToAir(x, y, z);
						}
						world().setBlock(x, GROUND - 1, z, Blocks.grass);
					}
				}
			}
			for (int[] chunk : chunksChanged) {
				setBiome(chunk[0], chunk[1], BiomeGenBase.plains);
			}
			for (EntityItem item : entitiesNear(EntityItem.class, 400.0D)) {
				item.setDead();
			}
		});
		boxes.clear();
		chunksChanged.clear();
	}

	// ---------------------------------------------------------------- buckets and cauldrons (§63.1)

	/** The lava cauldron: an empty bucket takes the lava; a water bucket makes it a full water cauldron. */
	public void testLavaCauldronBuckets() {
		int[] at = spot(0, 4);
		Block lava = lavaCauldron();
		survival();
		bringPlayer(at);
		onServer(() -> world().setBlock(at[0], GROUND, at[1], lava, 0, 3));
		ItemStack held = use(new ItemStack(Items.bucket), at[0], at[1]);
		assertEqual(Items.lava_bucket, held.getItem(), "the bucket from a lava cauldron");
		assertEqual(Blocks.cauldron, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "after taking the lava");
		assertEqual(0, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the empty cauldron's level");
		onServer(() -> world().setBlock(at[0], GROUND, at[1], lava, 0, 3));
		held = use(new ItemStack(Items.water_bucket), at[0], at[1]);
		assertEqual(Blocks.cauldron, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "water didn't replace the lava");
		assertEqual(3, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the water's level");
		assertEqual(Items.bucket, held.getItem(), "the bucket after pouring the water");
	}

	/** A full water cauldron gives a water bucket (not at 2); a lava bucket replaces water at any level. */
	public void testWaterCauldronBuckets() {
		int[] at = spot(1, 4);
		Block lava = lavaCauldron();
		survival();
		bringPlayer(at);
		onServer(() -> world().setBlock(at[0], GROUND, at[1], Blocks.cauldron, 3, 3));
		ItemStack held = use(new ItemStack(Items.bucket), at[0], at[1]);
		assertEqual(Items.water_bucket, held.getItem(), "the bucket from a full water cauldron");
		assertEqual(0, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the cauldron after");
		onServer(() -> world().setBlock(at[0], GROUND, at[1], Blocks.cauldron, 2, 3));
		held = use(new ItemStack(Items.bucket), at[0], at[1]);
		assertEqual(Items.bucket, held.getItem(), "a bucket from a cauldron at 2");
		assertEqual(2, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the cauldron at 2 after");
		use(new ItemStack(Items.lava_bucket), at[0], at[1]);
		assertEqual(lava, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "lava didn't replace the water at 2");
	}

	/** Creative: filling keeps the empty container and adds the filled one, once (a bucket, a bottle, milk). */
	public void testCreativeFilling() {
		int[] at = spot(2, 6);
		bringPlayer(at);
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.clearInventory(null, -1);
			world().setBlock(at[0], GROUND - 1, at[1] + 1, Blocks.water);
			world().setBlock(at[0] + 2, GROUND, at[1] + 1, Blocks.cauldron, 3, 3);
		});
		// looking straight down at the water from above it
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(at[0] + 0.5D, GROUND + 1, at[1] + 1.5D, 0.0F, 90.0F));
		ticks(3);
		ItemStack held = rightClickAir(new ItemStack(Items.bucket));
		assertEqual(Items.bucket, held.getItem(), "the empty bucket in creative");
		assertEqual(1, count(Items.water_bucket), "water buckets after the first");
		// a bucket takes the source in creative too (1.7.10 and 26.3): water again for each
		onServer(() -> world().setBlock(at[0], GROUND - 1, at[1] + 1, Blocks.water));
		rightClickAir(new ItemStack(Items.bucket));
		assertEqual(1, count(Items.water_bucket), "water buckets after the second (one is there)");
		onServer(() -> world().setBlock(at[0], GROUND - 1, at[1] + 1, Blocks.water));
		held = rightClickAir(new ItemStack(Items.glass_bottle, 3));
		assertEqual(3, held.stackSize, "the bottles in creative");
		assertEqual(1, count(Items.potionitem), "water bottles");
		ItemStack fromCauldron = use(new ItemStack(Items.glass_bottle, 3), at[0] + 2, at[1] + 1);
		assertEqual(3, fromCauldron.stackSize, "the bottles from a cauldron in creative");
		assertEqual(Items.bucket, onServer(() -> {
			EntityCow cow = new EntityCow(world());
			cow.setPosition(at[0] + 0.5D, GROUND, at[1] + 3.5D);
			world().spawnEntityInWorld(cow);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.bucket));
			player().interactWith(cow);
			return player().getHeldItem().getItem();
		}), "the bucket after milking in creative");
		assertEqual(1, count(Items.milk_bucket), "milk buckets");
	}

	// ---------------------------------------------------------------- strays (§63.2)

	/** A stray spawns only where it sees the sky (26.3: full sky light, looked for above any powder snow), or from a spawner. */
	public void testStraysNeedTheSky() {
		int[] at = spot(3, 8);
		bringPlayer(at);
		onServer(() -> {
			world().setWorldTime(18000);
			// a dark cave: stone round and over a 3 x 3
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					for (int y = GROUND; y <= GROUND + 3; y++) {
						boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2 || y == GROUND + 3;
						if (wall) {
							world().setBlock(at[0] + dx, y, at[1] + dz, Blocks.stone);
						}
					}
				}
			}
		});
		// the night's sky light comes with the next tick
		serverTicks(3);
		int inCave = onServer(() -> spawnTries(at[0] + 0.5D, at[1] + 0.5D));
		int openSky = onServer(() -> spawnTries(at[0] - 5.5D, at[1] + 0.5D));
		assertEqual(0, inCave, "strays that could spawn in the dark cave");
		assertTrue(openSky > 0, "no stray could spawn under the open sky at night");
		assertTrue(onServer(() -> {
			ganymedes01.etfuturum.entities.EntityStray stray = new EntityStray(world());
			stray.setLocationAndAngles(at[0] + 0.5D, GROUND, at[1] + 0.5D, 0.0F, 0.0F);
			return ganymedes01.etfuturum.entities.StraySpawning.fromSpawner(stray::getCanSpawnHere);
		}), "a spawner's stray can't spawn in the dark cave");
	}

	/** 26.3's lists: Ice Plains strays 80 and skeletons 20; Cold Taiga no strays, skeletons 100. */
	public void testStraySpawnLists() {
		int[] icePlains = weights(BiomeGenBase.icePlains);
		int[] coldTaiga = weights(BiomeGenBase.coldTaiga);
		assertEqual(80, icePlains[0], "Ice Plains' stray weight");
		assertEqual(20, icePlains[1], "Ice Plains' skeleton weight");
		assertEqual(0, coldTaiga[0], "Cold Taiga's stray weight");
		assertEqual(100, coldTaiga[1], "Cold Taiga's skeleton weight");
	}

	// ---------------------------------------------------------------- the weather's pace (§63.3)

	/**
	 * Precipitation once in 48 ticks a chunk (26.3), not 1.7.10's 16: 16 chunks of empty cauldrons in rain for 1200 ticks
	 * fill 1200 * 16 / 48 / 20 = 20 times on average (60 at 1.7.10's pace); 38 splits the two.
	 */
	public void testPrecipitationPace() {
		int x0 = 288, z0 = 288;
		boxes.add(new int[]{x0, z0, x0 + 63, z0 + 63});
		onServer(() -> {
			for (int x = x0; x < x0 + 64; x++) {
				for (int z = z0; z < z0 + 64; z++) {
					world().setBlock(x, GROUND, z, Blocks.cauldron, 0, 2);
				}
			}
		});
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x0 + 32.5D, GROUND + 2, z0 + 32.5D, 0.0F, 0.0F));
		ticks(20);
		onServer(() -> {
			world().getWorldInfo().setRaining(true);
			world().getWorldInfo().setRainTime(1000000);
			world().setRainStrength(1.0F);
		});
		serverTicks(1200);
		int fills = onServer(() -> {
			int sum = 0;
			for (int x = x0; x < x0 + 64; x++) {
				for (int z = z0; z < z0 + 64; z++) {
					if (world().getBlock(x, GROUND, z) == Blocks.cauldron) {
						sum += world().getBlockMetadata(x, GROUND, z);
					}
				}
			}
			return sum;
		});
		assertTrue(fills < 38, "the cauldrons filled " + fills + " times: 1.7.10's pace");
		assertTrue(fills > 6, "the cauldrons filled " + fills + " times: hardly at all");
	}

	// ---------------------------------------------------------------- panic (§63.4)

	/** A cow panics from cactus, lava, magma, freezing, magic, an explosion; not from a fall or drowning. */
	public void testCowsPanicFromHarm() {
		int[] at = spot(4, 14);
		bringPlayer(at);
		Object[][] causes = {{DamageSource.cactus, true}, {DamageSource.lava, true}, {BlockMagma.HOT_FLOOR, true},
				{Freezing.FREEZE, true}, {DamageSource.magic, true}, {DamageSource.setExplosionSource(null), true},
				{DamageSource.fall, false}, {DamageSource.drown, false}};
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < causes.length; i++) {
			DamageSource source = (DamageSource) causes[i][0];
			boolean shouldPanic = (Boolean) causes[i][1];
			int x = at[0] - 6 + i % 4 * 4;
			int z = at[1] - 4 + i / 4 * 6;
			EntityCow cow = spawn(EntityCow::new, x + 0.5D, GROUND, z + 0.5D);
			ticks(3);
			onServer(() -> cow.attackEntityFrom(source, 1.0F));
			// a panic run to a near spot ends early, so any tick of the next 12 counts
			boolean panics = false;
			for (int tick = 0; tick < 12 && !panics; tick++) {
				serverTicks(1);
				panics = onServer(() -> panicking(cow));
			}
			if (panics != shouldPanic) {
				wrong.add(source.damageType + (shouldPanic ? " didn't panic" : " panicked"));
			}
			onServer(cow::setDead);
		}
		assertEqual(new ArrayList<String>(), wrong);
	}

	/** A burning cow with water 3 blocks away heads for the water (26.3 lookForWater). */
	public void testBurningCowRunsToWater() {
		int[] at = spot(5, 6);
		bringPlayer(at);
		onServer(() -> world().setBlock(at[0] + 3, GROUND - 1, at[1], Blocks.water));
		EntityCow cow = spawn(EntityCow::new, at[0] + 0.5D, GROUND, at[1] + 0.5D);
		ticks(3);
		onServer(() -> cow.setFire(8));
		waitUntil("the cow panicking", () -> panicking(cow), 40);
		int[] end = onServer(() -> {
			PathEntity path = cow.getNavigator().getPath();
			if (path == null) {
				return null;
			}
			PathPoint last = path.getFinalPathPoint();
			return new int[]{last.xCoord, last.zCoord};
		});
		assertIsNotNone(end, "the panicking cow has no path");
		assertTrue(Math.abs(end[0] - (at[0] + 3)) <= 1 && Math.abs(end[1] - at[1]) <= 1, "the cow runs to " + end[0] + ", " + end[1] + ", not the water");
	}

	// ---------------------------------------------------------------- the cauldron's shape (§63.5)

	/** 26.3's shape: inside, stood on at 4/16; a gap under the middle between the legs; the legs at the corners. */
	public void testCauldronShape() {
		int[] at = spot(6, 2);
		onServer(() -> world().setBlock(at[0], GROUND, at[1], Blocks.cauldron, 0, 3));
		double x = at[0], y = GROUND, z = at[1];
		assertAlmostEqual(y + 0.25D, onServer(() -> topUnder(x + 0.5D, y + 0.9D, z + 0.5D)), 1.0E-6D);
		assertFalse(onServer(() -> collides(AxisAlignedBB.getBoundingBox(x + 0.45D, y + 0.05D, z + 0.45D, x + 0.55D, y + 0.15D, z + 0.55D))),
				"no gap under the middle");
		assertTrue(onServer(() -> collides(AxisAlignedBB.getBoundingBox(x + 0.02D, y + 0.05D, z + 0.02D, x + 0.08D, y + 0.15D, z + 0.08D))),
				"no leg at the corner");
	}

	// ---------------------------------------------------------------- helpers

	private static Block lavaCauldron() {
		assertTrue(ModBlocks.LAVA_CAULDRON.isEnabled(), "lava cauldrons are off");
		return ModBlocks.LAVA_CAULDRON.get();
	}

	private int[] spot(int n, int size) {
		int[] at = {100 + n * 20, 240};
		boxes.add(new int[]{at[0] - size, at[1] - size, at[0] + size, at[1] + size});
		return at;
	}

	private void bringPlayer(int[] at) {
		double x = at[0] + 0.5D, z = at[1] - 7.5D;
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, GROUND, z, 0.0F, 0.0F));
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - x) < 0.01D && Math.abs(mc().thePlayer.posZ - z) < 0.01D
				&& mc().theWorld.getChunkFromBlockCoords((int) Math.floor(x), (int) Math.floor(z)).isChunkLoaded, 60);
		ticks(2);
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	/** A right click on the block's top with the stack, as the server takes one (block first, then the item). */
	private ItemStack use(ItemStack stack, int x, int z) {
		return onServer(() -> {
			player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), stack, x, GROUND, z, 1, 0.5F, 1.0F, 0.5F);
			return player().getHeldItem() == null ? new ItemStack(Blocks.air) : player().getHeldItem();
		});
	}

	/** The item's right click in the air (a bucket or bottle at what the player looks at). */
	private ItemStack rightClickAir(ItemStack stack) {
		return onServer(() -> {
			player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
			ItemStack result = stack.useItemRightClick(world(), player());
			player().inventory.setInventorySlotContents(player().inventory.currentItem, result);
			return result;
		});
	}

	private int count(Item item) {
		return onServer(() -> {
			int found = 0;
			for (ItemStack stack : player().inventory.mainInventory) {
				if (stack != null && stack.getItem() == item) {
					found += stack.stackSize;
				}
			}
			return found;
		});
	}

	private static void setBiome(int chunkX, int chunkZ, BiomeGenBase biome) {
		Chunk chunk = world().getChunkFromChunkCoords(chunkX, chunkZ);
		byte[] biomes = chunk.getBiomeArray();
		for (int i = 0; i < biomes.length; i++) {
			biomes[i] = (byte) biome.biomeID;
		}
		chunk.setChunkModified();
	}

	/** Of 60 tries (the light rule is random), how many times a stray could spawn standing there. */
	private static int spawnTries(double x, double z) {
		int can = 0;
		for (int i = 0; i < 60; i++) {
			EntityStray stray = new EntityStray(world());
			stray.setLocationAndAngles(x, GROUND, z, 0.0F, 0.0F);
			if (stray.getCanSpawnHere()) {
				can++;
			}
		}
		return can;
	}

	/** A biome's monster weights: {stray, skeleton (not a stray)}. */
	@SuppressWarnings("unchecked")
	private static int[] weights(BiomeGenBase biome) {
		int[] found = new int[2];
		for (BiomeGenBase.SpawnListEntry entry : (List<BiomeGenBase.SpawnListEntry>) biome.getSpawnableList(EnumCreatureType.monster)) {
			if (entry.entityClass == EntityStray.class) {
				found[0] += entry.itemWeight;
			} else if (entry.entityClass == EntitySkeleton.class) {
				found[1] += entry.itemWeight;
			}
		}
		return found;
	}

	/** Whether the mob's panic AI is running (1.7.10's EntityAITasks keeps the running ones apart). */
	@SuppressWarnings("unchecked")
	private static boolean panicking(EntityCow cow) throws Exception {
		Field running = null;
		for (String name : new String[]{"executingTaskEntries", "field_75780_b"}) {
			try {
				running = EntityAITasks.class.getDeclaredField(name);
				break;
			} catch (NoSuchFieldException ignored) {
			}
		}
		if (running == null) {
			fail("1.7.10's EntityAITasks has no executingTaskEntries");
		}
		running.setAccessible(true);
		for (Object entry : (List<Object>) running.get(cow.tasks)) {
			if (((EntityAITasks.EntityAITaskEntry) entry).action instanceof EntityAIPanic) {
				return true;
			}
		}
		return false;
	}

	/** The highest collision top under the point within the block (an entity's feet there). */
	@SuppressWarnings("unchecked")
	private static double topUnder(double x, double y, double z) {
		List<AxisAlignedBB> found = new ArrayList<>();
		AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(x - 0.1D, Math.floor(y), z - 0.1D, x + 0.1D, y, z + 0.1D);
		Blocks.cauldron.addCollisionBoxesToList(world(), (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z), probe, found, null);
		double top = Double.NEGATIVE_INFINITY;
		for (AxisAlignedBB box : found) {
			top = Math.max(top, box.maxY);
		}
		return top;
	}

	@SuppressWarnings("unchecked")
	private static boolean collides(AxisAlignedBB probe) {
		List<AxisAlignedBB> found = new ArrayList<>();
		int x = (int) Math.floor(probe.minX), y = (int) Math.floor(probe.minY), z = (int) Math.floor(probe.minZ);
		Blocks.cauldron.addCollisionBoxesToList(world(), x, y, z, probe, found, null);
		return !found.isEmpty();
	}
}
