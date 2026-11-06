package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A pressure plate pressed or let go posts `block_activate` or `_deactivate` with its click (SPEC-modern-movement.md
 * §90.9, step H1; 26.3's `BasePressurePlateBlock.checkPressed`), by the entity that stepped on it (none from its tick).
 */
@Mixin(BlockBasePressurePlate.class)
public abstract class MixinBlockBasePressurePlate {

	/** The entity whose touch is being checked now (the server's thread). */
	@Unique
	private static Entity mcfly$touching;

	@Inject(method = "onEntityCollidedWithBlock", at = @At("HEAD"))
	private void mcfly$touched(World world, int x, int y, int z, Entity entity, CallbackInfo ci) {
		mcfly$touching = entity;
	}

	@Inject(method = "onEntityCollidedWithBlock", at = @At("RETURN"))
	private void mcfly$touchChecked(World world, int x, int y, int z, Entity entity, CallbackInfo ci) {
		mcfly$touching = null;
	}

	@Inject(method = "func_150062_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V", ordinal = 0))
	private void mcfly$letGo(World world, int x, int y, int z, int power, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_deactivate", x, y, z, mcfly$touching);
	}

	@Inject(method = "func_150062_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V", ordinal = 1))
	private void mcfly$pressed(World world, int x, int y, int z, int power, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_activate", x, y, z, mcfly$touching);
	}
}
