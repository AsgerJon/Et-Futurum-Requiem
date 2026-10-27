package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class SmithingTableRecipes {

	/** The template's index in the table's crafting matrix, after the tool (0) and the material (1), when templates are on. */
	public static final int TEMPLATE_SLOT = 2;

	private static final SmithingTableRecipes instance = new SmithingTableRecipes();
	private final List<SmithingTableRecipe> recipes = new ArrayList<>();

	public static SmithingTableRecipes getInstance() {
		return instance;
	}

	/** Whether the table has the template slot of 1.20: the netherite upgrade template exists and is wanted. */
	public static boolean useTemplates() {
		return ConfigBlocksItems.netheriteUpgradeTemplate && ModItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.isEnabled();
	}

	public static void init() {
		if (ConfigBlocksItems.enableNetherite) {
			Object template = useTemplates() ? ModItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE.newItemStack() : null;
			addNetheriteUpgrade(ModItems.NETHERITE_PICKAXE, Items.diamond_pickaxe, template);
			addNetheriteUpgrade(ModItems.NETHERITE_AXE, Items.diamond_axe, template);
			addNetheriteUpgrade(ModItems.NETHERITE_SPADE, Items.diamond_shovel, template);
			addNetheriteUpgrade(ModItems.NETHERITE_SWORD, Items.diamond_sword, template);
			addNetheriteUpgrade(ModItems.NETHERITE_HOE, Items.diamond_hoe, template);

			addNetheriteUpgrade(ModItems.NETHERITE_HELMET, Items.diamond_helmet, template);
			addNetheriteUpgrade(ModItems.NETHERITE_CHESTPLATE, Items.diamond_chestplate, template);
			addNetheriteUpgrade(ModItems.NETHERITE_LEGGINGS, Items.diamond_leggings, template);
			addNetheriteUpgrade(ModItems.NETHERITE_BOOTS, Items.diamond_boots, template);
		}
	}

	private static void addNetheriteUpgrade(ModItems result, Item diamondGear, Object template) {
		getInstance().addRecipe(result.newItemStack(), "ingotNetherite", new ItemStack(diamondGear, 1, OreDictionary.WILDCARD_VALUE), template);
	}

	public void addRecipe(ItemStack result, Object materialSlot, Object toolSlot) {
		addRecipe(result, materialSlot, toolSlot, null);
	}

	/** A recipe that also takes a template (1.20's third slot); null for none, which then has to stay empty. */
	public void addRecipe(ItemStack result, Object materialSlot, Object toolSlot, Object templateSlot) {
		instance.recipes.add(new SmithingTableRecipe(toolSlot, materialSlot, templateSlot, result, true));
	}

	public void addRecipeNoNBT(ItemStack result, Object materialSlot, Object toolSlot) {
		instance.recipes.add(new SmithingTableRecipe(toolSlot, materialSlot, result, false));
	}

	public void addRecipe(SmithingTableRecipe recipe) {
		instance.recipes.add(recipe);
	}

	public List<SmithingTableRecipe> getRecipes() {
		return recipes;
	}

	/** Whether any recipe takes this stack as its template; the template slot accepts nothing else. */
	public boolean isTemplate(ItemStack stack) {
		for (SmithingTableRecipe recipe : recipes) {
			if (recipe.template != null && SmithingTableRecipe.matchesIngredient(recipe.template, stack)) {
				return true;
			}
		}
		return false;
	}

	public ItemStack findMatchingRecipe(InventoryCrafting p_82787_1_, World p_82787_2_) {
		for (SmithingTableRecipe recipe : this.recipes) {
			if (recipe.matches(p_82787_1_, p_82787_2_)) {
				return recipe.getCraftingResult(p_82787_1_);
			}
		}

		return null;
	}

	public static class SmithingTableRecipe implements IRecipe {
		/**
		 * Should we copy the NBT values from the tool slot?
		 */
		private final boolean copyNBT;
		private final Object input;
		private final Object material;
		/** The template, or null for a recipe without one, which needs the template slot empty. */
		private final Object template;
		private final ItemStack output;

		public SmithingTableRecipe(Object toolSlot, Object materialSlot, ItemStack result, boolean copy) {
			this(toolSlot, materialSlot, null, result, copy);
		}

		public SmithingTableRecipe(Object toolSlot, Object materialSlot, Object templateSlot, ItemStack result, boolean copy) {
			copyNBT = copy;

			output = result;

			Object[] recipe = new Object[]{toolSlot, materialSlot, templateSlot};

			for (int idx = 0; idx < recipe.length; idx++) {
				Object in = recipe[idx];

				if (in == null && idx == 2) {
					continue;
				} else if (in instanceof ItemStack) {
					recipe[idx] = ((ItemStack) in).copy();
				} else if (in instanceof Item) {
					recipe[idx] = new ItemStack((Item) in);
				} else if (in instanceof Block) {
					recipe[idx] = new ItemStack((Block) in, 1, OreDictionary.WILDCARD_VALUE);
				} else if (in instanceof String) {
					recipe[idx] = OreDictionary.getOres((String) in);
				} else {
					String ret = "Invalid smithing table recipe: ";
					for (Object tmp : recipe) {
						ret += tmp + ", ";
					}
					ret += output;
					throw new RuntimeException(ret);
				}
			}

			input = recipe[0];
			material = recipe[1];
			template = recipe[2];
		}

		//TODO: Check if vanilla copies the whole tag compound or just the name and enchantments.
		@Override
		public ItemStack getCraftingResult(InventoryCrafting var1) {
			ItemStack stack = output.copy();
			if (copyNBT) {
				ItemStack toCopy = var1.getStackInSlot(0);
				if (toCopy.hasTagCompound()) {
					stack.setTagCompound(toCopy.getTagCompound());
				}
				if (toCopy.isItemStackDamageable() && stack.isItemStackDamageable()) {
					stack.setItemDamage(toCopy.getItemDamage());
				}
			}
			return stack;
		}

		/**
		 * Returns the size of the recipe area
		 */
		@Override
		public int getRecipeSize() {
			return template != null ? 3 : 2;
		}

		@Override
		public ItemStack getRecipeOutput() {
			return output;
		}

		public Object getInputItem() {
			return input;
		}

		public Object getMaterial() {
			return material;
		}

		public Object getTemplate() {
			return template;
		}

		/**
		 * Used to check if a recipe matches current crafting inventory
		 */
		@Override
		public boolean matches(InventoryCrafting inv, World world) {
			if (!matchesIngredient(input, inv.getStackInSlot(0)) || !matchesIngredient(material, inv.getStackInSlot(1))) {
				return false;
			}
			ItemStack inTemplateSlot = inv.getSizeInventory() > TEMPLATE_SLOT ? inv.getStackInSlot(TEMPLATE_SLOT) : null;
			return template == null ? inTemplateSlot == null : matchesIngredient(template, inTemplateSlot);
		}

		/** One ingredient (a stack, or an ore dictionary list) against one slot; nothing required means the slot is empty. */
		@SuppressWarnings("unchecked")
		static boolean matchesIngredient(Object target, ItemStack slot) {
			if (target instanceof ItemStack) {
				return OreDictionary.itemMatches((ItemStack) target, slot, false);
			}
			if (target instanceof ArrayList) {
				for (ItemStack option : (ArrayList<ItemStack>) target) {
					if (OreDictionary.itemMatches(option, slot, false)) {
						return true;
					}
				}
				return false;
			}
			return slot == null;
		}
	}
}
