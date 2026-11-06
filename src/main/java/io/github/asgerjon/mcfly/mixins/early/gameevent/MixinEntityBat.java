package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.Flapping;
import net.minecraft.entity.passive.EntityBat;
import org.spongepowered.asm.mixin.Mixin;

/** A bat flaps every 3rd tick while it isn't hanging (SPEC-modern-movement.md §90.9, step H1; 26.3's `Bat.isFlapping`, `TICKS_PER_FLAP`). */
@Mixin(EntityBat.class)
public abstract class MixinEntityBat implements Flapping {

	@Override
	public boolean mcfly$flapped(float moved) {
		EntityBat self = (EntityBat) (Object) this;
		return !self.getIsBatHanging() && self.ticksExisted % 3 == 0;
	}
}
