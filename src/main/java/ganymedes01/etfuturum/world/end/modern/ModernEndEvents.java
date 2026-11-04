package ganymedes01.etfuturum.world.end.modern;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** The dragon fight ticks at the end of the End's world tick, as 26.3's ServerLevel ticks it (spec §76.6). */
public final class ModernEndEvents {

	public static final ModernEndEvents INSTANCE = new ModernEndEvents();

	private ModernEndEvents() {
	}

	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase == TickEvent.Phase.END && !event.world.isRemote && event.world.provider instanceof WorldProviderModernEnd) {
			EnderDragonFight.of(event.world).tick();
		}
	}
}
