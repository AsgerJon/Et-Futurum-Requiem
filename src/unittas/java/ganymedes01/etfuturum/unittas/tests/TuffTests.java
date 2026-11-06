package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * Tuff and calcite as our own (SPEC-modern-movement.md §89, step E), as 26.3's: tuff, polished, bricks, chiseled and
 * chiseled bricks (`etfuturum:tuff` 0 to 4), its slab of three, three stairs, its wall of three, calcite. Ours, EFR's class
 * gone; EFR's stored form read as before; 26.3's strength (tuff 1.5 and 6, calcite 0.75 and 0.75, EFR's 0.45), tool, map
 * colours (terracotta's gray and white, the nearest 1.7.10 has) and sounds, each variant its own (tuff's, polished tuff's,
 * the bricks'); no `stone` ore name on tuff (EFR's made buttons, plates and repeaters of it); 26.3's recipes
 * (`tuff_recipes_26_3.txt`). At z 260.
 */
@TasSuite("stones")
public class TuffTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 260;
	private static final String[] BLOCKS = {"tuff", "tuff_slab", "double_tuff_slab", "tuff_stairs", "polished_tuff_stairs", "tuff_brick_stairs", "tuff_wall",
		"calcite"};
	/** "id meta hardness resistance sound" by 26.3's `Blocks` and `SoundType`. */
	private static final String[] WANT = {"tuff 0 1.5 6.0 tuff", "tuff 1 1.5 6.0 polished_tuff", "tuff 2 1.5 6.0 tuff_bricks", "tuff 3 1.5 6.0 tuff",
		"tuff 4 1.5 6.0 tuff_bricks", "tuff_slab 0 1.5 6.0 tuff", "tuff_slab 1 1.5 6.0 polished_tuff", "tuff_slab 10 1.5 6.0 tuff_bricks",
		"double_tuff_slab 1 1.5 6.0 polished_tuff", "double_tuff_slab 2 1.5 6.0 tuff_bricks", "tuff_stairs 0 1.5 6.0 tuff",
		"polished_tuff_stairs 0 1.5 6.0 polished_tuff", "tuff_brick_stairs 0 1.5 6.0 tuff_bricks", "tuff_wall 0 1.5 6.0 tuff",
		"tuff_wall 1 1.5 6.0 polished_tuff", "tuff_wall 2 1.5 6.0 tuff_bricks", "calcite 0 0.75 0.75 calcite"};

	@Override
	protected void setUp() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		return found == null ? "nothing" : found.type().id.replace("minecraft:", "");
	}

	/** The break sound a block there plays: its own, or what the client's per-variant table gives it (on the server thread). */
	private String breakSound(Block block) {
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer) variants).getSound(world(), X, Y, Z, own,
			MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	public void testTuffIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum.blocks.BlockTuff") != null) {
			wrong.add("BlockTuff still there");
		}
		assertEqual("[]", wrong.toString(), "tuff's and calcite's classes");
	}

	/** EFR's ids and metas, by 26.3's names. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta = 0; meta < 5; meta++) {
				out.append(name(block("tuff"), meta)).append(' ');
			}
			out.append(';');
			for (int meta = 0; meta < 3; meta++) {
				out.append(' ').append(name(block("tuff_slab"), meta));
			}
			out.append(';');
			for (String name : new String[]{"tuff_stairs", "polished_tuff_stairs", "tuff_brick_stairs"}) {
				out.append(' ').append(name(block(name), 0));
			}
			out.append(';');
			for (int meta = 0; meta < 3; meta++) {
				out.append(' ').append(name(block("tuff_wall"), meta));
			}
			return out.append("; ").append(name(block("calcite"), 0)).toString();
		});
		assertEqual("tuff polished_tuff tuff_bricks chiseled_tuff chiseled_tuff_bricks ; tuff_slab polished_tuff_slab tuff_brick_slab; tuff_stairs "
			+ "polished_tuff_stairs tuff_brick_stairs; tuff_wall polished_tuff_wall tuff_brick_wall; calcite", got, "26.3's names");
	}

	/** 26.3's strength, a pickaxe, and each variant's own sound. */
	public void testStrengthToolAndSounds() {
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String entry : WANT) {
				String[] parts = entry.split(" ");
				Block block = block(parts[0]);
				int meta = Integer.parseInt(parts[1]);
				world().setBlock(X, Y, Z, block, meta, 3);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z);
				String sound = breakSound(block);
				if (!(parts[2] + " " + parts[3]).equals(got) || !"pickaxe".equals(block.getHarvestTool(meta))
					|| !(McFly.MC_ASSETS + ":block." + parts[4] + ".break").equals(sound)) {
					found.add(parts[0] + ":" + meta + " " + got + " " + block.getHarvestTool(meta) + " " + sound);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength, the pickaxe, sounds (want: as listed)");
	}

	/** 26.3's terracotta gray (tuff's family) and white (calcite's), as 1.7.10's nearest: gray and wool's. */
	public void testMapColours() {
		String got = block("tuff").getMapColor(0).colorIndex + " " + block("tuff").getMapColor(4).colorIndex + " " + block("tuff_wall").getMapColor(2).colorIndex
			+ " " + block("calcite").getMapColor(0).colorIndex;
		int gray = MapColor.grayColor.colorIndex;
		assertEqual(gray + " " + gray + " " + gray + " " + MapColor.clothColor.colorIndex, got, "map colours");
	}

	/** 26.3's `#stone_crafting_materials` holds no tuff: EFR's `stone` ore name on it made buttons, plates and repeaters. */
	public void testNoStoneOreName() {
		boolean has = false;
		for (net.minecraft.item.ItemStack ore : OreDictionary.getOres("stone")) {
			has |= ore.getItem() == net.minecraft.item.Item.getItemFromBlock(block("tuff"));
		}
		assertTrue(!has, "tuff under the ore name stone");
	}

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("tuff_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(13, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
