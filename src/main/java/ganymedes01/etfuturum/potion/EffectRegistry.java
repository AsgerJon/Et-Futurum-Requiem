package ganymedes01.etfuturum.potion;

import net.minecraft.potion.Potion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Effects by name, as 26.3's registry (SPEC-modern-movement.md §70.3): a dict both ways, name -> effect (26.3's
 * `getValue`) and effect -> name (`getKey`). 1.7.10's effect object (`Potion`) stays the effect, so every mod's code
 * that holds one keeps working, and its number (`Potion.id`) is only what 1.7.10's code and every mod's use inside a
 * running game: chosen as effects register ({@link #claim}, through `MixinPotion`, the table growing for it), never
 * saved and never set in a config. Vanilla's effects have 26.3's names, EFR's theirs ({@link #name}), another mod's
 * the name it gives ({@link #name}) or one made from its `Potion`'s own (`potion.mod.name` is `mod:name`).
 */
public final class EffectRegistry {

	private static final Logger LOG = LogManager.getLogger("EFR named effects");

	/** 1.7.10's numbers 1 to 23 are 26.3's effects of these names. */
	private static final String[] VANILLA = {null, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
		"instant_damage", "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility",
		"blindness", "night_vision", "hunger", "weakness", "poison", "wither", "health_boost", "absorption", "saturation"};

	/** Where a moved effect's free number is looked for first: past 1.7.10's 32, where configs never put one. */
	private static final int MOVED_FROM = 32;

	/** Names their makers gave (EFR's, and a mod's through {@link #name}). */
	private static final Map<Potion, String> GIVEN = Collections.synchronizedMap(new IdentityHashMap<>());

	/** Goes up whenever an effect registers or is named, so the dict is built again. */
	private static volatile int version;
	private static int builtVersion = -1;
	private static volatile Map<String, Potion> byName = Collections.emptyMap();
	private static volatile Map<Potion, String> nameOf = Collections.emptyMap();

	private EffectRegistry() {
	}

	// ---------------------------------------------------------------- the dict

	/** The effect registered under this name, or null (26.3's `getValue`). A name without a namespace is minecraft's. */
	public static Potion get(String name) {
		if (name == null) {
			return null;
		}
		build();
		return byName.get(withNamespace(name));
	}

	/** The effect's name (26.3's `getKey`), or null. */
	public static String key(Potion potion) {
		if (potion == null) {
			return null;
		}
		build();
		return nameOf.get(potion);
	}

	/** Every name, sorted. */
	public static Collection<String> names() {
		build();
		return Collections.unmodifiableCollection(byName.keySet());
	}

	/** Names an effect (26.3's `Registry.register` key): EFR's under minecraft's names, a mod's under its own. */
	public static void name(Potion potion, String name) {
		GIVEN.put(potion, withNamespace(name));
		version++;
	}

	// ---------------------------------------------------------------- numbers

	/**
	 * The number a new effect gets, from 1.7.10's `Potion` constructor (`MixinPotion`): the one it asked for when that
	 * is free, else a free one past the old 32 with a warning (1.7.10 put the new effect over the old one silently).
	 */
	public static int claim(int wanted) {
		Potion[] table = Potion.potionTypes;
		int id = wanted;
		if (id < 0 || id < table.length && table[id] != null) {
			id = firstFree(MOVED_FROM);
			LOG.warn("An effect asked for number {}, which {} has; it gets {} (effects are known by name, the numbers are only for the running game)",
				wanted, wanted >= 0 && wanted < table.length ? describe(table[wanted]) : "nothing", id);
		}
		version++;
		return id;
	}

	/** A number for one of EFR's effects: its old default when free (worlds saved by number before still read it right). */
	public static int freeNumber(int preferred) {
		Potion[] table = Potion.potionTypes;
		return preferred >= 0 && (preferred >= table.length || table[preferred] == null) ? preferred : firstFree(MOVED_FROM);
	}

	private static int firstFree(int from) {
		Potion[] table = Potion.potionTypes;
		for (int id = from; id < table.length; id++) {
			if (table[id] == null) {
				return id;
			}
		}
		return Math.max(from, table.length);
	}

	// ---------------------------------------------------------------- building

	/** Builds the dict again on next use: from EFR's postInit, once every mod has made and named its effects. */
	public static void refresh() {
		version++;
	}

	private static synchronized void build() {
		if (builtVersion != version) {
			int building = version;
			Map<String, Potion> names = new TreeMap<>();
			Map<Potion, String> reverse = new HashMap<>();
			for (Potion potion : Potion.potionTypes) {
				if (potion == null) {
					continue;
				}
				String name = nameFor(potion);
				if (names.containsKey(name)) {
					name = name + "_" + potion.id;
				}
				names.put(name, potion);
				reverse.put(potion, name);
			}
			byName = names;
			nameOf = reverse;
			builtVersion = building;
		}
	}

	private static String nameFor(Potion potion) {
		String given = GIVEN.get(potion);
		if (given != null) {
			return given;
		}
		if (potion.id > 0 && potion.id < VANILLA.length && potion.getClass().getName().startsWith("net.minecraft.")) {
			return "minecraft:" + VANILLA[potion.id];
		}
		String name = potion.getName() == null ? "" : potion.getName().toLowerCase(Locale.ROOT);
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

	private static String describe(Potion potion) {
		return potion == null ? "nothing" : potion.getName() + " (" + potion.getClass().getName() + ")";
	}

	static String withNamespace(String name) {
		String clean = name.trim().toLowerCase(Locale.ROOT);
		return clean.indexOf(':') < 0 ? "minecraft:" + clean : clean;
	}
}
