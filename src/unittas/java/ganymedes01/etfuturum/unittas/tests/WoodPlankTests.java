package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The wood sets' planks, slabs, stairs and the mosaic as our own (SPEC-modern-movement.md §91.4, step B), as 26.3's:
 * EFR's shared ids (`wood_planks`, `wood_slab`, `double_wood_slab`: crimson, warped, mangrove, cherry, bamboo), the
 * stairs, the mosaic, its slab (EFR crafted it at damage 5) and stairs; strength, sounds by wood, map colours, fire,
 * fuel, drops, recipes, ore names and names. At z 560, y 8.
 */
@TasSuite("woods")
public class WoodPlankTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 560;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final String[] BLOCKS = {"wood_planks", "wood_slab", "double_wood_slab", "crimson_stairs", "warped_stairs", "mangrove_stairs",
		"cherry_stairs", "bamboo_stairs", "bamboo_mosaic", "bamboo_mosaic_slab", "double_bamboo_mosaic_slab", "bamboo_mosaic_stairs"};

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

	/** x 36 to 44, y 7 to 10, z 556 to 564 emptied, its items gone. */
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

	/** The 26.3 block it reads as, with a facing but north, a top half, a slab's type but bottom. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("facing") && !"north".equals(state.get("facing"))) {
			name.append(",facing=").append(state.get("facing"));
		}
		if ("top".equals(state.get("half"))) {
			name.append(",half=top");
		}
		if (state.containsKey("type") && !"bottom".equals(state.get("type"))) {
			name.append(",type=").append(state.get("type"));
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

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testPlanksAreOurs() {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockModernWoodPlanks", "blocks.BlockModernWoodSlab", "blocks.BlockBambooMosaic"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the planks' classes");
	}

	/** EFR's ids and metas by 26.3's names; EFR's crafted mosaic slabs (5, and 13 on top) read as the slab. */
	public void testStoredForm() {
		Object[][] metas = {{"wood_planks", 0}, {"wood_planks", 1}, {"wood_planks", 2}, {"wood_planks", 3}, {"wood_planks", 4}, {"wood_slab", 0},
			{"wood_slab", 8}, {"wood_slab", 4}, {"wood_slab", 12}, {"double_wood_slab", 3}, {"crimson_stairs", 0}, {"warped_stairs", 7},
			{"mangrove_stairs", 2}, {"cherry_stairs", 5}, {"bamboo_stairs", 3}, {"bamboo_mosaic", 0}, {"bamboo_mosaic_slab", 0}, {"bamboo_mosaic_slab", 8},
			{"bamboo_mosaic_slab", 5}, {"bamboo_mosaic_slab", 13}, {"double_bamboo_mosaic_slab", 0}, {"bamboo_mosaic_stairs", 1}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("crimson_planks warped_planks mangrove_planks cherry_planks bamboo_planks crimson_slab crimson_slab,type=top bamboo_slab "
			+ "bamboo_slab,type=top cherry_slab,type=double crimson_stairs,facing=east warped_stairs,half=top mangrove_stairs,facing=south "
			+ "cherry_stairs,facing=west,half=top bamboo_stairs bamboo_mosaic bamboo_mosaic_slab bamboo_mosaic_slab,type=top bamboo_mosaic_slab "
			+ "bamboo_mosaic_slab,type=top bamboo_mosaic_slab,type=double bamboo_mosaic_stairs,facing=west", got, "26.3's names");
	}

	/**
	 * 26.3's: 2 and 3, an axe; sounds by wood (nether wood, the wood's, cherry wood, bamboo wood); map colours (1.7.10's
	 * nearest: netherrack, cyan, red, cloth, yellow); fire 5 and 20, none for crimson and warped.
	 */
	public void testProperties() {
		String[] sounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		MapColor[] colours = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			String fire = i < 2 ? "0 0" : "5 20";
			want.add(new Object[]{"wood_planks", i, fire, sounds[i], colours[i]});
			want.add(new Object[]{"wood_slab", i, fire, sounds[i], colours[i]});
			want.add(new Object[]{"wood_slab", i + 8, fire, sounds[i], colours[i]});
			want.add(new Object[]{"double_wood_slab", i, fire, sounds[i], colours[i]});
			want.add(new Object[]{WOODS[i] + "_stairs", 0, fire, sounds[i], colours[i]});
		}
		for (String name : new String[]{"bamboo_mosaic", "bamboo_mosaic_slab", "double_bamboo_mosaic_slab", "bamboo_mosaic_stairs"}) {
			want.add(new Object[]{name, 0, "5 20", "block.bamboo_wood.break", MapColor.yellowColor});
		}
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				int meta = (Integer) entry[1];
				world().setBlock(X, Y, Z, block, meta, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getHarvestTool(meta) + " " + breakSound(X, Y, Z).replace(MODERN, "");
				String wantText = "2.0 3.0 " + entry[2] + " axe " + entry[3];
				if (!wantText.equals(got) || block.getMapColor(meta) != entry[4]) {
					found.add(entry[0] + ":" + meta + " " + got + " colour " + block.getMapColor(meta).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, tool, sound, colour (want: as listed)");
	}

	/** 26.3's fuel times: wood blocks 300, slabs 150, crimson's and warped's (and the stems) none. */
	public void testFuel() {
		Object[][] stacks = {{"wood_planks", 0}, {"wood_planks", 1}, {"wood_planks", 2}, {"wood_planks", 3}, {"wood_planks", 4}, {"wood_slab", 0},
			{"wood_slab", 2}, {"wood_slab", 4}, {"crimson_stairs", 0}, {"cherry_stairs", 0}, {"bamboo_mosaic", 0}, {"bamboo_mosaic_slab", 0},
			{"bamboo_mosaic_stairs", 0}, {"crimson_stem", 0}, {"warped_stem", 1}};
		StringBuilder got = new StringBuilder();
		for (Object[] entry : stacks) {
			got.append(got.length() == 0 ? "" : " ").append(TileEntityFurnace.getItemBurnTime(new ItemStack(block((String) entry[0]), 1, (Integer) entry[1])));
		}
		assertEqual("0 0 300 300 300 0 150 150 0 300 300 150 300 0 0", got.toString(), "burn times");
	}

	// ---------------------------------------------------------------- drops, recipes, ore names, names

	/** A slab of either half drops one, a double two, stairs themselves; EFR's crafted mosaic slab drops the slab. */
	public void testDrops() {
		Object[][] metas = {{"wood_slab", 8}, {"wood_slab", 3}, {"double_wood_slab", 2}, {"cherry_stairs", 5}, {"wood_planks", 4},
			{"bamboo_mosaic_slab", 13}, {"double_bamboo_mosaic_slab", 0}};
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
		assertEqual("{crimson_slab=1} {cherry_slab=1} {mangrove_slab=2} {cherry_stairs=1} {bamboo_planks=1} {bamboo_mosaic_slab=1} {bamboo_mosaic_slab=2}",
			got, "drops");
	}

	/** 26.3's (its jar's, `woods_planks_recipes_26_3.txt`): slabs, stairs, the mosaic, its slab and stairs. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_planks_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(13, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** EFR's `plankWood`, `slabWood`, `stairWood` (other mods' recipes read them). */
	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (int meta = 0; meta < 5; meta++) {
			checkOre(missing, "plankWood", "wood_planks", meta);
			checkOre(missing, "slabWood", "wood_slab", meta);
			checkOre(missing, "stairWood", WOODS[meta] + "_stairs", 0);
		}
		assertEqual("[]", missing.toString(), "without their ore name");
	}

	private static void checkOre(List<String> missing, String ore, String name, int meta) {
		ItemStack stack = new ItemStack(block(name), 1, meta);
		for (int id : OreDictionary.getOreIDs(stack)) {
			if (ore.equals(OreDictionary.getOreName(id))) {
				return;
			}
		}
		missing.add(name + ":" + meta + " " + ore);
	}

	/** 26.3's English names. */
	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			Object[][] stacks = {{"wood_planks", 0}, {"wood_planks", 3}, {"wood_slab", 1}, {"wood_slab", 4}, {"mangrove_stairs", 0},
				{"bamboo_mosaic", 0}, {"bamboo_mosaic_slab", 0}, {"bamboo_mosaic_stairs", 0}};
			for (Object[] entry : stacks) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(Item.getItemFromBlock(block((String) entry[0])), 1, (Integer) entry[1]).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Crimson Planks, Cherry Planks, Warped Slab, Bamboo Slab, Mangrove Stairs, Bamboo Mosaic, Bamboo Mosaic Slab, Bamboo Mosaic Stairs",
			got, "names");
	}
}
