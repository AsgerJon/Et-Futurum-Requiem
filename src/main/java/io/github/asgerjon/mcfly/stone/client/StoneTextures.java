package io.github.asgerjon.mcfly.stone.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraftforge.client.event.TextureStitchEvent;

/**
 * The granite, diorite and andesite block's textures when the block itself is left out (`disableBaseBountifulStonesOnly`,
 * SPEC-modern-movement.md §89, step D): its slabs, stairs and walls, still in, look like it, and 1.7.10 gives textures
 * only to registered blocks.
 */
public final class StoneTextures {

	public static final StoneTextures INSTANCE = new StoneTextures();

	private StoneTextures() {
	}

	/** Whether it's needed: the stones on, their block left out. */
	public static boolean needed() {
		return McFlyConfig.enableStones && !McFlyBlocks.STONE.isEnabled();
	}

	@SubscribeEvent
	public void registerIcons(TextureStitchEvent.Pre event) {
		if (event.map.getTextureType() == 0) {
			McFlyBlocks.STONE.get().registerBlockIcons(event.map);
		}
	}
}
