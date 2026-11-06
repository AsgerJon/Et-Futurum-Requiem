package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlySetType;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * 26.3's door rules (SPEC-modern-movement.md §91.6, step D; its `DoorBlock`) for McFly's doors and, by mixin, 1.7.10's
 * own, in 1.7.10's metas: the lower half `& 3` the facing and `4` open; the upper `8`, bit 0 the hinge, bit 1 26.3's
 * `POWERED`.
 */
public final class Doors {

	public static final int OPEN = 4, UPPER = 8, HINGE_RIGHT = 1, POWERED = 2;

	private Doors() {
	}

	/** The y of the door's lower half, from either. */
	public static int lowerY(World world, int x, int y, int z) {
		return (world.getBlockMetadata(x, y, z) & UPPER) != 0 ? y - 1 : y;
	}

	public static boolean isOpen(World world, int x, int lowerY, int z) {
		return (world.getBlockMetadata(x, lowerY, z) & OPEN) != 0;
	}

	public static boolean isPowered(World world, int x, int lowerY, int z) {
		return world.getBlock(x, lowerY + 1, z) instanceof BlockDoor && (world.getBlockMetadata(x, lowerY + 1, z) & POWERED) != 0;
	}

	/** Opened or closed (26.3's `setOpen`): the lower half's bit, the set type's sound by the new state, the game event. */
	public static void setOpen(Block door, World world, int x, int lowerY, int z, boolean open, Entity source) {
		int meta = world.getBlockMetadata(x, lowerY, z);
		if (((meta & OPEN) != 0) == open) {
			return;
		}
		world.setBlockMetadataWithNotify(x, lowerY, z, open ? meta | OPEN : meta & ~OPEN, 2);
		world.markBlockRangeForRenderUpdate(x, lowerY, z, x, lowerY + 1, z);
		if (!world.isRemote) {
			McFlySetType.of(door).playDoor(world, x, lowerY, z, open);
			GameEvents.postAtBlock(world, open ? "block_open" : "block_close", x, lowerY, z, source, null, 0);
		}
	}

	/** 26.3's `neighborChanged`: only a change in either half's signal opens or closes it, and sets the powered bit. */
	public static void redstone(Block door, World world, int x, int lowerY, int z) {
		if (world.isRemote || !(world.getBlock(x, lowerY + 1, z) instanceof BlockDoor)) {
			return;
		}
		boolean signal = world.isBlockIndirectlyGettingPowered(x, lowerY, z) || world.isBlockIndirectlyGettingPowered(x, lowerY + 1, z);
		if (signal == isPowered(world, x, lowerY, z)) {
			return;
		}
		setOpen(door, world, x, lowerY, z, signal, null);
		int upper = world.getBlockMetadata(x, lowerY + 1, z);
		world.setBlockMetadataWithNotify(x, lowerY + 1, z, signal ? upper | POWERED : upper & ~POWERED, 2);
	}

	/** 26.3's `onExplosionHit` for a wind burst: toggled, unless its set can't be or it's powered. */
	public static void burst(Block door, World world, int x, int y, int z) {
		int lowerY = lowerY(world, x, y, z);
		if (McFlySetType.of(door).byWind && !isPowered(world, x, lowerY, z)) {
			setOpen(door, world, x, lowerY, z, !isOpen(world, x, lowerY, z), null);
		}
	}
}
