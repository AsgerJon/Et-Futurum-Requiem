package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.command.modern.ModernCommands;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.network.NetHandlerPlayServer;

public class CommandSuggestionRequestHandler implements IMessageHandler<CommandSuggestionRequestMessage, IMessage> {

	@Override
	public IMessage onMessage(CommandSuggestionRequestMessage message, MessageContext ctx) {
		if (ConfigMixins.modernChat) {
			ModernCommands.requestSuggestions(((NetHandlerPlayServer) ctx.netHandler).playerEntity, message.id, message.text);
		}
		return null;
	}
}
