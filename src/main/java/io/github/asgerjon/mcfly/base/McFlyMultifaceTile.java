package io.github.asgerjon.mcfly.base;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * A multiface block's faces (SPEC-modern-movement.md §90.3), six bits too many for 1.7.10's meta: bit i the face toward
 * `ForgeDirection` i, as the int `State` (EFR's glow lichen's form, kept). Sent to the client whole.
 */
public class McFlyMultifaceTile extends TileEntity {

	private int faces;

	public int faces() {
		return faces;
	}

	public void setFaces(int faces) {
		this.faces = faces & 63;
		markDirty();
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("State", faces);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		faces = tag.getInteger("State") & 63;
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		readFromNBT(packet.func_148857_g());
		worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
	}

	@Override
	public boolean canUpdate() {
		return false;
	}
}
