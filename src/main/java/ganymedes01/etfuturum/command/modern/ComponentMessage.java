package ganymedes01.etfuturum.command.modern;

import com.mojang.brigadier.Message;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

/** A Brigadier message holding a chat component, so command errors and tooltips are translated where they are shown. */
public final class ComponentMessage implements Message {

	private final IChatComponent component;

	public ComponentMessage(IChatComponent component) {
		this.component = component;
	}

	public static ComponentMessage translatable(String key, Object... args) {
		return new ComponentMessage(new ChatComponentTranslation(key, args));
	}

	/** Any Brigadier message as a component: ours as they are, others as plain text. */
	public static IChatComponent toComponent(Message message) {
		return message instanceof ComponentMessage ours ? ours.component.createCopy() : new ChatComponentText(message.getString());
	}

	@Override
	public String getString() {
		return component.getUnformattedText();
	}
}
