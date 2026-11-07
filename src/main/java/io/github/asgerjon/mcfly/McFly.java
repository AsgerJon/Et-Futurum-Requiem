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

	/**
	 * The domain our ids, names, textures and translations live under (spec §79, B8): in this fork the mod is EFR's, so
	 * `etfuturum` (old saves hold `etfuturum:...`); `mcfly` once McFly is its own mod.
	 */
	public static final String DOMAIN = "etfuturum";

	/** The domain MCLib gives the sounds it fetches from the launcher's assets, by the version they're from (spec §79, B5). */
	public static final String MC_ASSETS = "minecraft_1.21.10";

	private static volatile String phase = "not started";

	private McFly() {
	}

	/** A block's or item's unlocalised name under our domain (`etfuturum.powder_snow`). */
	public static String name(String path) {
		return DOMAIN + "." + path;
	}

	/** Where its start-up is: "not started", "pre-init", "init", "post-init", "server started", "server stopped". */
	public static String phase() {
		return phase;
	}

	public static void preInit(FMLPreInitializationEvent event) {
		phase = "pre-init";
		McFlyNetwork.register();
		// spectator mode's game type (spec §83), before anything reads a player's
		io.github.asgerjon.mcfly.spectator.Spectator.init();
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
		// the observer's recipe (spec §86: 26.3's, cobblestone round two redstone and a quartz)
		if (McFlyConfig.enableObservers) {
			cpw.mods.fml.common.registry.GameRegistry.addShapedRecipe(io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER.newItemStack(), "###", "RRQ", "###",
				'#', net.minecraft.init.Blocks.cobblestone, 'R', net.minecraft.init.Items.redstone, 'Q', net.minecraft.init.Items.quartz);
		}
		// the end crystal's recipe (spec §85)
		io.github.asgerjon.mcfly.end.EndCrystalItem.registerRecipe(io.github.asgerjon.mcfly.registry.McFlyItems.END_CRYSTAL.newItemStack());
		// our shaped recipes sorted before vanilla's (spec §89, step D)
		io.github.asgerjon.mcfly.registry.McFlyRecipes.register();
		// copper (spec §87): its recipes, the wax and the axe by hand and dispenser, iron and gold ores' raw drops
		io.github.asgerjon.mcfly.copper.CopperRecipes.register();
		io.github.asgerjon.mcfly.nether.NetherStoneRecipes.register();
		io.github.asgerjon.mcfly.nether.NetheriteRecipes.register();
		io.github.asgerjon.mcfly.smithing.SmithingRecipes.register();
		io.github.asgerjon.mcfly.nether.NetherLightRecipes.register();
		io.github.asgerjon.mcfly.stone.DeepslateRecipes.register();
		// the old stones (spec §89, step D): their recipes, 1.7.10's seamless double slabs dropping the smooth blocks
		io.github.asgerjon.mcfly.stone.StoneRecipes.register();
		io.github.asgerjon.mcfly.stone.StoneEvents.register();
		// the caves (spec §90): amethyst's recipes
		io.github.asgerjon.mcfly.caves.CaveRecipes.register();
		io.github.asgerjon.mcfly.caves.MudBlocks.register();
		io.github.asgerjon.mcfly.gameevent.GameEventHooks.register();
		io.github.asgerjon.mcfly.caves.MossBlocks.register();
		io.github.asgerjon.mcfly.nether.TargetRecipes.register();
		io.github.asgerjon.mcfly.nether.NetherPlantRecipes.register();
		io.github.asgerjon.mcfly.copper.CopperEvents.register();
		// the wood sets (spec §91): the logs' recipes, charcoal and ore names; stripping by the axe
		io.github.asgerjon.mcfly.wood.WoodLogs.register();
		io.github.asgerjon.mcfly.wood.WoodPlanks.register();
		io.github.asgerjon.mcfly.wood.WoodFences.register();
		io.github.asgerjon.mcfly.wood.WoodDoors.register();
		io.github.asgerjon.mcfly.wood.WoodRedstone.register();
		io.github.asgerjon.mcfly.wood.WoodFuel.register();
		io.github.asgerjon.mcfly.wood.Stripping.register();
		// after the stripping table: the hanging signs' recipes take their stripped logs from it
		io.github.asgerjon.mcfly.wood.WoodSigns.register();
		io.github.asgerjon.mcfly.wood.WoodShelves.register();
		io.github.asgerjon.mcfly.wood.Bamboo.register();
		// the boats (spec 91.12): their recipes, ore names, dispensers, vanilla's boat as oak's
		io.github.asgerjon.mcfly.boat.BoatTypes.register();
		// the workstations (spec §92): their crafting
		io.github.asgerjon.mcfly.workstation.Workstations.register();
		// the End's blocks' recipes (spec §84)
		if (McFlyConfig.enableChorusBlocks) {
			io.github.asgerjon.mcfly.end.EndBlocks.registerRecipes();
		}
		if (event.getSide().isClient()) {
			// blocks drawn from 26.3's models (spec §84)
			io.github.asgerjon.mcfly.model.client.ModelBlockRenderer.register();
			// the outline of a multiface block's faces (spec §90.3)
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.model.client.ShapeOutline.INSTANCE);
			if (io.github.asgerjon.mcfly.stone.client.StoneTextures.needed()) {
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.stone.client.StoneTextures.INSTANCE);
			}
			if (io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.isEnabled()) {
				io.github.asgerjon.mcfly.end.client.EndGatewayRenderer.register();
			}
			// every sign's text, both sides (spec 91.8): in place of vanilla's sign renderer
			io.github.asgerjon.mcfly.wood.client.SignRenderer.register();
			// the shelves' items (spec 91.10)
			io.github.asgerjon.mcfly.wood.client.ShelfRenderer.register();
			// the boats (spec 91.12): 26.3's models, a chest boat's inventory key
			if (io.github.asgerjon.mcfly.config.McFlyConfig.enableNewBoats) {
				io.github.asgerjon.mcfly.boat.client.BoatClient.register();
			}
		}
		// the elytra's firework boost and game rule (spec §85)
		if (McFlyConfig.enableElytra) {
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.elytra.ElytraEvents.INSTANCE);
			if (event.getSide().isClient()) {
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.elytra.client.ElytraWings.INSTANCE);
			}
		}
		// tipped arrows from bows, a creeper's lingering cloud (spec §82)
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.potion.PotionEvents.INSTANCE);
		// spectator mode's rules (spec §83)
		if (McFlyConfig.enableSpectatorMode) {
			FMLCommonHandler.instance().bus().register(io.github.asgerjon.mcfly.spectator.Spectator.INSTANCE);
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.spectator.Spectator.INSTANCE);
			if (event.getSide().isClient()) {
				FMLCommonHandler.instance().bus().register(io.github.asgerjon.mcfly.spectator.SpectatorClient.INSTANCE);
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(io.github.asgerjon.mcfly.spectator.SpectatorClient.INSTANCE);
			}
		}
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
