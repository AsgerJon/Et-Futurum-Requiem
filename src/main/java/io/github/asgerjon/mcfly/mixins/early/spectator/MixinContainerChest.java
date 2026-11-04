package io.github.asgerjon.mcfly.mixins.early.spectator;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A spectator looking into a chest doesn't open its lid (spec §83): no open count, no sound. */
@Mixin(ContainerChest.class)
public class MixinContainerChest {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/IInventory;openInventory()V"))
	private void mcfly$noLidForSpectators(IInventory chest, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) IInventory playerInventory) {
		if (!(playerInventory instanceof InventoryPlayer inventory) || !Spectator.isSpectator(inventory.player)) {
			original.call(chest);
		}
	}

	@Inject(method = "onContainerClosed", at = @At("HEAD"), cancellable = true)
	private void mcfly$noLidClosingForSpectators(EntityPlayer player, CallbackInfo ci) {
		if (Spectator.isSpectator(player)) {
			ci.cancel();
		}
	}
}
