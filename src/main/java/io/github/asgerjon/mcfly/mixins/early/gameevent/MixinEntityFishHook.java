package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A fishing hook's first tick posts `projectile_shoot`, by its angler (SPEC-modern-movement.md §90.9, step H1; 26.3's `FishingHook` is a `Projectile`). */
@Mixin(EntityFishHook.class)
public abstract class MixinEntityFishHook {

	@Shadow
	public EntityPlayer field_146042_b;

	@Unique
	private boolean mcfly$shot;

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void mcfly$shoot(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!mcfly$shot && !self.worldObj.isRemote) {
			mcfly$shot = true;
			GameEventHooks.projectileShot(self, field_146042_b);
		}
	}
}
