package io.github.asgerjon.mcfly.mixins.early.namedeffects;

import ganymedes01.etfuturum.potion.EffectRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Effect instances saved by name (SPEC-modern-movement.md §70.3, stage B), as 26.3 saves them (`id`), with no
 * number: every effect 1.7.10 writes goes through here (an entity's ActiveEffects, a potion's CustomPotionEffects,
 * EFR's arrows). Reading takes the name, or an old save's `Id` byte when there is none; an unknown name is dropped,
 * as 26.3 drops an effect it doesn't have.
 */
@Mixin(PotionEffect.class)
public abstract class MixinPotionEffect {

	@Shadow
	private int potionID;

	@Inject(method = "writeCustomPotionEffectToNBT", at = @At("RETURN"))
	private void etfu$writeName(NBTTagCompound tag, CallbackInfoReturnable<NBTTagCompound> cir) {
		Potion potion = potionID >= 0 && potionID < Potion.potionTypes.length ? Potion.potionTypes[potionID] : null;
		String name = EffectRegistry.key(potion);
		if (name != null) {
			tag.setString("id", name);
			tag.removeTag("Id");
		}
	}

	@Inject(method = "readCustomPotionEffectFromNBT", at = @At("HEAD"), cancellable = true)
	private static void etfu$readName(NBTTagCompound tag, CallbackInfoReturnable<PotionEffect> cir) {
		if (tag.hasKey("id", 8)) {
			Potion potion = EffectRegistry.get(tag.getString("id"));
			cir.setReturnValue(potion == null ? null
				: new PotionEffect(potion.id, tag.getInteger("Duration"), tag.getByte("Amplifier"), tag.getBoolean("Ambient")));
		}
	}
}
