package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.blocks.BlockPowderSnow;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 {@code enderman_does_not_teleport_to} holds {@code dangerous_for_teleportation}, which holds powder snow
 * (SPEC-modern-movement.md §62.11): a landing box with powder snow in it fails, as one with a liquid does.
 */
@Mixin(EntityEnderman.class)
public abstract class MixinEntityEnderman {

	@WrapOperation(method = "teleportTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAnyLiquid(Lnet/minecraft/util/AxisAlignedBB;)Z"))
	private boolean etfu$notIntoPowderSnow(World world, AxisAlignedBB box, Operation<Boolean> original) {
		return original.call(world, box) || BlockPowderSnow.anyIn(world, box);
	}
}
