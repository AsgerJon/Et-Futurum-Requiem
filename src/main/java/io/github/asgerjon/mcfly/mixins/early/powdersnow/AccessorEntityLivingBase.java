package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.7.10's fall sound by damage (above 4 the big one), for powder snow's fall sounds (spec §62.1). */
@Mixin(EntityLivingBase.class)
public interface AccessorEntityLivingBase {

	@Invoker("func_146067_o")
	String etfu$fallSound(int damage);
}
