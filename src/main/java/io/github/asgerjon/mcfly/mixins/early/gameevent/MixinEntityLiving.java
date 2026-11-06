package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob's own use of the player's interaction posts `entity_interact` at it, by the player, and so does its leash let
 * go; a leash tied doesn't (SPEC-modern-movement.md §90.9, step H1; 26.3's `Mob.interact`, `Entity.interact`).
 */
@Mixin(EntityLiving.class)
public abstract class MixinEntityLiving {

	@Inject(method = "interactFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLiving;clearLeashed(ZZ)V"))
	private void mcfly$untied(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atEntity((EntityLiving) (Object) this, "entity_interact", player);
	}

	@WrapOperation(method = "interactFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLiving;interact(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
	private boolean mcfly$used(EntityLiving self, EntityPlayer player, Operation<Boolean> original) {
		boolean used = original.call(self, player);
		if (used) {
			GameEventHooks.atEntity(self, "entity_interact", player);
		}
		return used;
	}
}
