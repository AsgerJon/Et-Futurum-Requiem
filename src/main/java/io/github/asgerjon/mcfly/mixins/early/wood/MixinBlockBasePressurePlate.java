package io.github.asgerjon.mcfly.mixins.early.wood;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlySetType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every pressure plate's clicks are its set type's (SPEC-modern-movement.md §91.7, step E): `pressure_plate.click_on`
 * and `click_off` (1.7.10 played `random.click` higher or lower; EFR's `ClientEventHandler` swapped it by step sound).
 */
@Mixin(BlockBasePressurePlate.class)
public abstract class MixinBlockBasePressurePlate {

	@WrapOperation(method = "func_150062_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"))
	private void mcfly$setTypeClick(World world, double x, double y, double z, String name, float volume, float pitch, Operation<Void> original) {
		if ("random.click".equals(name)) {
			world.playSoundEffect(x, y, z, McFly.MC_ASSETS + ":" + McFlySetType.of((Block) (Object) this).plateSound(pitch > 0.55F), 1.0F, 1.0F);
		} else {
			original.call(world, x, y, z, name, volume, pitch);
		}
	}
}
