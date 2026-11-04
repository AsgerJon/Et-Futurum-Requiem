package ganymedes01.etfuturum.mixins.early.unittas.client;

import ganymedes01.etfuturum.unittas.TasTrace;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.client.network.NetHandlerHandshakeMemory;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.server.network.NetHandlerLoginServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** unitTAS's loading trace (TasTrace): the server's handshake (a local connection's is NetHandlerHandshakeMemory). */
@Mixin({NetHandlerHandshakeTCP.class, NetHandlerHandshakeMemory.class})
public abstract class MixinHandshakeTrace {
	@Inject(method = "processHandshake", at = @At("HEAD"), require = 0)
	private void etfu$handshake(C00Handshake packet, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.note("server handshake: to " + packet.func_149594_c());
		}
	}
}
