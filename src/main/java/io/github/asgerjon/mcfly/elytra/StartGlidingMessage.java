package io.github.asgerjon.mcfly.elytra;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;

/**
 * 26.3's `START_FALL_FLYING` player command (SPEC-modern-movement.md §18.2, §85): the client started gliding and says
 * so; the server tries it too, and where it can't glide it stops it, which the flag's sync puts right on the client.
 * No payload.
 */
public class StartGlidingMessage implements IMessage {

	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}

	public static class Handler implements IMessageHandler<StartGlidingMessage, IMessage> {

		@Override
		public IMessage onMessage(StartGlidingMessage message, MessageContext ctx) {
			EntityPlayer player = ((NetHandlerPlayServer) ctx.netHandler).playerEntity;
			if (!FallFlying.tryToStart(player)) {
				FallFlying.stop(player);
			}
			return null;
		}
	}
}
