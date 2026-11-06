package io.github.asgerjon.mcfly.stone;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

/**
 * The old stones' world side (SPEC-modern-movement.md §89, step D): 1.7.10's seamless double stone slabs (metas 8, 9 and
 * 15) are 26.3's smooth stone, smooth sandstone and smooth quartz (the command layer reads them so): broken, they drop
 * those, not two slabs.
 */
public final class StoneEvents {

	public static final StoneEvents INSTANCE = new StoneEvents();

	private StoneEvents() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	@SubscribeEvent
	public void onDrops(BlockEvent.HarvestDropsEvent event) {
		if (event.block != Blocks.double_stone_slab) {
			return;
		}
		McFlyBlocks smooth = event.blockMetadata == 8 ? McFlyBlocks.SMOOTH_STONE : event.blockMetadata == 9 ? McFlyBlocks.SMOOTH_SANDSTONE
			: event.blockMetadata == 15 ? McFlyBlocks.SMOOTH_QUARTZ : null;
		if (smooth != null && smooth.isEnabled()) {
			event.drops.clear();
			event.drops.add(smooth.newItemStack());
		}
	}
}
