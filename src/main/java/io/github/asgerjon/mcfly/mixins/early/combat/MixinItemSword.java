package io.github.asgerjon.mcfly.mixins.early.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swords no longer block (spec §46.5): right click does nothing with them, so it can go to the off-hand item. */
@Mixin(ItemSword.class)
public class MixinItemSword {

	@Inject(method = "getItemUseAction", at = @At("HEAD"), cancellable = true)
	private void etfu$noBlockAction(ItemStack stack, CallbackInfoReturnable<EnumAction> cir) {
		cir.setReturnValue(EnumAction.none);
	}

	@Inject(method = "getMaxItemUseDuration", at = @At("HEAD"), cancellable = true)
	private void etfu$noBlockDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(0);
	}

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void etfu$noBlocking(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		cir.setReturnValue(stack);
	}
}
