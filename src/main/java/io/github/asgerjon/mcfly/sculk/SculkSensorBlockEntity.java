package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.gameevent.VibrationSystem;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * A sculk sensor's block entity (26.3's `SculkSensorBlockEntity` and its `VibrationUser`; SPEC-modern-movement.md
 * §90.9, step H2).
 *
 * - **Listening:** it listens for vibrations out to 8 from its block's middle, registered while loaded, and ticks them
 *   (H1's `VibrationSystem`).
 * - **What it takes:** not its own placing or breaking, nothing of frequency 0, nothing unless the sensor is inactive,
 *   and only with the chunks round it loaded.
 * - **Taken:** the last frequency is the event's, and the power is the block distance's.
 * - **Saved:** `power` (26.3 keeps it in the block state), `last_vibration_frequency` and `listener`.
 * - **The calibrated one** (`Calibrated`) hears out to 16, and with a signal into its back only that frequency.
 */
public class SculkSensorBlockEntity extends TileEntity implements VibrationSystem.User {

	private VibrationSystem.Data data = new VibrationSystem.Data();
	private final VibrationSystem.Listener listener = new VibrationSystem.Listener(data, this);
	private int power;
	private int lastFrequency;
	private boolean registered;

	public int power() {
		return power;
	}

	void setPower(int power) {
		this.power = power;
		markDirty();
	}

	public int lastFrequency() {
		return lastFrequency;
	}

	// ---------------------------------------------------------------- listening

	private void listen() {
		if (!registered && worldObj != null && !worldObj.isRemote) {
			GameEvents.register(worldObj, listener);
			registered = true;
		}
	}

	private void stopListening() {
		if (registered && worldObj != null) {
			GameEvents.unregister(worldObj, listener);
		}
		registered = false;
	}

	@Override
	public void validate() {
		super.validate();
		listen();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		stopListening();
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		stopListening();
	}

	@Override
	public void updateEntity() {
		if (worldObj.isRemote) {
			return;
		}
		listen();
		VibrationSystem.tick(worldObj, data, this);
	}

	/** 26.3's `stepOn`: a step from the entity, past the checks, if it could take one at all. */
	void steppedOn(Entity entity) {
		GameEvent step = GameEvent.get("step");
		if (canReceive(worldObj, xCoord, yCoord, zCoord, step, new GameEvent.Context(null, getBlockType(), getBlockMetadata()))) {
			listener.forceSchedule(worldObj, step, entity, entity.posX, entity.boundingBox.minY, entity.posZ);
		}
	}

	// ---------------------------------------------------------------- 26.3's VibrationUser

	@Override
	public int radius() {
		return 8;
	}

	@Override
	public double[] position(World world) {
		return new double[]{xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D};
	}

	@Override
	public boolean canReceive(World world, int x, int y, int z, GameEvent event, GameEvent.Context context) {
		if (x == xCoord && y == yCoord && z == zCoord && (event.name.equals("minecraft:block_destroy") || event.name.equals("minecraft:block_place"))) {
			return false;
		}
		return event.frequency != 0 && SculkSensorBlock.canActivate(world, xCoord, yCoord, zCoord);
	}

	@Override
	public void onReceive(World world, int x, int y, int z, GameEvent event, Entity source, float distance) {
		Block block = world.getBlock(xCoord, yCoord, zCoord);
		if (block instanceof SculkSensorBlock && SculkSensorBlock.canActivate(world, xCoord, yCoord, zCoord)) {
			lastFrequency = event.frequency;
			((SculkSensorBlock) block).activate(world, xCoord, yCoord, zCoord, source, VibrationSystem.redstoneStrengthForDistance(distance, radius()),
				event.frequency);
			markDirty();
		}
	}

	@Override
	public void onDataChanged() {
		markDirty();
	}

	@Override
	public boolean requiresAdjacentChunks() {
		return true;
	}

	// ---------------------------------------------------------------- saving

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("power", power);
		tag.setInteger("last_vibration_frequency", lastFrequency);
		NBTTagCompound vibration = new NBTTagCompound();
		data.writeToNBT(vibration);
		tag.setTag("listener", vibration);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		power = tag.getInteger("power");
		lastFrequency = tag.getInteger("last_vibration_frequency");
		data.readFromNBT(tag.getCompoundTag("listener"));
	}

	/** 26.3's calibrated sculk sensor's: out to 16; with a signal into its back, only events of that frequency. */
	public static class Calibrated extends SculkSensorBlockEntity {

		@Override
		public int radius() {
			return 16;
		}

		@Override
		public boolean canReceive(World world, int x, int y, int z, GameEvent event, GameEvent.Context context) {
			int wanted = backSignal(world);
			return (wanted == 0 || event.frequency == wanted) && super.canReceive(world, x, y, z, event, context);
		}

		/** 26.3's `getBackSignal`: the signal of the block behind it (opposite its facing), toward it. */
		private int backSignal(World world) {
			ForgeDirection back = SculkSensorBlock.facing(world.getBlockMetadata(xCoord, yCoord, zCoord)).getOpposite();
			return world.getIndirectPowerLevelTo(xCoord + back.offsetX, yCoord + back.offsetY, zCoord + back.offsetZ, back.ordinal());
		}
	}
}
