package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A spawner's mob posts `entity_place` at the block it's made in, by itself (SPEC-modern-movement.md §90.9, step H1; 26.3's `BaseSpawner.serverTick`). */
@Mixin(MobSpawnerBaseLogic.class)
public abstract class MixinMobSpawnerBaseLogic {

	@WrapOperation(method = "updateSpawner", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/MobSpawnerBaseLogic;func_98265_a(Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/Entity;"))
	private Entity mcfly$spawned(MobSpawnerBaseLogic self, Entity entity, Operation<Entity> original) {
		Entity spawned = original.call(self, entity);
		if (entity.worldObj != null) {
			GameEventHooks.atBlock(entity.worldObj, "entity_place", MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY),
				MathHelper.floor_double(entity.posZ), entity);
		}
		return spawned;
	}
}
