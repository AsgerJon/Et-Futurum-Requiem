package ganymedes01.etfuturum.command.modern.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.arguments.EnchantmentIds;
import ganymedes01.etfuturum.command.modern.arguments.ResourceIds;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import ganymedes01.etfuturum.command.modern.nbt.SnbtReader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Modern's item components (spec §45.3): the ones ported, read from their SNBT values as modern's codecs read them,
 * and the names of the rest.
 */
public final class ItemComponents {

	/** What a component does to the stack once it is made. */
	@FunctionalInterface
	public interface Applier {
		void apply(ItemStack stack);
	}

	@FunctionalInterface
	private interface Decoder {
		Applier decode(Object value) throws DecodeException;
	}

	/** A decoded component; lower steps go first (the name and lore last: their colour depends on the rarity). */
	public static final class Decoded {
		public final int step;
		public final Applier applier;

		private Decoded(int step, Applier applier) {
			this.step = step;
			this.applier = applier;
		}
	}

	private static final class Ported {
		private final int step;
		private final Decoder decoder;

		private Ported(int step, Decoder decoder) {
			this.step = step;
			this.decoder = decoder;
		}
	}

	private static final Map<String, Ported> PORTED = new TreeMap<>();
	/** Every component name 26.3 has. */
	private static final Set<String> MODERN = new HashSet<>(Arrays.asList(("custom_data max_stack_size max_damage damage unbreakable use_effects "
		+ "custom_name minimum_attack_charge damage_type item_name item_model lore rarity enchantments can_place_on can_break attribute_modifiers "
		+ "custom_model_data tooltip_display repair_cost creative_slot_lock enchantment_glint_override intangible_projectile food consumable "
		+ "use_remainder use_cooldown damage_resistant tool weapon attack_range enchantable equippable repairable glider tooltip_style "
		+ "death_protection blocks_attacks piercing_weapon kinetic_weapon attack_animation interact_animation additional_trade_cost "
		+ "block_transformer villager_food stored_enchantments dye dyed_color map_id map_decorations map_post_processing charged_projectiles "
		+ "bundle_contents potion_contents potion_duration_scale suspicious_stew_effects writable_book_content written_book_content trim "
		+ "debug_stick_state entity_data bucket_entity_data block_entity_data instrument provides_trim_material ominous_bottle_amplifier "
		+ "jukebox_playable provides_banner_patterns recipes lodestone_tracker firework_explosion fireworks profile note_block_sound "
		+ "banner_patterns base_color pot_decorations container block_state bees sulfur_cube_content lock container_loot break_sound "
		+ "compostable cooking_fuel brewing_fuel mob_visibility villager/variant wolf/variant wolf/sound_variant wolf/collar fox/variant "
		+ "salmon/size parrot/variant tropical_fish/pattern tropical_fish/base_color tropical_fish/pattern_color mooshroom/variant "
		+ "rabbit/variant pig/variant pig/sound_variant cow/variant cow/sound_variant chicken/variant chicken/sound_variant "
		+ "zombie_nautilus/variant frog/variant horse/variant painting/variant llama/variant axolotl/variant cat/variant cat/sound_variant "
		+ "cat/collar sheep/color shulker/color provides_pottery_pattern sign_text_front sign_text_back waxed cushion/color").split(" ")));

	static {
		PORTED.put("minecraft:custom_data", new Ported(0, value -> {
			NBTTagCompound data = customData(value);
			return stack -> {
				NBTTagCompound tag = tag(stack);
				for (Object key : data.func_150296_c()) {
					tag.setTag((String) key, data.getTag((String) key).copy());
				}
			};
		}));
		PORTED.put("minecraft:damage", new Ported(1, value -> {
			int damage = nonNegativeInt(value);
			// on an item that doesn't wear, 1.7.10's damage is the variant, which the id already chose (spec §48.2)
			return stack -> {
				if (stack.isItemStackDamageable()) {
					stack.setItemDamage(damage);
				}
			};
		}));
		PORTED.put("minecraft:enchantments", new Ported(2, value -> enchantments(value, "ench")));
		// step 2's (spec §50)
		PORTED.put("minecraft:potion_contents", new Ported(3, ItemContents::potionContents));
		PORTED.put("minecraft:fireworks", new Ported(3, ItemContents::fireworks));
		PORTED.put("minecraft:firework_explosion", new Ported(3, ItemContents::fireworkExplosion));
		PORTED.put("minecraft:writable_book_content", new Ported(3, ItemContents::writableBook));
		PORTED.put("minecraft:written_book_content", new Ported(3, ItemContents::writtenBook));
		PORTED.put("minecraft:attribute_modifiers", new Ported(3, ItemContents::attributeModifiers));
		PORTED.put("minecraft:profile", new Ported(3, ItemContents::profile));
		PORTED.put("minecraft:banner_patterns", new Ported(3, ItemContents::bannerPatterns));
		PORTED.put("minecraft:stored_enchantments", new Ported(2, value -> enchantments(value, "StoredEnchantments")));
		PORTED.put("minecraft:unbreakable", new Ported(3, value -> {
			if (!(value instanceof Map)) {
				throw new DecodeException("Not a map: " + Snbt.toText(value));
			}
			return stack -> tag(stack).setBoolean("Unbreakable", true);
		}));
		PORTED.put("minecraft:repair_cost", new Ported(3, value -> {
			int cost = nonNegativeInt(value);
			return stack -> tag(stack).setInteger("RepairCost", cost);
		}));
		PORTED.put("minecraft:dyed_color", new Ported(3, value -> {
			int rgb = rgb(value);
			return stack -> display(stack).setInteger("color", rgb);
		}));
		PORTED.put("minecraft:custom_name", new Ported(4, value -> {
			TextComponent name = TextComponent.decode(value);
			return stack -> display(stack).setString("Name", name.toLegacy(rarityCode(stack), true));
		}));
		PORTED.put("minecraft:lore", new Ported(4, value -> {
			if (!(value instanceof List<?> list)) {
				throw new DecodeException("Not a list: " + Snbt.toText(value));
			}
			if (list.size() > 256) {
				throw new DecodeException("List is too long: " + list.size() + ", expected range [0-256]");
			}
			List<TextComponent> lines = new ArrayList<>();
			for (Object line : list) {
				lines.add(TextComponent.decode(line));
			}
			return stack -> {
				NBTTagList lore = new NBTTagList();
				for (TextComponent line : lines) {
					lore.appendTag(new NBTTagString(line.toLegacy('5', true)));
				}
				display(stack).setTag("Lore", lore);
			};
		}));
	}

	private ItemComponents() {
	}

	/** A modern component name (with its namespace); ported or not. */
	public static boolean exists(String id) {
		return id.startsWith("minecraft:") && MODERN.contains(id.substring("minecraft:".length()));
	}

	public static boolean isPorted(String id) {
		return PORTED.containsKey(id);
	}

	/** The ported ones, sorted; the only ones suggested, since the rest would only give an error. */
	public static Collection<String> portedIds() {
		return Collections.unmodifiableSet(PORTED.keySet());
	}

	public static Decoded decode(String id, Object value) throws DecodeException {
		Ported ported = PORTED.get(id);
		return new Decoded(ported.step, ported.decoder.decode(value));
	}

	private static NBTTagCompound tag(ItemStack stack) {
		if (stack.stackTagCompound == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.stackTagCompound;
	}

	private static NBTTagCompound display(ItemStack stack) {
		NBTTagCompound tag = tag(stack);
		if (!tag.hasKey("display", 10)) {
			tag.setTag("display", new NBTTagCompound());
		}
		return tag.getCompoundTag("display");
	}

	/** The rarity colour the name is drawn in, so text in the default style needs no codes. */
	private static char rarityCode(ItemStack stack) {
		return stack.getRarity().rarityColor.toString().charAt(1);
	}

	/** Any number, as modern's whole-number codecs take any numeric tag; at least 0. */
	private static int nonNegativeInt(Object value) throws DecodeException {
		if (!(value instanceof Number number)) {
			throw new DecodeException("Not a number: " + Snbt.toText(value));
		}
		int n = number.intValue();
		if (n < 0) {
			throw new DecodeException("Value must be non-negative: " + n);
		}
		return n;
	}

	/** `{id: level, ...}`; levels 1 to 255; written as 1.7.10's list of {id, lvl}, in the order given. */
	private static Applier enchantments(Object value, String key) throws DecodeException {
		if (!(value instanceof Map<?, ?> map)) {
			throw new DecodeException("Not a map: " + Snbt.toText(value));
		}
		Map<Enchantment, Integer> levels = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String id = enchantmentId((String) entry.getKey());
			Enchantment enchantment = id == null ? null : EnchantmentIds.byId(id);
			if (enchantment == null) {
				throw new DecodeException("Unknown registry key in ResourceKey[minecraft:root / minecraft:enchantment]: " + (id != null ? id : entry.getKey()));
			}
			if (!(entry.getValue() instanceof Number number)) {
				throw new DecodeException("Not a number: " + Snbt.toText(entry.getValue()));
			}
			int level = number.intValue();
			if (level < 1 || level > 255) {
				throw new DecodeException("Value " + level + " outside of range [1:255]");
			}
			levels.put(enchantment, level);
		}
		return stack -> {
			if (levels.isEmpty()) {
				// 1.7.10 counts an item as enchanted, with the glint, once the list exists
				return;
			}
			NBTTagList list = new NBTTagList();
			for (Map.Entry<Enchantment, Integer> entry : levels.entrySet()) {
				NBTTagCompound enchantment = new NBTTagCompound();
				enchantment.setShort("id", (short) entry.getKey().effectId);
				enchantment.setShort("lvl", entry.getValue().shortValue());
				list.appendTag(enchantment);
			}
			tag(stack).setTag(key, list);
		};
	}

	/** A key as a resource id; null when it isn't one. */
	private static String enchantmentId(String key) {
		StringReader reader = new StringReader(key);
		try {
			String id = ResourceIds.read(reader);
			return reader.canRead() ? null : id;
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	/** An int, or three floats from 0 to 1. */
	private static int rgb(Object value) throws DecodeException {
		if (value instanceof Number number) {
			return number.intValue() & 0xFFFFFF;
		}
		if (value instanceof List<?> list) {
			if (list.size() != 3) {
				throw new DecodeException("Input is not a list of 3 elements");
			}
			int rgb = 0;
			for (Object channel : list) {
				if (!(channel instanceof Number number)) {
					throw new DecodeException("Not a number: " + Snbt.toText(channel));
				}
				rgb = rgb << 8 | (int) Math.floor(number.floatValue() * 255.0F) & 255;
			}
			return rgb;
		}
		throw new DecodeException("Not a number: " + Snbt.toText(value));
	}

	/** A compound, or a string holding one in SNBT. */
	private static NBTTagCompound customData(Object value) throws DecodeException {
		if (value instanceof String text) {
			StringReader reader = new StringReader(text);
			try {
				value = SnbtReader.read(reader);
				reader.skipWhitespace();
				if (reader.canRead()) {
					throw new DecodeException("Unexpected trailing data at position " + reader.getCursor() + ": " + text);
				}
			} catch (CommandSyntaxException e) {
				throw new DecodeException(e.getMessage());
			}
			if (!(value instanceof Map)) {
				throw new DecodeException("Expected compound tag, got " + Snbt.toText(value));
			}
		}
		if (!(value instanceof Map)) {
			throw new DecodeException("Not a map: " + Snbt.toText(value));
		}
		try {
			NBTBase tag = Snbt.toNbt(value);
			return (NBTTagCompound) tag;
		} catch (IllegalArgumentException e) {
			throw new DecodeException(e.getMessage());
		}
	}
}
