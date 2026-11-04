package io.github.asgerjon.mcfly.mixins.early.potionsbyname.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.ThrownPotions;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A thrown potion is drawn in its item's colour (a named potion's 26.3 colour; spec §69.7, stage C). */
@Mixin(RenderSnowball.class)
public abstract class MixinRenderSnowball {

	@WrapOperation(method = "doRender(Lnet/minecraft/entity/Entity;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/potion/PotionHelper;func_77915_a(IZ)I"))
	private int etfu$thrownColour(int damage, boolean bars, Operation<Integer> original, @Local(argsOnly = true) Entity entity) {
		if (NamedPotions.isReady() && entity instanceof EntityPotion potion) {
			return ThrownPotions.colourOf(potion);
		}
		return original.call(damage, bars);
	}
}
