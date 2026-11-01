package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

/**
 * Placing a stair or slab next to yourself (SPEC-modern-movement.md §53.8, the user's report of 2026-10-30): modern
 * tests the player against the shape the block will have, not a full cube. Standing on a stair's lower step with the
 * hitbox 0.1 into the next block, a stair (high half away, from the yaw) or a bottom slab placed there doesn't touch
 * the player and goes in; standing on the ground there, the new stair's lower half would, and doesn't. The click is a
 * real one on the ground block's top, from where the player stands. The new stair makes an inner corner with the one
 * stood on, whose quarter starts at the hitbox's south face; the hitbox reaches 1.2E-8 into it (its float half width),
 * which modern doesn't count (overlaps under 1.0E-7).
 */
public class PlacementShapeTests extends TasTestCase {

	/** The stair stood on (its high half to the south) and, west of it, where the new block goes. */
	private static final int STAIR_X = 30, Z = 2, GROUND_X = 36;

	@Override
	protected void setUp() {
		onServer(() -> {
			for (int x = STAIR_X - 3; x <= GROUND_X + 2; x++) {
				for (int y = 4; y <= 6; y++) {
					for (int z = Z - 2; z <= Z + 2; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			world().setBlock(STAIR_X, 4, Z, Blocks.oak_stairs, 2, 3);
		});
	}

	public void testStairBesideYouOnTheLowerStep() {
		assertEqual(Blocks.oak_stairs, placeWest(STAIR_X, 4.5D, new ItemStack(Blocks.oak_stairs)));
	}

	public void testSlabBesideYouOnTheLowerStep() {
		assertEqual(Blocks.stone_slab, placeWest(STAIR_X, 4.5D, new ItemStack(Blocks.stone_slab)));
	}

	/** On the ground, the new stair's lower half would be in the player: still refused, as in modern. */
	public void testStairIntoYouStillRefused() {
		assertEqual(Blocks.air, placeWest(GROUND_X, 4.0D, new ItemStack(Blocks.oak_stairs)));
	}

	/**
	 * Stands at x + 0.2 (the hitbox 0.1 into the block to the west), z + 0.2, feet at the height, and right-clicks the
	 * middle of the top of the ground block to the west with the item; returns the block that is then there.
	 */
	private Block placeWest(int x, double feet, ItemStack item) {
		double eyeX = x + 0.2D, eyeY = feet + 1.62D, eyeZ = Z + 0.2D;
		double dx = x - 0.5D - eyeX, dy = 4.0D - eyeY, dz = Z + 0.5D - eyeZ;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(eyeX, feet, eyeZ, yaw, pitch));
		setHeld(item);
		look(yaw, pitch);
		waitUntilOnClient("standing there with the item", () -> mc().thePlayer.onGround && Math.abs(mc().thePlayer.posX - eyeX) < 0.01D
			&& mc().thePlayer.getHeldItem() != null, 40);
		waitUntilOnClient("the crosshair on the ground to the west", () -> {
			MovingObjectPosition hit = mc().objectMouseOver;
			return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& hit.blockX == x - 1 && hit.blockY == 3 && hit.blockZ == Z && hit.sideHit == 1;
		}, 20);
		tap(mc().gameSettings.keyBindUseItem);
		serverTicks(4);
		return onServer(() -> world().getBlock(x - 1, 4, Z));
	}
}
