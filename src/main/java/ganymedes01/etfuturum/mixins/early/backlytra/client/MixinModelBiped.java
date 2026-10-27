package ganymedes01.etfuturum.mixins.early.backlytra.client;

import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The gliding model (§21.2). Armour models go through here too and follow along.
 */
@Mixin(ModelBiped.class)
public class MixinModelBiped {

	@Shadow
	public ModelRenderer bipedHead;

	@Shadow
	public ModelRenderer bipedHeadwear;

	/**
	 * The arm and leg swing is slowed down while gliding fast. In 1.7.10 the swing amount only drives those four
	 * angles, so dividing it divides them.
	 */
	@ModifyVariable(method = "setRotationAngles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private float etfu$slowLimbsWhileGliding(float limbSwingAmount, @Local(argsOnly = true) Entity entity) {
		if (entity instanceof EntityLivingBase) {
			return limbSwingAmount / FallFlying.getLimbSlowdown((EntityLivingBase) entity);
		}
		return limbSwingAmount;
	}

	/**
	 * The head is held at -45° while gliding, whatever the look pitch.
	 */
	@Inject(method = "setRotationAngles", at = @At("RETURN"))
	private void etfu$glidingHead(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (FallFlying.isFallFlying(entity)) {
			bipedHead.rotateAngleX = -(float) Math.PI / 4.0F;
			bipedHeadwear.rotateAngleX = bipedHead.rotateAngleX;
		}
	}
}
