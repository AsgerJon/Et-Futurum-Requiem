package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 26.3's `ClientboundOpenSignEditorPacket` (SPEC-modern-movement.md 91.8): the sign's place and the side to edit. The
 * client opens McFly's editor if a sign is there (after the sign itself, sent just before on the same channel).
 * In place of EFR's `WoodSignOpenMessage` (its channel's 3).
 */
public class OpenSignEditorMessage implements IMessage {

	public int x, y, z;
	public boolean front;

	public OpenSignEditorMessage() {
	}

	public OpenSignEditorMessage(int x, int y, int z, boolean front) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.front = front;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		front = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeBoolean(front);
	}

	public static class Handler implements IMessageHandler<OpenSignEditorMessage, IMessage> {

		@Override
		public IMessage onMessage(OpenSignEditorMessage message, MessageContext ctx) {
			io.github.asgerjon.mcfly.wood.client.SignEditScreen.open(message.x, message.y, message.z, message.front);
			return null;
		}
	}
}
