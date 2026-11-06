package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Bamboo, the plant (SPEC-modern-movement.md 91.11, step I), as 26.3's `BambooStalkBlock` and `BambooSaplingBlock` in
 * EFR's ids and metas (the stalk's bit 0 age, bits 1 and 2 leaves, bit 3 stage; the shoot's 0): placing by the item,
 * held by `#supports_bamboo`, growth by random ticks and bonemeal (the leaves' pattern, the thickening), the shoot
 * turning into a stalk, shapes, properties, swords, drops. At z 740, y 8. The random ticks get a rigged random (always
 * the 1-in-3, never the early stop), so growth is exact.
 */
@TasSuite("woods")
public class BambooTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 740;
	private static final String MODERN = McFly.MC_ASSETS + ":";

	/** 26.3's rolls all taken: `nextInt` 0, `nextFloat` 0.9 (no early end to growing). */
	private static final Random RIGGED = new Random() {
		@Override
		public int nextInt(int bound) {
			return 0;
		}

		@Override
		public float nextFloat() {
			return 0.9F;
		}
	};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z - 2.5D, 0.0F, 0.0F);
		});
		serverTicks(2);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 37 to 43, y 8 to 28, z 737 to 743 emptied, a grass floor under it, its items gone. */
	private void clear() {
		for (int x = X - 3; x <= X + 3; x++) {
			for (int z = Z - 3; z <= Z + 3; z++) {
				for (int y = Y; y <= Y + 20; y++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
				world().setBlock(x, Y - 1, z, Blocks.grass, 0, 2);
			}
		}
		for (EntityItem item : items()) {
			item.setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 4, Y - 2, Z - 4, X + 5, Y + 22, Z + 5))) {
			if (!((EntityItem) entity).isDead) {
				found.add((EntityItem) entity);
			}
		}
		return found;
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (EntityItem item : items()) {
			ItemStack stack = item.getEntityItem();
			counts.merge(Recipes26_3.nameOf(stack), stack.stackSize, Integer::sum);
			item.setDead();
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + name);
		return (Block) block;
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(item instanceof Item, "no item " + name);
		return (Item) item;
	}

	/** The 26.3 block it reads as, with its age, leaves and stage. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		for (String key : new String[]{"age", "leaves", "stage"}) {
			if (state.containsKey(key)) {
				name.append(",").append(key).append("=").append(state.get(key));
			}
		}
		return name.toString();
	}

	private String breakSound(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((MultiBlockSoundContainer) variants).getSound(world(), x, y, z, own, MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	/** Uses the bamboo item on the top of the block at y - 1 (on the server, as a click comes); what's at y then. */
	private String placeOnTopOf(int y) {
		ItemStack stack = new ItemStack(item("bamboo"));
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), stack, X, y - 1, Z, 1, 0.5F, 1.0F, 0.5F);
		Block there = world().getBlock(X, y, Z);
		return there == Blocks.air ? "none" : name(there, world().getBlockMetadata(X, y, Z));
	}

	/** The metas from y up to y + n - 1, or what's there instead. */
	private String metas(int y, int n) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < n; i++) {
			Block there = world().getBlock(X, y + i, Z);
			result.append(i == 0 ? "" : " ").append(there == block("bamboo") ? String.valueOf(world().getBlockMetadata(X, y + i, Z))
				: there == Blocks.air ? "air" : there == block("bamboo_sapling") ? "shoot" : there.getUnlocalizedName());
		}
		return result.toString();
	}

	// ---------------------------------------------------------------- ours, stored form

	public void testBambooIsOurs() {
		List<String> wrong = new ArrayList<>();
		for (Object thing : new Object[]{block("bamboo"), block("bamboo_sapling"), item("bamboo")}) {
			if (!thing.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(thing.getClass().getName());
			}
		}
		for (String name : new String[]{"blocks.BlockBamboo", "blocks.BlockBambooShoot", "items.ItemBamboo", "client.renderer.block.BlockBambooRenderer"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "bamboo's classes");
	}

	/** EFR's metas by 26.3's names: bit 0 age, bits 1 and 2 leaves (none, small, large), bit 3 stage. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (int meta : new int[]{0, 1, 2, 4, 8, 13}) {
				names.append(names.length() == 0 ? "" : " ").append(name(block("bamboo"), meta));
			}
			return names.append(" ").append(name(block("bamboo_sapling"), 0)).toString();
		});
		assertEqual("bamboo,age=0,leaves=none,stage=0 bamboo,age=1,leaves=none,stage=0 bamboo,age=0,leaves=small,stage=0 bamboo,age=0,leaves=large,stage=0 "
			+ "bamboo,age=0,leaves=none,stage=1 bamboo,age=1,leaves=large,stage=1 bamboo_sapling", got, "26.3's names");
	}

	// ---------------------------------------------------------------- placing, holding

	/** On grass, sand and gravel a shoot; on a shoot a thin stalk (the shoot turning into one); on a thick stalk a thick one. */
	public void testPlacing() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Block ground : new Block[]{Blocks.grass, Blocks.sand, Blocks.gravel}) {
				world().setBlock(X, Y - 1, Z, ground, 0, 2);
				result.append(result.length() == 0 ? "" : " ").append(placeOnTopOf(Y));
				world().setBlock(X, Y, Z, Blocks.air, 0, 2);
			}
			world().setBlock(X, Y - 1, Z, Blocks.grass, 0, 2);
			world().setBlock(X, Y, Z, block("bamboo_sapling"), 0, 3);
			result.append(" ").append(placeOnTopOf(Y + 1)).append(" under it ").append(name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z)));
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 2);
			world().setBlock(X, Y, Z, block("bamboo"), 1, 3);
			result.append(" ").append(placeOnTopOf(Y + 1));
			return result.toString();
		});
		assertEqual("bamboo_sapling bamboo_sapling bamboo_sapling bamboo,age=0,leaves=none,stage=0 under it bamboo,age=0,leaves=none,stage=0 "
			+ "bamboo,age=1,leaves=none,stage=0", got, "on grass, sand, gravel; on a shoot (and the shoot); on a thick stalk");
	}

	/** Not on stone; not into water. */
	public void testNotPlaced() {
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
			String onStone = placeOnTopOf(Y);
			world().setBlock(X, Y - 1, Z, Blocks.grass, 0, 2);
			world().setBlock(X, Y, Z, Blocks.water, 0, 2);
			String inWater = placeOnTopOf(Y);
			return onStone + " " + (inWater.contains("bamboo") ? inWater : "none");
		});
		assertEqual("none none", got, "on stone; on grass under water");
	}

	/** Without its ground a stalk breaks (dropping bamboo) and so does a shoot. */
	public void testBreaksWithoutSupport() {
		onServer(() -> {
			world().setBlock(X, Y, Z, block("bamboo"), 0, 3);
			world().setBlock(X + 2, Y, Z, block("bamboo_sapling"), 0, 3);
			world().setBlock(X, Y - 1, Z, Blocks.air, 0, 3);
			world().setBlock(X + 2, Y - 1, Z, Blocks.air, 0, 3);
		});
		serverTicks(3);
		String got = onServer(() -> (world().getBlock(X, Y, Z) == Blocks.air ? "stalk gone" : "stalk stayed") + " "
			+ (world().getBlock(X + 2, Y, Z) == Blocks.air ? "shoot gone" : "shoot stayed") + " " + dropped());
		assertEqual("stalk gone shoot gone {bamboo=2}", got, "without the ground below");
	}

	// ---------------------------------------------------------------- growing

	/**
	 * A shoot's random tick grows a stalk with small leaves (the shoot turning into a stalk); the top stalk's ticks then
	 * grow on: from four tall it's thick, the top two with large leaves, small below them, bare under that (26.3's
	 * `growBamboo`). Metas from the bottom: 1, 1, 3, 5, 5.
	 */
	public void testGrowth() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("bamboo_sapling"), 0, 3);
			world().getBlock(X, Y, Z).updateTick(world(), X, Y, Z, RIGGED);
			String first = metas(Y, 2);
			for (int top = Y + 1; top <= Y + 3; top++) {
				world().getBlock(X, top, Z).updateTick(world(), X, top, Z, RIGGED);
			}
			return first + " | " + metas(Y, 6);
		});
		assertEqual("0 2 | 1 1 3 5 5 air", got, "after the shoot's tick; after three of the top's");
	}

	/** Bonemeal on a stalk 2 tall: 1 more (26.3's 1 or 2, the rigged roll's 1), the same pattern as growing. */
	public void testBonemeal() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("bamboo"), 0, 3);
			world().setBlock(X, Y + 1, Z, block("bamboo"), 2, 3);
			Block stalk = world().getBlock(X, Y, Z);
			if (!(stalk instanceof IGrowable growable) || !growable.func_149851_a(world(), X, Y, Z, false)) {
				return "not bonemealable";
			}
			growable.func_149853_b(world(), RIGGED, X, Y, Z);
			return metas(Y, 4);
		});
		assertEqual("0 2 2 air", got, "2 tall, then bonemealed once");
	}

	// ---------------------------------------------------------------- shapes, properties

	/** Shapes: a stalk 6 across (10 with large leaves), collision 3 across; a shoot 8 across and 12 high, no collision. */
	public void testShapes() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : new Object[][]{{"bamboo", 0}, {"bamboo", 4}, {"bamboo_sapling", 0}}) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, (Integer) entry[1], 2);
				block.setBlockBoundsBasedOnState(world(), X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(Math.round((block.getBlockBoundsMaxX() - block.getBlockBoundsMinX()) * 16)).append("x")
					.append(Math.round((block.getBlockBoundsMaxY() - block.getBlockBoundsMinY()) * 16));
				AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world(), X, Y, Z);
				result.append(box == null ? " passable" : " solid " + Math.round((box.maxX - box.minX) * 16));
			}
			return result.toString();
		});
		assertEqual("6x16 solid 3 10x16 solid 3 8x12 passable", got, "width x height, then collision's width");
	}

	/** 26.3's: 1.0; fire 60/60 for the stalk, none for the shoot; their sounds; plant's and wood's map colours. */
	public void testProperties() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"bamboo", "bamboo_sapling"}) {
				Block block = block(name);
				world().setBlock(X, Y, Z, block, 0, 2);
				result.append(result.length() == 0 ? "" : " | ").append(name).append(" ").append(block.getBlockHardness(world(), X, Y, Z)).append(" ")
					.append(block.getExplosionResistance(null)).append(" ").append(block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP)).append("/")
					.append(block.getFlammability(world(), X, Y, Z, ForgeDirection.UP)).append(" ").append(breakSound(X, Y, Z).replace(MODERN, "")).append(" ")
					.append(block.getMapColor(0) == MapColor.foliageColor ? "plant" : block.getMapColor(0) == MapColor.woodColor ? "wood"
						: String.valueOf(block.getMapColor(0).colorIndex));
			}
			return result.toString();
		});
		assertEqual("bamboo 1.0 1.0 60/60 block.bamboo.break plant | bamboo_sapling 1.0 1.0 0/0 block.bamboo_sapling.break wood", got,
			"hardness, resistance, fire, sound, colour");
	}

	/** A sword breaks either at once (26.3's `#sword_instantly_mines`); a hand doesn't. */
	public void testSwords() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"bamboo", "bamboo_sapling"}) {
				world().setBlock(X, Y, Z, block(name), 0, 2);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.iron_sword));
				float sword = block(name).getPlayerRelativeBlockHardness(player(), world(), X, Y, Z);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
				float hand = block(name).getPlayerRelativeBlockHardness(player(), world(), X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(name).append(sword >= 1.0F ? " at once" : " " + sword).append(hand >= 1.0F ? " (hand too)" : "");
			}
			return result.toString();
		});
		assertEqual("bamboo at once bamboo_sapling at once", got, "with an iron sword (and not by hand)");
	}

	/** Each drops the bamboo item; picking either gives it. */
	public void testDropsAndPick() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"bamboo", "bamboo_sapling"}) {
				world().setBlock(X, Y, Z, block(name), 0, 2);
				Item picked = block(name).getItem(world(), X, Y, Z);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
				player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(dropped()).append(" pick ").append(picked == null ? "none" : Recipes26_3.nameOf(new ItemStack(picked)));
			}
			return result.toString();
		});
		assertEqual("{bamboo=1} pick bamboo {bamboo=1} pick bamboo", got, "the stalk, the shoot");
	}
}
