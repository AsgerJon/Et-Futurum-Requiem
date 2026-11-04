package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A spectator holds nothing (spec §83): it uses no item. */
@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer {

	@Shadow
	public EntityPlayer player;

	@Inject(method = "getCurrentItem", at = @At("HEAD"), cancellable = true)
	private void mcfly$nothingHeld(CallbackInfoReturnable<ItemStack> cir) {
		if (Spectator.isSpectator(player)) {
			cir.setReturnValue(null);
		}
	}
}
