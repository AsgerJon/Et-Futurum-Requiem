package ganymedes01.etfuturum;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

public class ModMaterials {
	/**
	 * 26.3's `TURTLE_SCUTE`: 25 a slot's factor (275 uses for the helmet), the helmet 2 armour, enchantability 9.
	 * Mended with scutes by `ItemTurtleShell.getIsRepairable` (the static block below runs while ModItems is still
	 * being made, before the scute exists).
	 */
	public static final ItemArmor.ArmorMaterial TURTLE_ARMOUR = EnumHelper.addArmorMaterial("Turtle_Scute", 25, new int[]{2, 6, 5, 2}, 9);
	public static final ItemArmor.ArmorMaterial NETHERITE_ARMOUR = EnumHelper.addArmorMaterial("Netherite_Armour", ConfigBlocksItems.netheriteArmourDurabilityFactor, new int[]{ConfigBlocksItems.netheriteHelmetProtection, ConfigBlocksItems.netheriteChestplateProtection, ConfigBlocksItems.netheriteLeggingsProtection, ConfigBlocksItems.netheriteBootsProtection}, ConfigBlocksItems.netheriteEnchantability);
	public static final Item.ToolMaterial NETHERITE_TOOL = EnumHelper.addToolMaterial("Netherite_Tool", ConfigBlocksItems.netheriteHarvestLevel, ConfigBlocksItems.netheriteToolDurability, ConfigBlocksItems.netheriteSpeed, ConfigBlocksItems.netheriteDamageBase, ConfigBlocksItems.netheriteEnchantability);

	static {
		ModMaterials.NETHERITE_TOOL.setRepairItem(new ItemStack(ModItems.NETHERITE_INGOT.get()));
		ModMaterials.NETHERITE_ARMOUR.customCraftingMaterial = ModItems.NETHERITE_INGOT.get();
	}
}
