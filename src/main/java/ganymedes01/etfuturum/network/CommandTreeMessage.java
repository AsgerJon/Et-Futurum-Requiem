package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** The commands the receiving player may use, in CommandTree's network form (spec §40.1). */
public class CommandTreeMessage implements IMessage {

	public byte[] tree;

	public CommandTreeMessage() {
	}

	public CommandTreeMessage(byte[] tree) {
		this.tree = tree;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		tree = new byte[buf.readableBytes()];
		buf.readBytes(tree);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBytes(tree);
	}
}
