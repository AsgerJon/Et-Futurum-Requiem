package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** A dragon fireball's burst (26.3's level event 2006): 200 breath particles at the block, and its sound. */
public class DragonBreathBurstMessage implements IMessage {

	public int x;
	public int y;
	public int z;
	public boolean sound;

	public DragonBreathBurstMessage() {
	}

	public DragonBreathBurstMessage(int x, int y, int z, boolean sound) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.sound = sound;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		sound = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeBoolean(sound);
	}
}
