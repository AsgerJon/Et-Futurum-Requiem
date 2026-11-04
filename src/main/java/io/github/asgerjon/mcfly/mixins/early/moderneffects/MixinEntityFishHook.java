package io.github.asgerjon.mcfly.mixins.early.moderneffects;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.potion.ModernEffects;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fishing's luck: Luck of the Sea's level, and (26.3's `FishingHook`) the angler's luck from luck and bad luck (spec §69.11). */
@Mixin(EntityFishHook.class)
public abstract class MixinEntityFishHook {

	@WrapOperation(method = "func_146033_f", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;func_151386_g(Lnet/minecraft/entity/EntityLivingBase;)I"))
	private int etfu$luck(EntityLivingBase angler, Operation<Integer> original) {
		int luck = original.call(angler);
		return angler instanceof EntityPlayer player ? luck + ModernEffects.fishingLuck(player) : luck;
	}
}
