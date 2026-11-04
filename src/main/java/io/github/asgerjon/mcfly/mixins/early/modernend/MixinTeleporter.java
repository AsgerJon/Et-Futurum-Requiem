package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.ModernEndArrival;
import ganymedes01.etfuturum.world.end.modern.WorldProviderModernEnd;
import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arriving in 26.3's End (SPEC-modern-movement.md §76.5): its platform and place instead of 1.7.10's (the block corner,
 * motion and pitch reset, blocks replaced without drops).
 */
@Mixin(Teleporter.class)
public class MixinTeleporter {

	@Shadow
	@Final
	private WorldServer worldServerInstance;

	@Inject(method = "placeInPortal", at = @At("HEAD"), cancellable = true)
	private void etfu$modernEndArrival(Entity entity, double x, double y, double z, float yaw, CallbackInfo ci) {
		if (worldServerInstance.provider instanceof WorldProviderModernEnd) {
			ModernEndArrival.arrive(worldServerInstance, entity);
			ci.cancel();
		}
	}
}
