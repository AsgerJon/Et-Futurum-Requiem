package io.github.asgerjon.mcfly.mixins.early.combat.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modern's block pose (spec §46.9): besides 1.7.10's raised arm, the blocking arm turns 30° inwards, so a raised
 * shield covers the body. Set where 1.7.10 has just zeroed the arms' yaw, before the swing adds to it. With the
 * combat update only shields block (swords don't).
 */
@Mixin(ModelBiped.class)
public class MixinModelBiped {

	private static final int BLOCKING = 3;

	@Shadow
	public ModelRenderer bipedRightArm;

	@Shadow
	public ModelRenderer bipedLeftArm;

	@Shadow
	public int heldItemRight;

	@Shadow
	public int heldItemLeft;

	@Inject(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;onGround:F", opcode = Opcodes.GETFIELD, ordinal = 0))
	private void etfu$blockingArmTurnsIn(float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (heldItemRight == BLOCKING) {
			bipedRightArm.rotateAngleY = -(float) Math.PI / 6.0F;
		}
		if (heldItemLeft == BLOCKING) {
			bipedLeftArm.rotateAngleY = (float) Math.PI / 6.0F;
		}
	}
}
