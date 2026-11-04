package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.FilledResult;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An empty bucket filled from water or lava in creative adds the filled one, once (26.3; spec §63.1). */
@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {

	@Inject(method = "func_150910_a", at = @At("HEAD"), cancellable = true)
	private void etfu$creativeFilledResult(ItemStack empty, EntityPlayer player, Item filled, CallbackInfoReturnable<ItemStack> cir) {
		if (McFlyConfig.modernCreativeFilling && player.capabilities.isCreativeMode) {
			cir.setReturnValue(FilledResult.fill(player, empty, new ItemStack(filled)));
		}
	}
}
