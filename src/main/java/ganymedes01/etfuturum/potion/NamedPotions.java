package ganymedes01.etfuturum.potion;

import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The registry of potions by name (SPEC-modern-movement.md §69.3), as 26.3's `Potions`: 26.3's 46 in its order, each
 * with the drinkable one's 1.7.10 damage value where 1.7.10 has that potion. Another mod registers its own with
 * {@link #register} under its own namespace; a potion whose effects don't exist is registered but not offered
 * ({@link NamedPotion#isAvailable()}).
 */
public final class NamedPotions {

	private static final int DRINKABLE = 8192;
	private static final int SPLASH = 16384;

	private static final Map<String, NamedPotion> BY_ID = new LinkedHashMap<>();
	/** 1.7.10's drinkable damage values that are a named potion. */
	private static final Map<Integer, NamedPotion> BY_DAMAGE = new HashMap<>();

	static {
		vanilla("water", "water", 0);
		vanilla("mundane", "mundane", 64);
		vanilla("thick", "thick", 32);
		vanilla("awkward", "awkward", 16);
		vanilla("night_vision", "night_vision", 8198, "night_vision/3600/0");
		vanilla("long_night_vision", "night_vision", 8262, "night_vision/9600/0");
		vanilla("invisibility", "invisibility", 8206, "invisibility/3600/0");
		vanilla("long_invisibility", "invisibility", 8270, "invisibility/9600/0");
		vanilla("leaping", "leaping", efrLeaping(8203), "jump_boost/3600/0");
		vanilla("long_leaping", "leaping", efrLeaping(8267), "jump_boost/9600/0");
		vanilla("strong_leaping", "leaping", efrLeaping(8235), "jump_boost/1800/1");
		vanilla("fire_resistance", "fire_resistance", 8195, "fire_resistance/3600/0");
		vanilla("long_fire_resistance", "fire_resistance", 8259, "fire_resistance/9600/0");
		vanilla("swiftness", "swiftness", 8194, "speed/3600/0");
		vanilla("long_swiftness", "swiftness", 8258, "speed/9600/0");
		vanilla("strong_swiftness", "swiftness", 8226, "speed/1800/1");
		vanilla("slowness", "slowness", 8202, "slowness/1800/0");
		vanilla("long_slowness", "slowness", 8266, "slowness/4800/0");
		vanilla("strong_slowness", "slowness", -1, "slowness/400/3");
		vanilla("turtle_master", "turtle_master", -1, "slowness/400/3", "resistance/400/2");
		vanilla("long_turtle_master", "turtle_master", -1, "slowness/800/3", "resistance/800/2");
		vanilla("strong_turtle_master", "turtle_master", -1, "slowness/400/5", "resistance/400/3");
		vanilla("water_breathing", "water_breathing", 8205, "water_breathing/3600/0");
		vanilla("long_water_breathing", "water_breathing", 8269, "water_breathing/9600/0");
		vanilla("healing", "healing", 8197, "instant_health/1/0");
		vanilla("strong_healing", "healing", 8229, "instant_health/1/1");
		vanilla("harming", "harming", 8204, "instant_damage/1/0");
		vanilla("strong_harming", "harming", 8236, "instant_damage/1/1");
		vanilla("poison", "poison", 8196, "poison/900/0");
		vanilla("long_poison", "poison", 8260, "poison/1800/0");
		vanilla("strong_poison", "poison", 8228, "poison/432/1");
		vanilla("regeneration", "regeneration", 8193, "regeneration/900/0");
		vanilla("long_regeneration", "regeneration", 8257, "regeneration/1800/0");
		vanilla("strong_regeneration", "regeneration", 8225, "regeneration/450/1");
		vanilla("strength", "strength", 8201, "strength/3600/0");
		vanilla("long_strength", "strength", 8265, "strength/9600/0");
		vanilla("strong_strength", "strength", 8233, "strength/1800/1");
		vanilla("weakness", "weakness", 8200, "weakness/1800/0");
		vanilla("long_weakness", "weakness", 8264, "weakness/4800/0");
		vanilla("luck", "luck", -1, "luck/6000/0");
		vanilla("slow_falling", "slow_falling", -1, "slow_falling/1800/0");
		vanilla("long_slow_falling", "slow_falling", -1, "slow_falling/4800/0");
		vanilla("wind_charged", "wind_charged", -1, "wind_charged/3600/0");
		vanilla("weaving", "weaving", -1, "weaving/3600/0");
		vanilla("oozing", "oozing", -1, "oozing/3600/0");
		vanilla("infested", "infested", -1, "infested/3600/0");
		// 1.7.10 has two mundane potions: the one water and redstone brew (64), and 8192, the drinkable bit alone
		BY_DAMAGE.put(DRINKABLE, BY_ID.get("minecraft:mundane"));
	}

	/** Set in EFR's postInit, once every mod's effects and items exist (`EffectIds` and `ItemIds` are built once). */
	private static volatile boolean ready;

	private NamedPotions() {
	}

	/**
	 * Whether potions by name are working yet. 1.7.10 names every item while it starts, before any mod has loaded,
	 * and asks a potion for its effects then: the mixins leave potions to 1.7.10 until this.
	 */
	public static boolean isReady() {
		return ready;
	}

	public static void setReady() {
		ready = true;
	}

	/**
	 * Registers a potion; another mod's id in its own namespace. `legacyDamage` is the drinkable one's 1.7.10 damage
	 * value, or -1 for a potion 1.7.10 doesn't have (then it is kept on the item by its name).
	 */
	public static synchronized NamedPotion register(String id, String baseName, int legacyDamage, NamedPotion.Effect... effects) {
		String key = withNamespace(id);
		if (BY_ID.containsKey(key)) {
			throw new IllegalArgumentException("The potion " + key + " is registered already");
		}
		List<NamedPotion.Effect> list = new ArrayList<>();
		for (NamedPotion.Effect effect : effects) {
			list.add(new NamedPotion.Effect(withNamespace(effect.effect()), effect.duration(), effect.amplifier()));
		}
		NamedPotion potion = new NamedPotion(key, baseName, legacyDamage, list);
		BY_ID.put(key, potion);
		if (legacyDamage >= 0) {
			BY_DAMAGE.putIfAbsent(legacyDamage, potion);
		}
		return potion;
	}

	public static NamedPotion byId(String id) {
		return id == null ? null : BY_ID.get(withNamespace(id));
	}

	/** The named potion a 1.7.10 damage value is (drinkable or splash), or null. */
	public static NamedPotion byLegacyDamage(int damage) {
		int drinkable = (damage & SPLASH) != 0 ? damage & ~SPLASH | DRINKABLE : damage;
		return BY_DAMAGE.get(drinkable);
	}

	/** Every registered potion, 26.3's in 26.3's order, then other mods' in theirs. */
	public static List<NamedPotion> all() {
		return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
	}

	/** Those offered: every effect they have exists. */
	public static List<NamedPotion> available() {
		List<NamedPotion> list = new ArrayList<>();
		for (NamedPotion potion : BY_ID.values()) {
			if (potion.isAvailable()) {
				list.add(potion);
			}
		}
		return list;
	}

	public static NamedPotion.Effect effect(String effect, int duration, int amplifier) {
		return new NamedPotion.Effect(withNamespace(effect), duration, amplifier);
	}

	/**
	 * EFR brews leaping by 1.7.10's bits when its rabbit's foot is on (`ModRecipes`: jump boost as effect number 11),
	 * so worlds hold leaping potions as 8203, 8267 (long) and 8235 (strong); without it those numbers are no potion.
	 */
	private static int efrLeaping(int damage) {
		// the rabbit's foot's own switch (ModItems.RABBIT_FOOT); not ModItems itself, which mustn't load this early
		return ConfigEntities.enableRabbit ? damage : -1;
	}

	private static void vanilla(String name, String baseName, int legacyDamage, String... effects) {
		NamedPotion.Effect[] list = new NamedPotion.Effect[effects.length];
		for (int i = 0; i < effects.length; i++) {
			String[] parts = effects[i].split("/");
			list[i] = effect(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		}
		register("minecraft:" + name, baseName, legacyDamage, list);
	}

	static String withNamespace(String id) {
		String clean = id.trim().toLowerCase(Locale.ROOT);
		return clean.indexOf(':') < 0 ? "minecraft:" + clean : clean;
	}
}
