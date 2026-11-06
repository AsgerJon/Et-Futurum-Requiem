package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.unittas.TasSuite;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;

/**
 * Copper's views for the 26.3 comparison (SPEC-modern-movement.md §87.8), in the order and with the names of 26.3's run
 * ({@code ReferenceRunner.copperViews}): `copper.blocks`, a wall 5 ahead (x 1 to -2 the stages, left to right; y 4
 * up: the block, cut, chiseled, the grate, a lit bulb); `copper.doors`, a door of each stage 4 ahead (meta 1, 26.3's
 * facing south), a closed trapdoor of the same stage on the ground in front of each.
 */
@TasSuite("copper")
public class CopperViewTests extends ComparisonTestCase {

	private static final int Y = 4;
	private static final String[] STAGES = {"", "exposed_", "weathered_", "oxidized_"};

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	private static void put(int x, int y, int z, String modern, int extraMeta) {
		BlockIds.State state = BlockIds.byId("minecraft:" + modern).defaultState();
		world().setBlock(x, y, z, state.block(), state.meta() | extraMeta, 2);
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> {
				player().setGameType(WorldSettings.GameType.CREATIVE);
				player().playerNetServerHandler.setPlayerLocation(0.5, Y, 0.5, 0.0F, -10.0F);
			});
			waitUntilOnClient("at the start", () -> Math.abs(mc().thePlayer.posX - 0.5) < 0.01 && Math.abs(mc().thePlayer.posZ - 0.5) < 0.01, 40);
			look(0.0F, -10.0F);
			onServer(() -> {
				for (int stage = 0; stage < 4; stage++) {
					int x = 1 - stage;
					put(x, Y, 5, stage == 0 ? "copper_block" : STAGES[stage] + "copper", 0);
					put(x, Y + 1, 5, STAGES[stage] + "cut_copper", 0);
					put(x, Y + 2, 5, STAGES[stage] + "chiseled_copper", 0);
					put(x, Y + 3, 5, STAGES[stage] + "copper_grate", 0);
					put(x, Y + 4, 5, STAGES[stage] + "copper_bulb", 4);
				}
			});
			ticks(10);
			comparisonShot("copper.blocks");
			clear();
			look(0.0F, 25.0F);
			onServer(() -> {
				for (int stage = 0; stage < 4; stage++) {
					int x = 3 - 2 * stage;
					BlockIds.State door = BlockIds.byId("minecraft:" + STAGES[stage] + "copper_door").defaultState();
					world().setBlock(x, Y, 4, door.block(), 1, 2);
					world().setBlock(x, Y + 1, 4, door.block(), 8, 2);
					put(x, Y, 2, STAGES[stage] + "copper_trapdoor", 0);
				}
			});
			ticks(10);
			comparisonShot("copper.doors");
			clear();
			look(0.0F, 0.0F);
		});
	}

	/** The views' corner back: air (26.3's run: the same fill). */
	private void clear() {
		onServer(() -> {
			for (int x = -4; x <= 4; x++) {
				for (int y = Y; y <= Y + 5; y++) {
					for (int z = 1; z <= 6; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
		});
	}
}
