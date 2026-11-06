package io.github.asgerjon.mcfly.mixins.early.elytraequip;

import io.github.asgerjon.mcfly.elytra.ElytraItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-clicking an elytra in the inventory puts it on when the chest slot is empty, as for armour; 1.7.10 only does
 * that for ItemArmor. The chest slot decides whether it fits (the elytra refuses when one is already worn, also in a
 * bauble slot). Slots 0 to 8 are the crafting grid and the armour; everything after is the inventory, the hotbar
 * and the off-hand.
 */
@Mixin(ContainerPlayer.class)
public abstract class MixinContainerPlayer extends Container {

	private static final int CHEST_SLOT = 6;

	@Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true)
	private void etfu$equipElytra(EntityPlayer player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index < 9 || index >= inventorySlots.size()) {
			return;
		}
		Slot slot = (Slot) inventorySlots.get(index);
		if (slot == null || !slot.getHasStack() || !(slot.getStack().getItem() instanceof io.github.asgerjon.mcfly.elytra.ElytraItem)) {
			return;
		}
		Slot chest = (Slot) inventorySlots.get(CHEST_SLOT);
		ItemStack stack = slot.getStack();
		if (chest.getHasStack() || !chest.isItemValid(stack)) {
			return;
		}
		ItemStack before = stack.copy();
		if (!mergeItemStack(stack, CHEST_SLOT, CHEST_SLOT + 1, false)) {
			return;
		}
		if (stack.stackSize == 0) {
			slot.putStack(null);
		} else {
			slot.onSlotChanged();
		}
		slot.onPickupFromSlot(player, stack);
		cir.setReturnValue(before);
	}
}
