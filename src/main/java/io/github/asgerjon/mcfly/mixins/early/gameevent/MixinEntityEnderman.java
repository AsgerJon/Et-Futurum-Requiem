package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** An enderman's teleport posts `teleport` at where it was, with the sound there (SPEC-modern-movement.md §90.9, step H1; 26.3's `Enderman.teleport`). */
@Mixin(EntityEnderman.class)
public abstract class MixinEntityEnderman {

	@WrapOperation(method = "teleportTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"))
	private void mcfly$teleported(World world, double x, double y, double z, String sound, float volume, float pitch, Operation<Void> original) {
		GameEvents.post(world, GameEvent.get("teleport"), x, y, z, (EntityEnderman) (Object) this, null, 0);
		original.call(world, x, y, z, sound, volume, pitch);
	}
}
