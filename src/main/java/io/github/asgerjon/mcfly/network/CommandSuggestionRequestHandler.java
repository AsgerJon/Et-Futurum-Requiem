package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.command.modern.ModernCommands;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.network.NetHandlerPlayServer;

public class CommandSuggestionRequestHandler implements IMessageHandler<CommandSuggestionRequestMessage, IMessage> {

	@Override
	public IMessage onMessage(CommandSuggestionRequestMessage message, MessageContext ctx) {
		if (McFlyConfig.modernChat) {
			ModernCommands.requestSuggestions(((NetHandlerPlayServer) ctx.netHandler).playerEntity, message.id, message.text);
		}
		return null;
	}
}
