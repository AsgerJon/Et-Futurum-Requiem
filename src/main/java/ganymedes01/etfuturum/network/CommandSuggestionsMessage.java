package ganymedes01.etfuturum.network;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * The server's answer to a suggestion request: the request number, the range of the input the entries replace, and
 * each entry's text with its tooltip as a chat component, if any (spec §40.1).
 */
public class CommandSuggestionsMessage implements IMessage {

	public int id;
	public int start;
	public int length;
	public List<String> texts = new ArrayList<>();
	/** JSON of each entry's tooltip, or null. */
	public List<String> tooltips = new ArrayList<>();

	public CommandSuggestionsMessage() {
	}

	public CommandSuggestionsMessage(int id, Suggestions suggestions) {
		this.id = id;
		start = suggestions.getRange().getStart();
		length = suggestions.getRange().getLength();
		for (Suggestion suggestion : suggestions.getList()) {
			texts.add(suggestion.getText());
			tooltips.add(suggestion.getTooltip() == null ? null
				: IChatComponent.Serializer.func_150696_a(ComponentMessage.toComponent(suggestion.getTooltip())));
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		id = ByteBufUtils.readVarInt(buf, 5);
		start = ByteBufUtils.readVarInt(buf, 5);
		length = ByteBufUtils.readVarInt(buf, 5);
		int count = ByteBufUtils.readVarInt(buf, 5);
		for (int i = 0; i < count; i++) {
			texts.add(ByteBufUtils.readUTF8String(buf));
			tooltips.add(buf.readBoolean() ? ByteBufUtils.readUTF8String(buf) : null);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, id, 5);
		ByteBufUtils.writeVarInt(buf, start, 5);
		ByteBufUtils.writeVarInt(buf, length, 5);
		ByteBufUtils.writeVarInt(buf, texts.size(), 5);
		for (int i = 0; i < texts.size(); i++) {
			ByteBufUtils.writeUTF8String(buf, texts.get(i));
			buf.writeBoolean(tooltips.get(i) != null);
			if (tooltips.get(i) != null) {
				ByteBufUtils.writeUTF8String(buf, tooltips.get(i));
			}
		}
	}
}
