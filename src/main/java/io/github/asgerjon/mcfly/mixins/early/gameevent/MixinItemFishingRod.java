package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A rod cast posts `item_interact_start`, reeled in `item_interact_finish`, at the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `FishingRodItem.use`). */
@Mixin(ItemFishingRod.class)
public abstract class MixinItemFishingRod {

	@Inject(method = "onItemRightClick", at = @At("HEAD"))
	private void mcfly$before(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir, @Share("reeling") LocalBooleanRef reeling) {
		reeling.set(player.fishEntity != null);
	}

	@Inject(method = "onItemRightClick", at = @At("RETURN"))
	private void mcfly$after(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir, @Share("reeling") LocalBooleanRef reeling) {
		GameEventHooks.usedItem(player, !reeling.get());
	}
}
