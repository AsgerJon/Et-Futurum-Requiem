package io.github.asgerjon.mcfly.mixins.early.lodestone.client;

import ganymedes01.etfuturum.client.LodestoneNeedle;
import ganymedes01.etfuturum.items.LodestoneCompass;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The lodestone compass's look (SPEC-modern-movement.md §57.2): the glint, and its own needle frame in place of 1.7.10's
 * animated compass sprite. Screens, tooltips, dropped items and item frames ask getIconIndex; held items go through
 * getItemIcon (MixinEntityLivingBase, MixinEntityPlayer).
 */
@Mixin(Item.class)
public class MixinItem {

	@Inject(method = "getIconIndex", at = @At("HEAD"), cancellable = true)
	private void etfu$lodestoneNeedle(ItemStack stack, CallbackInfoReturnable<IIcon> cir) {
		if ((Object) this == Items.compass && LodestoneCompass.isLodestoneCompass(stack)) {
			cir.setReturnValue(LodestoneNeedle.icon(stack));
		}
	}

	@Inject(method = "hasEffect(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void etfu$lodestoneGlint(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == Items.compass && LodestoneCompass.isLodestoneCompass(stack)) {
			cir.setReturnValue(true);
		}
	}
}
