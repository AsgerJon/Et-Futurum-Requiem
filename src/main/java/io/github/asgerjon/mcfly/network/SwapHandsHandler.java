package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.spectator.Spectator;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;

public class SwapHandsHandler implements IMessageHandler<SwapHandsMessage, IMessage> {

	@Override
	public IMessage onMessage(SwapHandsMessage message, MessageContext ctx) {
		EntityPlayer player = ((NetHandlerPlayServer) ctx.netHandler).playerEntity;
		if (!Spectator.isSpectator(player)) {
			Offhand.swapHands(player);
		}
		return null;
	}
}
