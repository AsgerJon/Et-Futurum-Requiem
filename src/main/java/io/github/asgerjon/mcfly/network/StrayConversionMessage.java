package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** A skeleton started or stopped turning into a stray (spec §62.7), for its shaking on the clients. */
public class StrayConversionMessage implements IMessage {

	public int entityId;
	public boolean converting;

	public StrayConversionMessage() {
	}

	public StrayConversionMessage(int entityId, boolean converting) {
		this.entityId = entityId;
		this.converting = converting;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
		converting = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeBoolean(converting);
	}
}
