package io.github.asgerjon.mcfly.mixins.early.spyglass.client;

import ganymedes01.etfuturum.client.Spyglass;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3's SPYGLASS arm pose (SPEC-modern-movement.md §64.5; {@code HumanoidModel.poseRightArm}): the using arm raised
 * to the eye with the head, no bob on it. After EFR's off-hand poses.
 */
@Mixin(value = ModelBiped.class, priority = 1100)
public abstract class MixinModelBiped {

	@Shadow
	public ModelRenderer bipedHead;
	@Shadow
	public ModelRenderer bipedRightArm;
	@Shadow
	public ModelRenderer bipedLeftArm;
	@Shadow
	public boolean isSneak;

	@Inject(method = "setRotationAngles", at = @At("TAIL"))
	private void etfu$spyglassArm(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (!(entity instanceof EntityPlayer player) || !Spyglass.isScoping(player)) {
			return;
		}
		boolean right = Spyglass.usesRightArm(player);
		ModelRenderer arm = right ? bipedRightArm : bipedLeftArm;
		arm.rotateAngleX = MathHelper.clamp_float(bipedHead.rotateAngleX - 1.9198622F - (isSneak ? (float) (Math.PI / 12.0D) : 0.0F), -2.4F, 3.3F);
		arm.rotateAngleY = bipedHead.rotateAngleY + (right ? -1.0F : 1.0F) * (float) (Math.PI / 12.0D);
		arm.rotateAngleZ = 0.0F;
	}
}
