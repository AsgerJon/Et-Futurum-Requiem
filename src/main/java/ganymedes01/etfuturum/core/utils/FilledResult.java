package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * 26.3's {@code ItemUtils.createFilledResult} (SPEC-modern-movement.md §63.1): filling a container (a bucket, a
 * bottle) the player holds. In creative the empty one stays and the filled one is added if the inventory holds none
 * yet; otherwise one empty is used, the filled one takes its place if it was the last, else goes into the inventory
 * (or is dropped when there's no room).
 */
public final class FilledResult {

	private FilledResult() {
	}

	/** Gives the filled stack for one of the held empty one; returns what the hand holds after. */
	public static ItemStack fill(EntityPlayer player, ItemStack held, ItemStack filled) {
		ItemStack result;
		if (player.capabilities.isCreativeMode) {
			if (!holds(player, filled)) {
				player.inventory.addItemStackToInventory(filled.copy());
			}
			result = held;
		} else if (--held.stackSize <= 0) {
			setHeld(player, held, filled);
			result = filled;
		} else {
			if (!player.inventory.addItemStackToInventory(filled)) {
				player.dropPlayerItemWithRandomChoice(filled, false);
			}
			result = held;
		}
		if (player instanceof EntityPlayerMP) {
			player.inventoryContainer.detectAndSendChanges();
		}
		return result;
	}

	/** 26.3 {@code Inventory.contains}: the same item, damage and tag. */
	public static boolean holds(EntityPlayer player, ItemStack wanted) {
		for (ItemStack stack : player.inventory.mainInventory) {
			if (stack != null && stack.getItem() == wanted.getItem() && stack.getItemDamage() == wanted.getItemDamage()
					&& ItemStack.areItemStackTagsEqual(stack, wanted)) {
				return true;
			}
		}
		return false;
	}

	/** The slot the held stack is in (the main hand's, or EFR's off-hand) gets the new one. */
	private static void setHeld(EntityPlayer player, ItemStack held, ItemStack replacement) {
		if (Offhand.isEnabled() && Offhand.getOffhand(player) == held) {
			Offhand.setOffhand(player, replacement);
		} else {
			player.inventory.setInventorySlotContents(player.inventory.currentItem, replacement);
		}
	}
}
