package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** A click the main hand didn't use, for the off-hand (SPEC-modern-movement.md §36.2): on an entity, a block or the air. */
public class UseOffhandMessage implements IMessage {

	public static final byte AIR = 0, BLOCK = 1, ENTITY = 2;

	public byte kind;
	public int x, y, z, side, entityId;
	public float hitX, hitY, hitZ;

	public UseOffhandMessage() {
	}

	public static UseOffhandMessage air() {
		UseOffhandMessage m = new UseOffhandMessage();
		m.kind = AIR;
		return m;
	}

	public static UseOffhandMessage block(int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		UseOffhandMessage m = new UseOffhandMessage();
		m.kind = BLOCK;
		m.x = x;
		m.y = y;
		m.z = z;
		m.side = side;
		m.hitX = hitX;
		m.hitY = hitY;
		m.hitZ = hitZ;
		return m;
	}

	public static UseOffhandMessage entity(int entityId) {
		UseOffhandMessage m = new UseOffhandMessage();
		m.kind = ENTITY;
		m.entityId = entityId;
		return m;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		kind = buf.readByte();
		if (kind == BLOCK) {
			x = buf.readInt();
			y = buf.readInt();
			z = buf.readInt();
			side = buf.readByte();
			hitX = buf.readFloat();
			hitY = buf.readFloat();
			hitZ = buf.readFloat();
		} else if (kind == ENTITY) {
			entityId = buf.readInt();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(kind);
		if (kind == BLOCK) {
			buf.writeInt(x);
			buf.writeInt(y);
			buf.writeInt(z);
			buf.writeByte(side);
			buf.writeFloat(hitX);
			buf.writeFloat(hitY);
			buf.writeFloat(hitZ);
		} else if (kind == ENTITY) {
			buf.writeInt(entityId);
		}
	}
}
