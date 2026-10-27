package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;

public class SwapHandsHandler implements IMessageHandler<SwapHandsMessage, IMessage> {

	@Override
	public IMessage onMessage(SwapHandsMessage message, MessageContext ctx) {
		EntityPlayer player = ((NetHandlerPlayServer) ctx.netHandler).playerEntity;
		if (!SpectatorMode.isSpectator(player)) {
			Offhand.swapHands(player);
		}
		return null;
	}
}
