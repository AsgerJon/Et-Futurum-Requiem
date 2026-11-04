package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import ganymedes01.etfuturum.potion.EffectRegistry;
import net.minecraft.potion.Potion;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

/**
 * Named effects (SPEC-modern-movement.md §70.3): 1.7.10's effect table, 32 fixed slots, grows as effects register,
 * and an effect asking for a taken number gets a free one (`EffectRegistry.claim`) instead of silently replacing the
 * effect there. The number is chosen before the constructor stores it in `id` and the table.
 */
@Mixin(Potion.class)
public abstract class MixinPotion {

	@Shadow
	@Final
	@Mutable
	public static Potion[] potionTypes;

	@ModifyVariable(method = "<init>(IZI)V", at = @At(value = "FIELD", target = "Lnet/minecraft/potion/Potion;field_111188_I:Ljava/util/Map;",
		opcode = Opcodes.PUTFIELD), argsOnly = true, ordinal = 0)
	private int etfu$number(int wanted) {
		int id = EffectRegistry.claim(wanted);
		if (id >= potionTypes.length) {
			potionTypes = Arrays.copyOf(potionTypes, Math.max(id + 1, potionTypes.length * 2));
		}
		return id;
	}
}
