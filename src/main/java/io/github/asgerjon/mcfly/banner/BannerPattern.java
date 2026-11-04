package io.github.asgerjon.mcfly.banner;

import io.github.asgerjon.mcfly.McFly;

/**
 * A banner pattern (SPEC-modern-movement.md §80; 26.3's `BannerPattern`): its 26.3 name ("stripe_bottom"), the short id
 * a banner stores it by ("bs", 1.8's form, which old saves hold), and whether it's one of 26.3's own (drawn from 26.3's
 * textures by 26.3's rule) or one another mod added (drawn from its 1.8 mask, where such a mod puts it).
 */
public final class BannerPattern {

	private final String name;
	private final String shortId;
	private final boolean modern;

	BannerPattern(String name, String shortId, boolean modern) {
		this.name = name;
		this.shortId = shortId;
		this.modern = modern;
	}

	public String name() {
		return name;
	}

	public String shortId() {
		return shortId;
	}

	/** One of 26.3's 43. */
	public boolean isModern() {
		return modern;
	}

	/** Its banner texture: 26.3's under our domain, or another mod's 1.8 mask. */
	public String texture() {
		return modern ? McFly.DOMAIN + ":textures/entity/banner/" + name + ".png" : "textures/entity/banner/" + name + ".png";
	}

	/** Its shield texture (26.3's; only for 26.3's own). */
	public String shieldTexture() {
		return McFly.DOMAIN + ":textures/entity/shield/pattern/" + name + ".png";
	}

	@Override
	public String toString() {
		return name;
	}
}
