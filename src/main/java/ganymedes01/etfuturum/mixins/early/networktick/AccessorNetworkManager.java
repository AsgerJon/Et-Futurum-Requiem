package ganymedes01.etfuturum.mixins.early.networktick;

import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetworkManager.class)
public interface AccessorNetworkManager {
	/** Null until Netty reports the channel active, which is after the connection has been listed. */
	@Accessor("channel")
	Channel etfu$getChannel();
}
