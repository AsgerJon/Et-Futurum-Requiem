package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHangingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A painting or item frame hung posts `entity_place` at it, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `HangingEntityItem.useOn`). */
@Mixin(ItemHangingEntity.class)
public abstract class MixinItemHangingEntity {

	@WrapOperation(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z"))
	private boolean mcfly$hung(World world, Entity hanging, Operation<Boolean> original, @Local(argsOnly = true) EntityPlayer player) {
		boolean spawned = original.call(world, hanging);
		// 26.3's `HangingEntity.position()`: its middle, 1.7.10's posY too
		GameEvents.post(world, GameEvent.get("entity_place"), hanging.posX, hanging.posY, hanging.posZ, player, null, 0);
		return spawned;
	}
}
