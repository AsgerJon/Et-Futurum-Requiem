package io.github.asgerjon.mcfly.network;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import ganymedes01.etfuturum.EtFuturum;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.WorldServer;

/**
 * One of 26.3's level events the server tells clients near it of (`ClientboundLevelEventPacket`): what happens is the
 * client's (LevelEventHandler). SPEC-modern-movement.md §76.10: 3000, a kill's end gateway appearing; §76.13: 2016, a
 * shulker's teleport.
 */
public class LevelEventMessage implements IMessage {

	public int event;
	public int x;
	public int y;
	public int z;
	public int data;

	public LevelEventMessage() {
	}

	public LevelEventMessage(int event, int x, int y, int z, int data) {
		this.event = event;
		this.x = x;
		this.y = y;
		this.z = z;
		this.data = data;
	}

	/** 26.3's non-global level event: to the players within 64 in the dimension. */
	public static void sendAround(WorldServer world, int event, int x, int y, int z, int data) {
		McFlyNetwork.CHANNEL.sendToAllAround(new LevelEventMessage(event, x, y, z, data),
			new NetworkRegistry.TargetPoint(world.provider.dimensionId, x + 0.5, y + 0.5, z + 0.5, 64.0));
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		event = buf.readInt();
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		data = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(event);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(data);
	}
}
