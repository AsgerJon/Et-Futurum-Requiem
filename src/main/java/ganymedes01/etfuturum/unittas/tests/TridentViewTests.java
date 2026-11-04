package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import ganymedes01.etfuturum.trident.Riptide;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;

/**
 * The trident's views for unittas_mcfly-compare.sh (see ComparisonTestCase; SPEC-modern-movement.md §58): in hand, charging,
 * in the inventory, thrown into the ground, Riptide's spin, and the lightning rod in each facing and in hand.
 * unittas_mcfly-reference takes the same views in 26.3, in this order and with these names. Run it with ShieldTests'
 * testScreenshots, which takes the comparison's calibration.
 */
public class TridentViewTests extends ComparisonTestCase {

	/** The lightning rod's facings in 26.3's names, in the order of their metadata (ForgeDirection's). */
	private static final String[] FACINGS = {"down", "up", "north", "south", "west", "east"};

	/** In survival, as 26.3's run: 1.7.10's inventory screen turns into the creative one in creative. */
	@Override
	protected void setUp() {
		if (!McFlyItems.TRIDENT.isEnabled() || !McFlyBlocks.LIGHTNING_ROD.isEnabled() || !McFlyBlocks.WAXED_LIGHTNING_ROD.isEnabled()) {
			skipTest("the trident or the lightning rod is turned off");
		}
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		ticks(2);
	}

	@Override
	protected int timeoutSeconds() {
		return 180;
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			int feet = onServer(() -> MathHelper.floor_double(player().boundingBox.minY));
			look(0.0F, 0.0F);
			holdTrident();
			// the hand is lowered after a switch until the attack cooldown is full again (18 ticks for the trident): past it
			ticks(30);
			comparisonShot("trident.first_person");
			keyDown(mc().gameSettings.keyBindUseItem);
			// past the 10 ticks of a full charge
			ticks(15);
			comparisonShot("trident.first_person_charging");
			cameraView(1);
			comparisonShot("trident.third_person_back_charging");
			cameraView(2);
			comparisonShot("trident.third_person_front_charging");
			// let go without throwing: the hand empties first
			setHeld(null);
			ticks(3);
			keyUp(mc().gameSettings.keyBindUseItem);
			holdTrident();
			ticks(10);
			comparisonShot("trident.third_person_front");
			cameraView(1);
			comparisonShot("trident.third_person_back");
			cameraView(0);
			inventoryShot("trident.inventory");
			setHeld(null);

			// dropped straight down into the grass ahead, already facing the way it turns toward (straight down, yaw 0):
			// nothing to turn on the server or either client, and no sideways pull-back as it sticks (any sideways motion,
			// too small to reach a client, made the clients and the servers stick it apart)
			onServer(() -> {
				EntityThrownTrident thrown = new EntityThrownTrident(world());
				thrown.setItem(new ItemStack(trident()));
				thrown.setPosition(0.5D, feet + 0.5D, 4.5D);
				thrown.rotationYaw = thrown.prevRotationYaw = 0.0F;
				thrown.rotationPitch = thrown.prevRotationPitch = -90.0F;
				thrown.motionX = 0.0D;
				thrown.motionY = -1.0D;
				thrown.motionZ = 0.0D;
				world().spawnEntityInWorld(thrown);
			});
			ticks(10);
			comparisonShot("trident.in_ground");
			removeThrownTridents();

			// Riptide's spin, standing, seen from behind (it turns with time: for the eye, not measured)
			holdTrident();
			onServer(() -> Riptide.start(player()));
			cameraView(1);
			ticks(1);
			comparisonShot("riptide.spin");
			cameraView(0);
			waitUntil("the spin over", () -> !Riptide.isSpinning(player()), 30);
			setHeld(null);

			// the lightning rod, one block aside and three ahead, in each facing; waxed (the same model and texture), as
			// an unwaxed one may weather on a random tick into another block mid-view (seen once, 2026-11-02)
			for (int meta = 0; meta < FACINGS.length; meta++) {
				int facing = meta;
				onServer(() -> world().setBlock(1, feet + 1, 3, McFlyBlocks.WAXED_LIGHTNING_ROD.get(), facing, 3));
				ticks(5);
				comparisonShot("lightning_rod." + FACINGS[facing]);
				onServer(() -> world().setBlockToAir(1, feet + 1, 3));
			}
			// and in hand
			setHeld(new ItemStack(McFlyBlocks.LIGHTNING_ROD.get()));
			ticks(30);
			comparisonShot("lightning_rod.first_person");
			// from the front: from behind, the body hides it in both versions
			cameraView(2);
			comparisonShot("lightning_rod.third_person_front");
			cameraView(0);
			setHeld(null);
		});
	}

	private static Item trident() {
		return McFlyItems.TRIDENT.get();
	}

	private void holdTrident() {
		setHeld(new ItemStack(trident()));
		waitUntilOnClient("the trident in hand", () -> mc().thePlayer.getHeldItem() != null && mc().thePlayer.getHeldItem().getItem() == trident(), 40);
	}

	private void removeThrownTridents() {
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityThrownTrident) {
					((Entity) entity).setDead();
				}
			}
		});
		ticks(2);
	}
}
