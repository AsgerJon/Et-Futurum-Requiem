package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMinecart;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A minecart put on a rail posts `entity_place` at it, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `MinecartItem.useOn`). */
@Mixin(ItemMinecart.class)
public abstract class MixinItemMinecart {

	@WrapOperation(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z"))
	private boolean mcfly$placed(World world, Entity cart, Operation<Boolean> original, @Local(argsOnly = true) EntityPlayer player) {
		boolean spawned = original.call(world, cart);
		GameEventHooks.atEntity(cart, "entity_place", player);
		return spawned;
	}
}
