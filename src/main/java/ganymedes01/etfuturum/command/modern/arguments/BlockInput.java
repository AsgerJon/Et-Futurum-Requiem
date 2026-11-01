package ganymedes01.etfuturum.command.modern.arguments;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Map;

/**
 * A parsed block (spec §49.1): the modern block, the 1.7.10 state for its properties, the properties given, and the
 * block entity data in 1.7.10's form. As a test it matches that block with the given properties (the ones 1.7.10
 * keeps) and data.
 */
public final class BlockInput implements BlockTest {

	private final BlockIds.Type type;
	private final BlockIds.State state;
	private final Map<String, String> given;
	private final NBTTagCompound data;

	BlockInput(BlockIds.Type type, BlockIds.State state, Map<String, String> given, NBTTagCompound data) {
		this.type = type;
		this.state = state;
		this.given = given;
		this.data = data;
	}

	/** The block with its defaults, as /fill's hollow mode puts inside. */
	public static BlockInput of(String id) {
		BlockIds.Type type = BlockIds.byId(id);
		return new BlockInput(type, type.defaultState(), Collections.emptyMap(), null);
	}

	public BlockIds.State state() {
		return state;
	}

	public boolean isAir() {
		return state.block().getMaterial() == Material.air;
	}

	/**
	 * Puts the block in without telling the neighbours (the command does that afterwards, unless strict) and reads the
	 * data into its block entity. A container it replaces is emptied first, as modern's command doesn't spill one. True
	 * when anything changed.
	 */
	public boolean place(World world, int x, int y, int z) {
		boolean changed = false;
		if (world.getBlock(x, y, z) != state.block() || world.getBlockMetadata(x, y, z) != state.meta()) {
			emptyContainer(world, x, y, z);
			changed = setBlock(world, x, y, z, state.block(), state.meta());
		}
		TileEntity tile = data == null ? null : world.getTileEntity(x, y, z);
		if (tile != null) {
			NBTTagCompound before = new NBTTagCompound();
			tile.writeToNBT(before);
			NBTTagCompound loaded = (NBTTagCompound) data.copy();
			loaded.setString("id", before.getString("id"));
			loaded.setInteger("x", x);
			loaded.setInteger("y", y);
			loaded.setInteger("z", z);
			tile.readFromNBT(loaded);
			NBTTagCompound after = new NBTTagCompound();
			tile.writeToNBT(after);
			if (!after.equals(before)) {
				changed = true;
				tile.markDirty();
				world.markBlockForUpdate(x, y, z);
			}
		}
		return changed;
	}

	/**
	 * Sets the block without telling the neighbours, keeping the meta asked for: 1.7.10's furnaces, chests and
	 * dispensers turn themselves when placed, away from solid neighbours, where modern keeps the facing given. Rails
	 * are left to lay themselves as their neighbours ask.
	 */
	public static boolean setBlock(World world, int x, int y, int z, Block block, int meta) {
		boolean changed = world.setBlock(x, y, z, block, meta, 2);
		if (changed && !(block instanceof BlockRailBase) && world.getBlock(x, y, z) == block && world.getBlockMetadata(x, y, z) != meta) {
			world.setBlockMetadataWithNotify(x, y, z, meta, 2);
		}
		return changed;
	}

	/** Empties a container at the place, so that replacing it doesn't drop what it held. */
	public static void emptyContainer(World world, int x, int y, int z) {
		if (world.getTileEntity(x, y, z) instanceof IInventory inventory) {
			for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
				inventory.setInventorySlotContents(slot, null);
			}
		}
	}

	@Override
	public boolean test(World world, int x, int y, int z) {
		BlockIds.Found found = BlockIds.find(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
		if (found == null || found.type() != type) {
			return false;
		}
		for (Map.Entry<String, String> property : given.entrySet()) {
			if (type.isStored(property.getKey()) && !property.getValue().equals(found.state().properties().get(property.getKey()))) {
				return false;
			}
		}
		return BlockTest.dataMatches(world, x, y, z, data);
	}
}
