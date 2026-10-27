package ganymedes01.etfuturum.mixins.early.offhand;

import ganymedes01.etfuturum.offhand.IOffhandInventory;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The off-hand stack (SPEC-modern-movement.md §36.1, §36.8): one extra stack reached as index 40 through the slot
 * methods, saved as slot 150, ticked, cleared, dropped on death and copied on respawn with the rest. getSizeInventory stays 40: the
 * vanilla container derives its armour indices from it and mods iterate it.
 */
@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer implements IOffhandInventory {

	@Shadow
	public EntityPlayer player;
	@Shadow
	public ItemStack[] mainInventory;
	@Shadow
	public int currentItem;

	@Shadow
	public abstract int getInventoryStackLimit();

	@Unique
	private ItemStack etfu$offhand;

	@Override
	public ItemStack etfu$getOffhand() {
		return etfu$offhand;
	}

	@Override
	public void etfu$setOffhand(ItemStack stack) {
		etfu$offhand = stack;
	}

	@Unique
	private static boolean etfu$isOffhandSlot(int slot) {
		return slot == Offhand.INVENTORY_INDEX && Offhand.isEnabled();
	}

	/**
	 * A picked-up or given stack fills the selected hotbar slot first, then the off-hand, then the rest in order
	 * (modern's getSlotWithRemainingSpace, §36.1); 1.7.10 only scanned the main inventory in order. The two preferred
	 * slots are filled here, one per call: this method is called again while it makes progress, and vanilla's scan
	 * takes over once neither of them takes anything. An empty off-hand is never filled by a pickup, as in modern.
	 */
	@Inject(method = "storePartialItemStack", at = @At("HEAD"), cancellable = true)
	private void etfu$fillSelectedThenOffhand(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (!Offhand.isEnabled() || stack.getMaxStackSize() == 1) {
			return;
		}
		ItemStack selected = this.currentItem >= 0 && this.currentItem < 9 ? this.mainInventory[this.currentItem] : null;
		for (ItemStack target : new ItemStack[] {selected, etfu$offhand}) {
			int taken = etfu$mergeInto(target, stack);
			if (taken > 0) {
				cir.setReturnValue(stack.stackSize - taken);
				return;
			}
		}
	}

	/** Vanilla's merge rules and amounts (storeItemStack, storePartialItemStack) for one target stack. */
	@Unique
	private int etfu$mergeInto(ItemStack target, ItemStack stack) {
		if (target == null || target.getItem() != stack.getItem() || !target.isStackable()
				|| target.stackSize >= target.getMaxStackSize() || target.stackSize >= getInventoryStackLimit()
				|| target.getHasSubtypes() && target.getItemDamage() != stack.getItemDamage()
				|| !ItemStack.areItemStackTagsEqual(target, stack)) {
			return 0;
		}
		int taken = Math.min(stack.stackSize, Math.min(target.getMaxStackSize(), getInventoryStackLimit()) - target.stackSize);
		if (taken <= 0) {
			return 0;
		}
		target.stackSize += taken;
		target.animationsToGo = 5;
		return taken;
	}

	@Inject(method = "getStackInSlot", at = @At("HEAD"), cancellable = true)
	private void etfu$getOffhandSlot(int slot, CallbackInfoReturnable<ItemStack> cir) {
		if (etfu$isOffhandSlot(slot)) {
			cir.setReturnValue(etfu$offhand);
		}
	}

	@Inject(method = "setInventorySlotContents", at = @At("HEAD"), cancellable = true)
	private void etfu$setOffhandSlot(int slot, ItemStack stack, CallbackInfo ci) {
		if (etfu$isOffhandSlot(slot)) {
			etfu$offhand = stack;
			ci.cancel();
		}
	}

	@Inject(method = "decrStackSize", at = @At("HEAD"), cancellable = true)
	private void etfu$decrOffhandSlot(int slot, int count, CallbackInfoReturnable<ItemStack> cir) {
		if (!etfu$isOffhandSlot(slot)) {
			return;
		}
		if (etfu$offhand == null) {
			cir.setReturnValue(null);
		} else if (etfu$offhand.stackSize <= count) {
			ItemStack taken = etfu$offhand;
			etfu$offhand = null;
			cir.setReturnValue(taken);
		} else {
			ItemStack taken = etfu$offhand.splitStack(count);
			if (etfu$offhand.stackSize == 0) {
				etfu$offhand = null;
			}
			cir.setReturnValue(taken);
		}
	}

	@Inject(method = "getStackInSlotOnClosing", at = @At("HEAD"), cancellable = true)
	private void etfu$takeOffhandSlot(int slot, CallbackInfoReturnable<ItemStack> cir) {
		if (etfu$isOffhandSlot(slot)) {
			ItemStack taken = etfu$offhand;
			etfu$offhand = null;
			cir.setReturnValue(taken);
		}
	}

	@Inject(method = "writeToNBT", at = @At("RETURN"))
	private void etfu$saveOffhand(NBTTagList list, CallbackInfoReturnable<NBTTagList> cir) {
		if (etfu$offhand != null) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setByte("Slot", (byte) Offhand.NBT_SLOT);
			etfu$offhand.writeToNBT(tag);
			list.appendTag(tag);
		}
	}

	@Inject(method = "readFromNBT", at = @At("HEAD"))
	private void etfu$clearOffhand(NBTTagList list, CallbackInfo ci) {
		etfu$offhand = null;
	}

	@Inject(method = "readFromNBT", at = @At("TAIL"))
	private void etfu$loadOffhand(NBTTagList list, CallbackInfo ci) {
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			if ((tag.getByte("Slot") & 255) == Offhand.NBT_SLOT) {
				etfu$offhand = ItemStack.loadItemStackFromNBT(tag);
			}
		}
	}

	@Inject(method = "dropAllItems", at = @At("TAIL"))
	private void etfu$dropOffhand(CallbackInfo ci) {
		if (etfu$offhand != null) {
			player.func_146097_a(etfu$offhand, true, false);
			etfu$offhand = null;
		}
	}

	@Inject(method = "copyInventory", at = @At("TAIL"))
	private void etfu$copyOffhand(InventoryPlayer other, CallbackInfo ci) {
		etfu$offhand = ItemStack.copyItemStack(((IOffhandInventory) other).etfu$getOffhand());
	}

	/**
	 * The off-hand stack ticks with the rest, as in modern's inventory tick: its item's onUpdate runs with slot 40,
	 * not selected, and the pickup pop counts down.
	 */
	@Inject(method = "decrementAnimations", at = @At("TAIL"))
	private void etfu$tickOffhand(CallbackInfo ci) {
		if (etfu$offhand != null && Offhand.isEnabled()) {
			etfu$offhand.updateAnimation(player.worldObj, player, Offhand.INVENTORY_INDEX, false);
		}
	}

	/** Clearing the inventory by item (/clear) takes the off-hand too, with vanilla's own matching. */
	@Inject(method = "clearInventory", at = @At("RETURN"), cancellable = true)
	private void etfu$clearOffhand(Item item, int damage, CallbackInfoReturnable<Integer> cir) {
		if (etfu$offhand != null && Offhand.isEnabled() && (item == null || etfu$offhand.getItem() == item)
				&& (damage <= -1 || etfu$offhand.getItemDamage() == damage)) {
			cir.setReturnValue(cir.getReturnValue() + etfu$offhand.stackSize);
			etfu$offhand = null;
		}
	}
}
