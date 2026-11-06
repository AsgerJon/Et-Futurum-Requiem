package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * The old stones as our own (SPEC-modern-movement.md §89, step D), as 26.3's: granite, diorite, andesite and the polished
 * ones (`etfuturum:stone` 1 to 6, 0 EFR's look-alike of granite), smooth stone, sandstone, red sandstone and quartz, red
 * sandstone (3), their slabs, stairs and walls, and the vanilla stones' slabs, stairs and walls 1.7.10 lacks (the nether
 * brick wall with them). Ours, EFR's classes gone; EFR's stored form read as before; 26.3's strength, tool, sounds and map
 * colours; 1.7.10's seamless double slabs dropping the smooth blocks they are; EFR's ore names; 26.3's recipes
 * (`stones_recipes_26_3.txt`). At z 240 on the flat world.
 */
@TasSuite("stones")
public class StoneTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 240;
	private static final String[] BLOCKS = {"stone", "smooth_stone", "smooth_sandstone", "smooth_red_sandstone", "smooth_quartz", "red_sandstone",
		"red_sandstone_slab", "double_red_sandstone_slab", "stone_slab", "double_stone_slab", "stone_slab_2", "double_stone_slab_2", "smooth_sandstone_slab",
		"double_smooth_sandstone_slab", "smooth_red_sandstone_slab", "double_smooth_red_sandstone_slab", "smooth_quartz_slab", "double_smooth_quartz_slab",
		"smooth_sandstone_stairs", "smooth_red_sandstone_stairs", "smooth_quartz_stairs", "granite_stairs", "polished_granite_stairs", "diorite_stairs",
		"polished_diorite_stairs", "andesite_stairs", "polished_andesite_stairs", "mossy_stone_brick_stairs", "mossy_cobblestone_stairs", "stone_stairs",
		"red_sandstone_stairs", "stone_wall", "nether_brick_wall", "stone_wall_2", "red_sandstone_wall"};
	private static final String[] EFR_CLASSES = {"blocks.BlockBountifulStone", "blocks.BlockRedSandstone", "blocks.itemblocks.ItemBlockRedSandstone",
		"blocks.BlockRedSandstoneSlab", "blocks.BlockSmoothSandstoneSlab", "blocks.BlockSmoothQuartzSlab", "blocks.BlockStoneSlab1", "blocks.BlockStoneWall"};
	/** "id meta hardness resistance" by 26.3's `Blocks` (slabs, stairs and walls their block's `ofLegacyCopy`, some slabs' destroy time 2). */
	private static final String[] STRENGTH = {"stone 0 1.5 6.0", "stone 1 1.5 6.0", "stone 2 1.5 6.0", "stone 3 1.5 6.0", "stone 4 1.5 6.0", "stone 5 1.5 6.0",
		"stone 6 1.5 6.0", "smooth_stone 0 2.0 6.0", "smooth_sandstone 0 2.0 6.0", "smooth_red_sandstone 0 2.0 6.0", "smooth_quartz 0 2.0 6.0",
		"red_sandstone 0 0.8 0.8", "red_sandstone 1 0.8 0.8", "red_sandstone 2 0.8 0.8", "red_sandstone_slab 0 2.0 0.8", "red_sandstone_slab 1 2.0 0.8",
		"double_red_sandstone_slab 1 2.0 0.8", "stone_slab 0 2.0 6.0", "stone_slab 1 2.0 6.0", "stone_slab 2 1.5 6.0", "stone_slab 3 2.0 6.0",
		"double_stone_slab 2 1.5 6.0", "stone_slab_2 0 1.5 6.0", "stone_slab_2 3 1.5 6.0", "stone_slab_2 5 1.5 6.0", "double_stone_slab_2 4 1.5 6.0",
		"smooth_sandstone_slab 0 2.0 6.0", "smooth_red_sandstone_slab 0 2.0 6.0", "smooth_quartz_slab 0 2.0 6.0", "double_smooth_quartz_slab 0 2.0 6.0",
		"smooth_sandstone_stairs 0 2.0 6.0", "smooth_red_sandstone_stairs 0 2.0 6.0", "smooth_quartz_stairs 0 2.0 6.0", "granite_stairs 0 1.5 6.0",
		"polished_granite_stairs 0 1.5 6.0", "diorite_stairs 0 1.5 6.0", "polished_diorite_stairs 0 1.5 6.0", "andesite_stairs 0 1.5 6.0",
		"polished_andesite_stairs 0 1.5 6.0", "mossy_stone_brick_stairs 0 1.5 6.0", "mossy_cobblestone_stairs 0 2.0 6.0", "stone_stairs 0 1.5 6.0",
		"red_sandstone_stairs 0 0.8 0.8", "stone_wall 0 1.5 6.0", "stone_wall 1 1.5 6.0", "stone_wall 2 0.8 0.8", "stone_wall 3 2.0 6.0",
		"nether_brick_wall 0 2.0 6.0", "stone_wall_2 0 1.5 6.0", "stone_wall_2 1 1.5 6.0", "stone_wall_2 2 1.5 6.0", "red_sandstone_wall 0 0.8 0.8"};

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

	private static String stack(ItemStack stack) {
		return stack == null ? "null" : Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "") + ":" + stack.getItemDamage()
			+ (stack.stackSize != 1 ? " x" + stack.stackSize : "");
	}

	public void testStonesAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the stones' classes");
	}

	/** EFR's ids and metas, by 26.3's names. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta = 1; meta <= 6; meta++) {
				out.append(name(block("stone"), meta)).append(' ');
			}
			out.append(';');
			for (String name : new String[]{"smooth_stone", "smooth_sandstone", "smooth_red_sandstone", "smooth_quartz"}) {
				out.append(' ').append(name(block(name), 0));
			}
			out.append(';');
			for (int meta = 0; meta < 3; meta++) {
				out.append(' ').append(name(block("red_sandstone"), meta));
			}
			out.append("; ").append(name(block("red_sandstone_slab"), 0)).append(' ').append(name(block("red_sandstone_slab"), 9)).append(' ')
				.append(name(block("double_red_sandstone_slab"), 1)).append(';');
			for (int meta = 0; meta < 4; meta++) {
				out.append(' ').append(name(block("stone_slab"), meta));
			}
			out.append(';');
			for (int meta = 0; meta < 6; meta++) {
				out.append(' ').append(name(block("stone_slab_2"), meta));
			}
			out.append(';');
			for (String name : new String[]{"smooth_sandstone_slab", "smooth_red_sandstone_slab", "smooth_quartz_slab", "smooth_sandstone_stairs",
				"smooth_red_sandstone_stairs", "smooth_quartz_stairs", "granite_stairs", "polished_granite_stairs", "diorite_stairs", "polished_diorite_stairs",
				"andesite_stairs", "polished_andesite_stairs", "mossy_stone_brick_stairs", "mossy_cobblestone_stairs", "stone_stairs", "red_sandstone_stairs"}) {
				out.append(' ').append(name(block(name), 0));
			}
			out.append(';');
			for (int meta = 0; meta < 4; meta++) {
				out.append(' ').append(name(block("stone_wall"), meta));
			}
			out.append(' ').append(name(block("nether_brick_wall"), 0));
			for (int meta = 0; meta < 3; meta++) {
				out.append(' ').append(name(block("stone_wall_2"), meta));
			}
			out.append(' ').append(name(block("red_sandstone_wall"), 0));
			return out.toString();
		});
		assertEqual("granite polished_granite diorite polished_diorite andesite polished_andesite ; smooth_stone smooth_sandstone smooth_red_sandstone "
			+ "smooth_quartz; red_sandstone chiseled_red_sandstone cut_red_sandstone; red_sandstone_slab cut_red_sandstone_slab cut_red_sandstone_slab; "
			+ "stone_slab mossy_cobblestone_slab mossy_stone_brick_slab cut_sandstone_slab; granite_slab polished_granite_slab diorite_slab "
			+ "polished_diorite_slab andesite_slab polished_andesite_slab; smooth_sandstone_slab smooth_red_sandstone_slab smooth_quartz_slab "
			+ "smooth_sandstone_stairs smooth_red_sandstone_stairs smooth_quartz_stairs granite_stairs polished_granite_stairs diorite_stairs "
			+ "polished_diorite_stairs andesite_stairs polished_andesite_stairs mossy_stone_brick_stairs mossy_cobblestone_stairs stone_stairs "
			+ "red_sandstone_stairs; stone_brick_wall mossy_stone_brick_wall sandstone_wall brick_wall nether_brick_wall granite_wall diorite_wall "
			+ "andesite_wall red_sandstone_wall", got, "26.3's names");
	}

	/** 26.3's strength for each, a pickaxe, stone's sound (the nether brick wall nether bricks'). */
	public void testStrengthToolAndSound() {
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String entry : STRENGTH) {
				String[] parts = entry.split(" ");
				Block block = block(parts[0]);
				int meta = Integer.parseInt(parts[1]);
				world().setBlock(X, Y, Z, block, meta, 3);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z);
				String sound = "nether_brick_wall".equals(parts[0]) ? McFly.MC_ASSETS + ":block.nether_bricks.break" : "dig.stone";
				if (!(parts[2] + " " + parts[3]).equals(got) || !"pickaxe".equals(block.getHarvestTool(meta)) || block.getHarvestLevel(meta) != 0
					|| !sound.equals(block.stepSound.getBreakSound())) {
					found.add(parts[0] + ":" + meta + " " + got + " " + block.getHarvestTool(meta) + " " + block.getHarvestLevel(meta) + " "
						+ block.stepSound.getBreakSound());
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength, the pickaxe, sounds (want: as listed)");
	}

	/** 26.3's map colours (granite dirt's, diorite and smooth quartz quartz's, red sandstone orange); the slabs, stairs and walls their block's. */
	public void testMapColours() {
		Object[][] want = {{"stone", 1, MapColor.dirtColor}, {"stone", 2, MapColor.dirtColor}, {"stone", 3, MapColor.quartzColor},
			{"stone", 4, MapColor.quartzColor}, {"stone", 5, MapColor.stoneColor}, {"stone", 6, MapColor.stoneColor}, {"smooth_stone", 0, MapColor.stoneColor},
			{"smooth_sandstone", 0, MapColor.sandColor}, {"smooth_red_sandstone", 0, MapColor.adobeColor}, {"smooth_quartz", 0, MapColor.quartzColor},
			{"red_sandstone", 0, MapColor.adobeColor}, {"red_sandstone", 1, MapColor.adobeColor}, {"red_sandstone", 2, MapColor.adobeColor},
			{"stone_slab_2", 0, MapColor.dirtColor}, {"stone_wall_2", 1, MapColor.quartzColor}, {"red_sandstone_wall", 0, MapColor.adobeColor},
			{"smooth_quartz_stairs", 0, MapColor.quartzColor}, {"red_sandstone_slab", 1, MapColor.adobeColor}, {"granite_stairs", 0, MapColor.dirtColor}};
		List<String> wrong = new ArrayList<>();
		for (Object[] entry : want) {
			MapColor got = block((String) entry[0]).getMapColor((Integer) entry[1]);
			if (got != entry[2]) {
				wrong.add(entry[0] + ":" + entry[1] + " " + got.colorIndex + " (want " + ((MapColor) entry[2]).colorIndex + ")");
			}
		}
		assertEqual("[]", wrong.toString(), "map colours");
	}

	/** EFR's meta 0 of `stone`, never in a tab: read as granite, granite's look and strength, and it drops granite (meta 1), as picking it does. */
	public void testMetaZeroIsGranite() {
		String got = onServer(() -> {
			Block stone = block("stone");
			world().setBlock(X, Y, Z, stone, 0, 3);
			List<ItemStack> drops = stone.getDrops(world(), X, Y, Z, 0, 0);
			return name(stone, 0) + " " + name(stone, 1) + " " + (drops.size() == 1 ? stack(drops.get(0)) : drops.toString()) + " " + stone.getDamageValue(world(), X, Y, Z);
		});
		assertEqual("granite granite stone:1 1", got, "stone:0's and stone:1's names, stone:0's drop and picked meta");
	}

	/** 1.7.10's seamless double stone slabs (8, 9, 15) are 26.3's smooth stone, sandstone and quartz: broken, they drop those. */
	public void testSeamlessDoubleSlabsDropSmoothBlocks() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta : new int[]{8, 9, 15, 0}) {
				world().setBlock(X, Y, Z, Blocks.double_stone_slab, meta, 3);
				ArrayList<ItemStack> drops = Blocks.double_stone_slab.getDrops(world(), X, Y, Z, meta, 0);
				ForgeEventFactory.fireBlockHarvesting(drops, world(), Blocks.double_stone_slab, X, Y, Z, meta, 0, 1.0F, false, player());
				List<String> names = new ArrayList<>();
				for (ItemStack drop : drops) {
					names.add(stack(drop));
				}
				out.append(meta).append(' ').append(names).append("; ");
			}
			return out.toString();
		});
		assertEqual("8 [smooth_stone:0]; 9 [smooth_sandstone:0]; 15 [smooth_quartz:0]; 0 [minecraft:stone_slab:0, minecraft:stone_slab:0]; ", got, "the double slabs' drops");
	}

	/** EFR's ore names for the six (other mods' recipes and EFR's own read them). */
	public void testOreNames() {
		String[] names = {"stoneGranite", "stoneGranitePolished", "stoneDiorite", "stoneDioritePolished", "stoneAndesite", "stoneAndesitePolished"};
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < names.length; i++) {
			boolean has = false;
			for (ItemStack ore : OreDictionary.getOres(names[i])) {
				has |= ore.getItem() == Item.getItemFromBlock(block("stone")) && ore.getItemDamage() == i + 1;
			}
			if (!has) {
				wrong.add(names[i]);
			}
		}
		assertEqual("[]", wrong.toString(), "ore names without our stone");
	}

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("stones_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(62, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
