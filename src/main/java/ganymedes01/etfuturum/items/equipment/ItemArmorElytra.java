package ganymedes01.etfuturum.items.equipment;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleItemHelper;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.compat.CompatBaublesExpanded;
import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.items.BaseItem;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.block.BlockDispenser;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;

@Optional.Interface(modid = "Baubles|Expanded", iface = "baubles.api.expanded.IBaubleExpanded")
public class ItemArmorElytra extends BaseItem implements IBaubleExpanded {

	private IIcon broken;

	public ItemArmorElytra() {
		super("elytra");
		setMaxStackSize(1);
		setMaxDamage(432);
		BlockDispenser.dispenseBehaviorRegistry.putObject(this, BlockDispenser.dispenseBehaviorRegistry.getObject(Items.iron_chestplate));
	}

	/**
	 * An elytra one hit from breaking can't be used for gliding and shows as broken, like modern's. It never wears down
	 * further from gliding, so it doesn't break.
	 */
	public static boolean isBroken(ItemStack stack) {
		return stack.getItemDamage() >= stack.getMaxDamage() - 1;
	}

	/**
	 * Return whether this item is repairable in an anvil.
	 */
	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return repair.getItem() == Items.leather;
	}

	/**
	 * Right-click puts the elytra on. With rightClickArmorSwap (SPEC-modern-movement.md §31) it also swaps with the
	 * chestplate or the other elytra worn in the chest slot, as modern does; an elytra worn in the wings bauble slot is
	 * never swapped, since only one elytra may be worn.
	 */
	@Override
	public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
		boolean chestSlotAllowed = !ModsList.BAUBLES_EXPANDED.isLoaded() || ConfigModCompat.elytraBaublesExpandedCompat != 2;
		ItemStack worn = getElytra(playerIn);
		if (worn != null) {
			if (ConfigMixins.rightClickArmorSwap && chestSlotAllowed && worn == playerIn.getEquipmentInSlot(3) && !ItemStack.areItemStacksEqual(itemStackIn, worn)) {
				return swapWithChestSlot(itemStackIn, worldIn, playerIn, worn);
			}
			return itemStackIn;
		}
		if (ModsList.BAUBLES_EXPANDED.isLoaded() && ConfigModCompat.elytraBaublesExpandedCompat != 0) {
			itemStackIn = BaubleItemHelper.onBaubleRightClick(itemStackIn, worldIn, playerIn);
		}
		if (chestSlotAllowed && itemStackIn.stackSize > 0 && getElytra(playerIn) == null) {
			ItemStack itemStack = playerIn.getEquipmentInSlot(3);
			if (itemStack == null) {
				playerIn.setCurrentItemOrArmor(3, itemStackIn.copy());
				if (!playerIn.capabilities.isCreativeMode) {
					itemStackIn.stackSize = 0;
				}
				return itemStackIn;
			}
			if (ConfigMixins.rightClickArmorSwap) {
				return swapWithChestSlot(itemStackIn, worldIn, playerIn, itemStack);
			}
		}
		return itemStackIn;
	}

	/**
	 * The held elytra goes on and the worn piece comes into the hand (§31).
	 */
	private ItemStack swapWithChestSlot(ItemStack held, World world, EntityPlayer player, ItemStack worn) {
		if (!world.isRemote) {
			StatBase stat = StatList.objectUseStats[Item.getIdFromItem(this)];
			if (stat != null) {
				player.addStat(stat, 1);
			}
		}
		player.setCurrentItemOrArmor(3, held.copy());
		return worn;
	}

	@Override
	public boolean isValidArmor(ItemStack stack, int armorType, Entity entity) {
		return (!ModsList.BAUBLES_EXPANDED.isLoaded() || ConfigModCompat.elytraBaublesExpandedCompat != 2) && armorType == 1 && entity instanceof EntityLivingBase && getElytra((EntityLivingBase) entity) == null;
	}

	@Override
	public IIcon getIconFromDamage(int meta) {
		return meta >= getMaxDamage() - 1 ? broken : super.getIconFromDamage(meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		broken = reg.registerIcon("broken_elytra");
	}

	public static ItemStack getElytra(EntityLivingBase entity) {
		if (!ModsList.BAUBLES_EXPANDED.isLoaded() || ConfigModCompat.elytraBaublesExpandedCompat != 2) {
			ItemStack armorSlot = entity.getEquipmentInSlot(3);
			if (armorSlot != null && armorSlot.getItem() instanceof ItemArmorElytra) {
				return armorSlot;
			}
		}
		if (ModsList.BAUBLES_EXPANDED.isLoaded() && ConfigModCompat.elytraBaublesExpandedCompat != 0) {
			return CompatBaublesExpanded.getElytraFromBaubles(entity);
		}
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean debug) {
		if (Utils.badBetterFPSAlgorithm()) {
			tooltip.add(I18n.format("efr.elytra.betterfps.warn1"));
			tooltip.add(I18n.format("efr.elytra.betterfps.warn2"));
			tooltip.add(I18n.format("efr.elytra.betterfps.warn3"));
		} else if (ModsList.BAUBLES_EXPANDED.isLoaded()) {
			String[] slots;
			switch (ConfigModCompat.elytraBaublesExpandedCompat) {
				case 1:
					slots = new String[]{"chestplate", "wings"};
					break;
				case 2:
					slots = new String[]{"wings"};
					break;
				default:
					slots = new String[]{"chestplate"};
					break;
			}
			BaubleItemHelper.addSlotInformation(tooltip, slots);
		}
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public BaubleType getBaubleType(ItemStack itemstack) {
		return null;
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
		if (ConfigSounds.armorEquip) {
			player.worldObj.playSoundAtEntity(player, Reference.MCAssetVer + ":item.armor.equip_elytra", 1, 1);
		}
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
		return getElytra(player) == null;
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
		return true;
	}

	@Optional.Method(modid = "Baubles|Expanded")
	@Override
	public String[] getBaubleTypes(ItemStack itemstack) {
		if (ConfigModCompat.elytraBaublesExpandedCompat == 0) {
			return null;
		}
		return new String[]{"wings"};
	}

}
