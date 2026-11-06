package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Nether's plants and stems as our own (SPEC-modern-movement.md §88.7, step F), as 26.3's: ours, EFR's classes
 * gone, on by default (EFR's were experiments, off); the stored form EFR wrote, read as before (the vines' head and body
 * worked out from the column); strength, tools and sounds; nylium turned netherrack under a block that stops light, its
 * bonemeal's plants, netherrack's bonemeal next to nylium; fungi on 26.3's grounds only (not soul sand), the planted huge
 * fungus from bonemeal; the vines growing from their head; stems stripped by an axe; 26.3's recipes
 * (`netherplants_recipes_26_3.txt`). At z 160 on the flat world.
 */
@TasSuite("netherplants")
public class NetherPlantTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 160;
	private static final String[] EFR_CLASSES = {"blocks.BlockNylium", "blocks.BlockNetherFungus", "blocks.BlockNetherRoots", "blocks.BlockNetherSprouts",
		"blocks.BlockWeepingVines", "blocks.BlockTwistingVines", "blocks.BlockNetherwart", "blocks.BlockShroomlight", "blocks.BlockNetherStem",
		"world.generate.decorate.WorldGenNetherGrass", "world.generate.decorate.WorldGenHugeFungus"};

	@Override
	protected void setUp() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	@Override
	protected void tearDown() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	private void clear() {
		onServer(() -> {
			for (int x = X - 6; x <= X + 6; x++) {
				for (int y = Y - 1; y <= Y + 12; y++) {
					for (int z = Z - 6; z <= Z + 6; z++) {
						world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
					}
				}
			}
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem) {
					((EntityItem) entity).setDead();
				}
			}
		});
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static BlockIds.State state(String modern) {
		BlockIds.Type type = BlockIds.byId("minecraft:" + modern);
		assertTrue(type != null, "no block for minecraft:" + modern);
		return type.defaultState();
	}

	private static void put(int x, int y, int z, String modern) {
		BlockIds.State state = state(modern);
		world().setBlock(x, y, z, state.block(), state.meta(), 3);
	}

	/** 26.3's name of what's at a place (its properties too where it has some). */
	private static String at(int x, int y, int z) {
		BlockIds.Found found = BlockIds.find(world().getBlock(x, y, z), world().getBlockMetadata(x, y, z));
		return found == null ? "?" : found.type().id.replace("minecraft:", "");
	}

	private static String props(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		StringBuilder out = new StringBuilder(found.type().id.replace("minecraft:", ""));
		for (Map.Entry<String, String> entry : found.state().properties().entrySet()) {
			out.append(',').append(entry.getKey()).append('=').append(entry.getValue());
		}
		return out.toString();
	}

	private static void bonemeal(int x, int y, int z, Random random) {
		Block block = world().getBlock(x, y, z);
		assertTrue(block instanceof IGrowable, at(x, y, z) + " takes no bonemeal");
		IGrowable growable = (IGrowable) block;
		if (growable.func_149851_a(world(), x, y, z, false) && growable.func_149852_a(world(), random, x, y, z)) {
			growable.func_149853_b(world(), random, x, y, z);
		}
	}

	// ---------------------------------------------------------------- ours

	public void testPlantsAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"nylium", "nether_fungus", "nether_roots", "nether_sprouts", "weeping_vines", "twisting_vines", "nether_wart",
			"shroomlight", "crimson_stem", "warped_stem"}) {
			Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
			String type = block instanceof Block && block != Blocks.air ? block.getClass().getName() : "missing";
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the plants' classes");
	}

	/** EFR's ids and metas, by 26.3's names; the vines' head and body by the column. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			out.append(props(block("nylium"), 0)).append(' ').append(props(block("nylium"), 1)).append("; ");
			out.append(props(block("nether_fungus"), 0)).append(' ').append(props(block("nether_fungus"), 1)).append("; ");
			out.append(props(block("nether_roots"), 0)).append(' ').append(props(block("nether_roots"), 1)).append("; ");
			out.append(props(block("nether_sprouts"), 0)).append("; ");
			out.append(props(block("nether_wart"), 0)).append(' ').append(props(block("nether_wart"), 1)).append("; ");
			out.append(props(block("shroomlight"), 0)).append("; ");
			for (String wood : new String[]{"crimson", "warped"}) {
				for (int meta = 0; meta < 4; meta++) {
					out.append(props(block(wood + "_stem"), meta)).append(' ');
				}
				out.append(props(block(wood + "_stem"), 4)).append(' ').append(props(block(wood + "_stem"), 8)).append("; ");
			}
			// a weeping vine two long under netherrack: the top one the body, the bottom one the head
			world().setBlock(X, Y + 4, Z, Blocks.netherrack, 0, 3);
			world().setBlock(X, Y + 3, Z, block("weeping_vines"), 0, 3);
			world().setBlock(X, Y + 2, Z, block("weeping_vines"), 0, 3);
			// the command layer reads EFR's one block a kind as the head; the block itself tells head from body
			io.github.asgerjon.mcfly.nether.NetherVinesBlock vines = (io.github.asgerjon.mcfly.nether.NetherVinesBlock) block("weeping_vines");
			out.append("weeping column ").append(vines.isHead(world(), X, Y + 3, Z) ? "head" : "body").append(' ')
				.append(vines.isHead(world(), X, Y + 2, Z) ? "head" : "body");
			return out.toString();
		});
		assertEqual("crimson_nylium warped_nylium; crimson_fungus warped_fungus; crimson_roots warped_roots; nether_sprouts; nether_wart_block "
			+ "warped_wart_block; shroomlight; crimson_stem,axis=y crimson_hyphae,axis=y stripped_crimson_stem,axis=y stripped_crimson_hyphae,axis=y "
			+ "crimson_stem,axis=x crimson_stem,axis=z; warped_stem,axis=y warped_hyphae,axis=y stripped_warped_stem,axis=y stripped_warped_hyphae,axis=y "
			+ "warped_stem,axis=x warped_stem,axis=z; weeping column body head", got, "26.3's names");
	}

	/** 26.3's strength, the tool, by hand or not, light and sounds. */
	public void testStrengthAndSounds() {
		String[][] blocks = {{"crimson_nylium", "0.4 pickaxe tool", "nylium"}, {"crimson_fungus", "0.0 - hand", "fungus"},
			{"crimson_roots", "0.0 - hand", "roots"}, {"nether_sprouts", "0.0 - hand", "nether_sprouts"}, {"weeping_vines", "0.0 - hand", "weeping_vines"},
			{"nether_wart_block", "1.0 hoe hand", "wart_block"}, {"shroomlight", "1.0 hoe hand", "shroomlight"}, {"crimson_stem", "2.0 axe hand", "stem"},
			{"warped_hyphae", "2.0 axe hand", "stem"}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			world().setBlock(X, Y + 3, Z, Blocks.netherrack, 0, 3);
			world().setBlock(X, Y, Z, block("nylium"), 0, 3);
			for (String[] entry : blocks) {
				BlockIds.State state = state(entry[0]);
				int y = entry[0].equals("weeping_vines") ? Y + 2 : Y + 1;
				if (entry[0].equals("crimson_nylium")) {
					y = Y + 1;
					world().setBlock(X, Y + 1, Z, Blocks.air, 0, 3);
				}
				world().setBlock(X, y, Z, state.block(), state.meta(), 3);
				Block block = world().getBlock(X, y, Z);
				int meta = world().getBlockMetadata(X, y, Z);
				String tool = block.getHarvestTool(meta) == null ? "-" : block.getHarvestTool(meta);
				String got = block.getBlockHardness(world(), X, y, Z) + " " + tool + " " + (block.canHarvestBlock(player(), meta) ? "hand" : "tool");
				String sound = block.stepSound.getBreakSound();
				if (!entry[1].equals(got) || !(McFly.MC_ASSETS + ":block." + entry[2] + ".break").equals(sound)) {
					found.add(entry[0] + " " + got + " " + sound);
				}
				world().setBlock(X, y, Z, Blocks.air, 0, 3);
			}
			found.add("shroomlight light " + block("shroomlight").getLightValue());
			return found;
		});
		assertEqual("[shroomlight light 15]", wrong.toString(), "strength, tool, by hand, sounds (want: as listed)");
	}

	// ---------------------------------------------------------------- nylium and netherrack

	/** Nylium under a block that stops light, on a random tick: netherrack. Its drop without Silk Touch: netherrack. */
	public void testNyliumDiesUnderCover() {
		String got = onServer(() -> {
			Block nylium = block("nylium");
			world().setBlock(X, Y, Z, nylium, 0, 3);
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 3);
			nylium.updateTick(world(), X, Y, Z, new Random(1));
			String covered = at(X, Y, Z);
			world().setBlock(X + 2, Y, Z, nylium, 1, 3);
			String drop = String.valueOf(nylium.getItemDropped(1, new Random(1), 0) == net.minecraft.item.Item.getItemFromBlock(Blocks.netherrack));
			return covered + " " + drop;
		});
		assertEqual("netherrack true", got, "covered nylium, its drop netherrack");
	}

	/** Bonemeal on crimson nylium: crimson plants on the nylium around it; on netherrack beside nylium: nylium. */
	public void testBonemeal() {
		String got = onServer(() -> {
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					world().setBlock(X + dx, Y, Z + dz, block("nylium"), 0, 3);
				}
			}
			bonemeal(X, Y, Z, new Random(3));
			int plants = 0;
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					String above = at(X + dx, Y + 1, Z + dz);
					if (above.startsWith("crimson_") || above.startsWith("warped_")) {
						plants++;
					}
				}
			}
			world().setBlock(X + 5, Y, Z, Blocks.netherrack, 0, 3);
			world().setBlock(X + 6, Y, Z, block("nylium"), 1, 3);
			bonemeal(X + 5, Y, Z, new Random(3));
			return (plants > 0 ? "plants" : "none") + ", netherrack now " + at(X + 5, Y, Z);
		});
		assertEqual("plants, netherrack now warped_nylium", got, "bonemeal");
	}

	// ---------------------------------------------------------------- fungi and vines

	/** A fungus stands on nylium, soul soil, mycelium, grass; not on soul sand or stone (26.3's `#supports_*_fungus`). */
	public void testFungusGround() {
		String got = onServer(() -> {
			Block fungus = block("nether_fungus");
			StringBuilder out = new StringBuilder();
			Object[][] grounds = {{block("nylium"), "nylium"}, {block("soul_soil"), "soul_soil"}, {Blocks.mycelium, "mycelium"}, {Blocks.grass, "grass"},
				{Blocks.soul_sand, "soul_sand"}, {Blocks.stone, "stone"}};
			for (Object[] ground : grounds) {
				world().setBlock(X, Y, Z, (Block) ground[0], 0, 3);
				out.append(ground[1]).append(' ').append(fungus.canPlaceBlockAt(world(), X, Y + 1, Z) ? "yes" : "no").append("; ");
			}
			return out.toString();
		});
		assertEqual("nylium yes; soul_soil yes; mycelium yes; grass yes; soul_sand no; stone no; ", got, "where a fungus stands");
	}

	/** Bonemeal on a crimson fungus on crimson nylium: in time, a huge crimson fungus (its stem where the fungus was). */
	public void testHugeFungus() {
		String got = onServer(() -> {
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					world().setBlock(X + dx, Y, Z + dz, block("nylium"), 0, 3);
				}
			}
			world().setBlock(X, Y + 1, Z, block("nether_fungus"), 0, 3);
			Random random = new Random(5);
			for (int i = 0; i < 40 && at(X, Y + 1, Z).equals("crimson_fungus"); i++) {
				bonemeal(X, Y + 1, Z, random);
			}
			return at(X, Y + 1, Z) + " " + at(X, Y + 2, Z);
		});
		assertEqual("crimson_stem crimson_stem", got, "the fungus's place and above it");
	}

	/** A weeping vine's head grows a vine below it on random ticks (26.3's 0.1 a tick, an age under 25). */
	public void testVinesGrow() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 8, Z, Blocks.netherrack, 0, 3);
			world().setBlock(X, Y + 7, Z, block("weeping_vines"), 0, 3);
			Random random = new Random(7);
			for (int i = 0; i < 200; i++) {
				int y = Y + 7;
				while (world().getBlock(X, y - 1, Z) == block("weeping_vines")) {
					y--;
				}
				world().getBlock(X, y, Z).updateTick(world(), X, y, Z, random);
			}
			int length = 0;
			for (int y = Y + 7; world().getBlock(X, y, Z) == block("weeping_vines"); y--) {
				length++;
			}
			return length > 1 ? "grew" : "didn't grow";
		});
		assertEqual("grew", got, "a weeping vine after 200 ticks at its head");
	}

	/** An axe on a crimson stem along x: a stripped crimson stem, still along x. */
	public void testAxeStrips() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("crimson_stem"), 4, 3);
			ItemStack axe = new ItemStack(Items.iron_axe);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, axe);
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), axe, X, Y, Z, 1, 0.5F, 1.0F, 0.5F);
			return props(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z));
		});
		assertEqual("stripped_crimson_stem,axis=x", got, "the stem after the axe");
	}

	/** 26.3's recipes (its jar's, `netherplants_recipes_26_3.txt`). */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("netherplants_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(7, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
