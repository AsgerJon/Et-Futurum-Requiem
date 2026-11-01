package ganymedes01.etfuturum.command.modern.block;

import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemData;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.item.TextComponent;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Modern's block entity data as 1.7.10's (spec §49.3): items in containers, custom names, sign text and a spawner's
 * mob are translated; the rest goes over as it is.
 */
public final class LegacyBlockEntityData {

	private static final Random RANDOM = new Random();

	private LegacyBlockEntityData() {
	}

	/** The data in 1.7.10's form; the message names the key that couldn't be used, and why. */
	public static NBTTagCompound convert(Map<String, Object> data) throws DecodeException {
		NBTTagCompound tag = new NBTTagCompound();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			try {
				switch (key) {
					case "Items" -> tag.setTag("Items", items(value));
					case "CustomName" -> tag.setString("CustomName", TextComponent.decode(value).toLegacy('f', false));
					case "front_text" -> signText(value, tag);
					case "SpawnData" -> spawnData(value, tag);
					default -> tag.setTag(key, Snbt.toNbt(value));
				}
			} catch (DecodeException e) {
				throw new DecodeException(key + ": " + e.getMessage());
			}
		}
		return tag;
	}

	/** Modern items with their Slot, as 1.7.10's saved stacks. */
	private static NBTTagList items(Object value) throws DecodeException {
		if (!(value instanceof List<?> list)) {
			throw new DecodeException("Not a list: " + Snbt.toText(value));
		}
		NBTTagList items = new NBTTagList();
		for (Object element : list) {
			ItemStack stack = ItemData.toStack(element);
			NBTTagCompound saved = stack.writeToNBT(new NBTTagCompound());
			if (element instanceof Map<?, ?> map && map.get("Slot") instanceof Number slot) {
				saved.setByte("Slot", slot.byteValue());
			}
			items.appendTag(saved);
		}
		return items;
	}

	/** The front's four lines: 1.7.10's signs have one side, of plain lines. */
	private static void signText(Object value, NBTTagCompound tag) throws DecodeException {
		if (!(value instanceof Map<?, ?> front) || !(front.get("messages") instanceof List<?> messages)) {
			throw new DecodeException("Expected {messages:[...]}");
		}
		for (int line = 0; line < 4; line++) {
			String text = line < messages.size() ? TextComponent.decode(messages.get(line)).toLegacy('0', false) : "";
			tag.setString("Text" + (line + 1), text);
		}
	}

	/** The mob a spawner makes, by 1.7.10's name. */
	private static void spawnData(Object value, NBTTagCompound tag) throws DecodeException {
		if (!(value instanceof Map<?, ?> spawn) || !(spawn.get("entity") instanceof Map<?, ?> entity) || !(entity.get("id") instanceof String id)) {
			throw new DecodeException("Expected {entity:{id:...}}");
		}
		EntityTypeIds.Spawn found = EntityTypeIds.spawnFor(ItemData.withNamespace(id), RANDOM);
		if (found == null || found.name == null) {
			throw new DecodeException("Unknown entity type '" + id + "'");
		}
		tag.setString("EntityId", found.name);
	}

	/**
	 * Whether the actual data holds everything in the expected (modern's rule for block and entity data in tests): a
	 * compound's keys each hold, a list's elements are each found in the actual list, other values are equal.
	 */
	public static boolean contains(NBTBase actual, NBTBase expected) {
		if (expected instanceof NBTTagCompound wanted) {
			if (!(actual instanceof NBTTagCompound present)) {
				return false;
			}
			for (Object key : wanted.func_150296_c()) {
				String name = (String) key;
				if (!present.hasKey(name) || !contains(present.getTag(name), wanted.getTag(name))) {
					return false;
				}
			}
			return true;
		}
		if (expected instanceof NBTTagList wanted) {
			if (!(actual instanceof NBTTagList present)) {
				return false;
			}
			for (int i = 0; i < wanted.tagCount(); i++) {
				boolean found = false;
				NBTBase element = elementAt(wanted, i);
				for (int j = 0; j < present.tagCount() && !found; j++) {
					found = contains(elementAt(present, j), element);
				}
				if (!found) {
					return false;
				}
			}
			return true;
		}
		return expected.equals(actual);
	}

	/** 1.7.10's NBTTagList has no general getter; a copy hands the element out. */
	private static NBTBase elementAt(NBTTagList list, int index) {
		return ((NBTTagList) list.copy()).removeTag(index);
	}
}
