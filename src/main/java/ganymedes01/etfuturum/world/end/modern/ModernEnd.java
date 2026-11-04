package ganymedes01.etfuturum.world.end.modern;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;

/**
 * Whether the End is 26.3's (SPEC-modern-movement.md §76): `modernEnd`, unless Hardcore Ender Expansion makes its own
 * End. Asked only once mods are loaded (the option is read with the early mixins, before Forge knows the mods).
 */
public final class ModernEnd {

	private ModernEnd() {
	}

	public static boolean isActive() {
		return McFlyConfig.modernEnd && !ModsList.HARDCORE_ENDER_EXPANSION.isLoaded();
	}
}
