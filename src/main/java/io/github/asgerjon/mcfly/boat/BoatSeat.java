package io.github.asgerjon.mcfly.boat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * A boat's second seat (SPEC-modern-movement.md 91.12): 1.7.10 gives an entity one rider, so the second passenger rides
 * this, which follows its boat and is placed by it, on EFR's id (`etfuturum.new_boat_seat`). Never saved on its own: a
 * seat with a passenger is saved in that passenger's `Riding`, with its boat's UUID (EFR's `boatUUIDMost`,
 * `boatUUIDLeast`), and finds that boat again when loaded. Clients know its boat by id.
 */
public class BoatSeat extends Entity {

	private static final int BOAT_ID = 17;

	private McFlyBoat boat;
	private UUID boatUuid;
	private int unfoundTicks;

	public BoatSeat(World world) {
		super(world);
		setSize(0.0F, 0.0F);
		noClip = true;
	}

	BoatSeat(World world, McFlyBoat boat) {
		this(world);
		attach(boat);
		copyLocationAndAnglesFrom(boat);
	}

	@Override
	protected void entityInit() {
		dataWatcher.addObject(BOAT_ID, 0);
	}

	private void attach(McFlyBoat boat) {
		this.boat = boat;
		boatUuid = boat.getUniqueID();
		dataWatcher.updateObject(BOAT_ID, boat.getEntityId());
		boat.seat = this;
	}

	/** Its boat, once found. */
	public McFlyBoat boat() {
		return boat;
	}

	@Override
	public void onUpdate() {
		if (worldObj.isRemote) {
			Entity found = worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(BOAT_ID));
			boat = found instanceof McFlyBoat ? (McFlyBoat) found : null;
			if (boat != null && (boat.seat == null || boat.seat.isDead || boat.seat == this)) {
				boat.seat = this;
			}
		} else if (boat == null) {
			findBoat();
			if (boat == null) {
				// loaded with a passenger whose boat never came
				if (++unfoundTicks > 40) {
					setDead();
				}
				return;
			}
		}
		if (boat == null) {
			return;
		}
		if (boat.isDead) {
			if (!worldObj.isRemote) {
				setDead();
			}
			return;
		}
		prevPosX = lastTickPosX = posX;
		prevPosY = lastTickPosY = posY;
		prevPosZ = lastTickPosZ = posZ;
		prevRotationYaw = rotationYaw;
		setPosition(boat.posX, boat.posY, boat.posZ);
		rotationYaw = boat.rotationYaw;
	}

	/** By its boat's UUID, among the boats near (it was saved where its boat was). */
	private void findBoat() {
		if (boatUuid == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<McFlyBoat> near = worldObj.getEntitiesWithinAABB(McFlyBoat.class, AxisAlignedBB.getBoundingBox(posX - 3, posY - 3, posZ - 3, posX + 3, posY + 3, posZ + 3));
		for (McFlyBoat candidate : near) {
			if (!candidate.isDead && boatUuid.equals(candidate.getUniqueID()) && (candidate.seat == null || candidate.seat.isDead)) {
				attach(candidate);
				return;
			}
		}
	}

	@Override
	public void updateRiderPosition() {
		if (riddenByEntity != null) {
			if (boat != null) {
				boat.positionRider(riddenByEntity);
			} else {
				riddenByEntity.setPosition(posX, posY + riddenByEntity.yOffset, posZ);
			}
		}
	}

	@Override
	public void setDead() {
		if (!worldObj.isRemote && riddenByEntity != null) {
			riddenByEntity.mountEntity(null);
		}
		super.setDead();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
		// it follows its boat
	}

	@Override
	public boolean shouldDismountInWater(Entity rider) {
		return false;
	}

	@Override
	public boolean writeToNBTOptional(NBTTagCompound tag) {
		// only in its passenger's `Riding`
		return false;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("boatUUIDMost") && tag.hasKey("boatUUIDLeast")) {
			boatUuid = new UUID(tag.getLong("boatUUIDMost"), tag.getLong("boatUUIDLeast"));
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		if (boatUuid != null) {
			tag.setLong("boatUUIDMost", boatUuid.getMostSignificantBits());
			tag.setLong("boatUUIDLeast", boatUuid.getLeastSignificantBits());
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isEntityInvulnerable() {
		return true;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return null;
	}

	@Override
	public boolean isInvisible() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInvisibleToPlayer(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean interactFirst(EntityPlayer player) {
		return false;
	}
}
