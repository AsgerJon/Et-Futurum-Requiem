package io.github.asgerjon.mcfly.mixins.early.modernchests;

import ganymedes01.etfuturum.blocks.ModernChests;
import ganymedes01.etfuturum.ducks.IModernChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A chest's type (single, left, right; spec §65) in its block entity: saved as `ChestType`, sent to clients in a
 * description packet (1.7.10's chest had none: each side worked out its neighbours itself), and 1.7.10's neighbour
 * fields, which the renderer, the lid and its sound read, set from the other half only. An old world's chest, with
 * no type, is converted on its first server tick.
 */
@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest extends TileEntity implements IModernChest {

	@Unique
	private int etfu$type;

	@Override
	public int etfu$chestType() {
		return etfu$type;
	}

	@Override
	public void etfu$setChestType(int type) {
		etfu$type = type;
	}

	@Inject(method = "readFromNBT", at = @At("RETURN"))
	private void etfu$readType(NBTTagCompound tag, CallbackInfo ci) {
		etfu$type = etfu$named(tag.getString("ChestType"));
	}

	@Inject(method = "writeToNBT", at = @At("RETURN"))
	private void etfu$writeType(NBTTagCompound tag, CallbackInfo ci) {
		if (etfu$type != ModernChests.UNKNOWN) {
			tag.setString("ChestType", ModernChests.NAMES[etfu$type]);
		}
	}

	@Override
	public Packet getDescriptionPacket() {
		if (!ModernChests.isOn() || etfu$type == ModernChests.UNKNOWN) {
			return null;
		}
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("ChestType", ModernChests.NAMES[etfu$type]);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		etfu$type = etfu$named(packet.func_148857_g().getString("ChestType"));
		((TileEntityChest) (Object) this).updateContainingBlockInfo();
	}

	@Inject(method = "checkForAdjacentChests", at = @At("HEAD"), cancellable = true)
	private void etfu$partnerOnly(CallbackInfo ci) {
		if (!ModernChests.isOn() || etfu$type == ModernChests.UNKNOWN || worldObj == null) {
			return;
		}
		ci.cancel();
		TileEntityChest self = (TileEntityChest) (Object) this;
		if (self.adjacentChestChecked) {
			return;
		}
		self.adjacentChestChecked = true;
		self.adjacentChestXNeg = self.adjacentChestXPos = self.adjacentChestZNeg = self.adjacentChestZPos = null;
		TileEntityChest other = ModernChests.partner(worldObj, self, getBlockType());
		if (other == null) {
			return;
		}
		int side = ModernChests.partnerSideOf(worldObj, self, getBlockType());
		switch (side) {
			case 2 -> self.adjacentChestZNeg = other;
			case 3 -> self.adjacentChestZPos = other;
			case 4 -> self.adjacentChestXNeg = other;
			case 5 -> self.adjacentChestXPos = other;
			default -> {
			}
		}
	}

	@Inject(method = "updateEntity", at = @At("HEAD"))
	private void etfu$convertOld(CallbackInfo ci) {
		if (ModernChests.isOn() && etfu$type == ModernChests.UNKNOWN && worldObj != null && !worldObj.isRemote) {
			ModernChests.convert(worldObj, (TileEntityChest) (Object) this, getBlockType());
		}
	}

	@Unique
	private static int etfu$named(String name) {
		for (int i = 1; i < ModernChests.NAMES.length; i++) {
			if (ModernChests.NAMES[i].equals(name)) {
				return i;
			}
		}
		return ModernChests.UNKNOWN;
	}
}
