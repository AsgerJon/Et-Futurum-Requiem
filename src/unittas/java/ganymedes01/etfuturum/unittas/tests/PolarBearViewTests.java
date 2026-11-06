package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import net.minecraft.entity.passive.EntityAnimal;

/**
 * The polar bear's views for unittas_mcfly-compare.sh (see ComparisonTestCase; SPEC-modern-movement.md §60.4): an adult, a
 * cub and a standing adult, five blocks ahead, side on (facing west), still (no AI). unittas_mcfly-reference takes the same
 * views in 26.3 with these names. Run it with ShieldTests' testScreenshots, which takes the comparison's calibration.
 */
@TasSuite("polarbear")
public class PolarBearViewTests extends ComparisonTestCase {

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			look(0.0F, 0.0F);
			EntityAnimal adult = stillBear(false);
			ticks(10);
			faceWestOnTheClient(adult);
			comparisonShot("polar_bear.adult");
			onServer(() -> {
				try {
					adult.getClass().getMethod("setStanding", boolean.class).invoke(adult, true);
				} catch (ReflectiveOperationException e) {
					fail("the bear has no setStanding: " + e);
				}
			});
			// the stand takes 6 ticks on the client
			ticks(12);
			comparisonShot("polar_bear.standing");
			onServer(adult::setDead);
			EntityAnimal cub = stillBear(true);
			ticks(10);
			faceWestOnTheClient(cub);
			comparisonShot("polar_bear.cub");
		});
	}

	/**
	 * The bear's body on the client turned west too: 1.7.10's client starts a new mob's body at 0 and brings it round to
	 * the head over 10 to 20 still ticks, where 26.3's starts it at the spawn direction (spec §60.7).
	 */
	private void faceWestOnTheClient(EntityAnimal bear) {
		onClient(() -> {
			net.minecraft.entity.Entity seen = mc().theWorld.getEntityByID(bear.getEntityId());
			if (seen instanceof EntityAnimal animal) {
				animal.renderYawOffset = animal.prevRenderYawOffset = 90.0F;
				animal.rotationYawHead = animal.prevRotationYawHead = 90.0F;
				animal.rotationYaw = animal.prevRotationYaw = 90.0F;
			}
		});
		ticks(3);
	}

	/** A bear five ahead, facing west (side on), its AI gone so it stays as placed. */
	private EntityAnimal stillBear(boolean cub) {
		return onServer(() -> {
			EntityAnimal bear = PolarBearTests.newBear();
			if (cub) {
				bear.setGrowingAge(-24000);
			}
			bear.tasks.taskEntries.clear();
			bear.targetTasks.taskEntries.clear();
			bear.setLocationAndAngles(0.5D, 4.0D, 5.5D, 90.0F, 0.0F);
			bear.rotationYawHead = bear.prevRotationYawHead = 90.0F;
			bear.renderYawOffset = bear.prevRenderYawOffset = 90.0F;
			world().spawnEntityInWorld(bear);
			return bear;
		});
	}
}
