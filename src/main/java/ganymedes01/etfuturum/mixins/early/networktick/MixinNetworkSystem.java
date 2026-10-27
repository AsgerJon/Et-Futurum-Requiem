package ganymedes01.etfuturum.mixins.early.networktick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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

	@ModifyExpressionValue(method = "networkTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;isChannelOpen()Z"))
	private boolean etfu$notYetActiveIsNotClosed(boolean open, @Local NetworkManager manager) {
		return open || ((AccessorNetworkManager) manager).etfu$getChannel() == null;
	}

	@WrapOperation(method = "networkTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;processReceivedPackets()V"))
	private void etfu$readOnlyOnceActive(NetworkManager manager, Operation<Void> original) {
		if (((AccessorNetworkManager) manager).etfu$getChannel() != null) {
			original.call(manager);
		}
	}
}
