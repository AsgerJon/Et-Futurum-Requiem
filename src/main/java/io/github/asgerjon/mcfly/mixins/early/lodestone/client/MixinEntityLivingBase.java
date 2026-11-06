package io.github.asgerjon.mcfly.mixins.early.lodestone.client;

import io.github.asgerjon.mcfly.lodestone.client.LodestoneNeedle;
import io.github.asgerjon.mcfly.lodestone.LodestoneCompass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A held lodestone compass (first and third person, EFR's off-hand) points by whoever holds it
 * (SPEC-modern-movement.md §57.2). Players overwrite this one in their own getItemIcon: MixinEntityPlayer.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

	@Inject(method = "getItemIcon", at = @At("HEAD"), cancellable = true)
	private void etfu$heldLodestoneNeedle(ItemStack stack, int pass, CallbackInfoReturnable<IIcon> cir) {
		if (stack != null && stack.getItem() == Items.compass && LodestoneCompass.isLodestoneCompass(stack)) {
			Entity holder = (Entity) (Object) this;
			cir.setReturnValue(LodestoneNeedle.icon(stack, holder, holder.getEntityId()));
		}
	}
}
