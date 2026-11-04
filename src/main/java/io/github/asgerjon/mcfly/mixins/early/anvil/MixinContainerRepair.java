package io.github.asgerjon.mcfly.mixins.early.anvil;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * The anvil's result and price by modern's rules (SPEC-modern-movement.md §55.1), in place of 1.7.10's: the prior-work
 * charge of both inputs, then 1 per unit of material, 2 for mending by a second item, each enchantment's anvil cost
 * (halved for a book) times its resulting level and 1 per conflict, 1 for a name; the new repair cost doubles plus one.
 * Forge's AnvilUpdateEvent still decides first, and a book only goes onto what accepts it.
 */
@Mixin(ContainerRepair.class)
public abstract class MixinContainerRepair extends Container {

	@Shadow
	public IInventory outputSlot;
	@Shadow
	public IInventory inputSlots;
	@Shadow
	public int maximumCost;
	@Shadow
	public int stackSizeToBeUsedInRepair;
	@Shadow
	private String repairedItemName;
	@Shadow
	@Final
	private EntityPlayer thePlayer;

	@Inject(method = "updateRepairOutput", at = @At("HEAD"), cancellable = true)
	private void etfu$modernPrice(CallbackInfo ci) {
		ci.cancel();
		ItemStack input = inputSlots.getStackInSlot(0);
		if (input == null) {
			etfu$noResult();
			return;
		}
		boolean creative = thePlayer.capabilities.isCreativeMode;
		ItemStack result = input.copy();
		ItemStack addition = inputSlots.getStackInSlot(1);
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(result);
		long tax = (long) input.getRepairCost() + (addition == null ? 0 : addition.getRepairCost());
		int price = 0;
		int namingCost = 0;
		stackSizeToBeUsedInRepair = 0;
		if (addition != null) {
			if (!ForgeHooks.onAnvilChange((ContainerRepair) (Object) this, input, addition, outputSlot, repairedItemName, (int) Math.min(tax, Integer.MAX_VALUE))) {
				return;
			}
			boolean usingBook = addition.getItem() == Items.enchanted_book && Items.enchanted_book.func_92110_g(addition).tagCount() > 0;
			if (result.isItemStackDamageable() && result.getItem().getIsRepairable(input, addition)) {
				int repairAmount = Math.min(result.getItemDamageForDisplay(), result.getMaxDamage() / 4);
				if (repairAmount <= 0) {
					etfu$noResult();
					return;
				}
				int count;
				for (count = 0; repairAmount > 0 && count < addition.stackSize; count++) {
					result.setItemDamage(result.getItemDamageForDisplay() - repairAmount);
					price++;
					repairAmount = Math.min(result.getItemDamageForDisplay(), result.getMaxDamage() / 4);
				}
				stackSizeToBeUsedInRepair = count;
			} else {
				if (!usingBook && (result.getItem() != addition.getItem() || !result.isItemStackDamageable())) {
					etfu$noResult();
					return;
				}
				if (result.isItemStackDamageable() && !usingBook) {
					int remaining = input.getMaxDamage() - input.getItemDamageForDisplay()
						+ addition.getMaxDamage() - addition.getItemDamageForDisplay() + result.getMaxDamage() * 12 / 100;
					int damage = Math.max(0, result.getMaxDamage() - remaining);
					if (damage < result.getItemDamage()) {
						result.setItemDamage(damage);
						price += 2;
					}
				}
				boolean anyApplies = false;
				boolean anyConflicts = false;
				@SuppressWarnings("unchecked")
				Map<Integer, Integer> added = EnchantmentHelper.getEnchantments(addition);
				for (Map.Entry<Integer, Integer> entry : added.entrySet()) {
					int id = entry.getKey();
					Enchantment enchantment = Enchantment.enchantmentsList[id];
					if (enchantment == null) {
						continue;
					}
					int current = enchantments.containsKey(id) ? enchantments.get(id) : 0;
					int level = entry.getValue();
					level = current == level ? level + 1 : Math.max(level, current);
					boolean applies = enchantment.canApply(input) || creative || input.getItem() == Items.enchanted_book;
					for (int other : enchantments.keySet()) {
						Enchantment otherEnchantment = Enchantment.enchantmentsList[other];
						if (other != id && otherEnchantment != null
							&& !(enchantment.canApplyTogether(otherEnchantment) && otherEnchantment.canApplyTogether(enchantment))) {
							applies = false;
							price++;
						}
					}
					if (!applies) {
						anyConflicts = true;
						continue;
					}
					anyApplies = true;
					enchantments.put(id, Math.min(level, enchantment.getMaxLevel()));
					int fee = etfu$anvilCost(enchantment);
					if (usingBook) {
						fee = Math.max(1, fee / 2);
					}
					price += fee * Math.min(level, enchantment.getMaxLevel());
					if (input.stackSize > 1) {
						price = 40;
					}
				}
				if (anyConflicts && !anyApplies) {
					etfu$noResult();
					return;
				}
			}
		}
		if (StringUtils.isBlank(repairedItemName)) {
			if (input.hasDisplayName()) {
				namingCost = 1;
				price += namingCost;
				result.func_135074_t();
			}
		} else if (!repairedItemName.equals(input.getDisplayName())) {
			namingCost = 1;
			price += namingCost;
			result.setStackDisplayName(repairedItemName);
		}

		maximumCost = price <= 0 ? 0 : (int) Math.min(tax + price, Integer.MAX_VALUE);
		if (price <= 0) {
			result = null;
		}
		if (namingCost == price && namingCost > 0 && maximumCost >= 40) {
			maximumCost = 39;
		}
		if (maximumCost >= 40 && !creative) {
			result = null;
		}
		if (result != null && addition != null && addition.getItem() == Items.enchanted_book && !result.getItem().isBookEnchantable(result, addition)) {
			result = null;
		}
		if (result != null) {
			int repairCost = Math.max(result.getRepairCost(), addition == null ? 0 : addition.getRepairCost());
			if (namingCost != price || namingCost == 0) {
				repairCost = (int) Math.min(repairCost * 2L + 1L, Integer.MAX_VALUE);
			}
			result.setRepairCost(repairCost);
			EnchantmentHelper.setEnchantments(enchantments, result);
		}
		outputSlot.setInventorySlotContents(0, result);
		detectAndSendChanges();
	}

	@Unique
	private void etfu$noResult() {
		outputSlot.setInventorySlotContents(0, null);
		maximumCost = 0;
	}

	/** Modern's anvil cost by 1.7.10's weight: as the data files have it for vanilla's (10: 1, 5: 2, 2: 4, 1: 8). */
	@Unique
	private static int etfu$anvilCost(Enchantment enchantment) {
		int weight = enchantment.getWeight();
		return weight >= 10 ? 1 : weight >= 5 ? 2 : weight >= 2 ? 4 : 8;
	}
}
