package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.entities.StraySpawning;
import net.minecraft.entity.EntityLiving;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A spawner's spawn check is marked as one (26.3's {@code EntitySpawnReason.SPAWNER}; strays need no sky then, §63.2). */
@Mixin(MobSpawnerBaseLogic.class)
public abstract class MixinMobSpawnerBaseLogic {

	@WrapOperation(method = "updateSpawner", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLiving;getCanSpawnHere()Z"))
	private boolean etfu$fromSpawner(EntityLiving entity, Operation<Boolean> original) {
		return StraySpawning.fromSpawner(() -> original.call(entity));
	}
}
