package ganymedes01.etfuturum.command.modern.arguments;

import ganymedes01.etfuturum.command.modern.block.LegacyBlockEntityData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** A test of the block at a place: a block predicate (spec §49.1), or a block itself. */
public interface BlockTest {

	boolean test(World world, int x, int y, int z);

	/** The block entity there holds the data (when there is data to hold). */
	static boolean dataMatches(World world, int x, int y, int z, NBTTagCompound data) {
		if (data == null) {
			return true;
		}
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile == null) {
			return false;
		}
		NBTTagCompound saved = new NBTTagCompound();
		tile.writeToNBT(saved);
		return LegacyBlockEntityData.contains(saved, data);
	}
}
