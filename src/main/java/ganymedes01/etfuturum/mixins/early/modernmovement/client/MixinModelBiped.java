package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.ModernMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinModelBiped extends ModelBase {

	@Unique
	private static final float etfu$PI = (float) Math.PI;

	@Unique
	private static final float etfu$ARM_SWEEP = 1.8707964F;

	@Shadow
	public ModelRenderer bipedHead;
	@Shadow
	public ModelRenderer bipedHeadwear;
	@Shadow
	public ModelRenderer bipedBody;
	@Shadow
	public ModelRenderer bipedRightArm;
	@Shadow
	public ModelRenderer bipedLeftArm;
	@Shadow
	public ModelRenderer bipedRightLeg;
	@Shadow
	public ModelRenderer bipedLeftLeg;
	@Shadow
	public boolean isSneak;

	@Unique
	private float etfu$baseBodyY;
	@Unique
	private float etfu$baseRightArmY;
	@Unique
	private float etfu$baseLeftArmY;

	/**
	 * 1.7.10 never resets the body and arm pivots, so remember them to restore after crouching. The other constructors
	 * all end up here.
	 */
	@Inject(method = "<init>(FFII)V", at = @At("TAIL"))
	private void etfu$rememberPivots(float scale, float yOffset, int textureWidth, int textureHeight, CallbackInfo ci) {
		etfu$baseBodyY = bipedBody.rotationPointY;
		etfu$baseRightArmY = bipedRightArm.rotationPointY;
		etfu$baseLeftArmY = bipedLeftArm.rotationPointY;
	}

	/**
	 * Player poses on the model, applied last like in modern: the modern crouch offsets (§14.2), then the
	 * swimming/crawling limbs (§14.3) blended in by the swim amount, overriding the walk swing and the idle arm bob.
	 * Armour models go through here too and follow along.
	 */
	@Inject(method = "setRotationAngles", at = @At("RETURN"))
	private void etfu$playerPoseAnimation(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (!(entity instanceof IPosePlayer)) {
			return;
		}
		if (ConfigMixins.enableModernSneaking) {
			etfu$modernCrouch();
		}
		float swimAmount = ((IPosePlayer) entity).etfu$getSwimAmount(Minecraft.getMinecraft().timer.renderPartialTicks);
		if (swimAmount <= 0.0F) {
			return;
		}
		EntityPlayer player = (EntityPlayer) entity;

		if (!ModernMovement.isElytraFlying(player)) {
			bipedHead.rotateAngleX = ModernMovement.lerpAngle(swimAmount, bipedHead.rotateAngleX, -etfu$PI / 4.0F);
			bipedHeadwear.rotateAngleX = bipedHead.rotateAngleX;
		}

		if (!player.isUsingItem()) {
			// An arm that is mid-swing keeps its swing; in 1.7.10 only the right arm swings.
			float leftAmount = swimAmount;
			float rightAmount = this.onGround > 0.0F ? 0.0F : swimAmount;
			float stroke = limbSwing % 26.0F;
			float pitch;
			float leftRoll;
			float rightRoll;
			if (stroke < 14.0F) {
				float sweep = etfu$ARM_SWEEP * etfu$strokeCurve(stroke) / etfu$strokeCurve(14.0F);
				pitch = 0.0F;
				leftRoll = etfu$PI + sweep;
				rightRoll = etfu$PI - sweep;
			} else if (stroke < 22.0F) {
				float progress = (stroke - 14.0F) / 8.0F;
				pitch = etfu$PI / 2.0F * progress;
				leftRoll = 5.012389F - etfu$ARM_SWEEP * progress;
				rightRoll = 1.2707963F + etfu$ARM_SWEEP * progress;
			} else {
				float progress = (stroke - 22.0F) / 4.0F;
				pitch = etfu$PI / 2.0F - etfu$PI / 2.0F * progress;
				leftRoll = etfu$PI;
				rightRoll = etfu$PI;
			}
			// Modern blends the left arm the short way round and the right arm linearly; kept as is so the stroke looks
			// the same.
			bipedLeftArm.rotateAngleX = ModernMovement.lerpAngle(leftAmount, bipedLeftArm.rotateAngleX, pitch);
			bipedLeftArm.rotateAngleY = ModernMovement.lerpAngle(leftAmount, bipedLeftArm.rotateAngleY, etfu$PI);
			bipedLeftArm.rotateAngleZ = ModernMovement.lerpAngle(leftAmount, bipedLeftArm.rotateAngleZ, leftRoll);
			bipedRightArm.rotateAngleX = etfu$lerp(rightAmount, bipedRightArm.rotateAngleX, pitch);
			bipedRightArm.rotateAngleY = etfu$lerp(rightAmount, bipedRightArm.rotateAngleY, etfu$PI);
			bipedRightArm.rotateAngleZ = etfu$lerp(rightAmount, bipedRightArm.rotateAngleZ, rightRoll);
		}

		// Flutter kick, the legs half a cycle apart.
		bipedLeftLeg.rotateAngleX = etfu$lerp(swimAmount, bipedLeftLeg.rotateAngleX, 0.3F * MathHelper.cos(limbSwing * 0.33333334F + etfu$PI));
		bipedRightLeg.rotateAngleX = etfu$lerp(swimAmount, bipedRightLeg.rotateAngleX, 0.3F * MathHelper.cos(limbSwing * 0.33333334F));
	}

	/**
	 * Modern crouch look (§14.2). 1.7.10 already tilts the body and arms and moves the legs back; modern keeps the legs
	 * at their normal height and sinks the upper body instead: body and arms down 3.2, head down 4.2.
	 */
	@Unique
	private void etfu$modernCrouch() {
		if (isSneak) {
			bipedRightLeg.rotationPointY = 12.0F;
			bipedLeftLeg.rotationPointY = 12.0F;
			bipedHead.rotationPointY = 4.2F;
			bipedHeadwear.rotationPointY = 4.2F;
			bipedBody.rotationPointY = etfu$baseBodyY + 3.2F;
			bipedRightArm.rotationPointY = etfu$baseRightArmY + 3.2F;
			bipedLeftArm.rotationPointY = etfu$baseLeftArmY + 3.2F;
		} else {
			bipedBody.rotationPointY = etfu$baseBodyY;
			bipedRightArm.rotationPointY = etfu$baseRightArmY;
			bipedLeftArm.rotationPointY = etfu$baseLeftArmY;
		}
	}

	@Unique
	private static float etfu$strokeCurve(float x) {
		return x * x - 65.0F * x;
	}

	@Unique
	private static float etfu$lerp(float amount, float from, float to) {
		return from + amount * (to - from);
	}
}
