package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * The client's player settings, as modern's client information carries them (1.8+): the shown skin parts and the
 * main hand (SPEC-modern-movement.md §37). Sent when the local player joins a world, when the config reloads and
 * when the skin customization screen closes.
 */
public class PlayerSettingsMessage implements IMessage {

	public int skinParts;
	public boolean leftHanded;

	public PlayerSettingsMessage() {
	}

	public PlayerSettingsMessage(int skinParts, boolean leftHanded) {
		this.skinParts = skinParts;
		this.leftHanded = leftHanded;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		skinParts = buf.readByte() & 0xFF;
		leftHanded = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(skinParts);
		buf.writeBoolean(leftHanded);
	}
}
