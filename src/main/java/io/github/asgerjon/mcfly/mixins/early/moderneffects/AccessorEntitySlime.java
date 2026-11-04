package io.github.asgerjon.mcfly.mixins.early.moderneffects;

import net.minecraft.entity.monster.EntitySlime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A slime's size, which 1.7.10 sets only from inside (oozing's slimes, spec §69.11). */
@Mixin(EntitySlime.class)
public interface AccessorEntitySlime {

	@Invoker("setSlimeSize")
	void etfu$setSlimeSize(int size);
}
