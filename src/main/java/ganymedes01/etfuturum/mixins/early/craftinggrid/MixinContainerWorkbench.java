package ganymedes01.etfuturum.mixins.early.craftinggrid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A stack shift-clicked from the inventory or the hotbar goes into the crafting grid first, onto matching stacks
 * or the first empty cell, as since 1.12; only when the grid takes nothing does it move between the inventory and
 * the hotbar as 1.7.10 did. The result slot and the grid cells keep their own moves.
 */
@Mixin(ContainerWorkbench.class)
public abstract class MixinContainerWorkbench extends Container {

	@WrapOperation(method = "transferStackInSlot", at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/inventory/ContainerWorkbench;mergeItemStack(Lnet/minecraft/item/ItemStack;IIZ)Z", ordinal = 1),
			@At(value = "INVOKE", target = "Lnet/minecraft/inventory/ContainerWorkbench;mergeItemStack(Lnet/minecraft/item/ItemStack;IIZ)Z", ordinal = 2)})
	private boolean etfu$gridFirst(ContainerWorkbench self, ItemStack stack, int start, int end, boolean reverse, Operation<Boolean> original) {
		return this.mergeItemStack(stack, 1, 10, false) || original.call(self, stack, start, end, reverse);
	}
}
