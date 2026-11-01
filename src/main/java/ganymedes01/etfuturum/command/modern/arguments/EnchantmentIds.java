package ganymedes01.etfuturum.command.modern.arguments;

import ganymedes01.etfuturum.ModEnchantments;
import net.minecraft.enchantment.Enchantment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Modern's ids for 1.7.10's enchantments (spec §44.1). Built on first use, when every mod has registered its own. */
public final class EnchantmentIds {

	/** 1.7.10's numbers and modern's names for vanilla's enchantments. */
	private static final Map<Integer, String> VANILLA = new HashMap<>();

	static {
		String[] names = {"protection", "fire_protection", "feather_falling", "blast_protection", "projectile_protection",
			"respiration", "aqua_affinity", "thorns"};
		for (int i = 0; i < names.length; i++) {
			VANILLA.put(i, names[i]);
		}
		VANILLA.put(16, "sharpness");
		VANILLA.put(17, "smite");
		VANILLA.put(18, "bane_of_arthropods");
		VANILLA.put(19, "knockback");
		VANILLA.put(20, "fire_aspect");
		VANILLA.put(21, "looting");
		VANILLA.put(32, "efficiency");
		VANILLA.put(33, "silk_touch");
		VANILLA.put(34, "unbreaking");
		VANILLA.put(35, "fortune");
		VANILLA.put(48, "power");
		VANILLA.put(49, "punch");
		VANILLA.put(50, "flame");
		VANILLA.put(51, "infinity");
		VANILLA.put(61, "luck_of_the_sea");
		VANILLA.put(62, "lure");
	}

	private static Map<String, Enchantment> byId;
	/** Set last, so a thread that sees it sees the ids too. */
	private static volatile Map<Enchantment, String> idOf;

	private EnchantmentIds() {
	}

	public static Enchantment byId(String id) {
		build();
		return byId.get(id);
	}

	public static String idOf(Enchantment enchantment) {
		build();
		return idOf.get(enchantment);
	}

	/** Every id, sorted. */
	public static Collection<String> ids() {
		build();
		return Collections.unmodifiableCollection(byId.keySet());
	}

	private static synchronized void build() {
		if (idOf != null) {
			return;
		}
		Map<String, Enchantment> ids = new TreeMap<>();
		Map<Enchantment, String> reverse = new HashMap<>();
		for (Enchantment enchantment : Enchantment.enchantmentsList) {
			if (enchantment == null) {
				continue;
			}
			String id = idFor(enchantment);
			if (ids.containsKey(id)) {
				id = id + "_" + enchantment.effectId;
			}
			ids.put(id, enchantment);
			reverse.put(enchantment, id);
		}
		byId = ids;
		idOf = reverse;
	}

	/** Vanilla's and EFR's by modern's name; another mod's from its name key: `enchantment.mod.name` is `mod:name`. */
	private static String idFor(Enchantment enchantment) {
		String vanilla = VANILLA.get(enchantment.effectId);
		if (vanilla != null && enchantment.getClass().getName().startsWith("net.minecraft.")) {
			return "minecraft:" + vanilla;
		}
		if (enchantment == ModEnchantments.frostWalker) {
			return "minecraft:frost_walker";
		}
		if (enchantment == ModEnchantments.mending) {
			return "minecraft:mending";
		}
		if (enchantment == ModEnchantments.swiftSneak) {
			return "minecraft:swift_sneak";
		}
		if (enchantment == ModEnchantments.depthStrider) {
			return "minecraft:depth_strider";
		}
		if (enchantment == ModEnchantments.sweepingEdge) {
			return "minecraft:sweeping_edge";
		}
		String name = enchantment.getName().toLowerCase(Locale.ROOT);
		if (name.startsWith("enchantment.")) {
			name = name.substring("enchantment.".length());
		}
		StringBuilder path = new StringBuilder();
		for (char c : name.toCharArray()) {
			path.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-' || c == '/' ? c : '_');
		}
		String clean = path.length() == 0 ? "enchantment_" + enchantment.effectId : path.toString();
		int dot = clean.indexOf('.');
		return dot > 0 && dot < clean.length() - 1 ? clean.substring(0, dot) + ":" + clean.substring(dot + 1) : "minecraft:" + clean;
	}
}
