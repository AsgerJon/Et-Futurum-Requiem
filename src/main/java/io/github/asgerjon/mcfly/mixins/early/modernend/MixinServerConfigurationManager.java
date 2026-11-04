package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.ModernEndArrival;
import net.minecraft.entity.Entity;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10 zeroes an entity's pitch as it moves it into the End, before the platform step; 26.3 keeps it
 * (SPEC-modern-movement.md §76.5), so it's noted on the way in.
 */
@Mixin(ServerConfigurationManager.class)
public class MixinServerConfigurationManager {

	@Inject(method = "transferEntityToWorld(Lnet/minecraft/entity/Entity;ILnet/minecraft/world/WorldServer;Lnet/minecraft/world/WorldServer;Lnet/minecraft/world/Teleporter;)V", at = @At("HEAD"), remap = false)
	private void etfu$keepPitch(Entity entity, int fromDimension, WorldServer from, WorldServer to, Teleporter teleporter, CallbackInfo ci) {
		ModernEndArrival.rememberPitch(entity);
	}
}
