package io.github.asgerjon.mcfly.mixins.early.quickwins4.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.client.renderer.item.HeldItemTransforms;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The player's right-arm item placed as 26.3's (SPEC-modern-movement.md §63.6; the left arm's is EFR's off-hand pass).
 * A cast fishing rod stays a rod (1.7.10 draws a stick), as 26.3 draws the cast rod.
 */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

	@Shadow
	public ModelBiped modelBipedMain;

	@Inject(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At("HEAD"))
	private void etfu$forgetArm(AbstractClientPlayer player, float partialTicks, CallbackInfo ci) {
		HeldItemTransforms.forgetArm();
	}

	@WrapOperation(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;postRender(F)V"))
	private void etfu$noteArm(ModelRenderer part, float scale, Operation<Void> original) {
		original.call(part, scale);
		if (part == modelBipedMain.bipedRightArm) {
			HeldItemTransforms.noteArm();
		}
	}

	@WrapOperation(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;I)V"))
	private void etfu$modernHeldItem(ItemRenderer renderer, EntityLivingBase entity, ItemStack stack, int pass, Operation<Void> original) {
		ItemStack shown = HeldItemTransforms.isEnabled() && stack.getItem() == Items.stick && entity instanceof EntityPlayer
				&& ((EntityPlayer) entity).fishEntity != null ? new ItemStack(Items.fishing_rod) : stack;
		HeldItemTransforms.renderFromArm(shown, () -> original.call(renderer, entity, shown, pass));
	}
}
