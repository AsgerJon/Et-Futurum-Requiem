package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The model's swing and bow for either arm (SPEC-modern-movement.md §36.4, §36.6). 1.7.10 always swings the right
 * arm and aims the bow right-handed; modern swings the arm of the swinging hand, with the body twist mirrored, and
 * mirrors the bow pose when the drawing hand is on the left arm. Armour models go through here too.
 */
@Mixin(ModelBiped.class)
public abstract class MixinModelBiped extends ModelBase {
	@Unique
	private static final float etfu$PI = (float) Math.PI;

	@Shadow
	public ModelRenderer bipedHead;
	@Shadow
	public ModelRenderer bipedBody;
	@Shadow
	public ModelRenderer bipedRightArm;
	@Shadow
	public ModelRenderer bipedLeftArm;
	@Shadow
	public boolean aimedBow;

	/** Vanilla's swing block is the right arm's: it is skipped when the left arm is the one swinging. */
	@ModifyExpressionValue(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;onGround:F", opcode = Opcodes.GETFIELD, ordinal = 0))
	private float etfu$skipRightArmSwing(float swing, @Local(argsOnly = true) Entity entity) {
		return etfu$leftArmSwings(entity) ? -9999.0F : swing;
	}

	/**
	 * The left arm's swing, at the place of vanilla's block (right before the crouch pose): the same formulas with
	 * the body twist negated, as modern's attack animation does for the left arm.
	 */
	@Inject(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;isSneak:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
	private void etfu$leftArmSwing(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (!etfu$leftArmSwings(entity) || this.onGround <= -9990.0F) {
			return;
		}
		float swing = this.onGround;
		bipedBody.rotateAngleY = -MathHelper.sin(MathHelper.sqrt_float(swing) * etfu$PI * 2.0F) * 0.2F;
		bipedRightArm.rotationPointZ = MathHelper.sin(bipedBody.rotateAngleY) * 5.0F;
		bipedRightArm.rotationPointX = -MathHelper.cos(bipedBody.rotateAngleY) * 5.0F;
		bipedLeftArm.rotationPointZ = -MathHelper.sin(bipedBody.rotateAngleY) * 5.0F;
		bipedLeftArm.rotationPointX = MathHelper.cos(bipedBody.rotateAngleY) * 5.0F;
		bipedRightArm.rotateAngleY += bipedBody.rotateAngleY;
		bipedLeftArm.rotateAngleY += bipedBody.rotateAngleY;
		bipedLeftArm.rotateAngleX += bipedBody.rotateAngleY;
		float eased = 1.0F - swing;
		eased *= eased;
		eased *= eased;
		eased = 1.0F - eased;
		float lift = MathHelper.sin(eased * etfu$PI);
		float aim = MathHelper.sin(swing * etfu$PI) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
		bipedLeftArm.rotateAngleX -= lift * 1.2F + aim;
		bipedLeftArm.rotateAngleY += bipedBody.rotateAngleY * 2.0F;
		bipedLeftArm.rotateAngleZ = MathHelper.sin(swing * etfu$PI) * -0.4F;
	}

	/** The bow pose mirrored when the drawing hand is on the left arm: only the yaw offsets differ between the sides. */
	@Inject(method = "setRotationAngles", at = @At("RETURN"))
	private void etfu$leftArmBow(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (!aimedBow || !Offhand.isEnabled() || !(entity instanceof EntityPlayer)) {
			return;
		}
		EntityPlayer player = (EntityPlayer) entity;
		if (Offhand.getUsingHand(player).arm(Offhand.isLeftHanded(player)) == Arm.LEFT) {
			bipedLeftArm.rotateAngleY = 0.1F + bipedHead.rotateAngleY;
			bipedRightArm.rotateAngleY = -0.1F + bipedHead.rotateAngleY - 0.4F;
		}
	}

	@Unique
	private static boolean etfu$leftArmSwings(Entity entity) {
		return entity instanceof EntityLivingBase && Offhand.isLeftArmSwinging((EntityLivingBase) entity);
	}
}
