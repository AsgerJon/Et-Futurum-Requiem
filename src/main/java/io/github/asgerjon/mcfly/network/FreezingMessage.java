package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** An entity's frozen ticks, from the server (spec §62.3): to a player about itself, and to trackers about a mob. */
public class FreezingMessage implements IMessage {

	public int entityId;
	public int ticks;

	public FreezingMessage() {
	}

	public FreezingMessage(int entityId, int ticks) {
		this.entityId = entityId;
		this.ticks = ticks;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
		ticks = buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeShort(ticks);
	}
}
