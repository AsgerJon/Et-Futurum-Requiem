package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.chat.ClientCommands;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;

public class CommandTreeHandler implements IMessageHandler<CommandTreeMessage, IMessage> {

	@Override
	public IMessage onMessage(CommandTreeMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(CommandTreeMessage message) {
		if (ConfigMixins.modernChat) {
			ClientCommands.receiveTree(message.tree);
		}
	}
}
