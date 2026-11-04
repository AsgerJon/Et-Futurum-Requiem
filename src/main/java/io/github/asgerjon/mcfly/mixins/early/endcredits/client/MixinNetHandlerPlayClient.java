package io.github.asgerjon.mcfly.mixins.early.endcredits.client;

import ganymedes01.etfuturum.core.utils.EndCredits;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The credits notice (game state 4) for a player who has seen them (SPEC-modern-movement.md §53.2): no credits, the
 * respawn request the credits screen sends when it closes goes out at once.
 */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

	@Inject(method = "handleChangeGameState", at = @At("HEAD"), cancellable = true)
	private void etfu$skipSeenCredits(S2BPacketChangeGameState packet, CallbackInfo ci) {
		if (packet.func_149138_c() == 4 && packet.func_149137_d() == EndCredits.SKIP) {
			((NetHandlerPlayClient) (Object) this).addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
			ci.cancel();
		}
	}
}
