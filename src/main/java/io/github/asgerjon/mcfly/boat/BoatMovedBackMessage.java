package io.github.asgerjon.mcfly.boat;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * The server's place for a boat it didn't let its steering client move (SPEC-modern-movement.md 91.12; 26.3's
 * `ClientboundMoveVehiclePacket`): that client's boat is put there. 1.7.10's own position packets don't reach a boat its
 * client moves.
 */
public class BoatMovedBackMessage implements IMessage {

	private int entityId;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	public BoatMovedBackMessage() {
	}

	BoatMovedBackMessage(McFlyBoat boat) {
		entityId = boat.getEntityId();
		x = boat.posX;
		y = boat.posY;
		z = boat.posZ;
		yaw = boat.rotationYaw;
		pitch = boat.rotationPitch;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
	}

	public static class Handler implements IMessageHandler<BoatMovedBackMessage, IMessage> {

		@Override
		public IMessage onMessage(BoatMovedBackMessage message, MessageContext ctx) {
			io.github.asgerjon.mcfly.boat.client.BoatClient.movedBack(message.entityId, message.x, message.y, message.z, message.yaw, message.pitch);
			return null;
		}
	}
}
