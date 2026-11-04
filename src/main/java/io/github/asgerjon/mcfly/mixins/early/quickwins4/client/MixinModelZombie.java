package io.github.asgerjon.mcfly.mixins.early.quickwins4.client;

import ganymedes01.etfuturum.client.model.MobArms;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zombies' and skeletons' arms as 26.3's (SPEC-modern-movement.md §63.9, §64.1). 1.7.10's skeleton model (and EFR's
 * stray clothes) is this one: for skeletons the bow is aimed by {@code ModelBiped}'s own {@code aimedBow}, and this
 * model's raised arms are kept only while a skeleton is aggressive without a bow; it holds items with its arm down, as
 * 26.3's mobs (no item pose).
 */
@Mixin(ModelZombie.class)
public abstract class MixinModelZombie extends ModelBiped {

	@Inject(method = "setRotationAngles", at = @At("HEAD"))
	private void etfu$aimBow(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (MobArms.isModernSkeleton(entity)) {
			aimedBow = MobArms.aimsBow(entity);
			// 26.3's mobs hold items without the player's item pose (HumanoidMobRenderer.getArmPose: EMPTY)
			heldItemRight = 0;
			heldItemLeft = 0;
		}
	}

	@Inject(method = "setRotationAngles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBiped;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER), cancellable = true)
	private void etfu$humanoidSkeleton(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (MobArms.isModernSkeleton(entity) && !MobArms.raisesLikeZombie(entity)) {
			ci.cancel();
		}
	}

	@Inject(method = "setRotationAngles", at = @At("TAIL"))
	private void etfu$modernArms(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		MobArms.pose(this, entity, age);
	}
}
