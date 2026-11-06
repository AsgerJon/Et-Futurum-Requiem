package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlySetType;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 26.3's trapdoor rules (SPEC-modern-movement.md §91.6, step D; its `TrapDoorBlock`; the user: "make trapdoors as
 * capable as the 26.3") for McFly's trapdoors and, by mixin, vanilla's oak one. 1.7.10's metas use all four bits (`& 3`
 * the facing: 0 north, 1 south, 2 west, 3 east; `4` open; `8` the top half), so 26.3's `POWERED` is kept in a small
 * block entity, {@link Tile} (`etfuturum.trapdoor`), made when first asked for: an old trapdoor without one isn't powered.
 */
public final class Trapdoors {

	public static final int OPEN = 4, TOP = 8;

	private Trapdoors() {
	}

	/** The powered state's keeper. */
	public static final class Tile extends TileEntity {
		public boolean powered;

		@Override
		public boolean canUpdate() {
			return false;
		}

		@Override
		public void writeToNBT(NBTTagCompound tag) {
			super.writeToNBT(tag);
			tag.setBoolean("powered", powered);
		}

		@Override
		public void readFromNBT(NBTTagCompound tag) {
			super.readFromNBT(tag);
			powered = tag.getBoolean("powered");
		}
	}

	/** The trapdoor's keeper there, or null (a trapdoor that keeps none: another mod's). */
	public static Tile tile(World world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof Tile ? (Tile) tile : null;
	}

	public static boolean isPowered(World world, int x, int y, int z) {
		Tile tile = tile(world, x, y, z);
		return tile != null && tile.powered;
	}

	/** Opened or closed: the bit, the set type's sound by the new state, the game event. */
	public static void setOpen(Block trapdoor, World world, int x, int y, int z, boolean open, Entity source) {
		int meta = world.getBlockMetadata(x, y, z);
		if (((meta & OPEN) != 0) == open) {
			return;
		}
		world.setBlockMetadataWithNotify(x, y, z, open ? meta | OPEN : meta & ~OPEN, 2);
		if (!world.isRemote) {
			McFlySetType.of(trapdoor).playTrapdoor(world, x, y, z, open);
			GameEvents.postAtBlock(world, open ? "block_open" : "block_close", x, y, z, source, null, 0);
		}
	}

	/**
	 * 26.3's `neighborChanged`: only a change in signal opens or closes it, and sets the powered state; whether it was
	 * kept (false: no keeper there).
	 */
	public static boolean redstone(Block trapdoor, World world, int x, int y, int z) {
		Tile tile = tile(world, x, y, z);
		if (tile == null) {
			return false;
		}
		if (world.isRemote) {
			return true;
		}
		boolean signal = world.isBlockIndirectlyGettingPowered(x, y, z);
		if (signal != tile.powered) {
			setOpen(trapdoor, world, x, y, z, signal, null);
			tile.powered = signal;
			tile.markDirty();
		}
		return true;
	}

	/** 26.3's `onExplosionHit` for a wind burst: toggled, unless its set can't be or it's powered. */
	public static void burst(Block trapdoor, World world, int x, int y, int z) {
		if (McFlySetType.of(trapdoor).byWind && !isPowered(world, x, y, z)) {
			setOpen(trapdoor, world, x, y, z, (world.getBlockMetadata(x, y, z) & OPEN) == 0, null);
		}
	}

	/**
	 * 26.3's `LivingEntity.trapdoorUsableAsLadder`: an open trapdoor over a ladder of the same facing (1.7.10's ladder
	 * metas 2 north, 3 south, 4 west, 5 east; the trapdoor's 0 north, 1 south, 2 west, 3 east).
	 */
	public static boolean usableAsLadder(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		return (meta & OPEN) != 0 && world.getBlock(x, y - 1, z) instanceof BlockLadder && world.getBlockMetadata(x, y - 1, z) - 2 == (meta & 3);
	}
}
