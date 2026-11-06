package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.unittas.TasSuite;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;

/**
 * The Nether stones' views for the 26.3 comparison (SPEC-modern-movement.md §88.9), in the order and with the names of
 * 26.3's run ({@code ReferenceRunner.netherStoneViews}): `nether.stones`, a wall 5 ahead (x 2 to 0, y 4 up: blackstone's
 * five; gilded blackstone, basalt, polished and smooth basalt, nether gold ore; crying obsidian, red, cracked and
 * chiseled nether bricks, quartz bricks); `nether.shapes`, looked at from 25 down: slabs at z 2, stairs at z 4, walls at z
 * 6 (x 3, 1, -1, -3, each alone, so 26.3's `setblock` state is the one 1.7.10 works out), the plate at x 0 z 2, the
 * button on polished blackstone at x 0 z 4.
 */
@TasSuite("netherstones")
public class NetherStoneViewTests extends ComparisonTestCase {

	private static final int Y = 4;
	static final String[][] WALL = {
		{"blackstone", "polished_blackstone", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks", "chiseled_polished_blackstone"},
		{"gilded_blackstone", "basalt", "polished_basalt", "smooth_basalt", "nether_gold_ore"},
		{"crying_obsidian", "red_nether_bricks", "cracked_nether_bricks", "chiseled_nether_bricks", "quartz_bricks"}};
	static final String[] SLABS = {"blackstone_slab", "polished_blackstone_slab", "polished_blackstone_brick_slab", "red_nether_brick_slab"};
	static final String[] STAIRS = {"blackstone_stairs", "polished_blackstone_stairs", "polished_blackstone_brick_stairs", "red_nether_brick_stairs"};
	static final String[] WALLS = {"blackstone_wall", "polished_blackstone_wall", "polished_blackstone_brick_wall", "red_nether_brick_wall"};

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	private static void put(int x, int y, int z, String modern) {
		BlockIds.State state = BlockIds.byId("minecraft:" + modern).defaultState();
		world().setBlock(x, y, z, state.block(), state.meta(), 2);
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
				for (int column = 0; column < WALL.length; column++) {
					for (int row = 0; row < WALL[column].length; row++) {
						put(2 - column, Y + row, 5, WALL[column][row]);
					}
				}
			});
			ticks(10);
			comparisonShot("nether.stones");
			clear();
			look(0.0F, 25.0F);
			onServer(() -> {
				for (int i = 0; i < 4; i++) {
					int x = 3 - 2 * i;
					put(x, Y, 2, SLABS[i]);
					put(x, Y, 4, STAIRS[i]);
					put(x, Y, 6, WALLS[i]);
				}
				put(0, Y, 2, "polished_blackstone_pressure_plate");
				put(0, Y, 4, "polished_blackstone");
				BlockIds.State button = BlockIds.byId("minecraft:polished_blackstone_button").defaultState();
				// 26.3's [face=floor,facing=north]: 1.7.10's floor button (the fork's meta 5)
				world().setBlock(0, Y + 1, 4, button.block(), 5, 2);
			});
			ticks(10);
			comparisonShot("nether.shapes");
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
