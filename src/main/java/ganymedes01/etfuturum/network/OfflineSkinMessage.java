package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** The signed skin data an offline server found for the receiving player's name (OfflineSkins). */
public class OfflineSkinMessage implements IMessage {

	public String name;
	public String value;
	public String signature;

	public OfflineSkinMessage() {
	}

	public OfflineSkinMessage(String name, String value, String signature) {
		this.name = name;
		this.value = value;
		this.signature = signature;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		name = ByteBufUtils.readUTF8String(buf);
		value = ByteBufUtils.readUTF8String(buf);
		signature = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, name);
		ByteBufUtils.writeUTF8String(buf, value);
		ByteBufUtils.writeUTF8String(buf, signature);
	}
}
