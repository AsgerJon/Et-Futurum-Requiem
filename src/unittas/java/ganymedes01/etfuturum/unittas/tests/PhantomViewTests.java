package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.model.ModelPhantom;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityPhantom;
import net.minecraft.init.Blocks;

/**
 * The phantom's views for unittas_mcfly-compare.sh (see ComparisonTestCase; SPEC-modern-movement.md §68.7): side on five
 * ahead at about eye height, and from below (as one is seen in the sky), still, facing west, the wings held at flap
 * time 0 in both games (the live flap depends on the entity's id and age). Its body is painted magenta and its eyes
 * left as they are, so the eyes show as the same gaps in both. A plank roof high out of view keeps the daylight from
 * setting it on fire. unittas_mcfly-reference takes the same views in 26.3 with these names.
 */
@TasSuite("phantom")
public class PhantomViewTests extends ComparisonTestCase {

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	@Override
	protected void setUp() {
		if (!McFlyConfig.enablePhantoms) {
			skipTest("phantoms are off");
		}
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			ModelPhantom.heldFlapTime = Float.NaN;
		});
		onServer(() -> {
			for (EntityPhantom phantom : entitiesNear(EntityPhantom.class, 32.0D)) {
				phantom.setDead();
			}
			world().setBlockToAir(0, 11, 5);
			world().setBlockToAir(0, 15, 0);
		});
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onClient(() -> {
				ModelPhantom.heldFlapTime = 0.0F;
			});
			look(0.0F, 0.0F);
			onServer(() -> world().setBlock(0, 11, 5, Blocks.planks));
			EntityPhantom side = stillPhantom(0.5D, 5.0D, 5.5D);
			ticks(10);
			faceWestOnTheClient(side);
			ticks(7);
			comparisonShot("phantom.side");
			onServer(() -> {
				side.setDead();
				world().setBlockToAir(0, 11, 5);
				world().setBlock(0, 15, 0, Blocks.planks);
			});
			EntityPhantom above = stillPhantom(0.5D, 9.0D, 0.5D);
			look(0.0F, -90.0F);
			ticks(10);
			faceWestOnTheClient(above);
			ticks(7);
			comparisonShot("phantom.below");
			onServer(() -> {
				above.setDead();
				world().setBlockToAir(0, 15, 0);
			});
			look(0.0F, 0.0F);
		});
	}

	/** A phantom here, facing west, still (no goals, no motion), size 0. */
	private EntityPhantom stillPhantom(double x, double y, double z) {
		return onServer(() -> {
			EntityPhantom phantom = new EntityPhantom(world());
			phantom.setStill(true);
			phantom.setLocationAndAngles(x, y, z, 90.0F, 0.0F);
			phantom.rotationYawHead = phantom.prevRotationYawHead = 90.0F;
			phantom.renderYawOffset = phantom.prevRenderYawOffset = 90.0F;
			world().spawnEntityInWorld(phantom);
			return phantom;
		});
	}

	/** The body on the client turned west too, its pitch level (1.7.10's client starts a new mob's body at 0, §60.7). */
	private void faceWestOnTheClient(EntityPhantom phantom) {
		onClient(() -> {
			net.minecraft.entity.Entity seen = mc().theWorld.getEntityByID(phantom.getEntityId());
			if (seen instanceof EntityPhantom client) {
				client.renderYawOffset = client.prevRenderYawOffset = 90.0F;
				client.rotationYawHead = client.prevRotationYawHead = 90.0F;
				client.rotationYaw = client.prevRotationYaw = 90.0F;
				client.rotationPitch = client.prevRotationPitch = 0.0F;
			}
		});
		ticks(3);
	}
}
