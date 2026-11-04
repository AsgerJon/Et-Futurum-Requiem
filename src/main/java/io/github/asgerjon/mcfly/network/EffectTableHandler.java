package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.potion.EffectNumbers;

/** The client takes the server's effect numbers (spec §70.3, stage C); on the network thread, before effect packets. */
public class EffectTableHandler implements IMessageHandler<EffectTableMessage, IMessage> {

	@Override
	public IMessage onMessage(EffectTableMessage message, MessageContext context) {
		EffectNumbers.applyServerTable(message.table);
		return null;
	}
}
