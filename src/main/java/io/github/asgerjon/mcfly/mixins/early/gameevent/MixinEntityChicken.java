package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.Flapping;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.passive.EntityChicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A chicken laying posts `entity_place` at it with its plop (SPEC-modern-movement.md §90.9, step H1; 26.3's
 * `Chicken.aiStep`). It flaps each time its flown distance passes the next flap, which is then half its wings' spread
 * on (26.3's `Chicken.isFlapping`, `onFlap`; 1.7.10's `destPos` is 26.3's `flapSpeed`).
 */
@Mixin(EntityChicken.class)
public abstract class MixinEntityChicken implements Flapping {

	@Unique
	private float mcfly$flyDist;
	@Unique
	private float mcfly$nextFlap = 1.0F;

	@Override
	public boolean mcfly$flapped(float moved) {
		mcfly$flyDist += moved;
		if (mcfly$flyDist <= mcfly$nextFlap) {
			return false;
		}
		mcfly$nextFlap = mcfly$flyDist + ((EntityChicken) (Object) this).destPos / 2.0F;
		return true;
	}

	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/EntityChicken;playSound(Ljava/lang/String;FF)V", ordinal = 0))
	private void mcfly$laid(CallbackInfo ci) {
		EntityChicken self = (EntityChicken) (Object) this;
		GameEventHooks.atEntity(self, "entity_place", self);
	}
}
