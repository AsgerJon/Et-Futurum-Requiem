package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * Powder snow's views for the 26.3 comparison (SPEC-modern-movement.md §62.10), in the order and with the names of
 * 26.3's run ({@code ReferenceRunner.powderSnowViews}): the block, the camera inside it (the fog; for the eye), the
 * frost overlay, the frozen hearts (for the eye: the overlay is over them), the bucket in the inventory.
 */
@TasSuite("powdersnow")
public class PowderSnowViewTests extends ComparisonTestCase {

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	public void testScreenshots() throws Exception {
		Block snow = GameRegistry.findBlock(McFly.DOMAIN, "powder_snow");
		Item bucket = GameRegistry.findItem(McFly.DOMAIN, "powder_snow_bucket");
		assertIsNotNone(snow, "EFR has no powder snow block");
		assertIsNotNone(bucket, "EFR has no powder snow bucket");
		comparisonViews(() -> {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			look(0.0F, 0.0F);
			onServer(() -> {
				world().setBlock(0, 4, 3, snow);
			});
			ticks(10);
			comparisonShot("powder_snow.block");
			onServer(() -> {
				world().setBlockToAir(0, 4, 3);
			});
			// the cauldron, full and at level 1 (spec §62.8): the content is the powder snow texture
			Block cauldron = GameRegistry.findBlock(McFly.DOMAIN, "powder_snow_cauldron");
			assertIsNotNone(cauldron, "EFR has no powder snow cauldron");
			look(0.0F, 30.0F);
			onServer(() -> {
				world().setBlock(0, 4, 2, cauldron, 3, 3);
			});
			ticks(10);
			comparisonShot("powder_snow_cauldron.full");
			// level 1 sits low: one block ahead, looked into at 45 degrees, or the rim hides it
			look(0.0F, 45.0F);
			onServer(() -> {
				world().setBlockToAir(0, 4, 2);
				world().setBlock(0, 4, 1, cauldron, 1, 3);
			});
			ticks(10);
			comparisonShot("powder_snow_cauldron.level1");
			onServer(() -> {
				world().setBlockToAir(0, 4, 1);
			});
			look(0.0F, 0.0F);
			fill(snow);
			ticks(10);
			comparisonShot("powder_snow.inside");
			fill(null);
			onServer(() -> Freezing.setTicksFrozen(player(), 0));
			ticks(5);
			onServer(() -> Freezing.setTicksFrozen(player(), Freezing.TICKS_TO_FREEZE));
			ticks(2);
			comparisonShot("powder_snow.overlay");
			// fully frozen held there: powder snow at the feet (the eyes above it, no fog)
			onServer(() -> {
				world().setBlock(0, 4, 0, snow);
				player().setGameType(WorldSettings.GameType.SURVIVAL);
				Freezing.setTicksFrozen(player(), Freezing.TICKS_TO_FREEZE);
			});
			ticks(4);
			comparisonShot("powder_snow.hearts");
			// the survival inventory (in creative both games open the creative one)
			onServer(() -> {
				world().setBlockToAir(0, 4, 0);
				Freezing.setTicksFrozen(player(), 0);
			});
			ticks(5);
			setHeld(new ItemStack(bucket));
			ticks(5);
			inventoryShot("powder_snow_bucket.inventory");
			setHeld(null);
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
		});
	}

	/** A 3 x 3 x 3 of it round the player at the start (26.3's run: fill -1 ~ -1 1 ~2 1), or air. */
	private void fill(Block block) {
		onServer(() -> {
			for (int x = -1; x <= 1; x++) {
				for (int y = 4; y <= 6; y++) {
					for (int z = -1; z <= 1; z++) {
						if (block == null) {
							world().setBlockToAir(x, y, z);
						} else {
							world().setBlock(x, y, z, block);
						}
					}
				}
			}
		});
	}
}
