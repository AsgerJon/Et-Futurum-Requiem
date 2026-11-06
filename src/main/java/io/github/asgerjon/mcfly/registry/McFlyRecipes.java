package io.github.asgerjon.mcfly.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.McFly;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Our crafting recipes where 1.7.10's clash (SPEC-modern-movement.md §89, step D): shaped ones sorted before vanilla's,
 * so that 26.3's exact ingredient wins over a vanilla recipe that takes any meta (red sand's red sandstone over 1.7.10's
 * sandstone of any sand, the cut sandstone slab over its sandstone slab of any sandstone). Ingredients as Forge's ore
 * recipes take them (items, blocks, stacks, ore names).
 */
public final class McFlyRecipes {

	/** A shaped recipe of ours, sorted before vanilla's. */
	public static final class Shaped extends ShapedOreRecipe {

		public Shaped(ItemStack result, Object... recipe) {
			super(result, recipe);
		}
	}

	private McFlyRecipes() {
	}

	/** In init, before any of ours is added (Forge sorts the list after post-init). */
	public static void register() {
		RecipeSorter.register(McFly.DOMAIN + ":mcfly_shaped", Shaped.class, RecipeSorter.Category.SHAPED, "before:minecraft:shaped");
	}

	public static void shaped(ItemStack result, Object... recipe) {
		GameRegistry.addRecipe(new Shaped(result, recipe));
	}

	public static void shapeless(ItemStack result, Object... ingredients) {
		GameRegistry.addRecipe(new ShapelessOreRecipe(result, ingredients));
	}
}
