package ganymedes01.etfuturum.mixins.early.unittas.client;

import ganymedes01.etfuturum.unittas.TasTrace;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.server.S01PacketJoinGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** unitTAS's loading trace (TasTrace): the client joining the game. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinPlayClientTrace {
	@Inject(method = "handleJoinGame", at = @At("HEAD"), require = 0)
	private void etfu$joinGame(S01PacketJoinGame packet, CallbackInfo ci) {
		if (TasTrace.recording()) {
			TasTrace.note("client joins the game");
		}
	}
}
