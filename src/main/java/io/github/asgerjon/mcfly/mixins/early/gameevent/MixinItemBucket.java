package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBucket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bucket filled posts `fluid_pickup` at the fluid, by the player; one emptied posts `fluid_place` where it went, by
 * the player (none from a dispenser), not where water boils away (SPEC-modern-movement.md §90.9, step H1; 26.3's
 * `BucketItem`).
 */
@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {

	@WrapOperation(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(III)Z"))
	private boolean mcfly$filled(World world, int x, int y, int z, Operation<Boolean> original, @Local(argsOnly = true) EntityPlayer player) {
		boolean taken = original.call(world, x, y, z);
		if (taken) {
			GameEventHooks.atBlock(world, "fluid_pickup", x, y, z, player);
		}
		return taken;
	}

	@WrapOperation(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemBucket;tryPlaceContainedLiquid(Lnet/minecraft/world/World;III)Z"))
	private boolean mcfly$emptiedBy(ItemBucket bucket, World world, int x, int y, int z, Operation<Boolean> original, @Local(argsOnly = true) EntityPlayer player) {
		boolean[] placed = new boolean[1];
		GameEventHooks.byPlayer(player, () -> placed[0] = original.call(bucket, world, x, y, z));
		return placed[0];
	}

	@Inject(method = "tryPlaceContainedLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z", shift = At.Shift.AFTER))
	private void mcfly$emptied(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, "fluid_place", x, y, z, GameEventHooks.actor());
	}
}
