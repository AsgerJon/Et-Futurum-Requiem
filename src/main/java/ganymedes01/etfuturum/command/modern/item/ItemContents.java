package ganymedes01.etfuturum.command.modern.item;

import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemData;
import ganymedes01.etfuturum.command.modern.entity.LegacyEntityData;
import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import ganymedes01.etfuturum.items.ItemArrowTipped;
import ganymedes01.etfuturum.recipes.ModRecipes;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The item components of step 2 (spec §50): what they hold, read from modern's SNBT, put where 1.7.10 and EFR keep it.
 */
final class ItemContents {

	private static final int DRINKABLE = 8192;
	private static final int SPLASH = 16384;
	/** Modern's potions 1.7.10 has, by the damage of the drinkable one. */
	private static final Map<String, Integer> POTION_DAMAGE = new HashMap<>();
	/** The others as modern makes them: effect/duration/amplifier, as custom effects on a plain drinkable potion. */
	private static final Map<String, String> POTION_EFFECTS = new HashMap<>();
	private static final List<String> SHAPES = Arrays.asList("small_ball", "large_ball", "star", "creeper", "burst");

	static {
		String[] damages = {"water 0", "awkward 16", "thick 32", "mundane 64", "regeneration 8193", "long_regeneration 8257",
			"strong_regeneration 8225", "swiftness 8194", "long_swiftness 8258", "strong_swiftness 8226", "fire_resistance 8195",
			"long_fire_resistance 8259", "poison 8196", "long_poison 8260", "strong_poison 8228", "healing 8197", "strong_healing 8229",
			"night_vision 8198", "long_night_vision 8262", "weakness 8200", "long_weakness 8264", "strength 8201", "long_strength 8265",
			"strong_strength 8233", "slowness 8202", "long_slowness 8266", "harming 8204", "strong_harming 8236", "water_breathing 8205",
			"long_water_breathing 8269", "invisibility 8206", "long_invisibility 8270"};
		for (String entry : damages) {
			String[] pair = entry.split(" ");
			POTION_DAMAGE.put("minecraft:" + pair[0], Integer.parseInt(pair[1]));
		}
		String[] effects = {"leaping jump_boost/3600/0", "long_leaping jump_boost/9600/0", "strong_leaping jump_boost/1800/1",
			"strong_slowness slowness/400/3", "turtle_master slowness/400/3 resistance/400/2", "long_turtle_master slowness/800/3 resistance/800/2",
			"strong_turtle_master slowness/400/5 resistance/400/3", "luck luck/6000/0", "slow_falling slow_falling/1800/0",
			"long_slow_falling slow_falling/4800/0", "wind_charged wind_charged/3600/0", "weaving weaving/3600/0", "oozing oozing/3600/0",
			"infested infested/3600/0"};
		for (String entry : effects) {
			int space = entry.indexOf(' ');
			POTION_EFFECTS.put("minecraft:" + entry.substring(0, space), entry.substring(space + 1));
		}
	}

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
	 * A potion id, or {potion, custom_effects, custom_color, custom_name}. 1.7.10 keeps a potion in the item's damage:
	 * a potion it has goes there (splash keeps its splash bit), one it lacks becomes custom effects on a plain
	 * drinkable potion; custom effects are 1.7.10's CustomPotionEffects. The damage only goes on potions, EFR's lingering
	 * potions and tipped arrows. The colour and name base have nowhere to go.
	 */
	static ItemComponents.Applier potionContents(Object value) throws DecodeException {
		Object potionValue = value instanceof Map<?, ?> map ? map.get("potion") : value;
		Integer damage = null;
		NBTTagList effects = new NBTTagList();
		if (potionValue != null) {
			String id = ItemData.withNamespace(string(potionValue, "a potion id"));
			damage = POTION_DAMAGE.get(id);
			if (damage == null) {
				String made = POTION_EFFECTS.get(id);
				if (made == null) {
					throw new DecodeException("Unknown potion '" + id + "'");
				}
				damage = DRINKABLE;
				for (String effect : made.split(" ")) {
					String[] parts = effect.split("/");
					Potion potion = EffectIds.byId("minecraft:" + parts[0]);
					if (potion == null) {
						throw new DecodeException("Potion '" + id + "' needs the effect 'minecraft:" + parts[0] + "', which 1.7.10 doesn't have");
					}
					NBTTagCompound legacy = new NBTTagCompound();
					legacy.setByte("Id", (byte) potion.id);
					legacy.setByte("Amplifier", Byte.parseByte(parts[2]));
					legacy.setInteger("Duration", Integer.parseInt(parts[1]));
					effects.appendTag(legacy);
				}
			}
		}
		if (value instanceof Map<?, ?> map && map.containsKey("custom_effects")) {
			NBTTagList custom = LegacyEntityData.effectList(map.get("custom_effects"));
			for (int i = 0; i < custom.tagCount(); i++) {
				effects.appendTag(custom.getCompoundTagAt(i).copy());
			}
			if (damage == null) {
				damage = DRINKABLE;
			}
		}
		int potionDamage = damage == null ? 0 : damage;
		return stack -> {
			if (stack.getItem() instanceof ItemPotion || stack.getItem() instanceof ItemArrowTipped) {
				boolean splash = stack.getItem() == Items.potionitem && (stack.getItemDamage() & SPLASH) != 0;
				stack.setItemDamage(splash ? potionDamage & ~DRINKABLE | SPLASH : potionDamage);
			}
			if (effects.tagCount() > 0) {
				tag(stack).setTag("CustomPotionEffects", effects.copy());
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
			TileEntityBanner.EnumBannerPattern pattern = null;
			for (TileEntityBanner.EnumBannerPattern candidate : TileEntityBanner.EnumBannerPattern.values()) {
				if (patternId.equals("minecraft:" + candidate.getPatternName())) {
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
			saved.setString("Pattern", pattern.getPatternID());
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
