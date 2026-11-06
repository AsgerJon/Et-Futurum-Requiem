package io.github.asgerjon.mcfly.smithing;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The smithing table's recipes (SPEC-modern-movement.md §88.4, step C2), 26.3's `SmithingTransformRecipe`: a template,
 * a base and an addition make the result, the base's NBT and damage copied onto it (`transmuteCopy`). The netherite
 * upgrade for the nine diamond pieces. Without McFly's `netheriteUpgradeTemplate` option, 1.16's: no template.
 */
public final class SmithingRecipes {

	/** One recipe: its template (null: none), base and addition items, its result. */
	public static final class Recipe {
		final Item template;
		final Item base;
		final Item addition;
		final Item result;

		Recipe(Item template, Item base, Item addition, Item result) {
			this.template = template;
			this.base = base;
			this.addition = addition;
			this.result = result;
		}
	}

	private static final List<Recipe> RECIPES = new ArrayList<>();

	private SmithingRecipes() {
	}

	/** Whether the table has 26.3's template slot: the template exists and is wanted. */
	public static boolean useTemplates() {
		return McFlyConfig.netheriteUpgradeTemplate && McFlyItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.isEnabled();
	}

	public static void register() {
		if (io.github.asgerjon.mcfly.registry.McFlyBlocks.SMITHING_TABLE.isEnabled()) {
			// 26.3's: two iron ingots over four planks (its #planks: any wood's, so the ore name)
			cpw.mods.fml.common.registry.GameRegistry.addRecipe(new net.minecraftforge.oredict.ShapedOreRecipe(
				io.github.asgerjon.mcfly.registry.McFlyBlocks.SMITHING_TABLE.newItemStack(), "II", "PP", "PP", 'I', Items.iron_ingot, 'P', "plankWood"));
		}
		if (McFlyItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.isEnabled()) {
			// 26.3's copy: seven diamonds, netherrack and a template make two templates
			cpw.mods.fml.common.registry.GameRegistry.addRecipe(McFlyItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.newItemStack(2), "#S#", "#C#", "###",
				'#', Items.diamond, 'S', McFlyItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.get(), 'C', net.minecraft.init.Blocks.netherrack);
		}
		if (!McFlyItems.NETHERITE_INGOT.isEnabled() || !io.github.asgerjon.mcfly.registry.McFlyBlocks.SMITHING_TABLE.isEnabled()) {
			return;
		}
		Item template = useTemplates() ? McFlyItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.get() : null;
		Item ingot = McFlyItems.NETHERITE_INGOT.get();
		Object[][] upgrades = {{Items.diamond_sword, McFlyItems.NETHERITE_SWORD}, {Items.diamond_pickaxe, McFlyItems.NETHERITE_PICKAXE},
			{Items.diamond_axe, McFlyItems.NETHERITE_AXE}, {Items.diamond_shovel, McFlyItems.NETHERITE_SPADE}, {Items.diamond_hoe, McFlyItems.NETHERITE_HOE},
			{Items.diamond_helmet, McFlyItems.NETHERITE_HELMET}, {Items.diamond_chestplate, McFlyItems.NETHERITE_CHESTPLATE},
			{Items.diamond_leggings, McFlyItems.NETHERITE_LEGGINGS}, {Items.diamond_boots, McFlyItems.NETHERITE_BOOTS}};
		for (Object[] upgrade : upgrades) {
			RECIPES.add(new Recipe(template, (Item) upgrade[0], ingot, ((McFlyItems) upgrade[1]).get()));
		}
	}

	/** The result of these inputs (null for none): the base's NBT and damage on the result's item. */
	public static ItemStack result(ItemStack template, ItemStack base, ItemStack addition) {
		for (Recipe recipe : RECIPES) {
			if (is(template, recipe.template) && base != null && base.getItem() == recipe.base && addition != null && addition.getItem() == recipe.addition) {
				ItemStack result = new ItemStack(recipe.result);
				if (base.hasTagCompound()) {
					result.setTagCompound((net.minecraft.nbt.NBTTagCompound) base.getTagCompound().copy());
				}
				if (base.isItemStackDamageable() && result.isItemStackDamageable()) {
					result.setItemDamage(base.getItemDamage());
				}
				return result;
			}
		}
		return null;
	}

	private static boolean is(ItemStack stack, Item item) {
		return item == null ? stack == null : stack != null && stack.getItem() == item;
	}

	/** 26.3's `SMITHING_TEMPLATE` property set: an item some recipe takes as its template. */
	public static boolean isTemplate(ItemStack stack) {
		for (Recipe recipe : RECIPES) {
			if (recipe.template != null && stack != null && stack.getItem() == recipe.template) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's `SMITHING_BASE`: an item some recipe takes as its base. */
	public static boolean isBase(ItemStack stack) {
		for (Recipe recipe : RECIPES) {
			if (stack != null && stack.getItem() == recipe.base) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's `SMITHING_ADDITION`. */
	public static boolean isAddition(ItemStack stack) {
		for (Recipe recipe : RECIPES) {
			if (stack != null && stack.getItem() == recipe.addition) {
				return true;
			}
		}
		return false;
	}
}
