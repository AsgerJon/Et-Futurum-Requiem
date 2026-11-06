package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A mob from a spawn egg posts `entity_place` at the block it's made in, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `SpawnEggItem.spawnMob`). */
@Mixin(ItemMonsterPlacer.class)
public abstract class MixinItemMonsterPlacer {

	@WrapOperation(method = {"onItemUse", "onItemRightClick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemMonsterPlacer;spawnCreature(Lnet/minecraft/world/World;IDDD)Lnet/minecraft/entity/Entity;"))
	private Entity mcfly$spawned(World world, int id, double x, double y, double z, Operation<Entity> original, @Local(argsOnly = true) EntityPlayer player) {
		Entity entity = original.call(world, id, x, y, z);
		if (entity != null) {
			GameEventHooks.atBlock(world, "entity_place", MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z), player);
		}
		return entity;
	}
}
