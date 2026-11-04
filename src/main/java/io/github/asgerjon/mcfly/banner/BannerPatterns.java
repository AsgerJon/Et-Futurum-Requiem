package io.github.asgerjon.mcfly.banner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The banner patterns (SPEC-modern-movement.md §80; 26.3's `BannerPatterns`): 26.3's 43 in its order, each with the
 * short id it's stored by, and those other mods add ({@link #register}).
 */
public final class BannerPatterns {

	private static final Map<String, BannerPattern> BY_NAME = new LinkedHashMap<>();
	private static final Map<String, BannerPattern> BY_SHORT_ID = new LinkedHashMap<>();
	private static final List<BannerPattern> MODERN = new ArrayList<>();
	private static final List<BannerPattern> NO_ITEM_REQUIRED = new ArrayList<>();

	/** The patterns 26.3 adds only from a pattern item (its `pattern_item/*` tags). */
	private static final String[] FROM_ITEMS = {"curly_border", "bricks", "globe", "creeper", "skull", "flower", "mojang", "piglin", "flow", "guster"};

	public static final BannerPattern BASE;

	static {
		// 26.3's names in its order, each with 1.8's short id (glb and pig Java's own until 1.20.5; flw and gus ours)
		String[] modern = {"base b", "square_bottom_left bl", "square_bottom_right br", "square_top_left tl", "square_top_right tr",
			"stripe_bottom bs", "stripe_top ts", "stripe_left ls", "stripe_right rs", "stripe_center cs", "stripe_middle ms",
			"stripe_downright drs", "stripe_downleft dls", "small_stripes ss", "cross cr", "straight_cross sc", "triangle_bottom bt",
			"triangle_top tt", "triangles_bottom bts", "triangles_top tts", "diagonal_left ld", "diagonal_up_right rd",
			"diagonal_up_left lud", "diagonal_right rud", "circle mc", "rhombus mr", "half_vertical vh", "half_horizontal hh",
			"half_vertical_right vhr", "half_horizontal_bottom hhb", "border bo", "curly_border cbo", "gradient gra", "gradient_up gru",
			"bricks bri", "globe glb", "creeper cre", "skull sku", "flower flo", "mojang moj", "piglin pig", "flow flw", "guster gus"};
		List<String> fromItems = java.util.Arrays.asList(FROM_ITEMS);
		for (String entry : modern) {
			String[] parts = entry.split(" ");
			BannerPattern pattern = add(parts[0], parts[1], true);
			MODERN.add(pattern);
			if (!parts[0].equals("base") && !fromItems.contains(parts[0])) {
				NO_ITEM_REQUIRED.add(pattern);
			}
		}
		BASE = BY_NAME.get("base");
	}

	private BannerPatterns() {
	}

	private static BannerPattern add(String name, String shortId, boolean modern) {
		BannerPattern pattern = new BannerPattern(name, shortId, modern);
		BY_NAME.put(name, pattern);
		BY_SHORT_ID.put(shortId, pattern);
		return pattern;
	}

	/**
	 * A pattern another mod adds: its name, the short id banners store it by, its 1.8 mask at
	 * `minecraft:textures/entity/banner/<name>.png`. A name or short id already taken gives the one there.
	 */
	public static synchronized BannerPattern register(String name, String shortId) {
		BannerPattern known = BY_NAME.get(name);
		if (known != null) {
			return known;
		}
		known = BY_SHORT_ID.get(shortId);
		return known != null ? known : add(name, shortId, false);
	}

	/** By 26.3's name ("stripe_bottom", or with `minecraft:`), or null. */
	public static BannerPattern byName(String name) {
		return BY_NAME.get(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
	}

	/** By the short id a banner stores ("bs"), or null. */
	public static BannerPattern byShortId(String shortId) {
		return BY_SHORT_ID.get(shortId);
	}

	/** 26.3's 43, in its order. */
	public static List<BannerPattern> modern() {
		return Collections.unmodifiableList(MODERN);
	}

	/** 26.3's 32 that need no pattern item (its `no_item_required` tag), in its order: the loom's list with none in. */
	public static List<BannerPattern> noItemRequired() {
		return Collections.unmodifiableList(NO_ITEM_REQUIRED);
	}

	/** Every pattern, 26.3's then other mods'. */
	public static List<BannerPattern> all() {
		return Collections.unmodifiableList(new ArrayList<>(BY_NAME.values()));
	}
}
