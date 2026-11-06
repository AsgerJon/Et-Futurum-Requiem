package io.github.asgerjon.mcfly.mixins.early.sculk;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.base.LivingExperience;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3's death experience (SPEC-modern-movement.md §90.9, step H3): what it would give, whether it may, and once taken
 * (by a sculk catalyst) no orbs from `onDeathUpdate` (26.3's `wasExperienceConsumed`).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase implements LivingExperience {

	@Shadow
	protected EntityPlayer attackingPlayer;

	@Unique
	private boolean mcfly$experienceTaken;

	@Shadow
	protected abstract int getExperiencePoints(EntityPlayer player);

	@Shadow
	protected abstract boolean func_146066_aG();

	@Override
	public int mcfly$experienceReward() {
		return getExperiencePoints(attackingPlayer);
	}

	@Override
	public boolean mcfly$shouldDropExperience() {
		return func_146066_aG();
	}

	@Override
	public boolean mcfly$experienceConsumed() {
		return mcfly$experienceTaken;
	}

	@Override
	public void mcfly$skipDropExperience() {
		mcfly$experienceTaken = true;
	}

	@WrapOperation(method = "onDeathUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getExperiencePoints(Lnet/minecraft/entity/player/EntityPlayer;)I"))
	private int mcfly$noOrbsOnceTaken(EntityLivingBase self, EntityPlayer player, Operation<Integer> original) {
		return mcfly$experienceTaken ? 0 : original.call(self, player);
	}
}
