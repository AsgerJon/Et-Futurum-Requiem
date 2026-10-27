package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.offhand.OffhandUse;
import net.minecraft.network.NetHandlerPlayServer;

public class UseOffhandHandler implements IMessageHandler<UseOffhandMessage, IMessage> {

	@Override
	public IMessage onMessage(UseOffhandMessage message, MessageContext ctx) {
		OffhandUse.handleOnServer(((NetHandlerPlayServer) ctx.netHandler).playerEntity, message);
		return null;
	}
}
