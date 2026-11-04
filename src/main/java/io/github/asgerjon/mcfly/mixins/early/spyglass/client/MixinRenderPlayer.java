package io.github.asgerjon.mcfly.mixins.early.spyglass.client;

import ganymedes01.etfuturum.client.Spyglass;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The head's matrix for a scoping player's spyglass, held to the eye (SPEC-modern-movement.md §64.5). */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

	@Shadow
	public ModelBiped modelBipedMain;

	@Inject(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At("HEAD"))
	private void etfu$noteHead(AbstractClientPlayer player, float partialTicks, CallbackInfo ci) {
		Spyglass.noteHead(modelBipedMain, player);
	}
}
