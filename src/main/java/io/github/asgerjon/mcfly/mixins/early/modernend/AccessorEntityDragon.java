package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.7.10's dragon's hurt as a mob (its super's), which 26.3's damage rules end in. */
@Mixin(EntityDragon.class)
public interface AccessorEntityDragon {

	@Invoker("func_82195_e")
	boolean etfu$reallyHurt(DamageSource source, float amount);
}
