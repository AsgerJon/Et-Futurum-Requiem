package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.item.ItemComponents;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** An item in modern's saved form, `{id, count, components}` (spec §47.3), made as /give makes it (§45). */
public final class ItemData {

	private ItemData() {
	}

	/** A namespace for an id written without one. */
	public static String withNamespace(String id) {
		return id.indexOf(':') < 0 ? "minecraft:" + id : id;
	}

	public static ItemStack toStack(Object value) throws DecodeException {
		if (!(value instanceof Map<?, ?> map)) {
			throw new DecodeException("Not an item: " + Snbt.toText(value));
		}
		if (!(map.get("id") instanceof String rawId)) {
			throw new DecodeException("No item id in " + Snbt.toText(value));
		}
		String id = withNamespace(rawId);
		ItemIds.Entry item = ItemIds.byId(id);
		if (item == null) {
			throw new DecodeException("Unknown item '" + id + "'");
		}
		int count = 1;
		if (map.containsKey("count")) {
			if (!(map.get("count") instanceof Number number)) {
				throw new DecodeException("Not a number: " + Snbt.toText(map.get("count")));
			}
			count = number.intValue();
		}
		List<ItemComponents.Decoded> components = new ArrayList<>();
		if (map.get("components") instanceof Map<?, ?> given) {
			for (Map.Entry<?, ?> entry : given.entrySet()) {
				String key = (String) entry.getKey();
				if (key.startsWith("!")) {
					// a removed default: 1.7.10 items have none
					continue;
				}
				String component = withNamespace(key);
				if (!ItemComponents.exists(component)) {
					throw new DecodeException("Unknown item component '" + component + "'");
				}
				if (!ItemComponents.isPorted(component)) {
					throw new DecodeException("Item component '" + component + "' is not supported yet");
				}
				try {
					components.add(ItemComponents.decode(component, entry.getValue()));
				} catch (DecodeException e) {
					throw new DecodeException("Malformed '" + component + "' component: " + e.getMessage());
				}
			}
		}
		try {
			return new ItemInput(item, id, components).createItemStack(count);
		} catch (CommandSyntaxException e) {
			throw new DecodeException(e.getRawMessage().getString());
		}
	}
}
