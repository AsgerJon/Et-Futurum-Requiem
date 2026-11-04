package io.github.asgerjon.mcfly.mixins.early.potionsbyname;

import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotions;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10's own brewing stand, used when EFR's is off, brews by 26.3's table too (SPEC-modern-movement.md §69.3):
 * each slot through {@link BrewingTable#brew}, and its reagent slot takes the table's reagents. EFR's stand (a
 * subclass with its own brewing) calls the table itself.
 */
@Mixin(TileEntityBrewingStand.class)
public abstract class MixinTileEntityBrewingStand {

	@Shadow
	private ItemStack[] brewingItemStacks;

	@Inject(method = "canBrew", at = @At("HEAD"), cancellable = true)
	private void etfu$canBrewByTable(CallbackInfoReturnable<Boolean> cir) {
		if (!NamedPotions.isReady()) {
			return;
		}
		cir.setReturnValue(etfu$anySlotBrews());
	}

	@Inject(method = "brewPotions", at = @At("HEAD"), cancellable = true)
	private void etfu$brewByTable(CallbackInfo ci) {
		if (!NamedPotions.isReady()) {
			return;
		}
		ci.cancel();
		ItemStack[] stacks = brewingItemStacks;
		if (ForgeEventFactory.onPotionAttemptBreaw(new ItemStack[]{stacks[0], stacks[1], stacks[2], stacks[3]}) || !etfu$anySlotBrews()) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			ItemStack made = BrewingTable.brew(stacks[i], stacks[3]);
			if (made != null) {
				stacks[i] = made;
			}
		}
		if (stacks[3].getItem().hasContainerItem(stacks[3])) {
			stacks[3] = stacks[3].getItem().getContainerItem(stacks[3]);
		} else if (--stacks[3].stackSize <= 0) {
			stacks[3] = null;
		}
		ForgeEventFactory.onPotionBrewed(new ItemStack[]{stacks[0], stacks[1], stacks[2], stacks[3]});
	}

	@Inject(method = "isItemValidForSlot", at = @At("HEAD"), cancellable = true)
	private void etfu$tableReagents(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (slot == 3 && NamedPotions.isReady()) {
			cir.setReturnValue(BrewingTable.isIngredient(stack));
		}
	}

	private boolean etfu$anySlotBrews() {
		ItemStack reagent = brewingItemStacks[3];
		if (reagent == null || reagent.stackSize <= 0 || !BrewingTable.isIngredient(reagent)) {
			return false;
		}
		for (int i = 0; i < 3; i++) {
			if (BrewingTable.brew(brewingItemStacks[i], reagent) != null) {
				return true;
			}
		}
		return false;
	}
}
