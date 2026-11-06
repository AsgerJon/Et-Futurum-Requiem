package io.github.asgerjon.mcfly.mixins.early.boat;

import io.github.asgerjon.mcfly.boat.BoatItem;
import io.github.asgerjon.mcfly.boat.BoatTypes;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's boat is oak's while `replaceOldBoats` is on (SPEC-modern-movement.md 91.12): placed as 26.3's
 * `BoatItem`, as our oak boat (old vanilla boats in the world still turn into ours as they join it).
 */
@Mixin(ItemBoat.class)
public abstract class MixinItemBoat {

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void mcfly$asOak(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		if (McFlyConfig.enableNewBoats && McFlyConfig.replaceOldBoats) {
			BoatItem.use(stack, world, player, BoatTypes.OAK, false);
			cir.setReturnValue(stack);
		}
	}
}
