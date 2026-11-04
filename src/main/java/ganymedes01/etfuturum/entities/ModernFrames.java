package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

/**
 * Item frames as 26.3 (SPEC-modern-movement.md §67.3). 1.7.10's `hangingDirection` 0 to 3 are the walls (south, west,
 * north, east); the port adds 4, a frame on a floor (facing up), and 5, on a ceiling (facing down). 1.7.10's spawn
 * packet carries the number as it is, so clients get the new two unchanged.
 */
public final class ModernFrames {

	public static final int FLOOR = 4, CEILING = 5;
	/** 26.3's 6-way `Facing` (0 down, 1 up, 2 north, 3 south, 4 west, 5 east), by `hangingDirection`. */
	private static final int[] FACING_BY_DIRECTION = {3, 4, 2, 5, 1, 0};
	private static final int[] DIRECTION_BY_FACING = {CEILING, FLOOR, 2, 0, 1, 3};

	/** The floor or ceiling frame being drawn as a north wall's, and its own facing (its fields are the wall's then). */
	private static EntityItemFrame drawnTurned;
	private static int drawnTurnedDirection;

	private ModernFrames() {
	}

	/** Whether floor and ceiling frames can exist (and be drawn). */
	public static boolean active() {
		return McFlyConfig.modernItemFrames;
	}

	/** The frame's sounds' name part: `glow_item_frame` for a glow frame, else `item_frame` (26.3's own sets). */
	public static String soundName(Object frame) {
		return frame instanceof EntityGlowItemFrame ? "glow_item_frame" : "item_frame";
	}

	public static boolean isFloorOrCeiling(int direction) {
		return direction == FLOOR || direction == CEILING;
	}

	public static int facing(int direction) {
		return direction >= 0 && direction < FACING_BY_DIRECTION.length ? FACING_BY_DIRECTION[direction] : 3;
	}

	public static int directionOfFacing(int facing) {
		return facing >= 0 && facing < DIRECTION_BY_FACING.length ? DIRECTION_BY_FACING[facing] : 0;
	}

	/** The block the frame is in (in front of the block it hangs on). */
	public static int[] frameBlock(EntityItemFrame frame) {
		int direction = frame.hangingDirection;
		if (direction == FLOOR) {
			return new int[]{frame.field_146063_b, frame.field_146064_c + 1, frame.field_146062_d};
		}
		if (direction == CEILING) {
			return new int[]{frame.field_146063_b, frame.field_146064_c - 1, frame.field_146062_d};
		}
		return new int[]{frame.field_146063_b + Direction.offsetX[direction], frame.field_146064_c, frame.field_146062_d + Direction.offsetZ[direction]};
	}

	/**
	 * 26.3's `ItemFrame.getVisualRotationYInDegrees` (a framed compass's needle): `180 + facing x 90 + turn x 45` on a
	 * wall (1.7.10's wall number is 26.3's `get2DDataValue`), `180 + turn x 45` on a floor and `turn x 45` on a ceiling
	 * (-1 for the facing, then 90 up or down). Without the port's frames, 1.7.10's quarter turns.
	 */
	public static float visualYaw(EntityItemFrame frame) {
		int direction = frame == drawnTurned ? drawnTurnedDirection : frame.hangingDirection;
		int turn = frame.getRotation();
		if (!active()) {
			return 180.0F + direction * 90.0F + turn * 90.0F;
		}
		float yaw = direction == FLOOR ? 180.0F : direction == CEILING ? 0.0F : 180.0F + direction * 90.0F;
		return yaw + turn * 45.0F;
	}

	/** The degrees of one of the frame's turns: 26.3's 45, or 1.7.10's 90 without the port's frames. */
	public static float degreesPerTurn() {
		return active() ? 45.0F : 90.0F;
	}

	public static void setDrawnTurned(EntityItemFrame frame, int direction) {
		drawnTurned = frame;
		drawnTurnedDirection = direction;
	}

	/**
	 * A frame (a glow one if asked) placed on the face of the block, as 1.7.10's `ItemHangingEntity.onItemUse` places one
	 * on a wall, and on a top (side 1) or bottom (side 0) inside the build height too (26.3's `ItemFrameItem`).
	 */
	public static boolean place(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, boolean glow) {
		if (side < 0 || side > 5 || (side < 2 && !active())) {
			return false;
		}
		int direction = side == 0 ? CEILING : side == 1 ? FLOOR : Direction.facingToDirection[side];
		int frameY = side == 0 ? y - 1 : side == 1 ? y + 1 : y;
		if (frameY < 0 || frameY >= world.getHeight() || !player.canPlayerEdit(x, y, z, side, stack)) {
			return false;
		}
		EntityItemFrame frame = glow ? new EntityGlowItemFrame(world, x, y, z, direction) : new EntityItemFrame(world, x, y, z, direction);
		if (frame.onValidSurface()) {
			if (!world.isRemote) {
				world.spawnEntityInWorld(frame);
			}
			--stack.stackSize;
		}
		return true;
	}
}
