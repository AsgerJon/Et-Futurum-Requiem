package ganymedes01.etfuturum.mixins.early.hunger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.FoodStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Healing as in modern (1.11+). With saturation left, a full food bar and health to regain, health comes back every
 * ten ticks from saturation: min(saturation, 6) / 6 of a heart, paid for with that much exhaustion. Below a full bar
 * the slow healing of 1.7.10 stays, one heart per eighty ticks, but it costs six exhaustion instead of three. The
 * exhaustion drain, starvation and the timer are vanilla's own.
 */
@Mixin(FoodStats.class)
public abstract class MixinFoodStats {
	@Shadow
	private int foodLevel;
	@Shadow
	private float foodSaturationLevel;
	@Shadow
	private int foodTimer;

	@Shadow
	public abstract void addExhaustion(float exhaustion);

	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getGameRules()Lnet/minecraft/world/GameRules;"), cancellable = true)
	private void etfu$healFromSaturation(EntityPlayer player, CallbackInfo ci) {
		if (this.foodSaturationLevel <= 0.0F || this.foodLevel < 20 || !player.shouldHeal()
				|| !player.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration")) {
			return;
		}
		if (++this.foodTimer >= 10) {
			float spent = Math.min(this.foodSaturationLevel, 6.0F);
			player.heal(spent / 6.0F);
			this.addExhaustion(spent);
			this.foodTimer = 0;
		}
		ci.cancel();
	}

	@ModifyArg(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FoodStats;addExhaustion(F)V"))
	private float etfu$slowHealingCost(float exhaustion) {
		return 6.0F;
	}
}
