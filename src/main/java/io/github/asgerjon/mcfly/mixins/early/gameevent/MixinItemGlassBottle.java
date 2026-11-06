package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemGlassBottle;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bottle filled from water posts `fluid_pickup` at the water, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `BottleItem.use`). */
@Mixin(ItemGlassBottle.class)
public abstract class MixinItemGlassBottle {

	@ModifyExpressionValue(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGlassBottle;getMovingObjectPositionFromPlayer(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Z)Lnet/minecraft/util/MovingObjectPosition;"))
	private MovingObjectPosition mcfly$aimedAt(MovingObjectPosition hit, @Share("hit") LocalRef<MovingObjectPosition> aimed) {
		aimed.set(hit);
		return hit;
	}

	@Inject(method = "onItemRightClick", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;stackSize:I", opcode = Opcodes.PUTFIELD, ordinal = 0))
	private void mcfly$filled(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir, @Share("hit") LocalRef<MovingObjectPosition> aimed) {
		MovingObjectPosition hit = aimed.get();
		if (hit != null) {
			GameEventHooks.atBlock(world, "fluid_pickup", hit.blockX, hit.blockY, hit.blockZ, player);
		}
	}
}
