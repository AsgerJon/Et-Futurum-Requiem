package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlyHangingSign;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Hanging signs (SPEC-modern-movement.md 91.9, step G; 26.3's `CeilingHangingSignBlock`, `WallHangingSignBlock`,
 * `HangingSignBlockEntity`): their tile entity (with 26.3's `attached`, which doesn't fit the meta beside 16 rotations),
 * what holds a ceiling sign, and the rotation and facing helpers.
 */
public final class HangingSigns {

	/** 26.3's `HangingSignBlockEntity`: 60-pixel lines, 9 apart. */
	public static final int MAX_WIDTH = 60, LINE_HEIGHT = 9;
	private static final double EPSILON = 1.0E-4D;

	private HangingSigns() {
	}

	/**
	 * The hanging signs' tile entity, `etfuturum.hanging_sign`: a sign's (91.8) and `attached`, the ceiling sign's chain
	 * from the middle. Sent with the sign; the client redraws the block when it arrives (the chunk may have drawn first).
	 */
	public static class Tile extends TileEntitySign {

		public boolean attached;

		@Override
		public void writeToNBT(NBTTagCompound tag) {
			super.writeToNBT(tag);
			tag.setBoolean("attached", attached);
		}

		@Override
		public void readFromNBT(NBTTagCompound tag) {
			super.readFromNBT(tag);
			attached = tag.getBoolean("attached");
		}

		@Override
		public Packet getDescriptionPacket() {
			Packet packet = super.getDescriptionPacket();
			if (packet instanceof S35PacketUpdateTileEntity update && update.func_148857_g() != null) {
				update.func_148857_g().setBoolean("attached", attached);
			}
			return packet;
		}

		@Override
		public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
			super.onDataPacket(net, packet);
			boolean was = attached;
			attached = packet.func_148857_g().getBoolean("attached");
			if (was != attached && worldObj != null) {
				worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
			}
		}
	}

	public static boolean attached(World world, int x, int y, int z) {
		return world.getTileEntity(x, y, z) instanceof Tile tile && tile.attached;
	}

	/**
	 * 26.3's ceiling sign's `canSurvive`: the block above holds it by its bottom's middle (`SupportType.CENTER`: the
	 * middle 2 by 2 pixels): a full face, a fence's or wall's post, a chain, another hanging sign. Leaves never.
	 */
	public static boolean heldFromAbove(World world, int x, int y, int z) {
		int above = y + 1;
		Block block = world.getBlock(x, above, z);
		if (block instanceof McFlyHangingSign) {
			return true;
		}
		if (block.isAir(world, x, above, z) || block.isLeaves(world, x, above, z)) {
			return false;
		}
		if (world.isSideSolid(x, above, z, ForgeDirection.DOWN)) {
			return true;
		}
		List<AxisAlignedBB> boxes = new ArrayList<>();
		block.addCollisionBoxesToList(world, x, above, z, AxisAlignedBB.getBoundingBox(x, above, z, x + 1, above + 1, z + 1), boxes, null);
		for (AxisAlignedBB box : boxes) {
			if (box.minY <= above + EPSILON && box.minX <= x + 7.0D / 16.0D + EPSILON && box.maxX >= x + 9.0D / 16.0D - EPSILON
				&& box.minZ <= z + 7.0D / 16.0D + EPSILON && box.maxZ >= z + 9.0D / 16.0D - EPSILON) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's `RotationSegment.convertToDirection`: 0 north, 4 east, 8 south, 12 west as a side (2, 5, 3, 4), else -1. */
	public static int sideOfRotation(int rotation) {
		switch (rotation) {
			case 0:
				return 2;
			case 4:
				return 5;
			case 8:
				return 3;
			case 12:
				return 4;
			default:
				return -1;
		}
	}

	/** A horizontal side's segment (26.3's `convertToSegment(Direction)`): north 0, east 4, south 8, west 12. */
	public static int rotationOfSide(int side) {
		switch (side) {
			case 2:
				return 0;
			case 5:
				return 4;
			case 3:
				return 8;
			default:
				return 12;
		}
	}

	/** Whether two horizontal sides lie on the same axis (2, 3 along z; 4, 5 along x). */
	public static boolean sameAxis(int a, int b) {
		return a >= 2 && b >= 2 && (a >> 1) == (b >> 1);
	}

	/** 26.3's `Direction.fromYRot`: the horizontal side the yaw looks toward (0 south, 90 west, 180 north, 270 east). */
	public static int sideOfYaw(float yaw) {
		int quarter = (int) Math.floor(yaw / 90.0D + 0.5D) & 3;
		return new int[]{3, 4, 2, 5}[quarter];
	}
}
