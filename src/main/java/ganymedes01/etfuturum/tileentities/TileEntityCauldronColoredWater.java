package ganymedes01.etfuturum.tileentities;

import net.minecraft.init.Blocks;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class TileEntityCauldronColoredWater extends TileEntity {

	public abstract int getWaterColor();

	@Override
	public boolean canUpdate() {
		return false;
	}

	/** Cauldrons to turn back into plain ones when their world next ends a tick (see {@link #resetCauldron()}). */
	private static final Queue<TileEntityCauldronColoredWater> RESETS = new ConcurrentLinkedQueue<>();

	/**
	 * Back to a plain cauldron, on the server, at the end of its world's tick: this is called from reading the NBT,
	 * which runs inside chunk loading (no world yet; setting a block there loads the chunk again from inside its own
	 * loading). A client's copy (read from the server's packet) waits for the server's block change.
	 */
	protected void resetCauldron() {
		if (worldObj == null || !worldObj.isRemote) {
			RESETS.add(this);
		}
	}

	/** The queued resets of this world's cauldrons, from the world tick's end; one that was never placed is dropped. */
	public static void resetQueued(World world) {
		for (Iterator<TileEntityCauldronColoredWater> it = RESETS.iterator(); it.hasNext(); ) {
			TileEntityCauldronColoredWater cauldron = it.next();
			World its = cauldron.getWorldObj();
			if (its != world && its != null) {
				continue;
			}
			it.remove();
			if (its != null && !cauldron.isInvalid() && world.blockExists(cauldron.xCoord, cauldron.yCoord, cauldron.zCoord)
				&& world.getTileEntity(cauldron.xCoord, cauldron.yCoord, cauldron.zCoord) == cauldron) {
				world.setBlock(cauldron.xCoord, cauldron.yCoord, cauldron.zCoord, Blocks.cauldron, 0, 3);
			}
		}
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		readFromNBT(pkt.func_148857_g()); // getNbtCompound
	}
}
