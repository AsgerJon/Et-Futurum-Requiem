package io.github.asgerjon.mcfly.mixins.early.potionsbyname.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.potion.ThrownPotions;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A splash's particles (world event 2002) in a named potion's colour, and instant ones when it is instant: the
 * event's number carries them for such a potion (`ThrownPotions.splashData`; spec §69.7, stage C).
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

	@WrapOperation(method = "playAuxSFX", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemPotion;getColorFromDamage(I)I"))
	private int etfu$splashColour(ItemPotion item, int data, Operation<Integer> original) {
		return ThrownPotions.isEncoded(data) ? ThrownPotions.splashColour(data) : original.call(item, data);
	}

	@WrapOperation(method = "playAuxSFX", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemPotion;isEffectInstant(I)Z"))
	private boolean etfu$splashInstant(ItemPotion item, int data, Operation<Boolean> original) {
		return ThrownPotions.isEncoded(data) ? ThrownPotions.splashInstant(data) : original.call(item, data);
	}
}
