package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * `eat` or `drink` (SPEC-modern-movement.md §90.9, step H1; 26.3's `Consumable.onConsume`) where 1.7.10 finishes a use
 * (`onFoodEaten`, from the main hand's finish and McFly's off-hand one alike): after the item's own effects (a chorus
 * fruit's teleport), as 26.3's.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {

	@Inject(method = "onFoodEaten", at = @At("RETURN"))
	private void mcfly$consumed(World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		GameEventHooks.consumed((ItemStack) (Object) this, world, player);
	}
}
