package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;

public class StartElytraFlyingHandler implements IMessageHandler<StartElytraFlyingMessage, IMessage> {

	/**
	 * The client started gliding and asks the server (§18.2). If the server disagrees, it turns gliding off and sends
	 * that back, which corrects the client.
	 */
	@Override
	public IMessage onMessage(StartElytraFlyingMessage message, MessageContext ctx) {
		EntityPlayer player = ((NetHandlerPlayServer) ctx.netHandler).playerEntity;
		if (!FallFlying.tryToStart(player)) {
			FallFlying.stop(player);
		}
		return null;
	}
}
