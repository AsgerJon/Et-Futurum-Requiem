package io.github.asgerjon.mcfly.mixins.early.quickwins4.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.client.renderer.item.HeldItemTransforms;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zombies', skeletons' and other bipeds' held item placed as 26.3's (SPEC-modern-movement.md §63.6). */
@Mixin(RenderBiped.class)
public abstract class MixinRenderBiped {

	@Shadow
	protected ModelBiped modelBipedMain;

	@Unique
	private EntityLiving etfu$entity;

	@Inject(method = "renderEquippedItems(Lnet/minecraft/entity/EntityLiving;F)V", at = @At("HEAD"))
	private void etfu$forgetArm(EntityLiving entity, float partialTicks, CallbackInfo ci) {
		HeldItemTransforms.forgetArm();
		etfu$entity = entity;
	}

	@WrapOperation(method = "renderEquippedItems(Lnet/minecraft/entity/EntityLiving;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;postRender(F)V"))
	private void etfu$noteArm(ModelRenderer part, float scale, Operation<Void> original) {
		if (part != modelBipedMain.bipedRightArm) {
			original.call(part, scale);
			return;
		}
		if (HeldItemTransforms.isEnabled() && etfu$entity instanceof EntitySkeleton) {
			// 26.3's SkeletonModel.translateToHand: the thin arm's pivot a pixel in for the item (spec §64.1)
			GL11.glPushMatrix();
			GL11.glTranslatef(1.0F / 16.0F, 0.0F, 0.0F);
			original.call(part, scale);
			HeldItemTransforms.noteArm();
			GL11.glPopMatrix();
			original.call(part, scale);
			return;
		}
		original.call(part, scale);
		HeldItemTransforms.noteArm();
	}

	@WrapOperation(method = "renderEquippedItems(Lnet/minecraft/entity/EntityLiving;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;I)V"))
	private void etfu$modernHeldItem(ItemRenderer renderer, EntityLivingBase entity, ItemStack stack, int pass, Operation<Void> original) {
		HeldItemTransforms.renderFromArm(stack, () -> original.call(renderer, entity, stack, pass));
	}
}
