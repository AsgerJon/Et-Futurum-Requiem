package io.github.asgerjon.mcfly.nether;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.FireResistant;
import io.github.asgerjon.mcfly.base.McFlyItem;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

/**
 * Netherite's items (SPEC-modern-movement.md §88.4), under EFR's ids (`netherite_spade` is 26.3's shovel): the scrap and
 * the ingot (a beacon payment), the five tools and four pieces of armour on {@link NetheriteMaterials}, all fire
 * resistant, all repaired with the ingot.
 */
public final class NetheriteItems {

	private NetheriteItems() {
	}

	private static boolean ingot(ItemStack stack) {
		return stack != null && McFlyItems.NETHERITE_INGOT.isEnabled() && stack.getItem() == McFlyItems.NETHERITE_INGOT.get();
	}

	private static <T extends Item> T named(T item, String name) {
		item.setUnlocalizedName(McFly.name(name));
		item.setTextureName(name);
		item.setCreativeTab(McFlyTab.TAB);
		return item;
	}

	/** The scrap, or the ingot (a beacon payment). */
	public static class Metal extends McFlyItem implements FireResistant {
		private final boolean payment;

		public Metal(String name, boolean payment) {
			super(name);
			this.payment = payment;
		}

		@Override
		public boolean isBeaconPayment(ItemStack stack) {
			return payment;
		}
	}

	public static class Sword extends ItemSword implements FireResistant {
		public Sword() {
			super(NetheriteMaterials.TOOL);
			named(this, "netherite_sword");
		}

		@Override
		public boolean getIsRepairable(ItemStack stack, ItemStack with) {
			return ingot(with);
		}
	}

	public static class Pickaxe extends ItemPickaxe implements FireResistant {
		public Pickaxe() {
			super(NetheriteMaterials.TOOL);
			named(this, "netherite_pickaxe");
		}

		@Override
		public boolean getIsRepairable(ItemStack stack, ItemStack with) {
			return ingot(with);
		}
	}

	public static class Axe extends ItemAxe implements FireResistant {
		public Axe() {
			super(NetheriteMaterials.TOOL);
			named(this, "netherite_axe");
		}

		@Override
		public boolean getIsRepairable(ItemStack stack, ItemStack with) {
			return ingot(with);
		}
	}

	/** 26.3's netherite shovel, EFR's `netherite_spade`. */
	public static class Shovel extends ItemSpade implements FireResistant {
		public Shovel() {
			super(NetheriteMaterials.TOOL);
			named(this, "netherite_spade");
			setTextureName("netherite_shovel");
		}

		@Override
		public boolean getIsRepairable(ItemStack stack, ItemStack with) {
			return ingot(with);
		}
	}

	public static class Hoe extends ItemHoe implements FireResistant {
		public Hoe() {
			super(NetheriteMaterials.TOOL);
			named(this, "netherite_hoe");
		}

		/** 26.3's: the material's 15 (1.7.10's hoes have none). */
		@Override
		public int getItemEnchantability() {
			return NetheriteMaterials.TOOL.getEnchantability();
		}

		@Override
		public boolean getIsRepairable(ItemStack stack, ItemStack with) {
			return ingot(with);
		}
	}

	/** A piece of armour: 1.7.10's slot 0 helmet to 3 boots, 26.3's `netherite` equipment textures. */
	public static class Armor extends ItemArmor implements FireResistant {
		private static final String[] SLOTS = {"helmet", "chestplate", "leggings", "boots"};

		public Armor(int slot) {
			super(NetheriteMaterials.ARMOR, 0, slot);
			named(this, "netherite_" + SLOTS[slot]);
		}

		@Override
		public boolean getIsRepairable(ItemStack stack, ItemStack with) {
			return ingot(with);
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
			return "textures/models/armor/netherite_layer_" + (armorType == 2 ? 2 : 1) + ".png";
		}
	}
}
