package io.github.asgerjon.mcfly.mixins.early.base;

import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No entity takes id 0, as in 26.3 (its counter's first id is 1, `ENTITY_COUNTER.incrementAndGet()`). 1.7.10's counter
 * starts at 0, so the run's first entity, often a fresh world's player, was id 0, and code taking 0 for "no entity"
 * took it for none: vanilla's own spawn packets (a thrower's or an angler's id as the object's data, read only above
 * 0), a rocket's boosted entity (it pushed nothing). Both places that take an id from the counter, the constructor and
 * Forge's `resetEntityId`, take the next one when given 0. Ids aren't saved (a world keeps its entities by UUID).
 */
@Mixin(Entity.class)
public abstract class MixinEntityIds {

	@Shadow
	private static int nextEntityID;

	@Shadow
	private int entityId;

	// the methods' names aren't obfuscated (a constructor, a Forge method); the field is
	@Inject(method = {"<init>", "resetEntityId"}, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;entityId:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, remap = true), remap = false)
	private void mcfly$neverZero(CallbackInfo ci) {
		if (this.entityId == 0) {
			this.entityId = nextEntityID++;
		}
	}
}
