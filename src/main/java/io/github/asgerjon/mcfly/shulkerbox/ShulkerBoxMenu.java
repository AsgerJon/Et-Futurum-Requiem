package io.github.asgerjon.mcfly.shulkerbox;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * A shulker box's menu (SPEC-modern-movement.md §81; 26.3's `ShulkerBoxMenu`): its 27 slots in 3 rows of 9 (no
 * shulker box in them: `ShulkerBoxSlot`), the player's inventory and hotbar below; shift-clicks across.
 */
public class ShulkerBoxMenu extends Container {

	private final IInventory box;

	public ShulkerBoxMenu(InventoryPlayer player, IInventory box) {
		this.box = box;
		box.openInventory();
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlotToContainer(new Slot(box, column + row * 9, 8 + column * 18, 18 + row * 18) {
					@Override
					public boolean isItemValid(ItemStack stack) {
						return !ShulkerBoxBlock.isBox(stack);
					}
				});
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlotToContainer(new Slot(player, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlotToContainer(new Slot(player, column, 8 + column * 18, 142));
		}
	}

	public IInventory box() {
		return box;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return box.isUseableByPlayer(player);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = (Slot) inventorySlots.get(index);
		if (slot == null || !slot.getHasStack()) {
			return null;
		}
		ItemStack stack = slot.getStack();
		ItemStack before = stack.copy();
		if (index < box.getSizeInventory()) {
			if (!mergeItemStack(stack, box.getSizeInventory(), inventorySlots.size(), true)) {
				return null;
			}
		} else if (ShulkerBoxBlock.isBox(stack) || !mergeItemStack(stack, 0, box.getSizeInventory(), false)) {
			return null;
		}
		if (stack.stackSize == 0) {
			slot.putStack(null);
		} else {
			slot.onSlotChanged();
		}
		return before;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		box.closeInventory();
	}
}
