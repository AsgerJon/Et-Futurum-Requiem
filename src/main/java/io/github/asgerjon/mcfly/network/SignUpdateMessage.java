package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.github.asgerjon.mcfly.wood.Signs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.NetHandlerPlayServer;

/**
 * 26.3's `ServerboundSignUpdatePacket` (SPEC-modern-movement.md 91.8): the editor's four lines for a side of the sign at
 * a place, each at most 384 characters (a longer one fails the message, as 26.3's packet fails to decode). The server
 * takes it by `Signs.update`.
 */
public class SignUpdateMessage implements IMessage {

	public int x, y, z;
	public boolean front;
	public String[] lines = new String[Signs.LINES];
	private boolean valid = true;

	public SignUpdateMessage() {
	}

	public SignUpdateMessage(int x, int y, int z, boolean front, String[] lines) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.front = front;
		for (int i = 0; i < Signs.LINES; i++) {
			this.lines[i] = i < lines.length && lines[i] != null ? lines[i] : "";
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		front = buf.readBoolean();
		for (int i = 0; i < Signs.LINES; i++) {
			lines[i] = ByteBufUtils.readUTF8String(buf);
			if (lines[i].length() > Signs.MAX_CHARACTERS) {
				valid = false;
			}
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeBoolean(front);
		for (String line : lines) {
			ByteBufUtils.writeUTF8String(buf, line);
		}
	}

	public static class Handler implements IMessageHandler<SignUpdateMessage, IMessage> {

		@Override
		public IMessage onMessage(SignUpdateMessage message, MessageContext ctx) {
			if (message.valid) {
				Signs.update(((NetHandlerPlayServer) ctx.netHandler).playerEntity, message.x, message.y, message.z, message.front, message.lines);
			}
			return null;
		}
	}
}
