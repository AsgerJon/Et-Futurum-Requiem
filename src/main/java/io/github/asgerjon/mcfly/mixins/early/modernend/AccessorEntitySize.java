package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** An entity's size (26.3's dragon parts are smaller than 1.7.10's). */
@Mixin(Entity.class)
public interface AccessorEntitySize {

	@Invoker("setSize")
	void etfu$setSize(float width, float height);
}
