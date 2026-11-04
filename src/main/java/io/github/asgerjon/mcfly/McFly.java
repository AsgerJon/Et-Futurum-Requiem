package io.github.asgerjon.mcfly;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * McFly's base (SPEC-modern-movement.md §79, B1): our own start-up, called from EFR's mod class in this fork, and McFly's
 * own `@Mod` once it has its own repository. Its options are read before it (by the coremod, before mixins are
 * chosen); then its phases in order: pre-init (our channel, blocks and items), init, post-init, and the server started
 * and stopped. Our entities are registered from EFR's list in init (`McFlyEntities`), so their spawn eggs keep their
 * ids.
 */
public final class McFly {

	public static final String NAME = "McFly";
	public static final Logger LOG = LogManager.getLogger(NAME);

	private static volatile String phase = "not started";

	private McFly() {
	}

	/** Where its start-up is: "not started", "pre-init", "init", "post-init", "server started", "server stopped". */
	public static String phase() {
		return phase;
	}

	public static void preInit(FMLPreInitializationEvent event) {
		phase = "pre-init";
		McFlyNetwork.register();
		// our blocks and items (spec §79 B6), under the ids the mod being loaded gives them (EFR's in this fork)
		McFlyBlocks.register();
		McFlyItems.register();
		if (event.getSide().isClient()) {
			// the config screen's changes to our file (its closing event)
			FMLCommonHandler.instance().bus().register(McFlyConfig.SCREEN_LISTENER);
		}
	}

	public static void init(FMLInitializationEvent event) {
		phase = "init";
	}

	public static void postInit(FMLPostInitializationEvent event) {
		phase = "post-init";
	}

	public static void serverStarted(FMLServerStartedEvent event) {
		phase = "server started";
	}

	public static void serverStopped(FMLServerStoppedEvent event) {
		phase = "server stopped";
	}
}
