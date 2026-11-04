package io.github.asgerjon.mcfly.mixins.early.trident;

import ganymedes01.etfuturum.trident.Lightning;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Natural lightning as 26.3's (SPEC-modern-movement.md §58.3): after 1.7.10's 1 in 100 000 chance and its random
 * column, Lightning.natural picks where it strikes (a lightning rod, a mob, the surface) and whether it leaves a
 * skeleton horse trap; 1.7.10's own bolt at the surface doesn't come.
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

	@Redirect(method = "func_147456_g()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;canLightningStrikeAt(III)Z"))
	private boolean etfu$modernLightning(WorldServer world, int x, int y, int z) {
		Lightning.natural(world, x, z);
		return false;
	}
}
