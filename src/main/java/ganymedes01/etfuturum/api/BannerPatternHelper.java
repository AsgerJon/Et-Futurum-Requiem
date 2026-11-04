package ganymedes01.etfuturum.api;

import net.minecraft.item.ItemStack;

/**
 * EFR's API for other mods' banner patterns, kept for mods that call it; now McFly's pattern registry
 * (`io.github.asgerjon.mcfly.banner.BannerPatterns.register`, spec §80). A pattern's 1.8 mask goes in
 * `minecraft:textures/entity/banner/<name>.png`; the loom adds patterns, so the crafting arguments go unused.
 */
public class BannerPatternHelper {

	public static void addPattern(String name, String id, net.minecraft.item.ItemStack craftingItem) {
		io.github.asgerjon.mcfly.banner.BannerPatterns.register(name, id);
	}

	public static void addPattern(String name, String id, String craftingTop, String craftingMid, String craftingBot) {
		io.github.asgerjon.mcfly.banner.BannerPatterns.register(name, id);
	}
}
