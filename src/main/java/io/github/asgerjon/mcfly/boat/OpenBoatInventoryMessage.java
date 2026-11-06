package io.github.asgerjon.mcfly.boat;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * The inventory key pressed in a chest boat (SPEC-modern-movement.md 91.12; 26.3's `OPEN_INVENTORY` command for a
 * `HasCustomInventoryScreen` vehicle, in place of EFR's channel's 5): the server opens the boat's chest.
 */
public class OpenBoatInventoryMessage implements IMessage {

	public OpenBoatInventoryMessage() {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}

	public static class Handler implements IMessageHandler<OpenBoatInventoryMessage, IMessage> {

		@Override
		public IMessage onMessage(OpenBoatInventoryMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			if (player.ridingEntity instanceof McFlyChestBoat boat && !boat.isDead) {
				boat.openFor(player);
			}
			return null;
		}
	}
}
