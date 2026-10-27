package ganymedes01.etfuturum.mixins.early.skins.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.client.model.ModelPlayer;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandItemRenderer;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.skins.SkinPart;
import ganymedes01.etfuturum.skins.SkinParts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player's hidden skin parts are not drawn: the hat of any model, the cape, and, with the mod's player model, the
 * second layer's boxes (jacket, sleeves, pants legs). The models are shared, so the switches are set for each
 * player right before they are drawn.
 */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer extends RendererLivingEntity {
	@Shadow
	public ModelBiped modelBipedMain;

	public MixinRenderPlayer(ModelBase model, float shadowSize) {
		super(model, shadowSize);
	}

	@Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"))
	private void etfu$applySkinParts(AbstractClientPlayer player, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
		etfu$applySkinParts(player);
	}

	/**
	 * The arm drawn in first person is always the model's right arm, mirrored by the off-hand code when it stands for
	 * the left one: it wears the sleeve of the arm it stands for, which depends on the hand being drawn and the
	 * player's main hand.
	 */
	@Inject(method = "renderFirstPersonArm", at = @At("HEAD"))
	private void etfu$applySkinPartsToTheArm(EntityPlayer player, CallbackInfo ci) {
		etfu$applySkinParts(player);
		if (modelBipedMain instanceof ModelPlayer model) {
			Arm arm = Arm.RIGHT;
			if (Offhand.isEnabled()) {
				Hand hand = Minecraft.getMinecraft().entityRenderer.itemRenderer instanceof IOffhandItemRenderer renderer ? renderer.etfu$drawnHand() : Hand.MAIN;
				arm = hand.arm(Offhand.isLeftHanded(player));
			}
			model.bipedRightArmwear.showModel = SkinParts.shown(player, arm == Arm.LEFT ? SkinPart.LEFT_SLEEVE : SkinPart.RIGHT_SLEEVE);
		}
	}

	@Unique
	private void etfu$applySkinParts(EntityPlayer player) {
		modelBipedMain.bipedHeadwear.showModel = SkinParts.shown(player, SkinPart.HAT);
		if (modelBipedMain instanceof ModelPlayer model) {
			model.bipedBodyWear.showModel = SkinParts.shown(player, SkinPart.JACKET);
			model.bipedLeftArmwear.showModel = SkinParts.shown(player, SkinPart.LEFT_SLEEVE);
			model.bipedRightArmwear.showModel = SkinParts.shown(player, SkinPart.RIGHT_SLEEVE);
			model.bipedLeftLegwear.showModel = SkinParts.shown(player, SkinPart.LEFT_PANTS_LEG);
			model.bipedRightLegwear.showModel = SkinParts.shown(player, SkinPart.RIGHT_PANTS_LEG);
		}
	}

	/** The cape is one of the parts. */
	@ModifyExpressionValue(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getHideCape()Z"))
	private boolean etfu$capeIsAPart(boolean hidden, @Local(argsOnly = true) AbstractClientPlayer player) {
		return hidden || !SkinParts.shown(player, SkinPart.CAPE);
	}
}
