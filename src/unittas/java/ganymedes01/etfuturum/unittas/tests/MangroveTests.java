package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Mangrove, and cherry's leaves and sapling, as our own (SPEC-modern-movement.md §90.6, step E), as 26.3's: the mangrove
 * log's four kinds, the roots and muddy roots, the leaves (mangrove and cherry in EFR's one id), the sapling (the
 * propagule, standing or hanging by age, and the cherry sapling in EFR's one id); 26.3's trees from them (the mangrove's
 * roots, vines and hanging propagules; the cherry's branches), drops, falling leaves, recipes and compost. EFR's stored
 * form read by 26.3's names. At z 380, y 8 and up; the particles at x -3, z 3, by the player.
 */
@TasSuite("caves")
public class MangroveTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 380;
	private static final int SEEN_X = -3, SEEN_Y = 7, SEEN_Z = 3;
	private static final String[] BLOCKS = {"mangrove_log", "mangrove_roots", "muddy_mangrove_roots", "leaves", "sapling"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 32 to 48, y 5 to 32, z 372 to 388 emptied (room for a tall mangrove), its items gone. */
	private void clear() {
		for (int x = X - 8; x <= X + 8; x++) {
			for (int y = Y - 3; y <= Y + 24; y++) {
				for (int z = Z - 8; z <= Z + 8; z++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
			}
		}
		for (EntityItem item : items()) {
			item.setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 9, Y - 4, Z - 9, X + 10, Y + 26, Z + 10))) {
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
			counts.merge(Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "") + ":" + stack.getItemDamage(), stack.stackSize, Integer::sum);
			item.setDead();
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	/** The 26.3 block it reads as, with the states that tell its metas apart (an axis but y, persistent, stage 1, hanging and its age). */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("axis") && !"y".equals(state.get("axis"))) {
			name.append(",axis=").append(state.get("axis"));
		}
		if ("true".equals(state.get("persistent"))) {
			name.append(",persistent");
		}
		if ("1".equals(state.get("stage"))) {
			name.append(",stage=1");
		}
		if ("true".equals(state.get("hanging"))) {
			name.append(",hanging,age=").append(state.get("age"));
		}
		return name.toString();
	}

	private void harvest(int x, int y, int z, ItemStack held) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
		player().theItemInWorldManager.tryHarvestBlock(x, y, z);
	}

	private void floor(Block block, int radius, int depth) {
		for (int x = X - radius; x <= X + radius; x++) {
			for (int z = Z - radius; z <= Z + radius; z++) {
				for (int d = 1; d <= depth; d++) {
					world().setBlock(x, Y - d, z, block, 0, 2);
				}
			}
		}
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

	private void bonemeal(int x, int y, int z) {
		ItemDye.applyBonemeal(new ItemStack(Items.dye, 1, 15), world(), x, y, z, player());
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testMangroveIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockModernLeaves", "blocks.BlockModernSapling", "blocks.BlockMangroveRoots", "blocks.BlockMuddyMangroveRoots",
			"client.renderer.block.BlockMangroveRootsRenderer"}) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "mangrove's and cherry's leaves' and sapling's classes");
	}

	/**
	 * EFR's ids and metas by 26.3's names: the log's kind in meta & 3, its axis in meta & 12; the leaves' kind in meta & 3,
	 * 4 placed; the sapling's kind in meta & 1, 8 the stage; a hanging propagule's age on the other even metas (2, 4, 6,
	 * 10, 12: EFR reads them as a propagule).
	 */
	public void testStoredForm() {
		Object[][] metas = {{"mangrove_log", 0}, {"mangrove_log", 1}, {"mangrove_log", 2}, {"mangrove_log", 3}, {"mangrove_log", 4}, {"mangrove_roots", 0},
			{"muddy_mangrove_roots", 0}, {"muddy_mangrove_roots", 8}, {"leaves", 0}, {"leaves", 1}, {"leaves", 5}, {"sapling", 0}, {"sapling", 1},
			{"sapling", 8}, {"sapling", 9}, {"sapling", 2}, {"sapling", 4}, {"sapling", 6}, {"sapling", 10}, {"sapling", 12}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("mangrove_log mangrove_wood stripped_mangrove_log stripped_mangrove_wood mangrove_log,axis=x mangrove_roots muddy_mangrove_roots "
			+ "muddy_mangrove_roots,axis=z mangrove_leaves cherry_leaves cherry_leaves,persistent mangrove_propagule cherry_sapling "
			+ "mangrove_propagule,stage=1 cherry_sapling,stage=1 mangrove_propagule,hanging,age=0 mangrove_propagule,hanging,age=1 "
			+ "mangrove_propagule,hanging,age=2 mangrove_propagule,hanging,age=3 mangrove_propagule,hanging,age=4", got, "26.3's names");
	}

	/**
	 * 26.3's strength, fire, tools, sounds and colours: logs 2, fire 5 and 5, an axe, the wood sound, mangrove red (a log
	 * on its side podzol); the roots 0.7, fire 5 and 20, an axe; the muddy roots 0.7, a shovel; the leaves 0.2, fire 30
	 * and 60, a hoe (cherry's pink and their own sound); the sapling instabreak (cherry's pink and its own sound).
	 */
	public void testProperties() {
		Object[][] want = {{"mangrove_log", 0, "2.0 5 5 axe dig.wood", MapColor.redColor}, {"mangrove_log", 4, "2.0 5 5 axe dig.wood", MapColor.obsidianColor},
			{"mangrove_log", 1, "2.0 5 5 axe dig.wood", MapColor.redColor}, {"mangrove_log", 2, "2.0 5 5 axe dig.wood", MapColor.redColor},
			{"mangrove_log", 3, "2.0 5 5 axe dig.wood", MapColor.redColor},
			{"mangrove_roots", 0, "0.7 5 20 axe block.mangrove_roots.break", MapColor.obsidianColor},
			{"muddy_mangrove_roots", 0, "0.7 0 0 shovel block.muddy_mangrove_roots.break", MapColor.obsidianColor},
			{"leaves", 0, "0.2 30 60 hoe dig.grass", MapColor.foliageColor}, {"leaves", 1, "0.2 30 60 hoe block.cherry_leaves.break", MapColor.pinkColor},
			{"sapling", 0, "0.0 0 0 null dig.grass", MapColor.foliageColor}, {"sapling", 1, "0.0 0 0 null block.cherry_sapling.break", MapColor.pinkColor}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			floor(Blocks.grass, 1, 1);
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				int meta = (Integer) entry[1];
				world().setBlock(X, Y, Z, block, meta, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getHarvestTool(meta) + " "
					+ breakSound(X, Y, Z).replace(McFly.MC_ASSETS + ":", "");
				if (!entry[2].equals(got) || block.getMapColor(meta) != entry[3]) {
					found.add(entry[0] + ":" + meta + " " + got + " colour " + block.getMapColor(meta).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, fire, tool, sound, colour (want: as listed)");
	}

	// ---------------------------------------------------------------- the propagule and the trees

	/** Bonemeal on mangrove leaves with air below hangs a propagule there, age 0; not with a block below. */
	public void testLeavesBonemealHangsAPropagule() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 2, Z, block("leaves"), 0, 2);
			bonemeal(X, Y + 2, Z);
			String below = name(world().getBlock(X, Y + 1, Z), world().getBlockMetadata(X, Y + 1, Z));
			world().setBlock(X + 2, Y + 2, Z, block("leaves"), 0, 2);
			world().setBlock(X + 2, Y + 1, Z, Blocks.stone, 0, 2);
			bonemeal(X + 2, Y + 2, Z);
			return below + "; over stone " + name(world().getBlock(X + 2, Y + 1, Z), 0);
		});
		assertEqual("mangrove_propagule,hanging,age=0; over stone stone", got, "bonemealed mangrove leaves");
	}

	/**
	 * A hanging propagule: bonemeal ages it one a time up to 4, then is no use; random ticks age it too; it needs the
	 * mangrove leaves above (gone with them); 26.3's loot: a propagule only at age 4 (a standing one is always 4).
	 */
	public void testHangingPropaguleGrows() {
		String got = onServer(() -> {
			Block sapling = block("sapling");
			world().setBlock(X, Y + 2, Z, block("leaves"), 0, 2);
			world().setBlock(X, Y + 1, Z, sapling, 2, 2);
			StringBuilder ages = new StringBuilder();
			for (int i = 0; i < 5; i++) {
				bonemeal(X, Y + 1, Z);
				ages.append(name(world().getBlock(X, Y + 1, Z), world().getBlockMetadata(X, Y + 1, Z)).replace("mangrove_propagule,hanging,age=", ""));
			}
			world().setBlock(X + 2, Y + 2, Z, block("leaves"), 0, 2);
			world().setBlock(X + 2, Y + 1, Z, sapling, 2, 2);
			Random random = new Random(3);
			for (int i = 0; i < 10; i++) {
				sapling.updateTick(world(), X + 2, Y + 1, Z, random);
			}
			String ticked = name(world().getBlock(X + 2, Y + 1, Z), world().getBlockMetadata(X + 2, Y + 1, Z)).replace("mangrove_propagule,hanging,age=", "");
			world().setBlock(X, Y + 2, Z, Blocks.air, 0, 3);
			String unheld = (world().isAirBlock(X, Y + 1, Z) ? "gone " : "still there ") + dropped();
			harvest(X + 2, Y + 1, Z, null);
			String picked = dropped();
			world().setBlock(X + 2, Y + 1, Z, sapling, 2, 2);
			harvest(X + 2, Y + 1, Z, null);
			return "bonemeal " + ages + ", ticked " + ticked + ", leaves gone: " + unheld + ", picked " + picked + ", picked young " + dropped();
		});
		assertEqual("bonemeal 12344, ticked 4, leaves gone: gone {sapling:0=1}, picked {sapling:0=1}, picked young {}", got, "a hanging propagule");
	}

	/**
	 * A propagule on mud over stone, bonemealed till it grows (stage, then the tree, each at 0.45): 26.3's mangrove (15 in
	 * 100) or tall mangrove: mangrove logs up from above its roots, mangrove leaves, roots into the mud (there muddy roots).
	 */
	public void testPropaguleGrowsAMangrove() {
		String got = onServer(() -> {
			// two of mud over stone: 26.3's roots grow on through mud, and fail past 15 long, so stone stops them
			floor(Blocks.stone, 8, 3);
			floor(McFlyBlocksAccess.mud(), 8, 2);
			world().setBlock(X, Y, Z, block("sapling"), 0, 2);
			for (int i = 0; i < 60 && world().getBlock(X, Y, Z) == block("sapling") && (world().getBlockMetadata(X, Y, Z) & 7) == 0; i++) {
				bonemeal(X, Y, Z);
			}
			int logs = 0, leaves = 0, roots = 0, muddy = 0;
			for (int x = X - 8; x <= X + 8; x++) {
				for (int y = Y - 3; y <= Y + 24; y++) {
					for (int z = Z - 8; z <= Z + 8; z++) {
						Block there = world().getBlock(x, y, z);
						int meta = world().getBlockMetadata(x, y, z);
						logs += there == block("mangrove_log") && (meta & 3) == 0 ? 1 : 0;
						leaves += there == block("leaves") && (meta & 3) == 0 ? 1 : 0;
						roots += there == block("mangrove_roots") ? 1 : 0;
						muddy += there == block("muddy_mangrove_roots") ? 1 : 0;
					}
				}
			}
			Block origin = world().getBlock(X, Y, Z);
			return "origin " + (origin == block("sapling") ? "still a propagule" : "grown") + ", logs " + (logs >= 4 ? "4 or more" : logs) + ", leaves "
				+ (leaves >= 20 ? "20 or more" : leaves) + ", roots " + (roots >= 1 ? "some" : "none") + ", muddy roots " + (muddy >= 1 ? "some" : "none");
		});
		assertEqual("origin grown, logs 4 or more, leaves 20 or more, roots some, muddy roots some", got, "the grown mangrove");
	}

	/**
	 * A cherry sapling on grass, bonemealed till it grows: 26.3's cherry tree (`CherryTrunkPlacer`): a straight trunk of
	 * EFR's cherry log (the wood sets' family keeps it), one to three branches, the sideways ones all on one axis, cherry
	 * leaves round their ends (none placed).
	 */
	public void testCherrySaplingGrowsATree() {
		String got = onServer(() -> {
			floor(Blocks.grass, 8, 1);
			world().setBlock(X, Y, Z, block("sapling"), 1, 2);
			for (int i = 0; i < 60 && world().getBlock(X, Y, Z) == block("sapling"); i++) {
				bonemeal(X, Y, Z);
			}
			Block cherryLog = block("cherry_log");
			int logs = 0, leaves = 0, placed = 0;
			java.util.Set<Integer> axes = new java.util.TreeSet<>();
			for (int x = X - 8; x <= X + 8; x++) {
				for (int y = Y; y <= Y + 20; y++) {
					for (int z = Z - 8; z <= Z + 8; z++) {
						Block there = world().getBlock(x, y, z);
						int meta = world().getBlockMetadata(x, y, z);
						if (there == cherryLog) {
							logs++;
							if ((meta & 12) != 0) {
								axes.add(meta & 12);
							}
						}
						if (there == block("leaves") && (meta & 3) == 1) {
							leaves++;
							placed += (meta & 4) != 0 ? 1 : 0;
						}
					}
				}
			}
			int trunk = 0;
			while (world().getBlock(X, Y + trunk, Z) == cherryLog && (world().getBlockMetadata(X, Y + trunk, Z) & 12) == 0) {
				trunk++;
			}
			return "trunk " + (trunk >= 3 && trunk <= 8 ? "3 to 8" : trunk) + ", logs " + (logs > trunk ? "more than the trunk" : logs) + ", sideways axes "
				+ axes.size() + ", leaves " + (leaves >= 40 ? "40 or more" : leaves) + ", placed " + placed;
		});
		assertEqual("trunk 3 to 8, logs more than the trunk, sideways axes 1, leaves 40 or more, placed 0", got, "the grown cherry tree");
	}

	// ---------------------------------------------------------------- drops, roots, stripping

	/**
	 * 26.3's leaves' loot: shears give the leaves; by hand (over 2000) mangrove's only 1 to 2 sticks at 1 in 50, cherry's
	 * a cherry sapling at 1 in 20 and the sticks.
	 */
	public void testLeavesDrops() {
		String got = onServer(() -> {
			Block leaves = block("leaves");
			StringBuilder result = new StringBuilder();
			for (int kind = 0; kind < 2; kind++) {
				// 100 sheared: only the leaves (26.3's loot rolls no sapling or sticks with shears)
				for (int i = 0; i < 100; i++) {
					world().setBlock(X, Y, Z, leaves, kind, 2);
					harvest(X, Y, Z, new ItemStack(Items.shears));
				}
				String sheared = dropped();
				world().setBlock(X, Y, Z, leaves, kind, 2);
				int saplings = 0, sticks = 0, other = 0;
				for (int i = 0; i < 2000; i++) {
					for (ItemStack drop : leaves.getDrops(world(), X, Y, Z, kind, 0)) {
						if (drop.getItem() == Item.getItemFromBlock(block("sapling")) && drop.getItemDamage() == 1) {
							saplings += drop.stackSize;
						} else if (drop.getItem() == Items.stick) {
							sticks += drop.stackSize;
						} else {
							other += drop.stackSize;
						}
					}
				}
				result.append(kind == 0 ? "mangrove" : "; cherry").append(" sheared ").append(sheared).append(", saplings ")
					.append(saplings >= 60 && saplings <= 140 ? "about 100" : saplings).append(", sticks ").append(sticks >= 25 && sticks <= 100 ? "about 60" : sticks)
					.append(", other ").append(other);
			}
			return result.toString();
		});
		assertEqual("mangrove sheared {leaves:0=100}, saplings 0, sticks about 60, other 0; cherry sheared {leaves:1=100}, saplings about 100, sticks about 60, "
			+ "other 0", got, "the leaves' drops");
	}

	/** Mangrove roots draw no face against roots above or below (26.3's `skipRendering`), but do against air and sideways roots; see-through. */
	public void testRootsFaces() {
		String got = onServer(() -> {
			Block roots = block("mangrove_roots");
			world().setBlock(X, Y, Z, roots, 0, 2);
			world().setBlock(X, Y + 1, Z, roots, 0, 2);
			world().setBlock(X + 1, Y, Z, roots, 0, 2);
			// 1.7.10 asks the neighbour's place with the side seen from it: the top face of the lower one, from above
			return "up " + roots.shouldSideBeRendered(world(), X, Y + 1, Z, 1) + ", down " + roots.shouldSideBeRendered(world(), X, Y, Z, 0) + ", east "
				+ roots.shouldSideBeRendered(world(), X + 1, Y, Z, 5) + ", west " + roots.shouldSideBeRendered(world(), X - 1, Y, Z, 4) + ", opaque "
				+ roots.isOpaqueCube();
		});
		assertEqual("up false, down false, east true, west true, opaque false", got, "the roots' faces");
	}

	/** An axe strips a mangrove log and its wood (26.3's sound), keeping the axis; a stripped one stays. */
	public void testStripping() {
		String got = onServer(() -> {
			Block log = block("mangrove_log");
			StringBuilder result = new StringBuilder();
			for (int meta : new int[]{0, 5, 2}) {
				world().setBlock(X, Y, Z, log, meta, 2);
				ItemStack axe = new ItemStack(Items.iron_axe);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, axe);
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), axe, X, Y, Z, 1, 0.5F, 1.0F, 0.5F);
				result.append(result.length() == 0 ? "" : ", ").append(meta).append(" -> ").append(name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z)));
			}
			return result.toString();
		});
		assertEqual("0 -> stripped_mangrove_log, 5 -> stripped_mangrove_wood,axis=x, 2 -> stripped_mangrove_log", got, "stripping");
	}

	// ---------------------------------------------------------------- falling leaves, by the player

	/**
	 * 26.3's falling leaves: cherry's `cherry_leaves` at 0.1 a display tick (untinted), mangrove's `tinted_leaves` at
	 * 0.01 tinted as the leaves are there; each 0.05 under a random spot of the block, none over a full top face.
	 */
	public void testFallingLeaves() {
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			world().getWorldInfo().setRaining(false);
			world().setBlock(SEEN_X, SEEN_Y, SEEN_Z, block("leaves"), 5, 2);
			world().setBlock(SEEN_X, SEEN_Y - 1, SEEN_Z, Blocks.air, 0, 2);
		});
		try {
			waitUntilOnClient("the client's cherry leaves over air, no rain", () -> mc().theWorld.getBlock(SEEN_X, SEEN_Y, SEEN_Z) == block("leaves")
				&& mc().theWorld.isAirBlock(SEEN_X, SEEN_Y - 1, SEEN_Z) && !mc().theWorld.canLightningStrikeAt(SEEN_X, SEEN_Y + 1, SEEN_Z), 60);
			String cherry = onClient(() -> fallingLeaves(1L, 0.1F));
			onServer(() -> world().setBlock(SEEN_X, SEEN_Y, SEEN_Z, block("leaves"), 4, 2));
			waitUntilOnClient("the client's mangrove leaves", () -> mc().theWorld.getBlockMetadata(SEEN_X, SEEN_Y, SEEN_Z) == 4, 40);
			String mangrove = onClient(() -> fallingLeaves(2L, 0.01F));
			assertEqual("all as 26.3's; tint ffffff", cherry, "cherry's falling leaves");
			assertEqual("all as 26.3's; tint as the leaves'", mangrove, "mangrove's falling leaves");
		} finally {
			onServer(() -> world().setBlock(SEEN_X, SEEN_Y, SEEN_Z, Blocks.air, 0, 2));
		}
	}

	/** 2000 display ticks on a seeded random (the particle setting All): whether as many came as 26.3's would, and their tint. */
	private String fallingLeaves(long seed, float chance) {
		Block leaves = block("leaves");
		int expected = 0;
		Random replay = new Random(seed);
		for (int i = 0; i < 2000; i++) {
			if (replay.nextFloat() < chance) {
				expected++;
				replay.nextDouble();
				replay.nextDouble();
			}
		}
		int setting = mc().gameSettings.particleSetting;
		List<EntityFX> before = particlesUnderTheLeaves();
		try {
			mc().gameSettings.particleSetting = 0;
			Random random = new Random(seed);
			for (int i = 0; i < 2000; i++) {
				leaves.randomDisplayTick(mc().theWorld, SEEN_X, SEEN_Y, SEEN_Z, random);
			}
		} finally {
			mc().gameSettings.particleSetting = setting;
		}
		List<EntityFX> made = particlesUnderTheLeaves();
		made.removeAll(before);
		if (made.size() != expected) {
			return made.size() + " of " + expected;
		}
		int own = leaves.colorMultiplier(mc().theWorld, SEEN_X, SEEN_Y, SEEN_Z) & 0xFFFFFF;
		java.util.Set<String> tints = new java.util.TreeSet<>();
		for (EntityFX particle : made) {
			int rgb = Math.round(particle.getRedColorF() * 255.0F) << 16 | Math.round(particle.getGreenColorF() * 255.0F) << 8 | Math.round(particle.getBlueColorF() * 255.0F);
			tints.add(rgb == own && own != 0xFFFFFF ? "as the leaves'" : String.format("%06x", rgb));
		}
		return "all as 26.3's; tint " + String.join(",", tints);
	}

	@SuppressWarnings("unchecked")
	private List<EntityFX> particlesUnderTheLeaves() {
		List<?>[] layers = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.particle.EffectRenderer.class, mc().effectRenderer,
			"fxLayers", "field_78876_b");
		List<EntityFX> found = new ArrayList<>();
		for (List<?> layer : layers) {
			for (Object entry : layer) {
				EntityFX particle = (EntityFX) entry;
				if (particle.posX >= SEEN_X && particle.posX < SEEN_X + 1 && particle.posZ >= SEEN_Z && particle.posZ < SEEN_Z + 1
					&& particle.posY > SEEN_Y - 1 && particle.posY < SEEN_Y) {
					found.add(particle);
				}
			}
		}
		return found;
	}

	/**
	 * A flat item of a block of variants shows its own damage's picture: the propagule's and the cherry sapling's differ,
	 * §88's warped fungus isn't the crimson one, nor the leaves' kinds alike (1.7.10's `ItemBlock` showed meta 0's for
	 * every damage).
	 */
	public void testItemPicturesByDamage() {
		String got = onClient(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"sapling", "nether_fungus", "leaves", "azalea_leaves"}) {
				Item item = Item.getItemFromBlock(block(name));
				net.minecraft.util.IIcon first = item.getIconFromDamage(0), second = item.getIconFromDamage(1);
				result.append(result.length() == 0 ? "" : ", ").append(name).append(first == second ? " alike" : " differ").append(
					second == block(name).getIcon(2, 1) ? ", its own" : ", not its own");
			}
			return result.toString();
		});
		assertEqual("sapling differ, its own, nether_fungus differ, its own, leaves differ, its own, azalea_leaves differ, its own", got,
			"the items' pictures by damage");
	}

	// ---------------------------------------------------------------- recipes, compost

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("mangrove_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(3, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** 26.3's compost: the leaves, the propagule, the cherry sapling and the roots 0.3 each. */
	public void testCompost() {
		Map<ItemStack, Integer> composts = ganymedes01.etfuturum.api.CompostingRegistry.getComposts();
		String got = composts.get(new ItemStack(block("leaves"), 1, 0)) + " " + composts.get(new ItemStack(block("leaves"), 1, 1)) + " "
			+ composts.get(new ItemStack(block("sapling"), 1, 0)) + " " + composts.get(new ItemStack(block("sapling"), 1, 1)) + " "
			+ composts.get(new ItemStack(block("mangrove_roots")));
		assertEqual("30 30 30 30 30", got, "compost chances");
	}

	/** McFly's mud, by name (the test names no McFly registry class). */
	private static final class McFlyBlocksAccess {
		static Block mud() {
			return block("mud");
		}
	}
}
