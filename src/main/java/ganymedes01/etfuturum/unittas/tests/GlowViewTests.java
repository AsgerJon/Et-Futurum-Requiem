package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * Spec §67's views for the 26.3 comparison, in the order and with the names of 26.3's run
 * ({@code ReferenceRunner.glowViews}): an iron sword (painted) in a frame on the floor, on a ceiling, and on a wall
 * turned one eighth. The player at the spawn (feet y 4), facing south.
 */
public class GlowViewTests extends ComparisonTestCase {

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	public void testScreenshots() throws Exception {
		if (!McFlyConfig.modernItemFrames) {
			skipTest("modernItemFrames is off");
		}
		comparisonViews(() -> {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			// on the floor: the ground block in front, the frame above it
			look(0.0F, 50.0F);
			EntityItemFrame floor = frame(0, 3, 2, 4, 0);
			ticks(10);
			comparisonShot("frame.floor_sword");
			onServer(floor::setDead);
			// on a ceiling: stone above head height, the frame under it
			onServer(() -> world().setBlock(0, 6, 2, Blocks.stone));
			look(0.0F, -40.0F);
			EntityItemFrame ceiling = frame(0, 6, 2, 5, 0);
			ticks(10);
			comparisonShot("frame.ceiling_sword");
			onServer(() -> {
				ceiling.setDead();
				world().setBlockToAir(0, 6, 2);
			});
			// on a wall at eye height, facing the player, turned one eighth
			onServer(() -> world().setBlock(0, 5, 3, Blocks.stone));
			look(0.0F, 0.0F);
			EntityItemFrame wall = frame(0, 5, 3, 2, 1);
			ticks(10);
			comparisonShot("frame.wall_sword_45");
			onServer(() -> {
				wall.setDead();
				world().setBlockToAir(0, 5, 3);
			});
			ticks(5);
		});
	}

	/** A frame hung on the block (x, y, z), facing that way (1.7.10's numbers, 4 floor, 5 ceiling), an iron sword in it, turned. */
	private EntityItemFrame frame(int x, int y, int z, int facing, int turn) {
		return onServer(() -> {
			EntityItemFrame frame = new EntityItemFrame(world(), x, y, z, facing);
			world().spawnEntityInWorld(frame);
			frame.setDisplayedItem(new ItemStack(Items.iron_sword));
			frame.setItemRotation(turn);
			return frame;
		});
	}
}
