package io.github.asgerjon.mcfly.stone;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import static io.github.asgerjon.mcfly.registry.McFlyRecipes.shaped;
import static io.github.asgerjon.mcfly.registry.McFlyRecipes.shapeless;

/**
 * 26.3's recipes for the old stones (SPEC-modern-movement.md §89, step D): granite, diorite and andesite (granite 1 of a
 * diorite and a quartz, EFR's 2), the polished ones, their slabs, stairs and walls (by EFR's ore names `stoneGranite` and
 * the like, so that other mods' stones make them too); the smooth blocks by smelting (0.1), their slabs and stairs, 1.7.10's
 * stone slab (26.3's smooth stone slab) of smooth stone; red sandstone's set; the vanilla stones' slabs, stairs and walls
 * 1.7.10 lacks; mossy cobblestone and mossy stone bricks with a vine, chiseled stone bricks of two slabs, cracked by
 * smelting (0.1, EFR's 0). EFR's ore names for the six.
 */
public final class StoneRecipes {

	private static final String[] ORES = {"stoneGranite", "stoneGranitePolished", "stoneDiorite", "stoneDioritePolished", "stoneAndesite", "stoneAndesitePolished"};
	private static final String STAIRS_ROW1 = "#  ", STAIRS_ROW2 = "## ", STAIRS_ROW3 = "###";

	private StoneRecipes() {
	}

	public static void register() {
		stones();
		smooth();
		redSandstone();
		vanillaSets();
		tuff();
		if (McFlyConfig.enableStoneBrickRecipes) {
			shapeless(new ItemStack(Blocks.mossy_cobblestone), new ItemStack(Blocks.cobblestone), new ItemStack(Blocks.vine));
			shapeless(new ItemStack(Blocks.stonebrick, 1, 1), new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.vine));
			shaped(new ItemStack(Blocks.stonebrick, 1, 3), "#", "#", '#', new ItemStack(Blocks.stone_slab, 1, 5));
			GameRegistry.addSmelting(new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stonebrick, 1, 2), 0.1F);
		}
	}

	private static void stairs(McFlyBlocks stairs, Object base) {
		shaped(stairs.newItemStack(4), STAIRS_ROW1, STAIRS_ROW2, STAIRS_ROW3, '#', base);
	}

	private static void stones() {
		if (McFlyBlocks.STONE.isEnabled()) {
			for (int i = 0; i < ORES.length; i++) {
				OreDictionary.registerOre(ORES[i], McFlyBlocks.STONE.newItemStack(1, i + 1));
			}
			shapeless(McFlyBlocks.STONE.newItemStack(1, 1), "stoneDiorite", "gemQuartz");
			shaped(McFlyBlocks.STONE.newItemStack(2, 3), "CQ", "QC", 'C', new ItemStack(Blocks.cobblestone), 'Q', "gemQuartz");
			shapeless(McFlyBlocks.STONE.newItemStack(2, 5), "stoneDiorite", new ItemStack(Blocks.cobblestone));
			for (int i = 0; i < ORES.length; i += 2) {
				shaped(McFlyBlocks.STONE.newItemStack(4, i + 2), "SS", "SS", 'S', ORES[i]);
			}
		}
		if (McFlyBlocks.STONE_SLAB_2.isEnabled()) {
			McFlyBlocks[] stairs = {McFlyBlocks.GRANITE_STAIRS, McFlyBlocks.POLISHED_GRANITE_STAIRS, McFlyBlocks.DIORITE_STAIRS,
				McFlyBlocks.POLISHED_DIORITE_STAIRS, McFlyBlocks.ANDESITE_STAIRS, McFlyBlocks.POLISHED_ANDESITE_STAIRS};
			for (int i = 0; i < ORES.length; i++) {
				shaped(McFlyBlocks.STONE_SLAB_2.newItemStack(6, i), "###", '#', ORES[i]);
				stairs(stairs[i], ORES[i]);
			}
			for (int i = 0; i < 3; i++) {
				shaped(McFlyBlocks.STONE_WALL_2.newItemStack(6, i), "###", "###", '#', ORES[i * 2]);
			}
		}
	}

	private static void smooth() {
		if (McFlyBlocks.SMOOTH_STONE.isEnabled()) {
			GameRegistry.addSmelting(new ItemStack(Blocks.stone), McFlyBlocks.SMOOTH_STONE.newItemStack(), 0.1F);
			shaped(new ItemStack(Blocks.stone_slab, 6, 0), "###", '#', McFlyBlocks.SMOOTH_STONE.newItemStack());
		}
		smoothSet(McFlyBlocks.SMOOTH_SANDSTONE, new ItemStack(Blocks.sandstone, 1, 0), McFlyBlocks.SMOOTH_SANDSTONE_SLAB, McFlyBlocks.SMOOTH_SANDSTONE_STAIRS);
		if (McFlyBlocks.RED_SANDSTONE.isEnabled()) {
			smoothSet(McFlyBlocks.SMOOTH_RED_SANDSTONE, McFlyBlocks.RED_SANDSTONE.newItemStack(1, 0), McFlyBlocks.SMOOTH_RED_SANDSTONE_SLAB,
				McFlyBlocks.SMOOTH_RED_SANDSTONE_STAIRS);
		}
		smoothSet(McFlyBlocks.SMOOTH_QUARTZ, new ItemStack(Blocks.quartz_block, 1, 0), McFlyBlocks.SMOOTH_QUARTZ_SLAB, McFlyBlocks.SMOOTH_QUARTZ_STAIRS);
	}

	private static void smoothSet(McFlyBlocks block, ItemStack smelted, McFlyBlocks slab, McFlyBlocks stairs) {
		if (block.isEnabled()) {
			GameRegistry.addSmelting(smelted, block.newItemStack(), 0.1F);
			shaped(slab.newItemStack(6), "###", '#', block.newItemStack());
			stairs(stairs, block.newItemStack());
		}
	}

	private static void redSandstone() {
		if (!McFlyBlocks.RED_SANDSTONE.isEnabled()) {
			return;
		}
		ItemStack red = McFlyBlocks.RED_SANDSTONE.newItemStack(1, 0);
		shaped(McFlyBlocks.RED_SANDSTONE.newItemStack(1, 0), "##", "##", '#', new ItemStack(Blocks.sand, 1, 1));
		shaped(McFlyBlocks.RED_SANDSTONE.newItemStack(1, 1), "#", "#", '#', McFlyBlocks.RED_SANDSTONE_SLAB.newItemStack(1, 0));
		shaped(McFlyBlocks.RED_SANDSTONE.newItemStack(4, 2), "##", "##", '#', red);
		shaped(McFlyBlocks.RED_SANDSTONE_SLAB.newItemStack(6, 0), "###", '#', red);
		shaped(McFlyBlocks.RED_SANDSTONE_SLAB.newItemStack(6, 0), "###", '#', McFlyBlocks.RED_SANDSTONE.newItemStack(1, 1));
		shaped(McFlyBlocks.RED_SANDSTONE_SLAB.newItemStack(6, 1), "###", '#', McFlyBlocks.RED_SANDSTONE.newItemStack(1, 2));
		stairs(McFlyBlocks.RED_SANDSTONE_STAIRS, McFlyBlocks.RED_SANDSTONE.newItemStack(1, OreDictionary.WILDCARD_VALUE));
		shaped(McFlyBlocks.RED_SANDSTONE_WALL.newItemStack(6), "###", "###", '#', red);
	}

	private static void vanillaSets() {
		if (McFlyBlocks.STONE_SLAB.isEnabled()) {
			// 26.3's stone slab of stone; 1.7.10's stone slab is its smooth stone slab (of smooth stone, above)
			removeShaped(new ItemStack(Blocks.stone_slab, 6, 0), Item.getItemFromBlock(Blocks.stone));
			ItemStack[] bases = {new ItemStack(Blocks.stone), new ItemStack(Blocks.mossy_cobblestone), new ItemStack(Blocks.stonebrick, 1, 1),
				new ItemStack(Blocks.sandstone, 1, 2)};
			for (int i = 0; i < bases.length; i++) {
				shaped(McFlyBlocks.STONE_SLAB.newItemStack(6, i), "###", '#', bases[i]);
			}
		}
		if (McFlyBlocks.STONE_STAIRS.isEnabled()) {
			stairs(McFlyBlocks.STONE_STAIRS, new ItemStack(Blocks.stone));
			stairs(McFlyBlocks.MOSSY_COBBLESTONE_STAIRS, new ItemStack(Blocks.mossy_cobblestone));
			stairs(McFlyBlocks.MOSSY_STONE_BRICK_STAIRS, new ItemStack(Blocks.stonebrick, 1, 1));
		}
		if (McFlyBlocks.STONE_WALL.isEnabled()) {
			ItemStack[] bases = {new ItemStack(Blocks.stonebrick, 1, 0), new ItemStack(Blocks.stonebrick, 1, 1), new ItemStack(Blocks.sandstone, 1, 0),
				new ItemStack(Blocks.brick_block)};
			for (int i = 0; i < bases.length; i++) {
				shaped(McFlyBlocks.STONE_WALL.newItemStack(6, i), "###", "###", '#', bases[i]);
			}
			shaped(McFlyBlocks.NETHER_BRICK_WALL.newItemStack(6), "###", "###", '#', new ItemStack(Blocks.nether_brick));
		}
	}

	/** 26.3's tuff (spec §89, step E): polished of tuff, bricks of polished, the chiseled ones of two slabs, each one's slab, stairs and wall. */
	private static void tuff() {
		if (!McFlyBlocks.TUFF.isEnabled()) {
			return;
		}
		shaped(McFlyBlocks.TUFF.newItemStack(4, 1), "SS", "SS", 'S', McFlyBlocks.TUFF.newItemStack(1, 0));
		shaped(McFlyBlocks.TUFF.newItemStack(4, 2), "SS", "SS", 'S', McFlyBlocks.TUFF.newItemStack(1, 1));
		shaped(McFlyBlocks.TUFF.newItemStack(1, 3), "#", "#", '#', McFlyBlocks.TUFF_SLAB.newItemStack(1, 0));
		shaped(McFlyBlocks.TUFF.newItemStack(1, 4), "#", "#", '#', McFlyBlocks.TUFF_SLAB.newItemStack(1, 2));
		McFlyBlocks[] stairs = {McFlyBlocks.TUFF_STAIRS, McFlyBlocks.POLISHED_TUFF_STAIRS, McFlyBlocks.TUFF_BRICK_STAIRS};
		for (int i = 0; i < 3; i++) {
			ItemStack base = McFlyBlocks.TUFF.newItemStack(1, i);
			shaped(McFlyBlocks.TUFF_SLAB.newItemStack(6, i), "###", '#', base);
			stairs(stairs[i], base);
			shaped(McFlyBlocks.TUFF_WALL.newItemStack(6, i), "###", "###", '#', base);
		}
	}

	/** Takes out vanilla's shaped recipes for this result made of this item (1.7.10's stone slab of stone). */
	private static void removeShaped(ItemStack result, Item ingredient) {
		@SuppressWarnings("unchecked")
		java.util.List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
		recipes.removeIf(recipe -> recipe instanceof net.minecraft.item.crafting.ShapedRecipes shaped && ItemStack.areItemStacksEqual(shaped.getRecipeOutput(), result)
			&& java.util.Arrays.stream(shaped.recipeItems).anyMatch(stack -> stack != null && stack.getItem() == ingredient));
	}
}
