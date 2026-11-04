package io.github.asgerjon.mcfly.mixins.early.trident.client;

import ganymedes01.etfuturum.client.renderer.item.ItemTridentRenderer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Charging a trident raises the using arm overhead (SPEC-modern-movement.md §58.1, modern's THROW_TRIDENT pose): the
 * arm's walking swing halved, turned half a turn up, not turned sideways. Set where 1.7.10 has just posed the arms for
 * their held items, before the attack swing, as modern poses its arms before the attack animation.
 */
@Mixin(ModelBiped.class)
public abstract class MixinModelBiped {

	@Unique
	private static final float etfu$PI = (float) Math.PI;

	@Shadow
	public ModelRenderer bipedRightArm;
	@Shadow
	public ModelRenderer bipedLeftArm;
	@Shadow
	public int heldItemLeft;
	@Shadow
	public int heldItemRight;

	@Inject(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;onGround:F", opcode = Opcodes.GETFIELD, ordinal = 0))
	private void etfu$throwTridentPose(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
		if (!(entity instanceof EntityPlayer player)) {
			return;
		}
		if (ItemTridentRenderer.isChargingWithArm(player, false)) {
			etfu$raise(bipedRightArm, heldItemRight);
		} else if (ItemTridentRenderer.isChargingWithArm(player, true)) {
			etfu$raise(bipedLeftArm, heldItemLeft);
		}
	}

	/** 1.7.10 has set the arm to its swing halved less a tenth of a turn per held item (unhalved when it holds none). */
	@Unique
	private static void etfu$raise(ModelRenderer arm, int heldItem) {
		float halfSwing = heldItem != 0 ? arm.rotateAngleX + etfu$PI / 10.0F * heldItem : arm.rotateAngleX * 0.5F;
		arm.rotateAngleX = halfSwing - etfu$PI;
		arm.rotateAngleY = 0.0F;
	}
}
