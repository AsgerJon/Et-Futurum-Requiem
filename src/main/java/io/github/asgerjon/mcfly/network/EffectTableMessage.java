package io.github.asgerjon.mcfly.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The server's effects by name and the number each has there (spec §70.3, stage C), sent when a player joins, before
 * any effect packet: as 26.3 sends its registries' numbers, so a client whose mods registered effects in another
 * order still reads the server's numbers right.
 */
public class EffectTableMessage implements IMessage {

	public Map<String, Integer> table = new LinkedHashMap<>();

	public EffectTableMessage() {
	}

	public EffectTableMessage(Map<String, Integer> table) {
		this.table = table;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int count = buf.readInt();
		table = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			String name = ByteBufUtils.readUTF8String(buf);
			table.put(name, buf.readInt());
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(table.size());
		for (Map.Entry<String, Integer> entry : table.entrySet()) {
			ByteBufUtils.writeUTF8String(buf, entry.getKey());
			buf.writeInt(entry.getValue());
		}
	}
}
