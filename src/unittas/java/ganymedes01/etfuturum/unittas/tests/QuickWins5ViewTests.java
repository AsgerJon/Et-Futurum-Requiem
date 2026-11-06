package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * Quick wins batch 5's views for the 26.3 comparison (SPEC-modern-movement.md §64), in the order and with the names
 * of 26.3's run ({@code ReferenceRunner.quickWins5Views}): an idle skeleton holding a bow (painted), side on, west.
 */
@TasSuite("quick5")
public class QuickWins5ViewTests extends ComparisonTestCase {

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			look(0.0F, 0.0F);
			EntitySkeleton skeleton = onServer(() -> {
				EntitySkeleton made = new EntitySkeleton(world());
				made.tasks.taskEntries.clear();
				made.targetTasks.taskEntries.clear();
				made.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
				// a helmet: no burning in the sun (random in both games, the fire hides parts)
				made.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
				made.setLocationAndAngles(0.5D, 4.0D, 3.5D, 90.0F, 0.0F);
				made.rotationYawHead = made.prevRotationYawHead = 90.0F;
				made.renderYawOffset = made.prevRenderYawOffset = 90.0F;
				world().spawnEntityInWorld(made);
				return made;
			});
			ticks(10);
			onClient(() -> {
				Entity seen = mc().theWorld.getEntityByID(skeleton.getEntityId());
				if (seen instanceof EntitySkeleton) {
					EntitySkeleton client = (EntitySkeleton) seen;
					client.renderYawOffset = client.prevRenderYawOffset = 90.0F;
					client.rotationYawHead = client.prevRotationYawHead = 90.0F;
					client.rotationYaw = client.prevRotationYaw = 90.0F;
				}
			});
			ticks(3);
			comparisonShot("held.skeleton_bow");
			onServer(skeleton::setDead);
			// the spyglass's scope (spec §64.5), at its full size (it eases from half by half the gap a tick)
			if (io.github.asgerjon.mcfly.registry.McFlyItems.SPYGLASS.isEnabled()) {
				setHeld(io.github.asgerjon.mcfly.registry.McFlyItems.SPYGLASS.newItemStack());
				ticks(5);
				keyDown(mc().gameSettings.keyBindUseItem);
				ticks(30);
				comparisonShot("spyglass.scope");
				keyUp(mc().gameSettings.keyBindUseItem);
				ticks(5);
				// held, in third person front; looked through (at the eye); in first person
				cameraView(2);
				ticks(10);
				comparisonShot("spyglass.third_person_front");
				keyDown(mc().gameSettings.keyBindUseItem);
				ticks(15);
				comparisonShot("spyglass.third_person_scoping");
				keyUp(mc().gameSettings.keyBindUseItem);
				cameraView(0);
				ticks(30);
				comparisonShot("spyglass.first_person");
				setHeld(null);
				ticks(5);
			}
		});
	}
}
