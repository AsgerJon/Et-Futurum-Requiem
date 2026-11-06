package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;

/**
 * The chain's and lights' view for the 26.3 comparison (SPEC-modern-movement.md §88.9), as 26.3's run
 * ({@code ReferenceRunner.netherLightViews}): `nether.lights`, looked at from 25 down: chains along y, x and z at z 3 (x
 * 2, 0, -2), a standing and a hanging lantern at z 5 (x 3, 1; the hanging one under stone), a standing soul lantern (x
 * -1) and one hanging from a chain (x -3); a soul torch on the floor (0, z 2) and one on the stone's south face (1, z 4).
 */
@TasSuite("netherlights")
public class NetherLightViewTests extends ComparisonTestCase {

	private static final int Y = 4;

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	/** As 26.3's run does it: the modern /setblock, by the command layer. */
	private void put(int x, int y, int z, String modern) {
		assertNoError(command("setblock " + x + " " + y + " " + z + " minecraft:" + modern));
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> {
				player().setGameType(WorldSettings.GameType.CREATIVE);
				player().playerNetServerHandler.setPlayerLocation(0.5, Y, 0.5, 0.0F, 25.0F);
			});
			waitUntilOnClient("at the start", () -> Math.abs(mc().thePlayer.posX - 0.5) < 0.01 && Math.abs(mc().thePlayer.posZ - 0.5) < 0.01, 40);
			look(0.0F, 25.0F);
			{
				put(2, Y, 3, "iron_chain[axis=y]");
				put(0, Y + 1, 3, "iron_chain[axis=x]");
				put(-2, Y + 1, 3, "iron_chain[axis=z]");
				put(3, Y, 5, "lantern[hanging=false]");
				put(1, Y + 2, 5, "stone");
				put(1, Y + 1, 5, "lantern[hanging=true]");
				put(-1, Y, 5, "soul_lantern[hanging=false]");
				put(-3, Y + 2, 5, "iron_chain[axis=y]");
				put(-3, Y + 1, 5, "soul_lantern[hanging=true]");
				put(0, Y, 2, "soul_torch");
				put(1, Y, 4, "stone");
				put(1, Y, 3, "soul_wall_torch[facing=north]");
			}
			ticks(10);
			comparisonShot("nether.lights");
			clear();
			look(0.0F, 0.0F);
		});
	}

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
