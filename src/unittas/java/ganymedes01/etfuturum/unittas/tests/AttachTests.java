package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

/**
 * Buttons and levers on any full face (SPEC-modern-movement.md §53.4), placed with a real right click: the wall is the
 * block two south of the start at eye height (its north face looks at the player), the floor the ground block there.
 */
@TasSuite("quick")
public class AttachTests extends TasTestCase {

	private static final int WALL_X = 0, WALL_Y = 5, WALL_Z = 2;

	@Override
	protected void setUp() {
		onServer(() -> {
			for (int x = -1; x <= 1; x++) {
				for (int y = 4; y <= 6; y++) {
					for (int z = 1; z <= 3; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			world().setBlock(WALL_X, 3, WALL_Z, Blocks.grass, 0, 2);
		});
	}

	public void testButtonOnGlass() {
		assertTrue(placeOnWall(Blocks.glass, 0, Blocks.stone_button), "no button on the glass");
	}

	public void testLeverOnStainedGlass() {
		assertTrue(placeOnWall(Blocks.stained_glass, 14, Blocks.lever), "no lever on the stained glass");
	}

	public void testButtonOnIce() {
		assertTrue(placeOnWall(Blocks.ice, 0, Blocks.wooden_button), "no button on the ice");
	}

	/** Modern's leaves support nothing; 1.7.10 agreed. */
	public void testNothingOnLeaves() {
		assertFalse(placeOnWall(Blocks.leaves, 4, Blocks.stone_button), "a button on leaves");
	}

	/** Still on stone, as always. */
	public void testButtonOnStone() {
		assertTrue(placeOnWall(Blocks.stone, 0, Blocks.stone_button), "no button on stone");
	}

	/** Without its glass the button drops as an item. */
	public void testButtonDropsWithTheGlass() {
		assertTrue(placeOnWall(Blocks.glass, 0, Blocks.stone_button), "no button on the glass");
		onServer(() -> world().setBlockToAir(WALL_X, WALL_Y, WALL_Z));
		serverTicks(2);
		assertEqual(Blocks.air, onServer(() -> world().getBlock(WALL_X, WALL_Y, WALL_Z - 1)));
		assertTrue(onServer(() -> {
			for (EntityItem item : entitiesNear(EntityItem.class, 5.0D)) {
				if (item.getEntityItem().getItem() == Item.getItemFromBlock(Blocks.stone_button)) {
					return true;
				}
			}
			return false;
		}), "no dropped button");
	}

	/** EFR's floor buttons too: on top of a glass block in the ground. */
	public void testFloorButtonOnGlass() {
		onServer(() -> world().setBlock(WALL_X, 3, WALL_Z, Blocks.glass, 0, 3));
		setHeld(new ItemStack(Blocks.stone_button));
		waitUntilOnClient("the button in hand", () -> mc().thePlayer.getHeldItem() != null, 20);
		// from the eyes (1.62 up) to the middle of the glass's top, two blocks ahead
		look(0.0F, (float) Math.toDegrees(Math.atan2(1.62D, 2.0D)));
		aimAt(WALL_X, 3, WALL_Z, 1);
		tap(mc().gameSettings.keyBindUseItem);
		waitUntil("a button on the glass's top", () -> world().getBlock(WALL_X, 4, WALL_Z) == Blocks.stone_button, 10);
	}

	/**
	 * Builds the wall block, aims at its north face from the start and right-clicks it with the item; true when the
	 * block it places stands in front of the face.
	 */
	private boolean placeOnWall(Block wall, int meta, Block placed) {
		onServer(() -> world().setBlock(WALL_X, WALL_Y, WALL_Z, wall, meta, 3));
		setHeld(new ItemStack(placed));
		waitUntilOnClient("the item in hand", () -> mc().thePlayer.getHeldItem() != null, 20);
		look(0.0F, 0.0F);
		aimAt(WALL_X, WALL_Y, WALL_Z, 2);
		tap(mc().gameSettings.keyBindUseItem);
		serverTicks(4);
		return onServer(() -> world().getBlock(WALL_X, WALL_Y, WALL_Z - 1) == placed);
	}

	private void aimAt(int x, int y, int z, int side) {
		waitUntilOnClient("the crosshair on " + x + " " + y + " " + z + " side " + side, () -> {
			MovingObjectPosition hit = mc().objectMouseOver;
			return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& hit.blockX == x && hit.blockY == y && hit.blockZ == z && hit.sideHit == side;
		}, 20);
	}
}
