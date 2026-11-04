package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * 26.3's level particles packet (`ClientboundLevelParticlesPacket`), from the server: a particle by its name, where, its
 * spread and speed on each axis, how many, and how they're spread (SPEC-modern-movement.md §79, B7).
 */
public class ParticleMessage implements IMessage {

	public String name;
	public boolean overrideLimiter;
	public boolean alwaysShow;
	public double x, y, z;
	public float xDist, yDist, zDist;
	public float xMaxSpeed, yMaxSpeed, zMaxSpeed;
	public int count;
	public int randomization;

	public ParticleMessage() {
	}

	public ParticleMessage(String name, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, float xDist, float yDist,
		float zDist, float xMaxSpeed, float yMaxSpeed, float zMaxSpeed, int count, int randomization) {
		this.name = name;
		this.overrideLimiter = overrideLimiter;
		this.alwaysShow = alwaysShow;
		this.x = x;
		this.y = y;
		this.z = z;
		this.xDist = xDist;
		this.yDist = yDist;
		this.zDist = zDist;
		this.xMaxSpeed = xMaxSpeed;
		this.yMaxSpeed = yMaxSpeed;
		this.zMaxSpeed = zMaxSpeed;
		this.count = count;
		this.randomization = randomization;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		name = ByteBufUtils.readUTF8String(buf);
		overrideLimiter = buf.readBoolean();
		alwaysShow = buf.readBoolean();
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		xDist = buf.readFloat();
		yDist = buf.readFloat();
		zDist = buf.readFloat();
		xMaxSpeed = buf.readFloat();
		yMaxSpeed = buf.readFloat();
		zMaxSpeed = buf.readFloat();
		count = ByteBufUtils.readVarInt(buf, 5);
		randomization = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, name);
		buf.writeBoolean(overrideLimiter);
		buf.writeBoolean(alwaysShow);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(xDist);
		buf.writeFloat(yDist);
		buf.writeFloat(zDist);
		buf.writeFloat(xMaxSpeed);
		buf.writeFloat(yMaxSpeed);
		buf.writeFloat(zMaxSpeed);
		ByteBufUtils.writeVarInt(buf, count, 5);
		buf.writeByte(randomization);
	}
}
