package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.chat.ClientCommands;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;

public class CommandSuggestionsHandler implements IMessageHandler<CommandSuggestionsMessage, IMessage> {

	@Override
	public IMessage onMessage(CommandSuggestionsMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(CommandSuggestionsMessage message) {
		if (McFlyConfig.modernChat) {
			ClientCommands.source().completeServerSuggestions(message);
		}
	}
}
