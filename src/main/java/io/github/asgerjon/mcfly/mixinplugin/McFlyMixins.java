package io.github.asgerjon.mcfly.mixinplugin;

import java.util.ArrayList;
import java.util.List;

/**
 * McFly's early mixins (SPEC-modern-movement.md §79, B3), by name relative to `io.github.asgerjon.mcfly.mixins.early`:
 * filled by the coremod as it chooses them by our options (EFR's coremod in this fork, McFly's own once it has its own
 * repository), then handed to Mixin by our mixin config's plugin.
 */
public final class McFlyMixins {

	public static final List<String> EARLY = new ArrayList<>();

	/** Our mixin config, added once the list is chosen. */
	public static final String EARLY_CONFIG = "mixins.mcfly.early.json";

	private McFlyMixins() {
	}
}
