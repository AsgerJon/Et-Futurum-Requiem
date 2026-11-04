package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.handlers.ClientEventHandler;
import ganymedes01.etfuturum.core.utils.EndCredits;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Teleporter;
import org.lwjgl.input.Keyboard;

/**
 * Travelling between dimensions (SPEC-modern-movement.md §53.1, §53.2): the main hand survives it, and the End's exit
 * shows the credits only once. The runner brings the player back to the overworld after each test.
 */
public class EndTests extends TasTestCase {

	/** The End is made on the first trip there. */
	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			McFlyConfig.mainHandLeft = false;
			ClientEventHandler.sendPlayerSettings();
		});
	}

	/** The user's report: arriving in the End, the hands were back to right-handed. */
	public void testMainHandAfterTheEnd() {
		leftHanded();
		toTheEnd();
		ticks(5);
		assertTrue(onServer(() -> Offhand.isLeftHanded(player())), "left-handed on the server in the End");
		assertTrue(onClient(() -> Offhand.isLeftHanded(mc().thePlayer)), "left-handed on the player's own client in the End");
	}

	/** The same through the Nether (no portal search: the player is put straight in). */
	public void testMainHandAfterTheNether() {
		leftHanded();
		onServer(() -> {
			MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(player(), -1, new Teleporter(MinecraftServer.getServer().worldServerForDimension(-1)) {
				@Override
				public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
				}
			});
		});
		waitUntilOnClient("the Nether", () -> mc().thePlayer.dimension == -1 && !(mc().currentScreen instanceof GuiDownloadTerrain), 200);
		ticks(5);
		assertTrue(onClient(() -> Offhand.isLeftHanded(mc().thePlayer)), "left-handed on the player's own client in the Nether");
	}

	/** Modern's seenCredits: the credits the first time out of the End, straight to the spawn point after that. */
	public void testCreditsOnlyOnce() {
		onServer(() -> EndCredits.setSeen(player(), false));
		toTheEnd();
		leaveTheEnd();
		// the credits pause the game, so they are closed from the client: Escape asks to respawn
		waitUntilOnClient("the credits", () -> mc().currentScreen instanceof GuiWinGame, 100);
		onClient(() -> keyTyped(mc().currentScreen, '\0', Keyboard.KEY_ESCAPE));
		waitUntilOnClient("back in the overworld", () -> mc().thePlayer.dimension == 0 && mc().currentScreen == null, 200);
		assertTrue(onServer(() -> EndCredits.hasSeen(player())), "the respawned player remembers the credits");

		toTheEnd();
		leaveTheEnd();
		for (int tick = 0; ; tick++) {
			assertFalse(onClient(() -> mc().currentScreen instanceof GuiWinGame), "the credits a second time");
			if (onClient(() -> mc().thePlayer.dimension == 0 && mc().currentScreen == null)) {
				break;
			}
			if (tick > 200) {
				fail("not back in the overworld after 200 ticks");
			}
			ticks(1);
		}
		assertTrue(onServer(() -> player().dimension == 0 && EndCredits.hasSeen(player())), "in the overworld, credits still seen");
	}

	private void leftHanded() {
		onClient(() -> {
			McFlyConfig.mainHandLeft = true;
			ClientEventHandler.sendPlayerSettings();
		});
		waitUntil("the server knows the main hand", () -> Offhand.isLeftHanded(player()), 20);
		waitUntilOnClient("the client shows the main hand", () -> Offhand.isLeftHanded(mc().thePlayer), 20);
	}

	/** Into the End as through its portal: onto the obsidian platform. */
	private void toTheEnd() {
		onServer(() -> player().travelToDimension(1));
		waitUntilOnClient("the End", () -> mc().thePlayer.dimension == 1 && !(mc().currentScreen instanceof GuiDownloadTerrain), 200);
	}

	/** Out of the End as through its exit portal: 1.7.10 calls the same method there. */
	private void leaveTheEnd() {
		onServer(() -> player().travelToDimension(1));
	}
}
