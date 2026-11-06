package io.github.asgerjon.mcfly.mixins.early.networktick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A new connection is listed for the server's network tick before Netty has made its channel active, and 1.7.10's
 * tick took "no channel yet" for "closed": it dropped the connection from the list and never read from it again,
 * so the client waited on the loading screen forever, with nothing to time out. It needs the tick to land in the
 * instant between the two, hence the rare, random hangs. 1.8 fixed it with NetworkManager.hasNoChannel; this does
 * the same: a connection without a channel is left alone for the tick, neither dropped nor read.
 */
@Mixin(NetworkSystem.class)
public class MixinNetworkSystem {

	/**
	 * The channel is read once: Netty's thread sets it while the tick runs (it isn't volatile), so `isChannelOpen()`
	 * (one read) then `getChannel() == null` (another) could see no channel, then one, and drop an open connection as
	 * closed (2026-11-03: the trace of a hang showed the connection listed, dropped, then active, with no close).
	 */
	@ModifyExpressionValue(method = "networkTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;isChannelOpen()Z"))
	private boolean etfu$notYetActiveIsNotClosed(boolean open, @Local NetworkManager manager) {
		Channel channel = ((AccessorNetworkManager) manager).etfu$getChannel();
		return channel == null || channel.isOpen();
	}

	@WrapOperation(method = "networkTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;processReceivedPackets()V"))
	private void etfu$readOnlyOnceActive(NetworkManager manager, Operation<Void> original) {
		// a connection without a channel yet is left for the next tick (the 2026-10-27 fix)
		if (((AccessorNetworkManager) manager).etfu$getChannel() != null) {
			original.call(manager);
		}
	}
}
