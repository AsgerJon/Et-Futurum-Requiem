package io.github.asgerjon.mcfly.mixins.early.totem.client;

import ganymedes01.etfuturum.client.TotemEffects;
import ganymedes01.etfuturum.core.utils.EntityEvents;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A used totem's entity status (SPEC-modern-movement.md §55.2), taken where modern takes its entity event 35: before the
 * entity's own handleHealthUpdate, which many entities override.
 */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

	@Shadow
	private WorldClient clientWorldController;

	@Inject(method = "handleEntityStatus", at = @At("HEAD"), cancellable = true)
	private void etfu$totemUsed(S19PacketEntityStatus packet, CallbackInfo ci) {
		if (packet.func_149160_c() == EntityEvents.TOTEM_USED) {
			Entity entity = packet.func_149161_a(clientWorldController);
			if (entity != null) {
				TotemEffects.play(entity);
			}
			ci.cancel();
		}
	}
}
