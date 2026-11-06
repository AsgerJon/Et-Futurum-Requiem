package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An item frame's item put in, turned, or knocked out posts `block_change` at the frame, by the player or the attacker
 * (SPEC-modern-movement.md §90.9, step H1; 26.3's `ItemFrame.interact`, `hurtServer`). Glow frames are frames too.
 */
@Mixin(EntityItemFrame.class)
public abstract class MixinEntityItemFrame {

	@Unique
	private static void mcfly$changed(EntityItemFrame frame, Entity by) {
		GameEvents.post(frame.worldObj, GameEvent.get("block_change"), frame.posX, frame.posY, frame.posZ, by, null, 0);
	}

	@Inject(method = "interactFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItemFrame;setDisplayedItem(Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
	private void mcfly$put(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		mcfly$changed((EntityItemFrame) (Object) this, player);
	}

	@Inject(method = "interactFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItemFrame;setItemRotation(I)V", shift = At.Shift.AFTER))
	private void mcfly$turned(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		mcfly$changed((EntityItemFrame) (Object) this, player);
	}

	@Inject(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItemFrame;setDisplayedItem(Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
	private void mcfly$knockedOut(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		mcfly$changed((EntityItemFrame) (Object) this, source.getEntity());
	}
}
