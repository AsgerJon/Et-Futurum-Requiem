package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player's own game events (SPEC-modern-movement.md §90.9, step H1): `item_interact_start` when a use starts and
 * `item_interact_finish` when it ends (26.3's `LivingEntity.startUsingItem`, `stopUsingItem`), and the dismount of
 * 1.7.10's player, which doesn't go through `Entity.mountEntity` (that one's mount does, and posts there).
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

	@Shadow
	private ItemStack itemInUse;

	@Unique
	private ItemStack mcfly$useBefore;
	@Unique
	private Entity mcfly$dismounting;

	@Inject(method = "setItemInUse", at = @At("HEAD"))
	private void mcfly$beforeUse(ItemStack stack, int duration, CallbackInfo ci) {
		mcfly$useBefore = itemInUse;
	}

	@Inject(method = "setItemInUse", at = @At("RETURN"))
	private void mcfly$afterUse(ItemStack stack, int duration, CallbackInfo ci) {
		if (itemInUse != null && itemInUse != mcfly$useBefore && mcfly$useBefore == null) {
			GameEventHooks.usedItem((EntityPlayer) (Object) this, true);
		}
		mcfly$useBefore = null;
	}

	@Inject(method = "clearItemInUse", at = @At("HEAD"))
	private void mcfly$useEnds(CallbackInfo ci) {
		if (itemInUse != null) {
			GameEventHooks.usedItem((EntityPlayer) (Object) this, false);
		}
	}

	@Inject(method = "mountEntity", at = @At("HEAD"))
	private void mcfly$beforeRiding(Entity vehicle, CallbackInfo ci) {
		EntityPlayer self = (EntityPlayer) (Object) this;
		mcfly$dismounting = self.ridingEntity != null && vehicle == null ? self.ridingEntity : null;
	}

	@Inject(method = "mountEntity", at = @At("RETURN"))
	private void mcfly$afterRiding(Entity vehicle, CallbackInfo ci) {
		Entity before = mcfly$dismounting;
		mcfly$dismounting = null;
		EntityPlayer self = (EntityPlayer) (Object) this;
		if (before != null && self.ridingEntity == null) {
			GameEventHooks.rode(self, before, null);
		}
	}
}
