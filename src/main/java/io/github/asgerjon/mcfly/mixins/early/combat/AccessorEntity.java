package io.github.asgerjon.mcfly.mixins.early.combat;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An entity's fire ticks, which 1.7.10 keeps private (setFire only ever raises them). */
@Mixin(Entity.class)
public interface AccessorEntity {

	@Accessor("fire")
	int etfu$getFire();

	@Accessor("fire")
	void etfu$setFire(int ticks);
}
