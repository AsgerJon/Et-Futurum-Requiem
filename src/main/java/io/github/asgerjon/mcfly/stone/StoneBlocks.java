package io.github.asgerjon.mcfly.stone;

import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyMetaBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

/**
 * The old stones' blocks (SPEC-modern-movement.md §89, steps D and E), as 26.3's, in EFR's stored form: granite,
 * diorite, andesite and the polished ones (`etfuturum:stone` 1 to 6), red sandstone (3), the smooth blocks, tuff (5),
 * calcite.
 */
public final class StoneBlocks {

	private StoneBlocks() {
	}

	/**
	 * Granite, diorite, andesite and the polished ones as 26.3's (1.5 and 6; granite dirt's map colour, diorite quartz's,
	 * andesite stone's), at EFR's metas 1 to 6. Meta 0 is EFR's look-alike of granite, never in a tab: it is granite, and
	 * drops and picks as meta 1. Ores generate in it as in stone (EFR's veins hold them).
	 */
	public static final class OldStone extends McFlyMetaBlock {

		public OldStone() {
			super(Material.rock, Block.soundTypeStone, new Variant("granite", 1.5F, 6.0F, MapColor.dirtColor),
				new Variant("granite", 1.5F, 6.0F, MapColor.dirtColor), new Variant("polished_granite", 1.5F, 6.0F, MapColor.dirtColor),
				new Variant("diorite", 1.5F, 6.0F, MapColor.quartzColor), new Variant("polished_diorite", 1.5F, 6.0F, MapColor.quartzColor),
				new Variant("andesite", 1.5F, 6.0F, MapColor.stoneColor), new Variant("polished_andesite", 1.5F, 6.0F, MapColor.stoneColor));
			tool("pickaxe", 0);
		}

		@Override
		public int damageDropped(int meta) {
			return meta >= 1 && meta < variants() ? meta : 1;
		}

		@Override
		public boolean isVariant(int meta) {
			return meta >= 1 && meta < variants();
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			for (int meta = 1; meta < variants(); meta++) {
				list.add(new ItemStack(item, 1, meta));
			}
		}

		@Override
		public boolean isReplaceableOreGen(World world, int x, int y, int z, Block target) {
			return this == target || target == Blocks.stone;
		}
	}

	/**
	 * Tuff as 26.3's (1.5 and 6, terracotta's gray: 1.7.10's gray), EFR's metas: 0 tuff, 1 polished, 2 bricks, 3 chiseled,
	 * 4 chiseled bricks (the chiseled ones' tops their own). Tuff's own sound; polished tuff's and the bricks' by the client's
	 * per-variant table. Tuff itself takes ores as stone and deepslate do (EFR's layer and veins put it among them).
	 */
	public static final class Tuff extends McFlyMetaBlock {

		public Tuff() {
			super(Material.rock, io.github.asgerjon.mcfly.sound.McFlySoundType.TUFF, new Variant("tuff", 1.5F, 6.0F, MapColor.grayColor),
				new Variant("polished_tuff", 1.5F, 6.0F, MapColor.grayColor), new Variant("tuff_bricks", 1.5F, 6.0F, MapColor.grayColor),
				new Variant("chiseled_tuff", "chiseled_tuff", "chiseled_tuff_top", 1.5F, 6.0F, MapColor.grayColor),
				new Variant("chiseled_tuff_bricks", "chiseled_tuff_bricks", "chiseled_tuff_bricks_top", 1.5F, 6.0F, MapColor.grayColor));
			tool("pickaxe", 0);
		}

		@Override
		public boolean isReplaceableOreGen(World world, int x, int y, int z, Block target) {
			boolean replaceable = this == target;
			if (world.getBlock(x, y, z) == this && world.getBlockMetadata(x, y, z) == 0) {
				replaceable |= target == Blocks.stone || target == io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE.get();
				if (replaceable) {
					DeepslateBlocks.Deepslate.doDeepslateRedoCheck(world, x, y, z);
				}
			}
			return replaceable;
		}
	}

	/** Calcite as 26.3's: 0.75 and 0.75 (EFR's 0.45), terracotta's white (1.7.10's nearest: wool's), its own sound. */
	public static Block calcite() {
		return new McFlyBlock(Material.rock).setNames("calcite").strength(0.75F, 0.75F).tool("pickaxe", 0).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.CALCITE)
			.colour(MapColor.clothColor);
	}

	/** Red sandstone as 26.3's (0.8, orange): 0 red sandstone (side, top, bottom), 1 chiseled, 2 cut, 1.7.10 sandstone's layout. */
	public static McFlyMetaBlock redSandstone() {
		return new McFlyMetaBlock(Material.rock, Block.soundTypeStone,
			new McFlyMetaBlock.Variant("red_sandstone", "red_sandstone", "red_sandstone_top", "red_sandstone_bottom", 0.8F, 0.8F, MapColor.adobeColor),
			new McFlyMetaBlock.Variant("chiseled_red_sandstone", "chiseled_red_sandstone", "red_sandstone_top", 0.8F, 0.8F, MapColor.adobeColor),
			new McFlyMetaBlock.Variant("cut_red_sandstone", "cut_red_sandstone", "red_sandstone_top", 0.8F, 0.8F, MapColor.adobeColor)).tool("pickaxe", 0);
	}

	/** A smooth block as 26.3's (2 and 6), by the texture 1.7.10's seamless double slab (or the block's top) has. */
	public static Block smooth(String name, String texture, MapColor colour) {
		return new McFlyBlock(Material.rock).setNames(name).strength(2.0F, 6.0F).tool("pickaxe", 0).colour(colour).setBlockTextureName(texture);
	}
}
