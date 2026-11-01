package ganymedes01.etfuturum.combat;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import ganymedes01.etfuturum.ModMaterials;
import ganymedes01.etfuturum.mixins.early.combat.AccessorItemSword;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import java.util.Map;
import java.util.UUID;

/**
 * Modern's weapon damage and speed (spec §46.2): main-hand modifiers on top of the player's base attack damage of 1
 * and attack speed of 4. Swords, pickaxes and shovels follow one rule for any material; axes and hoes are set per
 * material, other mods' materials counted as iron; every hoe deals 1.
 */
public final class WeaponStats {

	/** 1.7.10's own damage modifier for weapons; modern uses the same one. */
	private static final UUID WEAPON_DAMAGE_ID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
	private static final String ATTACK_DAMAGE = SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName();
	private static final String ATTACK_SPEED = AttackCooldown.ATTACK_SPEED.getAttributeUnlocalizedName();

	private WeaponStats() {
	}

	/** Whether a modifier is a weapon's own damage or speed, which tooltips show as totals (§46.5). */
	public static boolean isWeaponModifier(AttributeModifier modifier) {
		return WEAPON_DAMAGE_ID.equals(modifier.getID()) || AttackCooldown.WEAPON_SPEED_ID.equals(modifier.getID());
	}

	/**
	 * The item's modifiers with modern's damage and speed in place of 1.7.10's damage, for the weapons modern
	 * changed; other items' unchanged.
	 */
	public static Multimap<String, AttributeModifier> withModernStats(Item item, Multimap<String, AttributeModifier> modifiers) {
		double[] stats = stats(item);
		if (stats == null) {
			return modifiers;
		}
		// damage, then speed, then the rest, the order modern's tooltip lists them in
		Multimap<String, AttributeModifier> result = LinkedHashMultimap.create();
		result.put(ATTACK_DAMAGE, new AttributeModifier(WEAPON_DAMAGE_ID, "Weapon modifier", stats[0], 0));
		result.put(ATTACK_SPEED, new AttributeModifier(AttackCooldown.WEAPON_SPEED_ID, "Weapon modifier", stats[1], 0));
		for (Map.Entry<String, AttributeModifier> entry : modifiers.entries()) {
			if (!(entry.getKey().equals(ATTACK_DAMAGE) && WEAPON_DAMAGE_ID.equals(entry.getValue().getID()))) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	/** {damage modifier, speed modifier}, or null for an item that isn't one of modern's weapons or tools. */
	private static double[] stats(Item item) {
		if (item instanceof ItemSword sword) {
			return new double[]{3.0D + bonus(((AccessorItemSword) sword).etfu$getMaterial()), -2.4D};
		}
		if (item instanceof ItemAxe axe) {
			Item.ToolMaterial material = ((ItemTool) axe).toolMaterial;
			double base;
			double speed;
			if (material == Item.ToolMaterial.WOOD) {
				base = 6.0D;
				speed = -3.2D;
			} else if (material == Item.ToolMaterial.STONE) {
				base = 7.0D;
				speed = -3.2D;
			} else if (material == Item.ToolMaterial.GOLD) {
				base = 6.0D;
				speed = -3.0D;
			} else if (material == Item.ToolMaterial.EMERALD || material == ModMaterials.NETHERITE_TOOL) {
				base = 5.0D;
				speed = -3.0D;
			} else {
				base = 6.0D;
				speed = -3.1D;
			}
			return new double[]{base + bonus(material), speed};
		}
		if (item instanceof ItemPickaxe pickaxe) {
			return new double[]{1.0D + bonus(pickaxe.toolMaterial), -2.8D};
		}
		if (item instanceof ItemSpade spade) {
			return new double[]{1.5D + bonus(spade.toolMaterial), -3.0D};
		}
		if (item instanceof ItemHoe hoe) {
			// every hoe deals 1 in modern: its base cancels the material's bonus
			Item.ToolMaterial material = hoe.theToolMaterial;
			double speed;
			if (material == Item.ToolMaterial.WOOD || material == Item.ToolMaterial.GOLD) {
				speed = -3.0D;
			} else if (material == Item.ToolMaterial.STONE) {
				speed = -2.0D;
			} else if (material == Item.ToolMaterial.EMERALD || material == ModMaterials.NETHERITE_TOOL) {
				speed = 0.0D;
			} else {
				speed = -1.0D;
			}
			return new double[]{0.0D, speed};
		}
		return null;
	}

	/** The material's attack bonus: 1.7.10's material damage is modern's number (wood 0 to diamond 3). */
	private static double bonus(Item.ToolMaterial material) {
		return material == null ? 0.0D : material.getDamageVsEntity();
	}
}
