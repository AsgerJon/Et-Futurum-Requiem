package io.github.asgerjon.mcfly.mixins.early.sculk;

import io.github.asgerjon.mcfly.sculk.WardenSpawnTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player's warden warning ticked once a server tick (SPEC-modern-movement.md §90.9, step H4; 26.3's `Player.tick`), not per movement packet. */
@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void mcfly$tickWarning(CallbackInfo ci) {
		WardenSpawnTracker.of((EntityPlayerMP) (Object) this).tick();
	}
}
