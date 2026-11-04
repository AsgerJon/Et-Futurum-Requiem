package ganymedes01.etfuturum.command.modern.item;

import io.github.asgerjon.mcfly.banner.BannerPatterns;
import io.github.asgerjon.mcfly.banner.BannerPattern;
import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemData;
import ganymedes01.etfuturum.command.modern.entity.LegacyEntityData;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.recipes.ModRecipes;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.PotionEffect;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The item components of step 2 (spec §50): what they hold, read from modern's SNBT, put where 1.7.10 and EFR keep it.
 */
final class ItemContents {

	private static final List<String> SHAPES = Arrays.asList("small_ball", "large_ball", "star", "creeper", "burst");

	private ItemContents() {
	}

	private static NBTTagCompound tag(ItemStack stack) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.getTagCompound();
	}

	private static String string(Object value, String what) throws DecodeException {
		if (!(value instanceof String text)) {
			throw new DecodeException("Expected " + what + ", got " + Snbt.toText(value));
		}
		return text;
	}

	/** A string, or {raw, filtered}: the raw one (1.7.10 has no chat filter). */
	private static Object filterable(Object value) throws DecodeException {
		if (value instanceof Map<?, ?> map && map.containsKey("raw")) {
			return map.get("raw");
		}
		return value;
	}

	// ---------------------------------------------------------------- potion_contents

	/**
	 * A potion id, or {potion, custom_effects, custom_color, custom_name}, by EFR's registry of potions by name (spec
	 * §69): on a potion or splash potion as `PotionContents` keeps it (1.7.10's damage value for a potion 1.7.10 has,
	 * else its name in NBT, the effects also in CustomPotionEffects). EFR's lingering potions and tipped arrows get
	 * 1.7.10's damage value and the rest as custom effects, until they take names too (§69's stage C).
	 */
	static ItemComponents.Applier potionContents(Object value) throws DecodeException {
		Map<?, ?> map = value instanceof Map<?, ?> fields ? fields : null;
		Object potionValue = map != null ? map.get("potion") : value;
		NamedPotion potion = null;
		if (potionValue != null) {
			String id = ItemData.withNamespace(string(potionValue, "a potion id"));
			potion = NamedPotions.byId(id);
			if (potion == null) {
				throw new DecodeException("Unknown potion '" + id + "'");
			}
			for (NamedPotion.Effect effect : potion.effects()) {
				if (EffectIds.byId(effect.effect()) == null) {
					throw new DecodeException("Potion '" + id + "' needs the effect '" + effect.effect() + "', which 1.7.10 doesn't have");
				}
			}
		}
		List<PotionEffect> custom = new ArrayList<>();
		if (map != null && map.containsKey("custom_effects")) {
			NBTTagList list = LegacyEntityData.effectList(map.get("custom_effects"));
			for (int i = 0; i < list.tagCount(); i++) {
				PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
				if (effect != null) {
					custom.add(effect);
				}
			}
		}
		Integer color = map != null && map.get("custom_color") != null ? (int) LegacyEntityData.number(map.get("custom_color"), 0) : null;
		String name = map != null && map.get("custom_name") != null ? string(map.get("custom_name"), "a potion name") : null;
		NamedPotion named = potion;
		return stack -> {
			if (stack.getItem() == Items.potionitem) {
				PotionContents.write(stack, PotionContents.isSplash(stack), named, custom, color, name);
				return;
			}
			int legacy = named == null ? -1 : named.legacyDamage();
			if (stack.getItem() instanceof ItemPotion || stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem) {
				stack.setItemDamage(legacy >= 0 ? legacy : named != null || !custom.isEmpty() ? PotionContents.DRINKABLE : 0);
			}
			List<PotionEffect> effects = new ArrayList<>();
			if (named != null && legacy < 0) {
				effects.addAll(named.potionEffects());
			}
			effects.addAll(custom);
			if (!effects.isEmpty()) {
				NBTTagList list = new NBTTagList();
				for (PotionEffect effect : effects) {
					list.appendTag(effect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
				}
				tag(stack).setTag("CustomPotionEffects", list);
			}
		};
	}

	// ---------------------------------------------------------------- fireworks

	/** {flight_duration, explosions}: a rocket's Fireworks {Flight, Explosions}. */
	static ItemComponents.Applier fireworks(Object value) throws DecodeException {
		Map<?, ?> map = LegacyEntityData.map(value);
		NBTTagCompound fireworks = new NBTTagCompound();
		int flight = (int) LegacyEntityData.number(map.get("flight_duration"), 0);
		if (flight < 0 || flight > 255) {
			throw new DecodeException("Value " + flight + " outside of range [0:255]");
		}
		fireworks.setByte("Flight", (byte) flight);
		NBTTagList explosions = new NBTTagList();
		if (map.containsKey("explosions")) {
			for (Object explosion : LegacyEntityData.list(map.get("explosions"))) {
				explosions.appendTag(explosion(explosion));
			}
		}
		fireworks.setTag("Explosions", explosions);
		return stack -> tag(stack).setTag("Fireworks", fireworks.copy());
	}

	/** One explosion: a star's Explosion. */
	static ItemComponents.Applier fireworkExplosion(Object value) throws DecodeException {
		NBTTagCompound explosion = explosion(value);
		return stack -> tag(stack).setTag("Explosion", explosion.copy());
	}

	/** {shape, colors, fade_colors, has_trail, has_twinkle} as {Type, Colors, FadeColors, Trail, Flicker}. */
	private static NBTTagCompound explosion(Object value) throws DecodeException {
		Map<?, ?> map = LegacyEntityData.map(value);
		int shape = SHAPES.indexOf(string(map.get("shape"), "a shape"));
		if (shape < 0) {
			throw new DecodeException("Unknown shape '" + map.get("shape") + "'");
		}
		NBTTagCompound explosion = new NBTTagCompound();
		explosion.setByte("Type", (byte) shape);
		explosion.setIntArray("Colors", colors(map.get("colors")));
		explosion.setIntArray("FadeColors", colors(map.get("fade_colors")));
		explosion.setBoolean("Trail", Boolean.TRUE.equals(asBoolean(map.get("has_trail"))));
		explosion.setBoolean("Flicker", Boolean.TRUE.equals(asBoolean(map.get("has_twinkle"))));
		return explosion;
	}

	private static Boolean asBoolean(Object value) throws DecodeException {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean flag) {
			return flag;
		}
		return LegacyEntityData.number(value, 0) != 0;
	}

	private static int[] colors(Object value) throws DecodeException {
		if (value == null) {
			return new int[0];
		}
		if (value instanceof int[] ints) {
			return ints;
		}
		List<?> list = LegacyEntityData.list(value);
		int[] colors = new int[list.size()];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = (int) LegacyEntityData.number(list.get(i), 0);
		}
		return colors;
	}

	// ---------------------------------------------------------------- books

	/** {pages}: plain pages, a string or {raw, filtered} each. */
	static ItemComponents.Applier writableBook(Object value) throws DecodeException {
		NBTTagList pages = new NBTTagList();
		Map<?, ?> map = LegacyEntityData.map(value);
		if (map.containsKey("pages")) {
			for (Object page : LegacyEntityData.list(map.get("pages"))) {
				pages.appendTag(new NBTTagString(string(filterable(page), "a page")));
			}
		}
		return stack -> tag(stack).setTag("pages", pages.copy());
	}

	/**
	 * {title, author, generation, pages, resolved}: the title, author and generation as they are, the pages (text
	 * components) as 1.7.10's plain pages with § codes. 1.7.10 calls a book with a title over 16 characters invalid.
	 */
	static ItemComponents.Applier writtenBook(Object value) throws DecodeException {
		Map<?, ?> map = LegacyEntityData.map(value);
		if (!map.containsKey("title") || !map.containsKey("author")) {
			throw new DecodeException("A written book needs a title and an author");
		}
		String title = string(filterable(map.get("title")), "a title");
		if (title.length() > 32) {
			throw new DecodeException("String too long: " + title.length() + " > 32");
		}
		if (title.length() > 16) {
			throw new DecodeException("1.7.10's books take titles of 16 characters at most");
		}
		String author = string(map.get("author"), "an author");
		int generation = (int) LegacyEntityData.number(map.get("generation"), 0);
		if (generation < 0 || generation > 3) {
			throw new DecodeException("Value " + generation + " outside of range [0:3]");
		}
		NBTTagList pages = new NBTTagList();
		if (map.containsKey("pages")) {
			for (Object page : LegacyEntityData.list(map.get("pages"))) {
				pages.appendTag(new NBTTagString(TextComponent.decode(filterable(page)).toLegacy('0', false)));
			}
		}
		return stack -> {
			NBTTagCompound tag = tag(stack);
			tag.setString("title", title);
			tag.setString("author", author);
			tag.setInteger("generation", generation);
			tag.setTag("pages", pages.copy());
		};
	}

	// ---------------------------------------------------------------- attribute_modifiers

	/**
	 * [{type, id, amount, operation, slot, display}]: 1.7.10's AttributeModifiers, which replace the item's own as
	 * modern's do. 1.7.10 applies them in whatever slot the item is worn or held, so the slot isn't kept.
	 */
	static ItemComponents.Applier attributeModifiers(Object value) throws DecodeException {
		Object entries = value instanceof Map<?, ?> map && map.containsKey("modifiers") ? map.get("modifiers") : value;
		NBTTagList modifiers = new NBTTagList();
		for (Object entry : LegacyEntityData.list(entries)) {
			Map<?, ?> map = LegacyEntityData.map(entry);
			String id = string(map.get("id"), "a modifier id");
			UUID uuid = UUID.nameUUIDFromBytes(ItemData.withNamespace(id).getBytes(StandardCharsets.UTF_8));
			NBTTagCompound modifier = new NBTTagCompound();
			modifier.setString("AttributeName", LegacyEntityData.attributeName(string(map.get("type"), "an attribute")));
			modifier.setString("Name", ItemData.withNamespace(id));
			modifier.setDouble("Amount", LegacyEntityData.number(map.get("amount"), 0));
			modifier.setInteger("Operation", LegacyEntityData.operation(map.get("operation")));
			modifier.setLong("UUIDMost", uuid.getMostSignificantBits());
			modifier.setLong("UUIDLeast", uuid.getLeastSignificantBits());
			modifiers.appendTag(modifier);
		}
		return stack -> tag(stack).setTag("AttributeModifiers", modifiers.copy());
	}

	// ---------------------------------------------------------------- profile

	/** A player name, or {name, id, properties}: a head's SkullOwner, which 1.7.10 fills in from the name when it can. */
	static ItemComponents.Applier profile(Object value) throws DecodeException {
		if (value instanceof String name) {
			return stack -> tag(stack).setString("SkullOwner", name);
		}
		Map<?, ?> map = LegacyEntityData.map(value);
		NBTTagCompound owner = new NBTTagCompound();
		if (map.get("name") instanceof String name) {
			owner.setString("Name", name);
		}
		if (map.containsKey("id")) {
			if (!(map.get("id") instanceof int[] ints) || ints.length != 4) {
				throw new DecodeException("Not a UUID: " + Snbt.toText(map.get("id")));
			}
			owner.setString("Id", new UUID((long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL, (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL).toString());
		}
		if (map.containsKey("properties")) {
			NBTTagCompound properties = new NBTTagCompound();
			for (Object property : LegacyEntityData.list(map.get("properties"))) {
				Map<?, ?> entry = LegacyEntityData.map(property);
				String name = string(entry.get("name"), "a property name");
				NBTTagCompound saved = new NBTTagCompound();
				saved.setString("Value", string(entry.get("value"), "a property value"));
				if (entry.get("signature") instanceof String signature) {
					saved.setString("Signature", signature);
				}
				NBTTagList values = properties.hasKey(name) ? properties.getTagList(name, 10) : new NBTTagList();
				values.appendTag(saved);
				properties.setTag(name, values);
			}
			owner.setTag("Properties", properties);
		}
		return stack -> tag(stack).setTag("SkullOwner", owner.copy());
	}

	// ---------------------------------------------------------------- banner_patterns

	/** [{pattern, color}]: EFR's banner keeps them as BlockEntityTag.Patterns, each a pattern code and a colour number. */
	static ItemComponents.Applier bannerPatterns(Object value) throws DecodeException {
		NBTTagList patterns = new NBTTagList();
		for (Object layer : LegacyEntityData.list(value)) {
			Map<?, ?> map = LegacyEntityData.map(layer);
			String patternId = ItemData.withNamespace(string(map.get("pattern"), "a pattern id"));
			BannerPattern pattern = null;
			for (BannerPattern candidate : BannerPatterns.all()) {
				if (patternId.equals("minecraft:" + candidate.name())) {
					pattern = candidate;
				}
			}
			if (pattern == null) {
				throw new DecodeException("Unknown banner pattern '" + patternId + "'");
			}
			int color = Arrays.asList(ModRecipes.dye_names).indexOf(string(map.get("color"), "a colour"));
			if (color < 0) {
				throw new DecodeException("Unknown colour '" + map.get("color") + "'");
			}
			NBTTagCompound saved = new NBTTagCompound();
			saved.setString("Pattern", pattern.shortId());
			saved.setInteger("Color", color);
			patterns.appendTag(saved);
		}
		return stack -> {
			NBTTagCompound tag = tag(stack);
			NBTTagCompound blockEntity = tag.getCompoundTag("BlockEntityTag");
			blockEntity.setTag("Patterns", patterns.copy());
			tag.setTag("BlockEntityTag", blockEntity);
		};
	}
}
