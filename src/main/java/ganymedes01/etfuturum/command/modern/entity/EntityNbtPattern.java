package ganymedes01.etfuturum.command.modern.entity;

import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemData;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.command.modern.block.LegacyBlockEntityData;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.item.TextComponent;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;

import java.util.Map;

/**
 * The `nbt` selector option (spec §52): modern's data pattern in 1.7.10's form, and the test of an entity against it.
 * Only the keys given are compared (modern's partial match); items, effects, names and UUIDs are translated as far as
 * the pattern gives them.
 */
public final class EntityNbtPattern {

	private final NBTTagCompound pattern;

	private EntityNbtPattern(NBTTagCompound pattern) {
		this.pattern = pattern;
	}

	public static EntityNbtPattern of(Map<?, ?> data) throws DecodeException {
		NBTTagCompound pattern = new NBTTagCompound();
		for (Map.Entry<?, ?> entry : data.entrySet()) {
			String key = String.valueOf(entry.getKey());
			Object value = entry.getValue();
			switch (key) {
				case "CustomName" -> pattern.setString("CustomName", TextComponent.decode(value).toLegacy('f', false));
				case "Inventory", "Items" -> pattern.setTag(key, items(value));
				case "SelectedItem" -> pattern.setTag(key, item(value));
				case "equipment" -> {
					NBTTagList equipment = new NBTTagList();
					for (Object piece : LegacyEntityData.map(value).values()) {
						equipment.appendTag(item(piece));
					}
					pattern.setTag("Equipment", equipment);
				}
				case "active_effects" -> pattern.setTag("ActiveEffects", effects(value));
				case "UUID" -> {
					if (!(value instanceof int[] ints) || ints.length != 4) {
						throw new DecodeException("Not a UUID: " + Snbt.toText(value));
					}
					pattern.setLong("UUIDMost", (long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL);
					pattern.setLong("UUIDLeast", (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
				}
				default -> pattern.setTag(key, Snbt.toNbt(value));
			}
		}
		return new EntityNbtPattern(pattern);
	}

	private static NBTTagList items(Object value) throws DecodeException {
		NBTTagList items = new NBTTagList();
		for (Object element : LegacyEntityData.list(value)) {
			items.appendTag(item(element));
		}
		return items;
	}

	/** {id, count, Slot} as 1.7.10 saves them: the numeric id, with the variant's damage; Count, Slot. */
	private static NBTTagCompound item(Object value) throws DecodeException {
		Map<?, ?> map = LegacyEntityData.map(value);
		NBTTagCompound item = new NBTTagCompound();
		if (map.get("id") instanceof String id) {
			ItemIds.Entry entry = ItemIds.byId(ItemData.withNamespace(id));
			if (entry == null) {
				throw new DecodeException("Unknown item '" + ItemData.withNamespace(id) + "'");
			}
			item.setShort("id", (short) Item.getIdFromItem(entry.item()));
			if (entry.item().getHasSubtypes()) {
				item.setShort("Damage", (short) entry.meta());
			}
		}
		if (map.containsKey("count")) {
			item.setByte("Count", (byte) LegacyEntityData.number(map.get("count"), 1));
		}
		if (map.containsKey("Slot")) {
			item.setByte("Slot", (byte) LegacyEntityData.number(map.get("Slot"), 0));
		}
		return item;
	}

	/** {id, amplifier, duration, ambient} as 1.7.10's, only what is given. */
	private static NBTTagList effects(Object value) throws DecodeException {
		NBTTagList effects = new NBTTagList();
		for (Object element : LegacyEntityData.list(value)) {
			Map<?, ?> map = LegacyEntityData.map(element);
			NBTTagCompound effect = new NBTTagCompound();
			if (map.get("id") instanceof String id) {
				Potion potion = EffectIds.byId(ItemData.withNamespace(id));
				if (potion == null) {
					throw new DecodeException("Unknown effect '" + ItemData.withNamespace(id) + "'");
				}
				effect.setByte("Id", (byte) potion.id);
			}
			if (map.containsKey("amplifier")) {
				effect.setByte("Amplifier", (byte) LegacyEntityData.number(map.get("amplifier"), 0));
			}
			if (map.containsKey("duration")) {
				effect.setInteger("Duration", (int) LegacyEntityData.number(map.get("duration"), 0));
			}
			effects.appendTag(effect);
		}
		return effects;
	}

	/** The entity's saved data, its tags and (for a player) the held item, hold everything in the pattern. */
	public boolean matches(Entity entity) {
		NBTTagCompound saved = new NBTTagCompound();
		entity.writeToNBT(saved);
		NBTTagList tags = new NBTTagList();
		for (String tag : EntityTags.get(entity)) {
			tags.appendTag(new NBTTagString(tag));
		}
		saved.setTag("Tags", tags);
		if (entity instanceof EntityPlayer player && player.getHeldItem() != null) {
			saved.setTag("SelectedItem", player.getHeldItem().writeToNBT(new NBTTagCompound()));
		}
		return LegacyBlockEntityData.contains(saved, pattern);
	}
}
