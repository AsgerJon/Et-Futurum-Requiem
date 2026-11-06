package io.github.asgerjon.mcfly.registry;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Our blocks' render types (SPEC-modern-movement.md §79, B8): each a render id of its own, which its block returns and
 * its renderer is registered under on the client.
 */
public final class McFlyRenderIds {

	public static final int KELP = RenderingRegistry.getNextAvailableRenderId();
	public static final int POWDER_SNOW = RenderingRegistry.getNextAvailableRenderId();
	public static final int POWDER_SNOW_CAULDRON = RenderingRegistry.getNextAvailableRenderId();
	/** Blocks drawn from 26.3's models (spec §84): `ModelledBlock`, `ModelBlockRenderer`. */
	public static final int MODEL = RenderingRegistry.getNextAvailableRenderId();
	/** The same, the item a flat sprite of the block's icon (spec §88.7: the vines). */
	public static final int MODEL_FLAT_ITEM = RenderingRegistry.getNextAvailableRenderId();

	private McFlyRenderIds() {
	}
}
