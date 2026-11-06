package io.github.asgerjon.mcfly.caves;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraftforge.oredict.OreDictionary;

import static io.github.asgerjon.mcfly.registry.McFlyRecipes.shaped;

/**
 * 26.3's recipes for the caves (SPEC-modern-movement.md §90): the amethyst block of 4 shards, 2 tinted glass of 4 shards
 * round glass (by EFR's ore names `gemAmethyst` and `blockGlassColorless`, kept for other mods); the ore names
 * `gemAmethyst` and `blockGlassTinted` on ours.
 */
public final class CaveRecipes {

	private CaveRecipes() {
	}

	public static void register() {
		if (McFlyBlocks.MOSS_BLOCK.isEnabled()) {
			// moss (step D): the carpet, mossy cobblestone and stone bricks of moss (EFR's moss of 8 vines round dirt gone: 26.3 has none)
			shaped(McFlyBlocks.MOSS_CARPET.newItemStack(3), "##", '#', McFlyBlocks.MOSS_BLOCK.newItemStack());
			io.github.asgerjon.mcfly.registry.McFlyRecipes.shapeless(new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.mossy_cobblestone),
				new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone), McFlyBlocks.MOSS_BLOCK.newItemStack());
			io.github.asgerjon.mcfly.registry.McFlyRecipes.shapeless(new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.stonebrick, 1, 1),
				new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.stonebrick, 1, 0), McFlyBlocks.MOSS_BLOCK.newItemStack());
			OreDictionary.registerOre("treeLeaves", McFlyBlocks.AZALEA_LEAVES.newItemStack(1, OreDictionary.WILDCARD_VALUE));
			OreDictionary.registerOre("treeSapling", McFlyBlocks.AZALEA.newItemStack(1, OreDictionary.WILDCARD_VALUE));
		}
		if (McFlyBlocks.MANGROVE_LOG.isEnabled()) {
			// mangrove (step E, spec §90.6): wood of logs, stripped wood of stripped logs, muddy roots of mud and roots (the planks are the wood set's)
			shaped(McFlyBlocks.MANGROVE_LOG.newItemStack(3, 1), "##", "##", '#', McFlyBlocks.MANGROVE_LOG.newItemStack(1, 0));
			shaped(McFlyBlocks.MANGROVE_LOG.newItemStack(3, 3), "##", "##", '#', McFlyBlocks.MANGROVE_LOG.newItemStack(1, 2));
			if (McFlyBlocks.MUD.isEnabled()) {
				io.github.asgerjon.mcfly.registry.McFlyRecipes.shapeless(McFlyBlocks.MUDDY_MANGROVE_ROOTS.newItemStack(), McFlyBlocks.MUD.newItemStack(),
					McFlyBlocks.MANGROVE_ROOTS.newItemStack());
			}
		}
		if (McFlyBlocks.POINTED_DRIPSTONE.isEnabled()) {
			// dripstone (step F, spec §90.7): the block of four pointed
			shaped(McFlyBlocks.DRIPSTONE_BLOCK.newItemStack(), "##", "##", '#', McFlyBlocks.POINTED_DRIPSTONE.newItemStack());
		}
		if (McFlyBlocks.MUD.isEnabled()) {
			// mud (step C): packed of mud and wheat, bricks of packed mud, their slab, stairs and wall
			io.github.asgerjon.mcfly.registry.McFlyRecipes.shapeless(McFlyBlocks.PACKED_MUD.newItemStack(1, 0), McFlyBlocks.MUD.newItemStack(),
				new net.minecraft.item.ItemStack(net.minecraft.init.Items.wheat));
			net.minecraft.item.ItemStack bricks = McFlyBlocks.PACKED_MUD.newItemStack(1, 1);
			shaped(McFlyBlocks.PACKED_MUD.newItemStack(4, 1), "##", "##", '#', McFlyBlocks.PACKED_MUD.newItemStack(1, 0));
			shaped(McFlyBlocks.MUD_BRICK_SLAB.newItemStack(6), "###", '#', bricks);
			shaped(McFlyBlocks.MUD_BRICK_STAIRS.newItemStack(4), "#  ", "## ", "###", '#', bricks);
			shaped(McFlyBlocks.MUD_BRICK_WALL.newItemStack(6), "###", "###", '#', bricks);
		}
		if (McFlyItems.AMETHYST_SHARD.isEnabled()) {
			OreDictionary.registerOre("gemAmethyst", McFlyItems.AMETHYST_SHARD.newItemStack());
			shaped(McFlyBlocks.AMETHYST_BLOCK.newItemStack(), "##", "##", '#', "gemAmethyst");
			shaped(McFlyBlocks.TINTED_GLASS.newItemStack(2), " S ", "SGS", " S ", 'S', "gemAmethyst", 'G', "blockGlassColorless");
			OreDictionary.registerOre("blockGlassTinted", McFlyBlocks.TINTED_GLASS.newItemStack());
		}
		if (McFlyBlocks.CALIBRATED_SCULK_SENSOR.isEnabled() && McFlyItems.AMETHYST_SHARD.isEnabled()) {
			// the calibrated sculk sensor (step H2, spec §90.9): three shards over a sensor
			shaped(McFlyBlocks.CALIBRATED_SCULK_SENSOR.newItemStack(), " # ", "#X#", '#', "gemAmethyst", 'X', McFlyBlocks.SCULK_SENSOR.newItemStack());
		}
	}
}
