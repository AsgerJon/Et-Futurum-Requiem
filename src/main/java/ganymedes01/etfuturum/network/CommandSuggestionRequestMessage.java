package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** The client asks for the server's suggestions: a request number and the input up to the cursor (spec §40.1). */
public class CommandSuggestionRequestMessage implements IMessage {

	/** Modern reads at most this much of the input. */
	private static final int MAX_LENGTH = 32500;

	public int id;
	public String text;

	public CommandSuggestionRequestMessage() {
	}

	public CommandSuggestionRequestMessage(int id, String text) {
		this.id = id;
		this.text = text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		id = ByteBufUtils.readVarInt(buf, 5);
		text = ByteBufUtils.readUTF8String(buf);
		if (text.length() > MAX_LENGTH) {
			text = text.substring(0, MAX_LENGTH);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, id, 5);
		ByteBufUtils.writeUTF8String(buf, text);
	}
}
