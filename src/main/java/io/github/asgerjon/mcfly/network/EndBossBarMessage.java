package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * The dragon fight's boss bar (SPEC-modern-movement.md §76.6): shown or not, and the dragon's health, to each player
 * within 192 of the End's middle, as 26.3's fight shows its bar whether or not the dragon is in sight.
 */
public class EndBossBarMessage implements IMessage {

	public boolean visible;
	public float progress;

	public EndBossBarMessage() {
	}

	public EndBossBarMessage(boolean visible, float progress) {
		this.visible = visible;
		this.progress = progress;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		visible = buf.readBoolean();
		progress = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(visible);
		buf.writeFloat(progress);
	}
}
