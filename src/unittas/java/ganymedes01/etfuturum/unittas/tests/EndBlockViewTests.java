package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldSettings;

/**
 * The End's blocks' views for the 26.3 comparison (SPEC-modern-movement.md §84.2), in the order and with the names of
 * 26.3's run ({@code ReferenceRunner.endBlockViews}), each on the ground 3 ahead of the start, looked at from 15 down:
 * purpur (the block, the pillar each way, the stairs, both slabs), end rods each way and a grown chorus plant (two
 * flowers, one dead), and end stone brick walls (two ends with posts, a middle with tall sides under a block, a lone
 * post). Metas as 1.7.10's: the pillar 0 up, 4 east-west, 8 north-south; the stairs 1 rising west; the slab 8 the top;
 * the rod's facing 0 down to 5 east; the flower's age.
 */
@TasSuite("endblocks")
public class EndBlockViewTests extends ComparisonTestCase {

	private static final int Y = 4;

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject("etfuturum:" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no etfuturum:" + name);
		return (Block) block;
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> {
				player().setGameType(WorldSettings.GameType.CREATIVE);
				player().playerNetServerHandler.setPlayerLocation(0.5, Y, 0.5, 0.0F, 15.0F);
			});
			waitUntilOnClient("at the start", () -> Math.abs(mc().thePlayer.posX - 0.5) < 0.01 && Math.abs(mc().thePlayer.posZ - 0.5) < 0.01, 40);
			look(0.0F, 15.0F);
			onServer(() -> {
				world().setBlock(-2, Y, 3, block("purpur_block"), 0, 3);
				world().setBlock(-1, Y, 3, block("purpur_pillar"), 0, 3);
				world().setBlock(-1, Y + 1, 3, block("purpur_pillar"), 8, 3);
				world().setBlock(0, Y, 3, block("purpur_pillar"), 4, 3);
				world().setBlock(1, Y, 3, block("purpur_stairs"), 1, 3);
				world().setBlock(2, Y, 3, block("purpur_slab"), 0, 3);
				world().setBlock(2, Y + 1, 3, block("purpur_slab"), 8, 3);
			});
			ticks(10);
			comparisonShot("endblocks.purpur");
			clear();

			onServer(() -> {
				Block rod = block("end_rod");
				world().setBlock(-3, Y, 3, rod, 1, 3);
				world().setBlock(-2, Y + 1, 3, rod, 0, 3);
				world().setBlock(-1, Y, 3, rod, 5, 3);
				world().setBlock(-2, Y, 4, rod, 3, 3);
				Block plant = block("chorus_plant");
				Block flower = block("chorus_flower");
				// a stem of two, a branch off its top (a stem block with a plant beside it has nothing above, by 26.3's
				// canSurvive), a flower on the branch, one beside the stem, a dead one at the branch's end
				world().setBlock(1, Y - 1, 3, Blocks.end_stone, 0, 3);
				world().setBlock(1, Y, 3, plant, 0, 3);
				world().setBlock(1, Y + 1, 3, plant, 0, 3);
				world().setBlock(2, Y + 1, 3, plant, 0, 3);
				world().setBlock(2, Y + 2, 3, flower, 0, 3);
				world().setBlock(0, Y + 1, 3, flower, 0, 3);
				world().setBlock(3, Y + 1, 3, flower, 5, 3);
			});
			ticks(10);
			comparisonShot("endblocks.rods_chorus");
			clear();

			onServer(() -> {
				Block wall = block("end_brick_wall");
				world().setBlock(-2, Y, 3, wall, 0, 3);
				world().setBlock(-1, Y, 3, wall, 0, 3);
				world().setBlock(0, Y, 3, wall, 0, 3);
				world().setBlock(-1, Y + 1, 3, block("end_bricks"), 0, 3);
				world().setBlock(2, Y, 3, wall, 0, 3);
			});
			ticks(10);
			comparisonShot("endblocks.walls");
			clear();
			look(0.0F, 0.0F);
		});
	}

	/** The views' corner back as the flat world has it: air over grass (26.3's run: the same two fills). */
	private void clear() {
		onServer(() -> {
			for (int x = -3; x <= 3; x++) {
				for (int z = 2; z <= 5; z++) {
					for (int y = Y; y <= Y + 3; y++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
					world().setBlock(x, Y - 1, z, Blocks.grass, 0, 2);
				}
			}
		});
	}
}
