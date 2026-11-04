package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

/**
 * Quick wins batch 4's views for the 26.3 comparison (SPEC-modern-movement.md §63.5, §63.6), in the order and with the
 * names of 26.3's run ({@code ReferenceRunner.quickWins4Views}): the cauldron (its textures, and the lava, painted
 * magenta), and items held in third person (the bucket, iron sword, fishing rod, bow and stone painted).
 */
public class QuickWins4ViewTests extends ComparisonTestCase {

	@Override
	protected int timeoutSeconds() {
		return 150;
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			// the cauldron: from above (30 degrees), level, and with lava
			look(0.0F, 30.0F);
			onServer(() -> {
				world().setBlock(0, 4, 2, Blocks.cauldron, 0, 3);
			});
			ticks(10);
			comparisonShot("cauldron.empty");
			look(0.0F, 0.0F);
			ticks(3);
			comparisonShot("cauldron.side");
			look(0.0F, 30.0F);
			onServer(() -> {
				world().setBlock(0, 4, 2, ModBlocks.LAVA_CAULDRON.get(), 0, 3);
			});
			ticks(10);
			comparisonShot("cauldron.lava");
			onServer(() -> {
				world().setBlockToAir(0, 4, 2);
			});
			look(0.0F, 0.0F);
			// items in the main hand, third person (2: front, 1: back)
			held("held.flat", new ItemStack(Items.bucket), true);
			held("held.sword", new ItemStack(Items.iron_sword), true);
			held("held.rod", new ItemStack(Items.fishing_rod), false);
			held("held.bow", new ItemStack(Items.bow), false);
			held("held.block", new ItemStack(Blocks.stone), false);
			// the off-hand
			setHeld(null);
			onServer(() -> {
				Offhand.setOffhand(player(), new ItemStack(Items.bucket));
				player().inventoryContainer.detectAndSendChanges();
			});
			cameraView(2);
			ticks(10);
			comparisonShot("held.offhand_flat.front");
			cameraView(0);
			onServer(() -> {
				Offhand.setOffhand(player(), null);
				player().inventoryContainer.detectAndSendChanges();
			});
			// a zombie with a sword, side on, west
			EntityZombie zombie = onServer(() -> {
				EntityZombie made = new EntityZombie(world());
				made.tasks.taskEntries.clear();
				made.targetTasks.taskEntries.clear();
				made.setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
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
				net.minecraft.entity.Entity seen = mc().theWorld.getEntityByID(zombie.getEntityId());
				if (seen instanceof EntityZombie) {
					EntityZombie client = (EntityZombie) seen;
					client.renderYawOffset = client.prevRenderYawOffset = 90.0F;
					client.rotationYawHead = client.prevRotationYawHead = 90.0F;
					client.rotationYaw = client.prevRotationYaw = 90.0F;
				}
			});
			ticks(3);
			comparisonShot("held.zombie_sword");
			onServer(zombie::setDead);
		});
	}

	/** The stack in the main hand: third person front, and back too if asked. */
	private void held(String name, ItemStack stack, boolean back) {
		setHeld(stack);
		cameraView(2);
		ticks(10);
		comparisonShot(name + ".front");
		if (back) {
			cameraView(1);
			ticks(5);
			comparisonShot(name + ".back");
		}
		cameraView(0);
		setHeld(null);
		ticks(3);
	}
}
