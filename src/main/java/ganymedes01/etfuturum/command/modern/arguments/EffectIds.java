package ganymedes01.etfuturum.command.modern.arguments;

import ganymedes01.etfuturum.potion.ModPotions;
import net.minecraft.potion.Potion;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Modern's ids for 1.7.10's effects (spec §41.3). Built on first use, when every mod has registered its effects. */
public final class EffectIds {

	/** 1.7.10's numeric ids 1 to 23 are modern's effects of these names. */
	private static final String[] VANILLA = {null, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
		"instant_damage", "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility",
		"blindness", "night_vision", "hunger", "weakness", "poison", "wither", "health_boost", "absorption", "saturation"};

	private static Map<String, Potion> byId;
	private static Map<Potion, String> idOf;

	private EffectIds() {
	}

	public static Potion byId(String id) {
		build();
		return byId.get(id);
	}

	public static String idOf(Potion potion) {
		build();
		return idOf.get(potion);
	}

	/** Every id, sorted. */
	public static Collection<String> ids() {
		build();
		return Collections.unmodifiableCollection(byId.keySet());
	}

	private static synchronized void build() {
		if (byId != null) {
			return;
		}
		Map<String, Potion> ids = new TreeMap<>();
		Map<Potion, String> reverse = new HashMap<>();
		for (Potion potion : Potion.potionTypes) {
			if (potion == null) {
				continue;
			}
			String id = idFor(potion);
			if (ids.containsKey(id)) {
				id = id + "_" + potion.id;
			}
			ids.put(id, potion);
			reverse.put(potion, id);
		}
		idOf = reverse;
		byId = ids;
	}

	/** Vanilla's and EFR's by modern's name; another mod's from its name: `potion.mod.name` is `mod:name`. */
	private static String idFor(Potion potion) {
		if (potion.id < VANILLA.length && VANILLA[potion.id] != null && potion.getClass().getName().startsWith("net.minecraft.")) {
			return "minecraft:" + VANILLA[potion.id];
		}
		if (potion == ModPotions.levitation) {
			return "minecraft:levitation";
		}
		String name = potion.getName().toLowerCase(Locale.ROOT);
		if (name.startsWith("potion.")) {
			name = name.substring("potion.".length());
		}
		StringBuilder path = new StringBuilder();
		for (char c : name.toCharArray()) {
			path.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-' || c == '/' ? c : '_');
		}
		String clean = path.length() == 0 ? "effect_" + potion.id : path.toString();
		int dot = clean.indexOf('.');
		return dot > 0 && dot < clean.length() - 1 ? clean.substring(0, dot) + ":" + clean.substring(dot + 1) : "minecraft:" + clean;
	}
}
