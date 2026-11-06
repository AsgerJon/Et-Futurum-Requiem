package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBoat;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A boat put down posts `entity_place` where the player's look hit, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `BoatItem.use`). */
@Mixin(ItemBoat.class)
public abstract class MixinItemBoat {

	@ModifyExpressionValue(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;Z)Lnet/minecraft/util/MovingObjectPosition;"))
	private MovingObjectPosition mcfly$aimedAt(MovingObjectPosition hit, @Share("hit") LocalRef<MovingObjectPosition> aimed) {
		aimed.set(hit);
		return hit;
	}

	@WrapOperation(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z"))
	private boolean mcfly$placed(World world, Entity boat, Operation<Boolean> original, @Local(argsOnly = true) EntityPlayer player,
		@Share("hit") LocalRef<MovingObjectPosition> aimed) {
		boolean spawned = original.call(world, boat);
		MovingObjectPosition hit = aimed.get();
		if (hit != null && hit.hitVec != null) {
			GameEvents.post(world, GameEvent.get("entity_place"), hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, player, null, 0);
		}
		return spawned;
	}
}
