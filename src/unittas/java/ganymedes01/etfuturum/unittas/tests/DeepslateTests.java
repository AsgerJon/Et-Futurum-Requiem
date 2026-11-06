package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deepslate and its ores as our own (SPEC-modern-movement.md §89, step B), as 26.3's: ours, EFR's classes gone; EFR's
 * stored form read as before; 26.3's strength (deepslate 3 and 6, cobbled and polished 3.5 and 6, the ores 4.5 and 3),
 * tools and sounds (polished deepslate its own); deepslate dropping cobbled deepslate; the ores dropping as the stone
 * ones do; EFR's `DeepslateOreRegistry` answering from McFly's. At z 200 on the flat world.
 */
@TasSuite("deepslate")
public class DeepslateTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 200;
	private static final String[] ORES = {"coal", "iron", "gold", "redstone", "lapis", "diamond", "emerald"};
	private static final String[] EFR_CLASSES = {"blocks.BlockDeepslate", "blocks.ores.BlockDeepslateOre", "blocks.ores.BlockDeepslateRedstoneOre"};

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

	public void testDeepslateIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		List<String> names = new ArrayList<>();
		java.util.Collections.addAll(names, "deepslate", "cobbled_deepslate", "polished_deepslate", "deepslate_lit_redstone_ore");
		for (String ore : ORES) {
			names.add("deepslate_" + ore + "_ore");
		}
		for (String name : names) {
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
		assertEqual("[]", wrong.toString(), "deepslate's classes");
	}

	/** EFR's ids and metas by 26.3's names: deepslate's axis, the ores, lit redstone. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta : new int[]{0, 4, 8}) {
				out.append(props(block("deepslate"), meta)).append(' ');
			}
			out.append(props(block("cobbled_deepslate"), 0)).append(' ').append(props(block("polished_deepslate"), 0)).append("; ");
			for (String ore : ORES) {
				out.append(props(block("deepslate_" + ore + "_ore"), 0).replace(",lit=false", "")).append(' ');
			}
			out.append(props(block("deepslate_lit_redstone_ore"), 0));
			return out.toString();
		});
		assertEqual("deepslate,axis=y deepslate,axis=x deepslate,axis=z cobbled_deepslate polished_deepslate; deepslate_coal_ore deepslate_iron_ore "
			+ "deepslate_gold_ore deepslate_redstone_ore deepslate_lapis_ore deepslate_diamond_ore deepslate_emerald_ore deepslate_redstone_ore,lit=true",
			got, "26.3's names");
	}

	/** 26.3's strength, the pickaxe's level, and sounds. */
	public void testStrengthAndSounds() {
		String[][] blocks = {{"deepslate", "3.0 6.0 0", "deepslate"}, {"cobbled_deepslate", "3.5 6.0 0", "deepslate"},
			{"polished_deepslate", "3.5 6.0 0", "polished_deepslate"}, {"deepslate_coal_ore", "4.5 3.0 0", "deepslate"},
			{"deepslate_iron_ore", "4.5 3.0 1", "deepslate"}, {"deepslate_gold_ore", "4.5 3.0 2", "deepslate"},
			{"deepslate_redstone_ore", "4.5 3.0 2", "deepslate"}, {"deepslate_lapis_ore", "4.5 3.0 1", "deepslate"},
			{"deepslate_diamond_ore", "4.5 3.0 2", "deepslate"}, {"deepslate_emerald_ore", "4.5 3.0 2", "deepslate"}};
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (String[] entry : blocks) {
				Block block = block(entry[0]);
				world().setBlock(X, Y, Z, block, 0, 3);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z) + " "
					+ block.getHarvestLevel(0);
				String sound = block.stepSound.getBreakSound();
				if (!entry[1].equals(got) || !(McFly.MC_ASSETS + ":block." + entry[2] + ".break").equals(sound) || !"pickaxe".equals(block.getHarvestTool(0))) {
					found.add(entry[0] + " " + got + " " + block.getHarvestTool(0) + " " + sound);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "strength, the pickaxe's level, sounds (want: as listed)");
	}

	/** Deepslate drops cobbled deepslate; the ores drop as the stone ores do (coal, diamonds, emeralds; iron and gold themselves). */
	public void testDrops() {
		String got = onServer(() -> {
			Random random = new Random(1);
			StringBuilder out = new StringBuilder();
			out.append(Item.itemRegistry.getNameForObject(block("deepslate").getItemDropped(0, random, 0)).replace(McFly.DOMAIN + ":", "")).append(' ');
			out.append(block("deepslate_coal_ore").getItemDropped(0, random, 0) == Items.coal).append(' ');
			out.append(block("deepslate_diamond_ore").getItemDropped(0, random, 0) == Items.diamond).append(' ');
			out.append(block("deepslate_emerald_ore").getItemDropped(0, random, 0) == Items.emerald);
			return out.toString();
		});
		assertEqual("cobbled_deepslate true true true", got, "the drops");
	}

	/** EFR's `DeepslateOreRegistry` (other mods read it) gives McFly's ores for the stone ones. */
	public void testOreRegistryAnswersWithOurs() throws Exception {
		Class<?> registry = Class.forName("ganymedes01.etfuturum.api.DeepslateOreRegistry");
		Object mapping = registry.getMethod("getOre", Block.class, int.class).invoke(null, Blocks.diamond_ore, 0);
		Object ore = mapping == null ? null : mapping.getClass().getMethod("getObject").invoke(mapping);
		assertTrue(ore == block("deepslate_diamond_ore"), "the diamond ore's deepslate one: " + ore);
	}
}
