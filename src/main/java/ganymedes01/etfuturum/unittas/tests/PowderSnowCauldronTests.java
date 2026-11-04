package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.Freezing;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Powder snow, stage 2 (SPEC-modern-movement.md §62.7, §62.8): skeletons turning into strays, the powder snow
 * cauldron (snowfall, buckets, fire), dispensers. Each test on its own spot (x 100 on, z 160 on), cleared after.
 */
public class PowderSnowCauldronTests extends TasTestCase {

	private static final int GROUND = 4;

	private final List<int[]> boxesUsed = new ArrayList<>();
	private final List<int[]> chunksMadeCold = new ArrayList<>();

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().getGameRules().setOrCreateGameRule("mobGriefing", "false");
			Freezing.setTicksFrozen(player(), 0);
			for (int[] box : boxesUsed) {
				for (int x = box[0]; x <= box[2]; x++) {
					for (int z = box[1]; z <= box[3]; z++) {
						for (int y = GROUND + 8; y >= GROUND; y--) {
							world().setBlockToAir(x, y, z);
						}
						world().setBlock(x, GROUND - 1, z, Blocks.grass);
					}
				}
			}
			for (int[] chunk : chunksMadeCold) {
				setBiome(chunk[0], chunk[1], BiomeGenBase.plains);
			}
			for (EntityItem item : entitiesNear(EntityItem.class, 400.0D)) {
				item.setDead();
			}
		});
		boxesUsed.clear();
		chunksMadeCold.clear();
	}

	// ---------------------------------------------------------------- skeletons (§62.7)

	/**
	 * 140 ticks in powder snow, then 300 converting (shaking, the client knows), then a stray with the skeleton's name
	 * and bow. Beside it: a wither skeleton stays one; a skeleton taken out of the snow while converting stays one.
	 */
	public void testSkeletonsTurnIntoStrays() {
		int[] at = spot(0, 12);
		cell(at[0], at[1], 2);
		cell(at[0] + 3, at[1], 2);
		cell(at[0] + 6, at[1], 2);
		bringPlayer(at);
		EntitySkeleton skeleton = skeleton(at[0], at[1], 0);
		EntitySkeleton wither = skeleton(at[0] + 3, at[1], 1);
		EntitySkeleton takenOut = skeleton(at[0] + 6, at[1], 0);
		onServer(() -> {
			skeleton.setCustomNameTag("Bob");
			skeleton.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
		});
		waitUntil("the skeleton converting", () -> Freezing.isConvertingToStray(skeleton), 170);
		int id = skeleton.getEntityId();
		waitUntilOnClient("the client knowing it converts", () -> {
			Entity seen = mc().theWorld.getEntityByID(id);
			return seen != null && Freezing.isConvertingToStray(seen);
		}, 10);
		serverTicks(20);
		assertTrue(onServer(() -> Freezing.isConvertingToStray(takenOut)), "the third skeleton isn't converting");
		onServer(() -> {
			world().setBlockToAir(at[0] + 6, GROUND, at[1]);
			world().setBlockToAir(at[0] + 6, GROUND + 1, at[1]);
		});
		clearSounds();
		waitUntil("the skeleton gone", () -> skeleton.isDead, 320);
		ticks(5);
		boolean converted = false;
		for (String sound : soundsHeard()) {
			converted |= sound.endsWith("entity.skeleton.converted_to_stray");
		}
		assertTrue(converted, "no conversion sound in " + soundsHeard());
		EntityStray stray = onServer(() -> {
			for (Object loaded : world().loadedEntityList) {
				if (loaded instanceof EntityStray && !((EntityStray) loaded).isDead && ((EntityStray) loaded).getDistance(at[0] + 0.5D, GROUND, at[1] + 0.5D) < 1.5D) {
					return (EntityStray) loaded;
				}
			}
			return null;
		});
		assertIsNotNone(stray, "no stray where the skeleton was");
		assertEqual("Bob", stray.getCustomNameTag(), "the stray's name");
		assertIsNotNone(stray.getHeldItem(), "the stray holds nothing");
		assertEqual(Items.bow, stray.getHeldItem().getItem(), "the stray's held item");
		assertFalse(onServer(() -> wither.isDead), "the wither skeleton converted");
		assertFalse(onServer(() -> takenOut.isDead), "the skeleton taken out of the snow converted");
		assertFalse(onServer(() -> Freezing.isConvertingToStray(takenOut)), "the skeleton taken out still converts");
	}

	// ---------------------------------------------------------------- the cauldron (§62.8)

	/** The buckets: powder snow fills any cauldron to 3; an empty bucket takes a full one back; water replaces it. */
	public void testCauldronBuckets() {
		int[] at = spot(1, 6);
		Block cauldron = cauldron();
		survival();
		bringPlayer(at);
		onServer(() -> world().setBlock(at[0], GROUND, at[1], Blocks.cauldron, 0, 3));
		ItemStack held = use(new ItemStack(bucket()), at[0], at[1]);
		assertEqual(cauldron, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "an empty cauldron didn't take the powder snow");
		assertEqual(3, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "level");
		assertEqual(Items.bucket, held.getItem(), "the bucket after emptying it");
		held = use(new ItemStack(Items.bucket), at[0], at[1]);
		assertEqual(bucket(), held.getItem(), "the bucket after taking it back");
		assertEqual(Blocks.cauldron, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "not an empty cauldron after");
		assertEqual(0, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the empty cauldron's water");
		onServer(() -> world().setBlock(at[0], GROUND, at[1], Blocks.cauldron, 2, 3));
		use(new ItemStack(bucket()), at[0], at[1]);
		assertEqual(cauldron, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "a water cauldron didn't take the powder snow");
		use(new ItemStack(Items.water_bucket), at[0], at[1]);
		assertEqual(Blocks.cauldron, onServer(() -> world().getBlock(at[0], GROUND, at[1])), "water didn't replace it");
		assertEqual(3, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the water's level");
		onServer(() -> world().setBlock(at[0], GROUND, at[1], cauldron, 2, 3));
		held = use(new ItemStack(Items.bucket), at[0], at[1]);
		assertEqual(Items.bucket, held.getItem(), "a bucket took a cauldron that isn't full");
		assertEqual(2, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "level after an empty bucket at 2");
	}

	/** A burning player in it: a water cauldron one level lower, the fire out; in it, no freezing. */
	public void testBurningPlayerInTheCauldron() {
		int[] at = spot(2, 6);
		Block cauldron = cauldron();
		survival();
		onServer(() -> world().setBlock(at[0], GROUND, at[1], cauldron, 3, 3));
		stand(at[0] + 0.5D, GROUND + 0.25D, at[1] + 0.5D);
		serverTicks(20);
		assertEqual(0, onServer(() -> Freezing.getTicksFrozen(player())), "froze in a powder snow cauldron");
		onServer(() -> player().setFire(5));
		waitUntil("the cauldron turning to water", () -> world().getBlock(at[0], GROUND, at[1]) == Blocks.cauldron, 20);
		assertEqual(2, onServer(() -> world().getBlockMetadata(at[0], GROUND, at[1])), "the water's level");
		assertFalse(onServer(() -> player().isBurning()), "still burning");
	}

	/** The comparator reads its level; broken, it drops a cauldron; it has no item of its own. */
	public void testCauldronComparatorAndDrop() {
		int[] at = spot(3, 6);
		Block cauldron = cauldron();
		onServer(() -> world().setBlock(at[0], GROUND, at[1], cauldron, 2, 3));
		assertTrue(onServer(() -> cauldron.hasComparatorInputOverride()), "no comparator output");
		assertEqual(2, onServer(() -> cauldron.getComparatorInputOverride(world(), at[0], GROUND, at[1], 0)), "comparator");
		List<ItemStack> drops = onServer(() -> cauldron.getDrops(world(), at[0], GROUND, at[1], 2, 0));
		assertEqual(1, drops.size(), "drops");
		assertEqual(Items.cauldron, drops.get(0).getItem(), "the drop");
		assertIsNone(Item.getItemFromBlock(cauldron));
	}

	/**
	 * Snowfall: cauldrons in a cold biome under the open sky fill with powder snow (26.3's 10% at a precipitation tick
	 * once in 48, 1.7.10's ticks once in 16), never with water; in a warm one with water, never with powder snow. Four
	 * cold chunks and two warm ones full of cauldrons, so it doesn't take long.
	 */
	public void testCauldronsFillInSnowfall() {
		Block cauldron = cauldron();
		// chunks 14..15 x 12..13 cold, 16 x 12..13 warm
		int coldX = 224, coldZ = 192, warmX = 256;
		boxesUsed.add(new int[]{coldX, coldZ, coldX + 31, coldZ + 31});
		boxesUsed.add(new int[]{warmX, coldZ, warmX + 15, coldZ + 31});
		onServer(() -> {
			for (int cx = 14; cx <= 15; cx++) {
				for (int cz = 12; cz <= 13; cz++) {
					setBiome(cx, cz, BiomeGenBase.icePlains);
					chunksMadeCold.add(new int[]{cx, cz});
				}
			}
			setBiome(16, 12, BiomeGenBase.plains);
			setBiome(16, 13, BiomeGenBase.plains);
			for (int x = coldX; x < warmX + 16; x++) {
				for (int z = coldZ; z < coldZ + 32; z++) {
					world().setBlock(x, GROUND, z, Blocks.cauldron, 0, 2);
				}
			}
		});
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(warmX + 0.5D, GROUND + 2, coldZ + 16.5D, 0.0F, 0.0F));
		ticks(20);
		onServer(() -> {
			world().getWorldInfo().setRaining(true);
			world().getWorldInfo().setRainTime(1000000);
			world().setRainStrength(1.0F);
		});
		waitUntil("powder snow in a cold cauldron", () -> count(coldX, coldZ, 32, cauldron, -1) > 0, 1500);
		waitUntil("water in a warm cauldron", () -> count(warmX, coldZ, 16, Blocks.cauldron, 1) > 0, 1500);
		assertEqual(0, onServer(() -> count(coldX, coldZ, 32, Blocks.cauldron, 1)), "water in a cold cauldron");
		assertEqual(0, onServer(() -> count(warmX, coldZ, 16, cauldron, -1)), "powder snow in a warm cauldron");
	}

	// ---------------------------------------------------------------- dispensers (§62.8)

	/** A dispenser places powder snow from the bucket (an empty bucket back in it) and takes it up with an empty one. */
	public void testDispensers() {
		int[] at = spot(4, 6);
		Block snow = snow();
		// facing east (meta 5): the block at x + 1; set after placing, since 1.7.10's dispenser picks its own facing then
		onServer(() -> {
			world().setBlock(at[0], GROUND, at[1], Blocks.dispenser, 5, 3);
			world().setBlockMetadataWithNotify(at[0], GROUND, at[1], 5, 2);
			((TileEntityDispenser) world().getTileEntity(at[0], GROUND, at[1])).setInventorySlotContents(0, new ItemStack(bucket()));
			world().setBlock(at[0], GROUND, at[1] + 2, Blocks.dispenser, 5, 3);
			world().setBlockMetadataWithNotify(at[0], GROUND, at[1] + 2, 5, 2);
			((TileEntityDispenser) world().getTileEntity(at[0], GROUND, at[1] + 2)).setInventorySlotContents(0, new ItemStack(Items.bucket));
			world().setBlock(at[0] + 1, GROUND, at[1] + 2, snow);
		});
		pulse(at[0] - 1, at[1]);
		pulse(at[0] - 1, at[1] + 2);
		waitUntil("powder snow placed", () -> world().getBlock(at[0] + 1, GROUND, at[1]) == snow, 20);
		waitUntil("powder snow taken", () -> world().getBlock(at[0] + 1, GROUND, at[1] + 2) == Blocks.air, 20);
		assertEqual(Items.bucket, onServer(() -> heldBy(at[0], at[1])), "the first dispenser's bucket after");
		assertEqual(bucket(), onServer(() -> heldBy(at[0], at[1] + 2)), "the second dispenser's bucket after");
	}

	// ---------------------------------------------------------------- helpers

	private static Block snow() {
		Block block = GameRegistry.findBlock(Reference.MOD_ID, "powder_snow");
		assertIsNotNone(block, "EFR has no powder snow block");
		return block;
	}

	private static Block cauldron() {
		Block block = GameRegistry.findBlock(Reference.MOD_ID, "powder_snow_cauldron");
		assertIsNotNone(block, "EFR has no powder snow cauldron");
		return block;
	}

	private static Item bucket() {
		Item item = GameRegistry.findItem(Reference.MOD_ID, "powder_snow_bucket");
		assertIsNotNone(item, "EFR has no powder snow bucket");
		return item;
	}

	/** Spot n, `size` blocks around it cleared after. */
	private int[] spot(int n, int size) {
		int[] at = {100 + n * 16, 160};
		boxesUsed.add(new int[]{at[0] - size, at[1] - size, at[0] + size, at[1] + size});
		return at;
	}

	/** A glass cell (1 x 1, 4 high) with powder snow `height` high at its bottom. */
	private void cell(int x, int z, int height) {
		Block snow = snow();
		onServer(() -> {
			for (int y = GROUND; y < GROUND + height + 4; y++) {
				world().setBlock(x + 1, y, z, Blocks.glass);
				world().setBlock(x - 1, y, z, Blocks.glass);
				world().setBlock(x, y, z + 1, Blocks.glass);
				world().setBlock(x, y, z - 1, Blocks.glass);
			}
			for (int y = GROUND; y < GROUND + height; y++) {
				world().setBlock(x, y, z, snow);
			}
		});
	}

	private EntitySkeleton skeleton(int x, int z, int type) {
		return spawn(world -> {
			EntitySkeleton made = new EntitySkeleton(world);
			made.setSkeletonType(type);
			return made;
		}, x + 0.5D, GROUND, z + 0.5D);
	}

	private void bringPlayer(int[] at) {
		stand(at[0] + 0.5D, GROUND, at[1] - 5.5D);
	}

	private void stand(double x, double y, double z) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, y, z, 0.0F, 0.0F));
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

	private static void setBiome(int chunkX, int chunkZ, BiomeGenBase biome) {
		Chunk chunk = world().getChunkFromChunkCoords(chunkX, chunkZ);
		byte[] biomes = chunk.getBiomeArray();
		for (int i = 0; i < biomes.length; i++) {
			biomes[i] = (byte) biome.biomeID;
		}
		chunk.setChunkModified();
	}

	/** Blocks of the kind in the square at ground level; meta -1 any, else at least it. */
	private static int count(int x0, int z0, int size, Block block, int minMeta) {
		int found = 0;
		for (int x = x0; x < x0 + size; x++) {
			for (int z = z0; z < z0 + 32; z++) {
				if (world().getBlock(x, GROUND, z) == block && (minMeta < 0 || world().getBlockMetadata(x, GROUND, z) >= minMeta)) {
					found++;
				}
			}
		}
		return found;
	}

	/** A redstone block beside, then gone: one pulse. */
	private void pulse(int x, int z) {
		onServer(() -> world().setBlock(x, GROUND, z, Blocks.redstone_block));
		ticks(4);
		onServer(() -> world().setBlockToAir(x, GROUND, z));
	}

	private static Item heldBy(int x, int z) {
		TileEntityDispenser dispenser = (TileEntityDispenser) world().getTileEntity(x, GROUND, z);
		for (int slot = 0; slot < dispenser.getSizeInventory(); slot++) {
			ItemStack stack = dispenser.getStackInSlot(slot);
			if (stack != null) {
				return stack.getItem();
			}
		}
		return null;
	}
}
