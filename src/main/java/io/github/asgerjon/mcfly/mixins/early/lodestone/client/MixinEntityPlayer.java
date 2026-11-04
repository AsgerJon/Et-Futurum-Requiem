package io.github.asgerjon.mcfly.mixins.early.lodestone.client;

import ganymedes01.etfuturum.client.LodestoneNeedle;
import ganymedes01.etfuturum.items.LodestoneCompass;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A lodestone compass a player holds points by that player (SPEC-modern-movement.md §57.2). Forge's
 * EntityPlayer.getItemIcon calls EntityLivingBase's first but then replaces it with Item.getIcon, which never asks
 * getIconIndex, so MixinEntityLivingBase alone doesn't reach a player's hand.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

	@Inject(method = "getItemIcon", at = @At("HEAD"), cancellable = true)
	private void etfu$heldLodestoneNeedle(ItemStack stack, int pass, CallbackInfoReturnable<IIcon> cir) {
		if (stack != null && stack.getItem() == Items.compass && LodestoneCompass.isLodestoneCompass(stack)) {
			EntityPlayer holder = (EntityPlayer) (Object) this;
			cir.setReturnValue(LodestoneNeedle.icon(stack, holder, holder.getEntityId()));
		}
	}
}
