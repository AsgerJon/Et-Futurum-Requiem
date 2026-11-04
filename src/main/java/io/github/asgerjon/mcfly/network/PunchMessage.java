package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** A left click went ahead (an entity, a block or the air), sent after it, as modern's punch packet (spec §46.1). */
public class PunchMessage implements IMessage {
	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}
}
