package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlyKindLog;
import io.github.asgerjon.mcfly.base.McFlyKindLog.Kind;
import io.github.asgerjon.mcfly.base.McFlyLog;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The wood sets' logs (SPEC-modern-movement.md §91.3, step A), in EFR's ids and metas: the vanilla woods' stripped logs,
 * wood (bark) and stripped wood, four woods or two an id (`& 3` the wood: oak, spruce, birch, jungle, then acacia, dark
 * oak); the cherry log's four kinds; the bamboo block and its stripped kind; Biomes O' Plenty's and Witchery's, by EFR's
 * names and textures. 26.3's map colours as 1.7.10's ids where it has them, else its nearest (white terracotta cloth,
 * grey terracotta black, pink terracotta red). Their recipes (26.3's), charcoal and EFR's ore name `logWood`.
 */
public final class WoodLogs {

	private WoodLogs() {
	}

	private static final String[] OLD = {"oak", "spruce", "birch", "jungle"}, NEW = {"acacia", "dark_oak"};
	/** 1.7.10's own log textures for the vanilla woods (`log_<name>`; dark oak is `big_oak`). */
	private static final String[] OLD_BARK = {"log_oak", "log_spruce", "log_birch", "log_jungle"}, NEW_BARK = {"log_acacia", "log_big_oak"};
	/** The planks' colours (the stripped logs' and woods'), then the woods' own (acacia's grey). */
	private static final MapColor[] OLD_COLOUR = {MapColor.woodColor, MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor};
	private static final MapColor[] NEW_COLOUR = {MapColor.adobeColor, MapColor.brownColor}, NEW_WOOD_COLOUR = {MapColor.grayColor, MapColor.brownColor};

	/** `log_stripped` (old) or `log2_stripped` (new). */
	public static Block strippedLogs(boolean old) {
		String[] woods = old ? OLD : NEW;
		MapColor[] colours = old ? OLD_COLOUR : NEW_COLOUR;
		Kind[] kinds = new Kind[woods.length];
		for (int i = 0; i < woods.length; i++) {
			kinds[i] = Kind.log("stripped_" + woods[i] + "_log", colours[i]);
		}
		return new McFlyKindLog(old ? "log_stripped" : "log2_stripped", Block.soundTypeWood, kinds);
	}

	/** `bark` (old) or `bark2` (new): the woods, 1.7.10's bark all round. */
	public static Block woods(boolean old) {
		String[] woods = old ? OLD : NEW;
		String[] bark = old ? OLD_BARK : NEW_BARK;
		MapColor[] colours = old ? OLD_COLOUR : NEW_WOOD_COLOUR;
		Kind[] kinds = new Kind[woods.length];
		for (int i = 0; i < woods.length; i++) {
			kinds[i] = Kind.wood(woods[i] + "_wood", colours[i], bark[i]);
		}
		return new McFlyKindLog(old ? "bark" : "bark2", Block.soundTypeWood, kinds);
	}

	/** `wood_stripped` (old) or `wood2_stripped` (new). */
	public static Block strippedWoods(boolean old) {
		String[] woods = old ? OLD : NEW;
		MapColor[] colours = old ? OLD_COLOUR : NEW_COLOUR;
		Kind[] kinds = new Kind[woods.length];
		for (int i = 0; i < woods.length; i++) {
			kinds[i] = Kind.wood("stripped_" + woods[i] + "_wood", colours[i], "stripped_" + woods[i] + "_log");
		}
		return new McFlyKindLog(old ? "wood_stripped" : "wood2_stripped", Block.soundTypeWood, kinds);
	}

	/** `cherry_log`: log, wood, stripped log, stripped wood; the log white and grey terracotta, stripped white and pink. */
	public static Block cherryLog() {
		return new McFlyLog("cherry", McFlySoundType.CHERRY_WOOD, MapColor.clothColor, MapColor.blackColor, MapColor.blackColor, MapColor.clothColor,
			MapColor.redColor, MapColor.redColor);
	}

	/** `bamboo_block`: 0 the block (yellow upright, plant green lying), 1 stripped (yellow). */
	public static Block bambooBlock() {
		return new McFlyKindLog("bamboo_block", McFlySoundType.BAMBOO_WOOD,
			new Kind("bamboo_block", MapColor.yellowColor, MapColor.foliageColor, "bamboo_block", "bamboo_block_top"),
			Kind.log("stripped_bamboo_block", MapColor.yellowColor));
	}

	/**
	 * A mod's logs as EFR made them (`BlockStrippedLogCompat`): `<mod>_[stripped_]<wood>_(log|wood)`, its textures in the
	 * mod's domain (Biomes O' Plenty's `biomesoplenty`): a stripped kind `stripped_<wood>_log` (`_top` its end), a wood
	 * the mod's own bark (`log_<wood>_side` for Biomes O' Plenty, `bigflowerstem_side` its stem; Witchery's `log_<wood>`).
	 */
	public static Block compat(String mod, String id, boolean wood, boolean stripped, String... woods) {
		String domain = mod.equals("bop") ? "biomesoplenty" : mod;
		Kind[] kinds = new Kind[woods.length];
		for (int i = 0; i < woods.length; i++) {
			String name = mod + "_" + (stripped ? "stripped_" : "") + woods[i] + (wood ? "_wood" : "_log");
			String side = stripped ? domain + ":stripped_" + woods[i] + "_log"
				: mod.equals("bop") ? domain + (woods[i].equals("bigflowerstem") ? ":bigflowerstem_side" : ":log_" + woods[i] + "_side") : domain + ":log_" + woods[i];
			String end = wood ? side : domain + ":stripped_" + woods[i] + "_log_top";
			kinds[i] = new Kind(name, MapColor.woodColor, MapColor.woodColor, side, end);
		}
		return new McFlyKindLog(id, Block.soundTypeWood, kinds);
	}

	// ---------------------------------------------------------------- recipes, charcoal, ore names

	public static void register() {
		McFlyBlocks[][] oldAndNew = {{McFlyBlocks.LOG_STRIPPED, McFlyBlocks.BARK, McFlyBlocks.WOOD_STRIPPED}, {McFlyBlocks.LOG2_STRIPPED, McFlyBlocks.BARK2, McFlyBlocks.WOOD2_STRIPPED}};
		for (int set = 0; set < 2; set++) {
			Block vanillaLog = set == 0 ? Blocks.log : Blocks.log2;
			int woods = set == 0 ? 4 : 2;
			McFlyBlocks strippedLog = oldAndNew[set][0], wood = oldAndNew[set][1], strippedWood = oldAndNew[set][2];
			for (int i = 0; i < woods; i++) {
				ItemStack planks = new ItemStack(Blocks.planks, 4, set * 4 + i);
				if (wood.isEnabled()) {
					McFlyRecipes.shaped(wood.newItemStack(3, i), "##", "##", '#', new ItemStack(vanillaLog, 1, i));
					McFlyRecipes.shapeless(planks, wood.newItemStack(1, i));
				}
				if (strippedLog.isEnabled()) {
					McFlyRecipes.shapeless(planks, strippedLog.newItemStack(1, i));
					if (strippedWood.isEnabled()) {
						McFlyRecipes.shaped(strippedWood.newItemStack(3, i), "##", "##", '#', strippedLog.newItemStack(1, i));
						McFlyRecipes.shapeless(planks, strippedWood.newItemStack(1, i));
					}
				}
				for (McFlyBlocks kind : oldAndNew[set]) {
					if (kind.isEnabled()) {
						GameRegistry.addSmelting(kind.newItemStack(1, i), new ItemStack(Items.coal, 1, 1), 0.15F);
						OreDictionary.registerOre("logWood", kind.newItemStack(1, i));
					}
				}
			}
		}
		Block efrPlanks = io.github.asgerjon.mcfly.registry.McFlyBlocks.WOOD_PLANKS.isEnabled() ? io.github.asgerjon.mcfly.registry.McFlyBlocks.WOOD_PLANKS.get() : null;
		// one wood's four kinds in an id: wood of logs, stripped wood of stripped logs (those two the caller's for
		// mangrove, CaveRecipes), planks of any kind (EFR's wood_planks till step B), charcoal, logWood
		for (McFlyBlocks log : new McFlyBlocks[]{McFlyBlocks.CHERRY_LOG, McFlyBlocks.MANGROVE_LOG}) {
			if (!log.isEnabled()) {
				continue;
			}
			if (log == McFlyBlocks.CHERRY_LOG) {
				McFlyRecipes.shaped(log.newItemStack(3, 1), "##", "##", '#', log.newItemStack(1, 0));
				McFlyRecipes.shaped(log.newItemStack(3, 3), "##", "##", '#', log.newItemStack(1, 2));
			}
			for (int kind = 0; kind < 4; kind++) {
				if (efrPlanks != null) {
					McFlyRecipes.shapeless(new ItemStack(efrPlanks, 4, log == McFlyBlocks.CHERRY_LOG ? 3 : 2), log.newItemStack(1, kind));
				}
				GameRegistry.addSmelting(log.newItemStack(1, kind), new ItemStack(Items.coal, 1, 1), 0.15F);
				OreDictionary.registerOre("logWood", log.newItemStack(1, kind));
			}
		}
		for (McFlyBlocks stem : new McFlyBlocks[]{McFlyBlocks.CRIMSON_STEM, McFlyBlocks.WARPED_STEM}) {
			if (stem.isEnabled()) {
				for (int kind = 0; kind < 4; kind++) {
					OreDictionary.registerOre("logWood", stem.newItemStack(1, kind));
				}
			}
		}
		if (McFlyBlocks.BAMBOO_BLOCK.isEnabled()) {
			if (efrPlanks != null) {
				McFlyRecipes.shapeless(new ItemStack(efrPlanks, 2, 4), McFlyBlocks.BAMBOO_BLOCK.newItemStack(1, 0));
				McFlyRecipes.shapeless(new ItemStack(efrPlanks, 2, 4), McFlyBlocks.BAMBOO_BLOCK.newItemStack(1, 1));
			}
			if (io.github.asgerjon.mcfly.registry.McFlyItems.BAMBOO.isEnabled()) {
				ItemStack bamboo = io.github.asgerjon.mcfly.registry.McFlyItems.BAMBOO.newItemStack();
				McFlyRecipes.shaped(McFlyBlocks.BAMBOO_BLOCK.newItemStack(), "###", "###", "###", '#', bamboo);
			}
		}
		for (McFlyBlocks compat : new McFlyBlocks[]{McFlyBlocks.BOP_WOOD, McFlyBlocks.BOP_WOOD2, McFlyBlocks.BOP_WOOD3, McFlyBlocks.BOP_WOOD4,
			McFlyBlocks.BOP_LOG_STRIPPED, McFlyBlocks.BOP_LOG_STRIPPED2, McFlyBlocks.BOP_LOG_STRIPPED3, McFlyBlocks.BOP_LOG_STRIPPED4,
			McFlyBlocks.BOP_WOOD_STRIPPED, McFlyBlocks.BOP_WOOD_STRIPPED2, McFlyBlocks.BOP_WOOD_STRIPPED3, McFlyBlocks.BOP_WOOD_STRIPPED4,
			McFlyBlocks.WITCHERY_WOOD, McFlyBlocks.WITCHERY_LOG_STRIPPED, McFlyBlocks.WITCHERY_WOOD_STRIPPED}) {
			if (compat.isEnabled()) {
				int kinds = ((McFlyKindLog) compat.get()).kinds();
				for (int kind = 0; kind < kinds; kind++) {
					OreDictionary.registerOre("logWood", compat.newItemStack(1, kind));
				}
			}
		}
	}
}
