package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Deepslate's set as our own (SPEC-modern-movement.md §89, step C), as 26.3's: ours; EFR's stored form read as before
 * (`deepslate_bricks` 0 to 4, the two slab ids and their doubles, four stairs, two walls); 26.3's strength (3.5 and 6
 * throughout, a pickaxe); 26.3's recipes (`deepslateset_recipes_26_3.txt`). At z 220.
 */
@TasSuite("deepslate")
public class DeepslateSetTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 220;
	private static final String[] BLOCKS = {"deepslate_bricks", "deepslate_slab", "double_deepslate_slab", "deepslate_brick_slab",
		"double_deepslate_brick_slab", "cobbled_deepslate_stairs", "polished_deepslate_stairs", "deepslate_brick_stairs", "deepslate_tile_stairs",
		"deepslate_wall", "deepslate_brick_wall"};

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		return found == null ? "nothing" : found.type().id.replace("minecraft:", "");
	}

	public void testSetIsOurs() {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		assertEqual("[]", wrong.toString(), "the set's classes");
	}

	/** EFR's ids and metas, by 26.3's names. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta = 0; meta < 5; meta++) {
				out.append(name(block("deepslate_bricks"), meta)).append(' ');
			}
			out.append("; ").append(name(block("deepslate_slab"), 0)).append(' ').append(name(block("deepslate_slab"), 9)).append(' ')
				.append(name(block("deepslate_brick_slab"), 0)).append(' ').append(name(block("deepslate_brick_slab"), 1)).append("; ");
			for (String stairs : new String[]{"cobbled_deepslate_stairs", "polished_deepslate_stairs", "deepslate_brick_stairs", "deepslate_tile_stairs"}) {
				out.append(name(block(stairs), 0)).append(' ');
			}
			out.append("; ").append(name(block("deepslate_wall"), 0)).append(' ').append(name(block("deepslate_wall"), 1)).append(' ')
				.append(name(block("deepslate_brick_wall"), 0)).append(' ').append(name(block("deepslate_brick_wall"), 1));
			return out.toString();
		});
		assertEqual("deepslate_bricks cracked_deepslate_bricks deepslate_tiles cracked_deepslate_tiles chiseled_deepslate ; cobbled_deepslate_slab "
			+ "polished_deepslate_slab deepslate_brick_slab deepslate_tile_slab; cobbled_deepslate_stairs polished_deepslate_stairs deepslate_brick_stairs "
			+ "deepslate_tile_stairs ; cobbled_deepslate_wall polished_deepslate_wall deepslate_brick_wall deepslate_tile_wall", got, "26.3's names");
	}

	/** 26.3's 3.5 and 6 for every one, a pickaxe. */
	public void testStrength() {
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String name : BLOCKS) {
				Block block = block(name);
				world().setBlock(X, Y, Z, block, 0, 3);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
					+ block.getHarvestTool(0);
				if (!"3.5 6.0 pickaxe".equals(got)) {
					found.add(name + " " + got);
				}
			}
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength and tool (want 3.5 6.0 pickaxe)");
	}

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("deepslateset_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(17, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
