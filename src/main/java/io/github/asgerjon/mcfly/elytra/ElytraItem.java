package io.github.asgerjon.mcfly.elytra;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyItem;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.block.BlockDispenser;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * 26.3's elytra (`Items.ELYTRA`: a glider worn in the chest slot; SPEC-modern-movement.md §17 to §22, §85): 432
 * durability, epic, mended with phantom membrane, put on by a dispenser as a chestplate is, worn by a right-click or
 * swapped with what the chest slot holds (26.3's equippable swap, McFly's `rightClickArmorSwap`). One hit from
 * breaking it can't glide and shows broken (26.3's `canGlide` asks for a use left); gliding never breaks it.
 */
public class ElytraItem extends McFlyItem {

	@SideOnly(Side.CLIENT)
	private IIcon broken;

	public ElytraItem() {
		super("elytra");
		setMaxStackSize(1);
		setMaxDamage(432);
		BlockDispenser.dispenseBehaviorRegistry.putObject(this, BlockDispenser.dispenseBehaviorRegistry.getObject(Items.iron_chestplate));
	}

	/** Worn out but one: no gliding, the broken look. */
	public static boolean isBroken(ItemStack stack) {
		return stack.getItemDamage() >= stack.getMaxDamage() - 1;
	}

	/** The elytra the entity wears in its chest slot, or null. */
	public static ItemStack worn(EntityLivingBase entity) {
		ItemStack chest = entity.getEquipmentInSlot(3);
		return chest != null && chest.getItem() instanceof ElytraItem ? chest : null;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return McFlyItems.PHANTOM_MEMBRANE.isEnabled() && repair.getItem() == McFlyItems.PHANTOM_MEMBRANE.get();
	}

	@Override
	public boolean isValidArmor(ItemStack stack, int armorType, Entity entity) {
		return armorType == 1;
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return EnumRarity.epic;
	}

	/** 26.3's equippable use: into an empty chest slot, or swapped with what's there (not with an equal stack). */
	@Override
	public ItemStack onItemRightClick(ItemStack held, World world, EntityPlayer player) {
		ItemStack chest = player.getEquipmentInSlot(3);
		if (chest == null) {
			player.setCurrentItemOrArmor(3, held.copy());
			countUse(world, player);
			if (!player.capabilities.isCreativeMode) {
				held.stackSize = 0;
			}
			return held;
		}
		if (!McFlyConfig.rightClickArmorSwap || ItemStack.areItemStacksEqual(held, chest)) {
			return held;
		}
		countUse(world, player);
		player.setCurrentItemOrArmor(3, held.copy());
		return chest;
	}

	private void countUse(World world, EntityPlayer player) {
		if (!world.isRemote) {
			StatBase stat = StatList.objectUseStats[Item.getIdFromItem(this)];
			if (stat != null) {
				player.addStat(stat, 1);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int damage) {
		return damage >= getMaxDamage() - 1 ? broken : super.getIconFromDamage(damage);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister icons) {
		super.registerIcons(icons);
		broken = icons.registerIcon("broken_elytra");
	}
}
