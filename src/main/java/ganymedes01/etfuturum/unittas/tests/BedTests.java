package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.client.gui.OverlayMessageHud;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;

/**
 * Using a bed by day (SPEC-modern-movement.md §53.3): out of reach it says so above the hotbar; in reach it sets the
 * spawn point and says so in the chat, then says above the hotbar that sleeping has to wait. The test world is always
 * at noon. The bed is two blocks east of the start, its foot north of its head.
 */
public class BedTests extends TasTestCase {

	private static final int X = 3, Y = 4, FOOT_Z = 0, HEAD_Z = 1;
	private static final String NO_SLEEP = "You can sleep only at night or during thunderstorms";
	private static final String TOO_FAR = "You may not rest now; the bed is too far away";

	@Override
	protected void setUp() {
		onServer(() -> {
			world().setBlock(X, Y, FOOT_Z, Blocks.bed, 0, 3);
			world().setBlock(X, Y, HEAD_Z, Blocks.bed, 8, 3);
		});
		onClient(OverlayMessageHud::clear);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setBlock(X, Y, HEAD_Z, Blocks.air, 0, 2);
			world().setBlock(X, Y, FOOT_Z, Blocks.air, 0, 2);
		});
	}

	public void testSpawnSetByDay() {
		assertTrue(onServer(() -> world().isDaytime()), "it is day");
		int mark = chatMark();
		// the foot: 1.7.10 goes to the head, which is where the spawn point goes
		useBed(FOOT_Z);
		assertAboveTheHotbar(NO_SLEEP);
		String chat = String.join("\n", chatSince(mark));
		assertIn("Respawn point set", chat);
		assertNotIn("sleep", chat);
		assertEqual(new ChunkCoordinates(X, Y, HEAD_Z), onServer(() -> player().getBedLocation(0)));
		assertFalse(onServer(() -> player().isSpawnForced(0)), "a bed spawn is checked for the bed on respawning");
		assertFalse(onServer(() -> player().isPlayerSleeping()), "asleep by day");

		// the same bed again: the spot didn't change, so nothing about the spawn
		onClient(OverlayMessageHud::clear);
		mark = chatMark();
		useBed(HEAD_Z);
		assertAboveTheHotbar(NO_SLEEP);
		assertNotIn("Respawn point set", String.join("\n", chatSince(mark)));
	}

	/** Out of reach (both halves' middles more than 3 across), modern says so before anything about the time. */
	public void testTooFarAway() {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(X + 5.5, Y, HEAD_Z + 0.5, 90.0F, 0.0F));
		ticks(2);
		int mark = chatMark();
		useBed(HEAD_Z);
		assertAboveTheHotbar(TOO_FAR);
		screenshot("bed.too_far_away");
		assertEqual("", String.join("\n", chatSince(mark)));
		assertIsNone(onServer(() -> player().getBedLocation(0)));
	}

	/**
	 * Modern measures from either half's middle: 2.9 from the foot's is in reach, though 3.4 from the head's corner
	 * (1.7.10's measure) was not.
	 */
	public void testReachFromTheFoot() {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(X + 0.5, Y, FOOT_Z + 0.5 - 2.9, 0.0F, 0.0F));
		ticks(2);
		int mark = chatMark();
		useBed(FOOT_Z);
		assertAboveTheHotbar(NO_SLEEP);
		assertIn("Respawn point set", String.join("\n", chatSince(mark)));
		assertEqual(new ChunkCoordinates(X, Y, HEAD_Z), onServer(() -> player().getBedLocation(0)));
	}

	/** A right click on the bed block, as the server takes it from a player's click. */
	private void useBed(int z) {
		onServer(() -> world().getBlock(X, Y, z).onBlockActivated(world(), X, Y, z, player(), 1, 0.5F, 0.5F, 0.5F));
		ticks(3);
	}

	private void assertAboveTheHotbar(String text) {
		waitUntilOnClient("'" + text + "' above the hotbar", () -> text.equals(OverlayMessageHud.shown()), 10);
	}
}
