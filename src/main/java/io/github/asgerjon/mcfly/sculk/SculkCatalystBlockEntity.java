package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.LivingExperience;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * A sculk catalyst's block entity (26.3's `SculkCatalystBlockEntity` and its `CatalystListener`;
 * SPEC-modern-movement.md §90.9, step H3), under EFR's id `etfuturum.sculk_catalyst`:
 *
 * - It hears `entity_die` out to 8, by distance.
 * - A living thing that dies there with experience to give leaves its experience as cursors at the block above where
 *   it died, and drops no orbs (26.3 doesn't ask whether a player hit it).
 * - Then the catalyst blooms: meta 1, a tick in 8, `sculk_soul` particles above it, its bloom's sound.
 * - Its cursors are saved as 26.3's `cursors`.
 */
public class SculkCatalystBlockEntity extends TileEntity implements GameEventListener {

	private final SculkSpreader spreader = SculkSpreader.createLevelSpreader();
	private boolean registered;

	public SculkSpreader spreader() {
		return spreader;
	}

	// ---------------------------------------------------------------- listening

	private void listen() {
		if (!registered && worldObj != null && !worldObj.isRemote) {
			GameEvents.register(worldObj, this);
			registered = true;
		}
	}

	private void stopListening() {
		if (registered && worldObj != null) {
			GameEvents.unregister(worldObj, this);
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
		// 26.3's `serverTick`: the cursors' update, spreading veins
		spreader.updateCursors(worldObj, xCoord, yCoord, zCoord, worldObj.rand, true);
	}

	@Override
	public double[] position(World world) {
		return new double[]{xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D};
	}

	@Override
	public int radius() {
		return 8;
	}

	@Override
	public boolean byDistance() {
		return true;
	}

	/** 26.3's `handleGameEvent`: a death near it. */
	@Override
	public boolean handle(World world, GameEvent event, GameEvent.Context context, double x, double y, double z) {
		if (!event.name.equals("minecraft:entity_die") || !(context.source instanceof EntityLivingBase) || !(context.source instanceof LivingExperience)) {
			return false;
		}
		LivingExperience dying = (LivingExperience) context.source;
		if (!dying.mcfly$experienceConsumed()) {
			int experience = dying.mcfly$experienceReward();
			if (dying.mcfly$shouldDropExperience() && experience > 0) {
				spreader.addCursors(MathHelper.floor_double(x), MathHelper.floor_double(y + 0.5D), MathHelper.floor_double(z), experience);
				markDirty();
			}
			dying.mcfly$skipDropExperience();
			bloom(world);
		}
		return true;
	}

	/** 26.3's `bloom`. */
	private void bloom(World world) {
		world.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 3);
		world.scheduleBlockUpdate(xCoord, yCoord, zCoord, getBlockType(), SculkCatalystBlock.BLOOM_TICKS);
		SculkParticles.soul(world, xCoord + 0.5D, yCoord + 1.15D, zCoord + 0.5D);
		world.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, McFly.MC_ASSETS + ":block.sculk_catalyst.bloom", 2.0F,
			0.6F + world.rand.nextFloat() * 0.4F);
	}

	// ---------------------------------------------------------------- saving

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		spreader.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		spreader.readFromNBT(tag);
	}
}
