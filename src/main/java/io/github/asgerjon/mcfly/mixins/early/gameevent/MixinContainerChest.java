package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A chest's menu opens and closes its chest with the player known (SPEC-modern-movement.md §90.9, step H1): 26.3's
 * `container_open` and `_close` are by the player, which 1.7.10's `openInventory` and `closeInventory` don't get.
 */
@Mixin(ContainerChest.class)
public abstract class MixinContainerChest {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/IInventory;openInventory()V"))
	private void mcfly$openedBy(IInventory chest, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) IInventory playerInventory) {
		EntityPlayer player = playerInventory instanceof InventoryPlayer ? ((InventoryPlayer) playerInventory).player : null;
		GameEventHooks.byPlayer(player, () -> original.call(chest));
	}

	@WrapOperation(method = "onContainerClosed", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/IInventory;closeInventory()V"))
	private void mcfly$closedBy(IInventory chest, Operation<Void> original, @Local(argsOnly = true) EntityPlayer player) {
		GameEventHooks.byPlayer(player, () -> original.call(chest));
	}
}
