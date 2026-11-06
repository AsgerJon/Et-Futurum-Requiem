package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.caves.GlowLichenItem;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Arrays;

/**
 * Placing a sign (SPEC-modern-movement.md 91.8; 26.3's `StandingAndWallBlockItem`, `WallSignBlock` and
 * `StandingSignBlock.getStateForPlacement`, `SignBlock.setPlacedBy`), for McFly's sign items and vanilla's `sign`. Into
 * the clicked space if it's replaceable, else the one before the face; then the player's nearest looking directions,
 * the one into the clicked face first: down a standing sign turned to face them, across a wall sign facing back out
 * (its own pass over the same directions), up skipped; the first that's held. Then the editor opens on its front.
 */
public final class SignPlacing {

	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	/** A wall sign's meta facing away from a direction (north 2 into the wall to the north... facing its opposite). */
	private static final int[] FACING_AWAY = {-1, -1, 3, 2, 5, 4};

	private SignPlacing() {
	}

	/** 26.3's `isSolid` for a sign's support, as 1.7.10's sign checks it (the material's). */
	private static boolean solid(World world, int x, int y, int z) {
		return world.getBlock(x, y, z).getMaterial().isSolid();
	}

	public static boolean place(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, Block standing, Block wall) {
		Block clicked = world.getBlock(x, y, z);
		boolean replaceClicked = clicked == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1 || clicked.isReplaceable(world, x, y, z);
		if (!replaceClicked) {
			x += OFFSET[side][0];
			y += OFFSET[side][1];
			z += OFFSET[side][2];
		}
		if (stack.stackSize <= 0 || !player.canPlayerEdit(x, y, z, side, stack) || y < 0 || y >= world.getHeight()) {
			return false;
		}
		if (!world.getBlock(x, y, z).isReplaceable(world, x, y, z)) {
			return false;
		}
		Integer[] order = GlowLichenItem.nearestLooking(player);
		if (!replaceClicked) {
			int toward = side ^ 1;
			int index = Arrays.asList(order).indexOf(toward);
			System.arraycopy(order, 0, order, 1, index);
			order[0] = toward;
		}
		// the wall sign's own pass: the first horizontal direction with a solid block that way, facing away from it
		int wallMeta = -1;
		for (int direction : order) {
			if (direction >= 2 && solid(world, x + OFFSET[direction][0], y, z + OFFSET[direction][2])) {
				wallMeta = FACING_AWAY[direction];
				break;
			}
		}
		Block block = null;
		int meta = 0;
		for (int direction : order) {
			if (direction == 1) {
				continue;
			}
			if (direction == 0) {
				if (solid(world, x, y - 1, z)) {
					block = standing;
					meta = MathHelper.floor_double((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15;
					break;
				}
			} else if (wallMeta >= 0) {
				block = wall;
				meta = wallMeta;
				break;
			}
		}
		if (block == null) {
			return false;
		}
		if (world.isRemote) {
			return true;
		}
		if (!world.setBlock(x, y, z, block, meta, 3)) {
			return false;
		}
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, block.stepSound.func_150496_b(), (block.stepSound.getVolume() + 1.0F) / 2.0F,
			block.stepSound.getPitch() * 0.8F);
		stack.stackSize--;
		TileEntity tile = world.getTileEntity(x, y, z);
		if (player instanceof EntityPlayerMP editor && tile instanceof TileEntitySign sign && tile instanceof Signs.Sided sided && !sided.mcfly$isWaxed()) {
			Signs.openEditor(editor, sign, true);
		}
		return true;
	}

	/**
	 * Placing a hanging sign (SPEC-modern-movement.md 91.9; 26.3's `HangingSignItem`, `CeilingHangingSignBlock` and
	 * `WallHangingSignBlock.getStateForPlacement`): the nearest looking directions, the one into the clicked face first;
	 * up gives the ceiling sign if the block above holds it; across gives the wall sign, whose own pass skips the clicked
	 * face's axis and needs a side that holds it; down is skipped. Then its editor opens on the front.
	 */
	public static boolean placeHanging(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, Block ceilingSign, Block wallSign) {
		Block clicked = world.getBlock(x, y, z);
		boolean replaceClicked = clicked == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1 || clicked.isReplaceable(world, x, y, z);
		if (!replaceClicked) {
			x += OFFSET[side][0];
			y += OFFSET[side][1];
			z += OFFSET[side][2];
		}
		if (stack.stackSize <= 0 || !player.canPlayerEdit(x, y, z, side, stack) || y < 0 || y >= world.getHeight()) {
			return false;
		}
		if (!world.getBlock(x, y, z).isReplaceable(world, x, y, z)) {
			return false;
		}
		Integer[] order = GlowLichenItem.nearestLooking(player);
		if (!replaceClicked) {
			int toward = side ^ 1;
			int index = Arrays.asList(order).indexOf(toward);
			System.arraycopy(order, 0, order, 1, index);
			order[0] = toward;
		}
		// the wall sign's own pass: across, off the clicked face's axis, facing away, a side holding it
		int wallMeta = -1;
		for (int direction : order) {
			if (direction >= 2 && !HangingSigns.sameAxis(direction, side) && wallHeld(world, x, y, z, FACING_AWAY[direction])) {
				wallMeta = FACING_AWAY[direction];
				break;
			}
		}
		Block block = null;
		int meta = 0;
		boolean attached = false;
		for (int direction : order) {
			if (direction == 0) {
				continue;
			}
			if (direction == 1) {
				if (HangingSigns.heldFromAbove(world, x, y, z)) {
					block = ceilingSign;
					// 26.3's: attached to the middle without a full bottom face above, or sneaking; under a hanging sign
					// on the same axis, hung from its chains
					int facing = HangingSigns.sideOfYaw(player.rotationYaw);
					attached = !ganymedes01.etfuturum.core.utils.BlockSupport.isFaceFull(world, x, y + 1, z, net.minecraftforge.common.util.ForgeDirection.DOWN)
						|| player.isSneaking();
					Block above = world.getBlock(x, y + 1, z);
					if (above instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign hanging && !player.isSneaking()) {
						int aboveMeta = world.getBlockMetadata(x, y + 1, z);
						int aboveSide = hanging.ceiling ? HangingSigns.sideOfRotation(aboveMeta & 15) : aboveMeta;
						if (aboveSide >= 2 && HangingSigns.sameAxis(aboveSide, facing)) {
							attached = false;
						}
					}
					meta = attached ? MathHelper.floor_double((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15
						: HangingSigns.rotationOfSide(facing ^ 1);
					break;
				}
			} else if (wallMeta >= 0) {
				block = wallSign;
				meta = wallMeta;
				break;
			}
		}
		if (block == null) {
			return false;
		}
		if (world.isRemote) {
			return true;
		}
		if (!world.setBlock(x, y, z, block, meta, 3)) {
			return false;
		}
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile instanceof HangingSigns.Tile hanging) {
			hanging.attached = attached;
			hanging.markDirty();
		}
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, block.stepSound.func_150496_b(), (block.stepSound.getVolume() + 1.0F) / 2.0F,
			block.stepSound.getPitch() * 0.8F);
		stack.stackSize--;
		if (player instanceof EntityPlayerMP editor && tile instanceof TileEntitySign sign && tile instanceof Signs.Sided sided && !sided.mcfly$isWaxed()) {
			Signs.openEditor(editor, sign, true);
		}
		return true;
	}

	/**
	 * 26.3's `WallHangingSignBlock.canPlace`: a side across its facing holds it, by a full face toward it or by being a
	 * wall hanging sign on the same axis.
	 */
	private static boolean wallHeld(World world, int x, int y, int z, int facing) {
		int[] across = facing == 2 || facing == 3 ? new int[]{4, 5} : new int[]{2, 3};
		for (int to : across) {
			int nx = x + OFFSET[to][0], nz = z + OFFSET[to][2];
			Block there = world.getBlock(nx, y, nz);
			if (there instanceof io.github.asgerjon.mcfly.base.McFlyHangingSign hanging && !hanging.ceiling) {
				if (HangingSigns.sameAxis(world.getBlockMetadata(nx, y, nz), facing)) {
					return true;
				}
			} else if (ganymedes01.etfuturum.core.utils.BlockSupport.isFaceFull(world, nx, y, nz, net.minecraftforge.common.util.ForgeDirection.getOrientation(to ^ 1))
				|| world.isSideSolid(nx, y, nz, net.minecraftforge.common.util.ForgeDirection.getOrientation(to ^ 1))) {
				return true;
			}
		}
		return false;
	}
}
