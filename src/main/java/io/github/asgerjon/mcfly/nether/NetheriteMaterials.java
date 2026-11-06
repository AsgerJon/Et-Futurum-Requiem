package io.github.asgerjon.mcfly.nether;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraftforge.common.util.EnumHelper;

/**
 * Netherite's tool and armour materials (SPEC-modern-movement.md §88.4), 26.3's: `ToolMaterial.NETHERITE` (2031 uses,
 * speed 9, 4 more damage, tier 4, enchantability 15) and `ArmorMaterials.NETHERITE` (37 times the slot's base, defence
 * 3, 8, 6, 3 from helmet to boots, enchantability 15). Toughness 3 and knockback resistance 0.1 are the fork's combat
 * code's (`DamageRules`, by this material); both are repaired with the ingot (each item's own `getIsRepairable`).
 */
public final class NetheriteMaterials {

	public static final Item.ToolMaterial TOOL = EnumHelper.addToolMaterial("MCFLY_NETHERITE", 4, 2031, 9.0F, 4.0F, 15);
	public static final ItemArmor.ArmorMaterial ARMOR = EnumHelper.addArmorMaterial("MCFLY_NETHERITE", 37, new int[]{3, 8, 6, 3}, 15);

	private NetheriteMaterials() {
	}
}
