package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.client.EndBossBar;

public class EndBossBarHandler implements IMessageHandler<EndBossBarMessage, IMessage> {

	@Override
	public IMessage onMessage(EndBossBarMessage message, MessageContext ctx) {
		EndBossBar.set(message.visible, message.progress);
		return null;
	}
}
