package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyMetaBlock;
import io.github.asgerjon.mcfly.base.McFlyStairs;
import io.github.asgerjon.mcfly.base.McFlyVariantSlab;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * The wood sets' planks, slabs, stairs and the bamboo mosaic (SPEC-modern-movement.md §91.4, step B), in EFR's ids and
 * metas: `wood_planks` and its slab and double (0 crimson, 1 warped, 2 mangrove, 3 cherry, 4 bamboo), each wood's
 * stairs, the mosaic, its slab (EFR crafted it at damage 5: read as the slab) and stairs. 26.3's 2 and 3, the axe's,
 * sounds by wood, map colours as 1.7.10's nearest; fire 5 and 20 but for crimson and warped (`#non_flammable_wood`).
 */
public final class WoodPlanks {

	private WoodPlanks() {
	}

	public static final String[] WOODS = {"crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final MapColor[] COLOURS = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};

	/** Whether the wood burns (crimson and warped don't). */
	public static boolean burns(int wood) {
		return wood >= 2;
	}

	/** Whether the wood's set is on: crimson's and warped's with the Nether's plants, mangrove's, cherry's, bamboo's. */
	public static boolean enabled(int wood) {
		switch (wood) {
			case 0:
			case 1:
				return McFlyConfig.enableNetherPlants;
			case 2:
				return McFlyConfig.enableMangrove;
			case 3:
				return McFlyConfig.enableCherryBlocks;
			default:
				return McFlyConfig.enableBambooBlocks;
		}
	}

	public static boolean anyEnabled() {
		for (int wood = 0; wood < WOODS.length; wood++) {
			if (enabled(wood)) {
				return true;
			}
		}
		return false;
	}

	/** The wood's sound: nether wood, the wood's own, cherry wood, bamboo wood. */
	public static Block.SoundType sound(int wood) {
		return wood < 2 ? McFlySoundType.NETHER_WOOD : wood == 3 ? McFlySoundType.CHERRY_WOOD : wood == 4 ? McFlySoundType.BAMBOO_WOOD : Block.soundTypeWood;
	}

	// ---------------------------------------------------------------- the blocks

	/** `wood_planks`: the five, fire by wood, only the woods that are on in the tab. */
	public static final class Planks extends McFlyMetaBlock {
		Planks(Variant... variants) {
			super(Material.wood, Block.soundTypeWood, variants);
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burns(damageDropped(world.getBlockMetadata(x, y, z)));
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return isFlammable(world, x, y, z, face) ? 20 : 0;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return isFlammable(world, x, y, z, face) ? 5 : 0;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			for (int wood = 0; wood < WOODS.length; wood++) {
				if (enabled(wood)) {
					list.add(new ItemStack(item, 1, wood));
				}
			}
		}
	}

	/** A wooden slab of variants (`wood_slab`, the mosaic's): fire by its burning variants. */
	public static final class Slab extends McFlyVariantSlab {
		private final boolean mosaic;

		Slab(boolean mosaic, Variant... variants) {
			super(variants);
			this.mosaic = mosaic;
		}

		Slab(Slab single) {
			super(single);
			this.mosaic = single.mosaic;
		}

		private boolean burnsAt(IBlockAccess world, int x, int y, int z) {
			return mosaic || burns(damageDropped(world.getBlockMetadata(x, y, z)));
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burnsAt(world, x, y, z);
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burnsAt(world, x, y, z) ? 20 : 0;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burnsAt(world, x, y, z) ? 5 : 0;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			if (isDouble()) {
				return;
			}
			if (mosaic) {
				list.add(new ItemStack(item, 1, 0));
				return;
			}
			for (int wood = 0; wood < WOODS.length; wood++) {
				if (enabled(wood)) {
					list.add(new ItemStack(item, 1, wood));
				}
			}
		}
	}

	/** A wood's stairs (or the mosaic's): its sound, its fire. */
	public static final class Stairs extends McFlyStairs {
		private final boolean burns;

		Stairs(Block base, int baseMeta, String name, boolean burns) {
			super(base, baseMeta, name);
			this.burns = burns;
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burns;
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burns ? 20 : 0;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return burns ? 5 : 0;
		}
	}

	/** The bamboo mosaic: 2 and 3, bamboo wood, yellow, fire 5 and 20. */
	public static final class Mosaic extends McFlyBlock {
		Mosaic() {
			super(Material.wood);
			setNames("bamboo_mosaic");
			strength(2.0F, 3.0F);
			tool("axe", 0);
			sound(McFlySoundType.BAMBOO_WOOD);
			colour(MapColor.yellowColor);
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return true;
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 20;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 5;
		}
	}

	public static Block planks() {
		McFlyMetaBlock.Variant[] variants = new McFlyMetaBlock.Variant[WOODS.length];
		for (int wood = 0; wood < WOODS.length; wood++) {
			variants[wood] = new McFlyMetaBlock.Variant(WOODS[wood] + "_planks", 2.0F, 3.0F, COLOURS[wood]);
		}
		return new Planks(variants).tool("axe", 0);
	}

	public static Block slab(Block planks) {
		McFlyVariantSlab.Variant[] variants = new McFlyVariantSlab.Variant[WOODS.length];
		for (int wood = 0; wood < WOODS.length; wood++) {
			variants[wood] = new McFlyVariantSlab.Variant(WOODS[wood] + "_slab", planks, wood, 2.0F, 3.0F);
		}
		return new Slab(false, variants).tool("axe", 0);
	}

	public static Block doubled(Block single) {
		return new Slab((Slab) single);
	}

	public static Block stairs(Block planks, int wood) {
		return new Stairs(planks, wood, WOODS[wood] + "_stairs", burns(wood)).strength(2.0F, 3.0F).tool("axe", 0).sound(sound(wood));
	}

	public static Block mosaic() {
		return new Mosaic();
	}

	public static Block mosaicSlab(Block mosaic) {
		return new Slab(true, new McFlyVariantSlab.Variant("bamboo_mosaic_slab", mosaic, 0, 2.0F, 3.0F)).tool("axe", 0).sound(McFlySoundType.BAMBOO_WOOD);
	}

	public static Block mosaicStairs(Block mosaic) {
		return new Stairs(mosaic, 0, "bamboo_mosaic_stairs", true).strength(2.0F, 3.0F).tool("axe", 0).sound(McFlySoundType.BAMBOO_WOOD);
	}

	// ---------------------------------------------------------------- recipes, ore names

	/** 26.3's recipes: slabs 6 of 3 planks, stairs 4; the mosaic of two bamboo slabs, its slab 6, its stairs 4. EFR's ore names. */
	public static void register() {
		if (!McFlyBlocks.WOOD_PLANKS.isEnabled()) {
			return;
		}
		// here, not a static field: this class is loaded while McFlyBlocks' entries are still being made
		McFlyBlocks[] stairsOf = {McFlyBlocks.CRIMSON_STAIRS, McFlyBlocks.WARPED_STAIRS, McFlyBlocks.MANGROVE_STAIRS, McFlyBlocks.CHERRY_STAIRS,
			McFlyBlocks.BAMBOO_STAIRS};
		for (int wood = 0; wood < WOODS.length; wood++) {
			if (!enabled(wood)) {
				continue;
			}
			ItemStack planks = McFlyBlocks.WOOD_PLANKS.newItemStack(1, wood);
			OreDictionary.registerOre("plankWood", planks);
			McFlyRecipes.shaped(McFlyBlocks.WOOD_SLAB.newItemStack(6, wood), "###", '#', planks);
			OreDictionary.registerOre("slabWood", McFlyBlocks.WOOD_SLAB.newItemStack(1, wood));
			if (stairsOf[wood].isEnabled()) {
				McFlyRecipes.shaped(stairsOf[wood].newItemStack(4), "#  ", "## ", "###", '#', planks);
				OreDictionary.registerOre("stairWood", stairsOf[wood].newItemStack());
			}
		}
		if (McFlyBlocks.BAMBOO_MOSAIC.isEnabled()) {
			ItemStack mosaic = McFlyBlocks.BAMBOO_MOSAIC.newItemStack();
			McFlyRecipes.shaped(mosaic, "#", "#", '#', McFlyBlocks.WOOD_SLAB.newItemStack(1, 4));
			McFlyRecipes.shaped(McFlyBlocks.BAMBOO_MOSAIC_SLAB.newItemStack(6), "###", '#', mosaic);
			McFlyRecipes.shaped(McFlyBlocks.BAMBOO_MOSAIC_STAIRS.newItemStack(4), "#  ", "## ", "###", '#', mosaic);
		}
	}
}
