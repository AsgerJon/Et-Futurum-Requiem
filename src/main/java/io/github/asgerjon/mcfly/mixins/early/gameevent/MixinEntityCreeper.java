package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.monster.EntityCreeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A creeper starting to swell posts `prime_fuse` with its hiss (SPEC-modern-movement.md §90.9, step H1; 26.3's `Creeper.tick`). */
@Mixin(EntityCreeper.class)
public abstract class MixinEntityCreeper {

	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/EntityCreeper;playSound(Ljava/lang/String;FF)V"))
	private void mcfly$hiss(CallbackInfo ci) {
		GameEvents.postAt((EntityCreeper) (Object) this, "prime_fuse");
	}
}
