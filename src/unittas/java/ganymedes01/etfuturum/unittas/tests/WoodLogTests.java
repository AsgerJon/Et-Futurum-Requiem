package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' logs as our own (SPEC-modern-movement.md §91.3, step A), as 26.3's: EFR's stripped logs, the vanilla
 * woods' wood (bark), the stripped woods, the cherry log and the bamboo block in EFR's ids and metas; strength, fire,
 * sounds, map colours; one stripping table (26.3's `AXE` transformer) for every log 1.7.10, EFR and McFly have; drops,
 * recipes, ore names and names. At z 540, y 8.
 */
@TasSuite("woods")
public class WoodLogTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 540;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] LOGS = {"log_stripped", "log2_stripped", "bark", "bark2", "wood_stripped", "wood2_stripped", "cherry_log", "bamboo_block"};

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

	/** x 36 to 44, y 7 to 10, z 536 to 544 emptied, its items gone. */
	private void clear() {
		for (int x = X - 4; x <= X + 4; x++) {
			for (int y = Y - 1; y <= Y + 2; y++) {
				for (int z = Z - 4; z <= Z + 4; z++) {
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
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 4, Z + 6))) {
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

	/** The 26.3 block it reads as, with its axis but y. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		String name = found.type().id.replace("minecraft:", "");
		return state.containsKey("axis") && !"y".equals(state.get("axis")) ? name + ",axis=" + state.get("axis") : name;
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

	/** The held axe used on the block at X, Y, Z, as a right click (Forge's interact event first). */
	private void useAxe(ItemStack axe) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, axe);
		player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), axe, X, Y, Z, 1, 0.5F, 1.0F, 0.5F);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testLogsAreOurs() {
		List<String> wrong = new ArrayList<>();
		for (String name : LOGS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BaseLog", "blocks.BlockStrippedOldLog", "blocks.BlockStrippedNewLog", "blocks.BlockWoodBarkOld",
			"blocks.BlockWoodBarkNew", "blocks.BlockStrippedOldWood", "blocks.BlockStrippedNewWood", "blocks.BlockBambooBlock",
			"blocks.BlockStrippedLogCompat", "api.StrippedLogRegistry"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the logs' classes");
	}

	/** EFR's ids and metas by 26.3's names: the kind in meta & 3, the axis in meta & 12. */
	public void testStoredForm() {
		Object[][] metas = {{"log_stripped", 0}, {"log_stripped", 1}, {"log_stripped", 2}, {"log_stripped", 3}, {"log_stripped", 4}, {"log_stripped", 8},
			{"log2_stripped", 0}, {"log2_stripped", 1}, {"bark", 0}, {"bark", 1}, {"bark", 2}, {"bark", 3}, {"bark", 6}, {"bark2", 0}, {"bark2", 1},
			{"wood_stripped", 0}, {"wood_stripped", 1}, {"wood_stripped", 2}, {"wood_stripped", 3}, {"wood2_stripped", 0}, {"wood2_stripped", 9},
			{"cherry_log", 0}, {"cherry_log", 1}, {"cherry_log", 2}, {"cherry_log", 3}, {"cherry_log", 4}, {"bamboo_block", 0}, {"bamboo_block", 1},
			{"bamboo_block", 4}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("stripped_oak_log stripped_spruce_log stripped_birch_log stripped_jungle_log stripped_oak_log,axis=x stripped_oak_log,axis=z "
			+ "stripped_acacia_log stripped_dark_oak_log oak_wood spruce_wood birch_wood jungle_wood birch_wood,axis=x acacia_wood dark_oak_wood "
			+ "stripped_oak_wood stripped_spruce_wood stripped_birch_wood stripped_jungle_wood stripped_acacia_wood stripped_dark_oak_wood,axis=z "
			+ "cherry_log cherry_wood stripped_cherry_log stripped_cherry_wood cherry_log,axis=x bamboo_block stripped_bamboo_block bamboo_block,axis=x",
			got, "26.3's names");
	}

	/**
	 * 26.3's strength, fire, tool, sound and map colours (1.7.10's ids where it has them, else its nearest): 2, fire 5
	 * and 5, an axe; the wood sound, cherry's and bamboo's their own. Colours upright, then lying (x).
	 */
	public void testProperties() {
		Object[][] want = {
			{"log_stripped", 0, "dig.wood", MapColor.woodColor, MapColor.woodColor}, {"log_stripped", 1, "dig.wood", MapColor.obsidianColor, MapColor.obsidianColor},
			{"log_stripped", 2, "dig.wood", MapColor.sandColor, MapColor.sandColor}, {"log_stripped", 3, "dig.wood", MapColor.dirtColor, MapColor.dirtColor},
			{"log2_stripped", 0, "dig.wood", MapColor.adobeColor, MapColor.adobeColor}, {"log2_stripped", 1, "dig.wood", MapColor.brownColor, MapColor.brownColor},
			{"bark", 0, "dig.wood", MapColor.woodColor, MapColor.woodColor}, {"bark", 1, "dig.wood", MapColor.obsidianColor, MapColor.obsidianColor},
			{"bark", 2, "dig.wood", MapColor.sandColor, MapColor.sandColor}, {"bark", 3, "dig.wood", MapColor.dirtColor, MapColor.dirtColor},
			{"bark2", 0, "dig.wood", MapColor.grayColor, MapColor.grayColor}, {"bark2", 1, "dig.wood", MapColor.brownColor, MapColor.brownColor},
			{"wood_stripped", 0, "dig.wood", MapColor.woodColor, MapColor.woodColor}, {"wood_stripped", 3, "dig.wood", MapColor.dirtColor, MapColor.dirtColor},
			{"wood2_stripped", 0, "dig.wood", MapColor.adobeColor, MapColor.adobeColor}, {"wood2_stripped", 1, "dig.wood", MapColor.brownColor, MapColor.brownColor},
			{"cherry_log", 0, "block.cherry_wood.break", MapColor.clothColor, MapColor.blackColor},
			{"cherry_log", 1, "block.cherry_wood.break", MapColor.blackColor, MapColor.blackColor},
			{"cherry_log", 2, "block.cherry_wood.break", MapColor.clothColor, MapColor.redColor},
			{"cherry_log", 3, "block.cherry_wood.break", MapColor.redColor, MapColor.redColor},
			{"bamboo_block", 0, "block.bamboo_wood.break", MapColor.yellowColor, MapColor.foliageColor},
			{"bamboo_block", 1, "block.bamboo_wood.break", MapColor.yellowColor, MapColor.yellowColor}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				int meta = (Integer) entry[1];
				world().setBlock(X, Y, Z, block, meta, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getHarvestTool(meta) + " "
					+ breakSound(X, Y, Z).replace(MODERN, "");
				String wantText = "2.0 5 5 axe " + entry[2];
				if (!wantText.equals(got) || block.getMapColor(meta) != entry[3] || block.getMapColor(meta | 4) != entry[4]) {
					found.add(entry[0] + ":" + meta + " " + got + " colours " + block.getMapColor(meta).colorIndex + " " + block.getMapColor(meta | 4).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, fire, tool, sound, colours (want: as listed)");
	}

	// ---------------------------------------------------------------- stripping

	/**
	 * 26.3's `AXE` list, every log here: the vanilla logs (their six-sided bark, 12 to 15, as wood), EFR's woods, the
	 * cherry log, the bamboo block, McFly's mangrove log and stems; the axis kept; a stripped one not stripped again.
	 */
	public void testStripping() {
		Object[][] from = {{"minecraft:log", 4}, {"minecraft:log", 1}, {"minecraft:log", 10}, {"minecraft:log", 3}, {"minecraft:log", 12},
			{"minecraft:log", 15}, {"minecraft:log2", 0}, {"minecraft:log2", 9}, {"bark", 2}, {"bark", 4}, {"bark2", 1}, {"cherry_log", 0},
			{"cherry_log", 5}, {"bamboo_block", 0}, {"bamboo_block", 4}, {"mangrove_log", 0}, {"crimson_stem", 0}, {"crimson_stem", 1},
			{"warped_stem", 4}, {"log_stripped", 0}, {"cherry_log", 2}};
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : from) {
				world().setBlock(X, Y, Z, block((String) entry[0]), (Integer) entry[1], 2);
				useAxe(new ItemStack(Items.iron_axe));
				result.append(result.length() == 0 ? "" : ", ").append(name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z)));
			}
			return result.toString();
		});
		assertEqual("stripped_oak_log,axis=x, stripped_spruce_log, stripped_birch_log,axis=z, stripped_jungle_log, stripped_oak_wood, "
			+ "stripped_jungle_wood, stripped_acacia_log, stripped_dark_oak_log,axis=z, stripped_birch_wood, stripped_oak_wood,axis=x, "
			+ "stripped_dark_oak_wood, stripped_cherry_log, stripped_cherry_wood,axis=x, stripped_bamboo_block, stripped_bamboo_block,axis=x, "
			+ "stripped_mangrove_log, stripped_crimson_stem, stripped_crimson_hyphae, stripped_warped_stem,axis=x, stripped_oak_log, stripped_cherry_log",
			got, "after the axe");
	}

	/** The axe worn once a strip in survival, not in creative; nothing worn when there's nothing to strip. */
	public void testStrippingWearsTheAxe() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.log, 0, 2);
			ItemStack axe = new ItemStack(Items.iron_axe);
			useAxe(axe);
			int survival = axe.getItemDamage();
			useAxe(axe);
			int again = axe.getItemDamage();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			world().setBlock(X, Y, Z, block("cherry_log"), 0, 2);
			ItemStack creative = new ItemStack(Items.iron_axe);
			useAxe(creative);
			return survival + " " + again + " " + creative.getItemDamage() + " " + name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z));
		});
		assertEqual("1 1 0 stripped_cherry_log", got, "the axe's damage: survival, stripped again, creative; the creative strip");
	}

	/** 26.3's `block_change` from the player, once a strip (H1 left it undone). */
	public void testStrippingPostsBlockChange() {
		List<String> heard = new ArrayList<>();
		String got = onServer(() -> {
			GameEventListener recorder = new GameEventListener() {
				@Override
				public double[] position(World world) {
					return new double[]{X + 0.5D, Y + 0.5D, Z + 0.5D};
				}

				@Override
				public int radius() {
					return 8;
				}

				@Override
				public boolean handle(World world, GameEvent event, GameEvent.Context context, double ex, double ey, double ez) {
					heard.add(event.name.replace("minecraft:", "") + (context.source == player() ? " by the player" : ""));
					return true;
				}
			};
			world().setBlock(X, Y, Z, block("bark"), 0, 2);
			GameEvents.register(world(), recorder);
			try {
				useAxe(new ItemStack(Items.iron_axe));
			} finally {
				GameEvents.unregister(world(), recorder);
			}
			return heard.toString();
		});
		assertTrue(got.contains("block_change by the player"), "the strip's game events: " + got);
		assertEqual(1L, heard.stream().filter(e -> e.startsWith("block_change")).count(), "block_change events: " + got);
	}

	/** 26.3's `item.axe.strip`, heard by the player. */
	public void testStrippingSound() {
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y + 1.0D, Z - 1.5D, 0.0F, 0.0F);
			world().setBlock(X, Y, Z, block("cherry_log"), 0, 2);
		});
		serverTicks(2);
		clearSounds();
		onServer(() -> useAxe(new ItemStack(Items.iron_axe)));
		waitForSound(MODERN + "item.axe.strip", 20);
	}

	// ---------------------------------------------------------------- drops, recipes, ore names, names

	/** Each drops itself by kind, its axis gone. */
	public void testDrops() {
		Object[][] metas = {{"log_stripped", 6}, {"log2_stripped", 1}, {"bark", 7}, {"bark2", 0}, {"wood_stripped", 2}, {"wood2_stripped", 9},
			{"cherry_log", 5}, {"cherry_log", 3}, {"bamboo_block", 5}};
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : metas) {
				world().setBlock(X, Y, Z, block((String) entry[0]), (Integer) entry[1], 2);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, new ItemStack(Items.iron_axe));
				player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(dropped());
			}
			return result.toString();
		});
		assertEqual("{stripped_birch_log=1} {stripped_dark_oak_log=1} {jungle_wood=1} {acacia_wood=1} {stripped_birch_wood=1} {stripped_dark_oak_wood=1} "
			+ "{cherry_wood=1} {stripped_cherry_wood=1} {stripped_bamboo_block=1}", got, "drops");
	}

	/** 26.3's (its jar's, `woods_logs_recipes_26_3.txt`): woods, stripped woods, planks, charcoal. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_logs_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(24, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** EFR's `logWood` on every kind of every log here (other mods' recipes read it). */
	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (String name : new String[]{"log_stripped", "bark", "wood_stripped"}) {
			for (int meta = 0; meta < 4; meta++) {
				checkOre(missing, name, meta);
			}
		}
		for (String name : new String[]{"log2_stripped", "bark2", "wood2_stripped"}) {
			for (int meta = 0; meta < 2; meta++) {
				checkOre(missing, name, meta);
			}
		}
		for (int meta = 0; meta < 4; meta++) {
			checkOre(missing, "cherry_log", meta);
		}
		assertEqual("[]", missing.toString(), "kinds without logWood");
	}

	private static void checkOre(List<String> missing, String name, int meta) {
		ItemStack stack = new ItemStack(block(name), 1, meta);
		for (int id : OreDictionary.getOreIDs(stack)) {
			if ("logWood".equals(OreDictionary.getOreName(id))) {
				return;
			}
		}
		missing.add(name + ":" + meta);
	}

	/** 26.3's English names. */
	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			Object[][] stacks = {{"log_stripped", 0}, {"log2_stripped", 1}, {"bark", 2}, {"bark2", 0}, {"wood_stripped", 3}, {"wood2_stripped", 1},
				{"cherry_log", 0}, {"cherry_log", 1}, {"cherry_log", 2}, {"cherry_log", 3}, {"bamboo_block", 0}, {"bamboo_block", 1}};
			for (Object[] entry : stacks) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(Item.getItemFromBlock(block((String) entry[0])), 1, (Integer) entry[1]).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Stripped Oak Log, Stripped Dark Oak Log, Birch Wood, Acacia Wood, Stripped Jungle Wood, Stripped Dark Oak Wood, Cherry Log, Cherry Wood, "
			+ "Stripped Cherry Log, Stripped Cherry Wood, Block of Bamboo, Block of Stripped Bamboo", got, "names");
	}
}
