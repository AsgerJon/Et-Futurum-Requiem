package ganymedes01.etfuturum.command.modern.entity;

import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemData;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.item.TextComponent;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modern's saved entity data as 1.7.10's (spec §47.3): the keys whose form changed are translated, the rest goes
 * over as it is (most of 1.7.10's own keys are the same). `id` and `Passengers` are left to the caller.
 */
public final class LegacyEntityData {

	/** Modern's equipment slots in 1.7.10's Equipment order: hand, boots, leggings, chestplate, helmet. */
	private static final String[] SLOTS = {"mainhand", "feet", "legs", "chest", "head"};
	private static final float DEFAULT_DROP_CHANCE = 0.085F;
	private static final Map<String, String> ATTRIBUTES = new HashMap<>();

	static {
		ATTRIBUTES.put("max_health", "generic.maxHealth");
		ATTRIBUTES.put("follow_range", "generic.followRange");
		ATTRIBUTES.put("knockback_resistance", "generic.knockbackResistance");
		ATTRIBUTES.put("movement_speed", "generic.movementSpeed");
		ATTRIBUTES.put("attack_damage", "generic.attackDamage");
		ATTRIBUTES.put("attack_speed", "generic.attackSpeed");
		ATTRIBUTES.put("jump_strength", "horse.jumpStrength");
		ATTRIBUTES.put("spawn_reinforcements", "zombie.spawnReinforcements");
	}

	private LegacyEntityData() {
	}

	/** The data for an entity of this modern type; the message names the key that couldn't be used, and why. */
	public static NBTTagCompound convert(Map<String, Object> data, String type) throws DecodeException {
		NBTTagCompound tag = new NBTTagCompound();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			try {
				switch (key) {
					case "id", "Passengers" -> {
					}
					case "equipment" -> equipment(value, type, tag);
					case "drop_chances" -> dropChances(value, tag);
					case "CustomName" -> tag.setString("CustomName", TextComponent.decode(value).toLegacy('f', false));
					case "active_effects" -> effects(value, tag);
					case "attributes" -> attributes(value, tag);
					case "UUID" -> uuid(value, tag);
					default -> tag.setTag(key, Snbt.toNbt(value));
				}
			} catch (DecodeException | IllegalArgumentException e) {
				throw new DecodeException(key + ": " + e.getMessage());
			}
		}
		return tag;
	}

	public static Map<?, ?> map(Object value) throws DecodeException {
		if (!(value instanceof Map<?, ?> map)) {
			throw new DecodeException("Not a map: " + Snbt.toText(value));
		}
		return map;
	}

	public static List<?> list(Object value) throws DecodeException {
		if (!(value instanceof List<?> list)) {
			throw new DecodeException("Not a list: " + Snbt.toText(value));
		}
		return list;
	}

	public static double number(Object value, double fallback) throws DecodeException {
		if (value == null) {
			return fallback;
		}
		if (!(value instanceof Number number)) {
			throw new DecodeException("Not a number: " + Snbt.toText(value));
		}
		return number.doubleValue();
	}

	/** Hand and armour into Equipment; a pig's saddle into Saddle, a horse's into SaddleItem; off-hand and body have no place. */
	private static void equipment(Object value, String type, NBTTagCompound tag) throws DecodeException {
		Map<?, ?> slots = map(value);
		NBTTagList equipment = new NBTTagList();
		for (String slot : SLOTS) {
			NBTTagCompound item = new NBTTagCompound();
			if (slots.containsKey(slot)) {
				ItemData.toStack(slots.get(slot)).writeToNBT(item);
			}
			equipment.appendTag(item);
		}
		tag.setTag("Equipment", equipment);
		if (slots.containsKey("saddle")) {
			ItemStack saddle = ItemData.toStack(slots.get("saddle"));
			if (type.equals("minecraft:pig")) {
				tag.setBoolean("Saddle", saddle.getItem() == Items.saddle);
			} else {
				tag.setTag("SaddleItem", saddle.writeToNBT(new NBTTagCompound()));
			}
		}
	}

	private static void dropChances(Object value, NBTTagCompound tag) throws DecodeException {
		Map<?, ?> chances = map(value);
		NBTTagList list = new NBTTagList();
		for (String slot : SLOTS) {
			list.appendTag(new NBTTagFloat((float) number(chances.get(slot), DEFAULT_DROP_CHANCE)));
		}
		tag.setTag("DropChances", list);
	}

	/** {id, amplifier, duration, ambient}; a duration of -1 (endless) as the longest 1.7.10 has. */
	private static void effects(Object value, NBTTagCompound tag) throws DecodeException {
		tag.setTag("ActiveEffects", effectList(value));
	}

	/** Modern effect instances [{id, amplifier, duration, ambient}] in 1.7.10's form, as entities and potions keep them. */
	public static NBTTagList effectList(Object value) throws DecodeException {
		NBTTagList list = new NBTTagList();
		for (Object element : list(value)) {
			Map<?, ?> effect = map(element);
			if (!(effect.get("id") instanceof String id)) {
				throw new DecodeException("No effect id in " + Snbt.toText(element));
			}
			Potion potion = EffectIds.byId(ItemData.withNamespace(id));
			if (potion == null) {
				throw new DecodeException("Unknown effect '" + ItemData.withNamespace(id) + "'");
			}
			int duration = (int) number(effect.get("duration"), 0);
			NBTTagCompound legacy = new NBTTagCompound();
			// by name, as 1.7.10's effects are saved now (spec §70)
			legacy.setString("id", EffectIds.idOf(potion));
			legacy.setByte("Amplifier", (byte) number(effect.get("amplifier"), 0));
			legacy.setInteger("Duration", duration == -1 ? Integer.MAX_VALUE : duration);
			legacy.setBoolean("Ambient", number(effect.get("ambient"), 0) != 0);
			list.appendTag(legacy);
		}
		return list;
	}

	/** {id, base, modifiers}: modern's names to 1.7.10's; a modifier's id becomes a UUID made from it. */
	private static void attributes(Object value, NBTTagCompound tag) throws DecodeException {
		NBTTagList list = new NBTTagList();
		for (Object element : list(value)) {
			Map<?, ?> attribute = map(element);
			if (!(attribute.get("id") instanceof String id)) {
				throw new DecodeException("No attribute id in " + Snbt.toText(element));
			}
			String name = attributeName(id);
			NBTTagCompound legacy = new NBTTagCompound();
			legacy.setString("Name", name);
			legacy.setDouble("Base", number(attribute.get("base"), 0));
			NBTTagList modifiers = new NBTTagList();
			if (attribute.containsKey("modifiers")) {
				for (Object modifierValue : list(attribute.get("modifiers"))) {
					Map<?, ?> modifier = map(modifierValue);
					String modifierId = String.valueOf(modifier.get("id"));
					UUID uuid = UUID.nameUUIDFromBytes(modifierId.getBytes(StandardCharsets.UTF_8));
					NBTTagCompound legacyModifier = new NBTTagCompound();
					legacyModifier.setString("Name", modifierId);
					legacyModifier.setDouble("Amount", number(modifier.get("amount"), 0));
					legacyModifier.setInteger("Operation", operation(modifier.get("operation")));
					legacyModifier.setLong("UUIDMost", uuid.getMostSignificantBits());
					legacyModifier.setLong("UUIDLeast", uuid.getLeastSignificantBits());
					modifiers.appendTag(legacyModifier);
				}
			}
			legacy.setTag("Modifiers", modifiers);
			list.appendTag(legacy);
		}
		tag.setTag("Attributes", list);
	}

	/** 1.7.10's name for a modern attribute (`max_health` is `generic.maxHealth`). */
	public static String attributeName(String id) throws DecodeException {
		String path = ItemData.withNamespace(id).substring(ItemData.withNamespace(id).indexOf(':') + 1);
		for (String prefix : new String[]{"generic.", "horse.", "zombie.", "player."}) {
			if (path.startsWith(prefix)) {
				path = path.substring(prefix.length());
			}
		}
		String name = ATTRIBUTES.get(path);
		if (name == null) {
			throw new DecodeException("Unknown attribute '" + ItemData.withNamespace(id) + "'");
		}
		return name;
	}

	public static int operation(Object value) throws DecodeException {
		if (value instanceof String name) {
			switch (name) {
				case "add_value":
					return 0;
				case "add_multiplied_base":
					return 1;
				case "add_multiplied_total":
					return 2;
				default:
					throw new DecodeException("Unknown operation '" + name + "'");
			}
		}
		return (int) number(value, 0);
	}

	/** Four ints, most significant first, as 1.7.10's two longs. */
	private static void uuid(Object value, NBTTagCompound tag) throws DecodeException {
		if (!(value instanceof int[] ints) || ints.length != 4) {
			throw new DecodeException("Not a UUID: " + Snbt.toText(value));
		}
		tag.setLong("UUIDMost", (long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL);
		tag.setLong("UUIDLeast", (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
	}
}
