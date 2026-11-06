package io.github.asgerjon.mcfly.boat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.network.McFlyNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * A boat or raft (SPEC-modern-movement.md 91.12, step J): 26.3's `AbstractBoat` and `VehicleEntity` on EFR's entity id
 * (`etfuturum.new_boat`) and NBT (`Type`, `Domain`, `CustomName`). Its status each tick (in water, under water, on land,
 * in the air) gives its buoyancy and friction; the steering player's client moves it and sends where it is
 * ({@link BoatMoveMessage}), the server moves it otherwise; under water 60 ticks it throws its passengers off; over a
 * bubble column it's launched or pulled down after 60 ticks; mobs that fit get in; hits break it past 40 damage.
 * <p>
 * 1.7.10 gives an entity one rider, so a second passenger sits on a {@link BoatSeat}, made when needed and gone when
 * empty; the passengers are the rider then the seat's rider, as 26.3's list.
 */
public class McFlyBoat extends Entity {

	private static final int HURT = 17;
	private static final int HURT_DIR = 18;
	private static final int DAMAGE = 19;
	private static final int PADDLE_LEFT = 20;
	private static final int PADDLE_RIGHT = 21;
	private static final int BUBBLE_TIME = 22;
	private static final int TYPE = 23;

	private static final float PADDLE_SPEED = (float) (Math.PI / 8);
	private static final float PADDLE_SOUND_TIME = (float) (Math.PI / 4);
	private static final float TWO_PI = (float) (Math.PI * 2);

	public enum Status {
		IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR
	}

	private final float[] paddlePositions = new float[2];
	private float outOfControlTicks;
	private float deltaRotation;
	private boolean inputLeft;
	private boolean inputRight;
	private boolean inputUp;
	private boolean inputDown;
	private double waterLevel;
	private float landFriction;
	private Status status;
	private Status oldStatus;
	private double lastYd;
	private boolean isAboveBubbleColumn;
	private boolean bubbleColumnDirectionIsDown;
	private float bubbleMultiplier;
	private float bubbleAngle;
	private float bubbleAngleO;
	private String customName;
	/** The second passenger's seat while there is one (on the client, the one that names this boat). */
	BoatSeat seat;
	private int lerpSteps;
	private double lerpX;
	private double lerpY;
	private double lerpZ;
	private float lerpYRot;
	private float lerpXRot;
	/** Where it was as the server's tick began (26.3's `vehicleFirstGood`). */
	double firstGoodX;
	double firstGoodY;
	double firstGoodZ;

	public McFlyBoat(World world) {
		super(world);
		preventEntitySpawning = true;
		setSize(1.375F, 0.5625F);
	}

	@Override
	protected void entityInit() {
		dataWatcher.addObject(HURT, 0);
		dataWatcher.addObject(HURT_DIR, 1);
		dataWatcher.addObject(DAMAGE, 0.0F);
		dataWatcher.addObject(PADDLE_LEFT, (byte) 0);
		dataWatcher.addObject(PADDLE_RIGHT, (byte) 0);
		dataWatcher.addObject(BUBBLE_TIME, 0);
		dataWatcher.addObject(TYPE, BoatTypes.OAK.key());
	}

	public void setInitialPos(double x, double y, double z) {
		setPosition(x, y, z);
		prevPosX = lastTickPosX = x;
		prevPosY = lastTickPosY = y;
		prevPosZ = lastTickPosZ = z;
	}

	// ---------------------------------------------------------------- the wood, the item, the name

	/** Its wood (one whose mod is missing too: `Type` and `Domain` are kept as read). */
	public BoatTypes.Type type() {
		BoatTypes.Type type = BoatTypes.byKey(dataWatcher.getWatchableObjectString(TYPE));
		return type == null ? BoatTypes.OAK : type;
	}

	public void setType(BoatTypes.Type type) {
		dataWatcher.updateObject(TYPE, type.key());
	}

	public boolean isRaft() {
		return type().raft;
	}

	public boolean isChest() {
		return false;
	}

	/** Its item (oak's while its wood's items are off). */
	public Item dropItem() {
		BoatTypes.Type type = type().isEnabled() ? type() : BoatTypes.OAK;
		return isChest() ? type.chestBoatItem() : type.boatItem();
	}

	public String getCustomName() {
		return customName;
	}

	public void setCustomName(String name) {
		customName = name == null || name.isEmpty() ? null : name;
	}

	@Override
	public String getCommandSenderName() {
		if (customName != null) {
			return customName;
		}
		Item item = dropItem();
		return item == null ? super.getCommandSenderName() : StatCollector.translateToLocal(item.getUnlocalizedName() + ".name");
	}

	@Override
	public ItemStack getPickedResult(MovingObjectPosition target) {
		Item item = dropItem();
		return item == null ? null : new ItemStack(item);
	}

	// ---------------------------------------------------------------- synced state

	public int getHurtTime() {
		return dataWatcher.getWatchableObjectInt(HURT);
	}

	public void setHurtTime(int time) {
		dataWatcher.updateObject(HURT, time);
	}

	public int getHurtDir() {
		return dataWatcher.getWatchableObjectInt(HURT_DIR);
	}

	public void setHurtDir(int dir) {
		dataWatcher.updateObject(HURT_DIR, dir);
	}

	public float getDamage() {
		return dataWatcher.getWatchableObjectFloat(DAMAGE);
	}

	public void setDamage(float damage) {
		dataWatcher.updateObject(DAMAGE, damage);
	}

	private int getBubbleTime() {
		return dataWatcher.getWatchableObjectInt(BUBBLE_TIME);
	}

	private void setBubbleTime(int time) {
		dataWatcher.updateObject(BUBBLE_TIME, time);
	}

	public void setPaddleState(boolean left, boolean right) {
		dataWatcher.updateObject(PADDLE_LEFT, (byte) (left ? 1 : 0));
		dataWatcher.updateObject(PADDLE_RIGHT, (byte) (right ? 1 : 0));
	}

	public boolean getPaddleState(int side) {
		return dataWatcher.getWatchableObjectByte(side == 0 ? PADDLE_LEFT : PADDLE_RIGHT) != 0 && controllingPassenger() != null;
	}

	public float getRowingTime(int side, float partialTicks) {
		return getPaddleState(side) ? clampedLerp(partialTicks, paddlePositions[side] - PADDLE_SPEED, paddlePositions[side]) : 0.0F;
	}

	public float getBubbleAngle(float partialTicks) {
		return bubbleAngleO + (bubbleAngle - bubbleAngleO) * partialTicks;
	}

	public boolean isUnderWater() {
		return status == Status.UNDER_WATER || status == Status.UNDER_FLOWING_WATER;
	}

	static float clampedLerp(float factor, float min, float max) {
		return factor < 0.0F ? min : factor > 1.0F ? max : min + factor * (max - min);
	}

	// ---------------------------------------------------------------- passengers

	/** The first passenger (the one that steers, if a player). */
	public Entity front() {
		return riddenByEntity;
	}

	/** The second passenger, on the seat. */
	public Entity back() {
		return seat != null && !seat.isDead ? seat.riddenByEntity : null;
	}

	public List<Entity> passengers() {
		List<Entity> list = new ArrayList<>(2);
		if (riddenByEntity != null) {
			list.add(riddenByEntity);
		}
		Entity back = back();
		if (back != null) {
			list.add(back);
		}
		return list;
	}

	public boolean isPassenger(Entity entity) {
		return entity != null && (entity == riddenByEntity || entity == back());
	}

	public boolean hasPassengers() {
		return riddenByEntity != null || back() != null;
	}

	protected int maxPassengers() {
		return McFlyConfig.newBoatPassengerSeat ? 2 : 1;
	}

	protected float singlePassengerOffset() {
		return 0.0F;
	}

	/** 26.3's `rideHeight`: a third of its height, a raft's 0.889 of it. */
	protected double rideHeight() {
		return isRaft() ? height * 0.8888889F : height / 3.0F;
	}

	/** The first passenger if it's a player (mobs don't steer). */
	public Entity controllingPassenger() {
		return riddenByEntity instanceof EntityPlayer ? riddenByEntity : null;
	}

	/** 26.3's `isLocalInstanceAuthoritative`: the steering player's client, else the server. */
	public boolean isAuthoritative() {
		Entity controller = controllingPassenger();
		if (controller != null) {
			return worldObj.isRemote && ((EntityPlayer) controller).isClientWorld();
		}
		return !worldObj.isRemote;
	}

	/** Whether this side moves the entity (its own client for a player, the server for the rest). */
	static boolean movesItself(Entity entity) {
		if (entity instanceof EntityPlayer player) {
			return entity.worldObj.isRemote && player.isClientWorld();
		}
		return !entity.worldObj.isRemote;
	}

	protected boolean canAddPassenger(Entity entity) {
		return passengers().size() < maxPassengers() && !isInsideOfMaterial(Material.water);
	}

	/** 26.3's `startRiding` into this boat (on the server): a player goes first when the first isn't one. */
	public boolean addPassenger(Entity entity) {
		if (worldObj.isRemote || isDead || entity == this || isPassenger(entity) || entity instanceof BoatSeat || entity.isSneaking()) {
			return false;
		}
		for (Entity vehicle = ridingEntity; vehicle != null; vehicle = vehicle.ridingEntity) {
			if (vehicle == entity) {
				return false;
			}
		}
		if (!canAddPassenger(entity)) {
			return false;
		}
		Entity front = riddenByEntity;
		if (front == null) {
			entity.mountEntity(this);
		} else if (entity instanceof EntityPlayer && !(front instanceof EntityPlayer)) {
			front.mountEntity(seat());
			entity.mountEntity(this);
		} else {
			entity.mountEntity(seat());
		}
		return true;
	}

	/** Its seat, made now if it has none. */
	private BoatSeat seat() {
		if (seat == null || seat.isDead) {
			seat = new BoatSeat(worldObj, this);
			seat.forceSpawn = true;
			worldObj.spawnEntityInWorld(seat);
		}
		return seat;
	}

	/** 26.3's `ejectPassengers`: the last first. */
	public void ejectPassengers() {
		Entity back = back();
		if (back != null) {
			back.mountEntity(null);
		}
		if (riddenByEntity != null) {
			riddenByEntity.mountEntity(null);
		}
	}

	@Override
	public void updateRiderPosition() {
		if (riddenByEntity != null) {
			positionRider(riddenByEntity);
		}
	}

	@Override
	public double getMountedYOffset() {
		return rideHeight();
	}

	/**
	 * 26.3's `getPassengerAttachmentPoint` and `positionRider`: up by its ride height (a player's feet 0.6 under that, its
	 * vehicle attachment), forward by its place (one alone 0 or a chest boat's 0.15; of two, 0.2 and -0.6, an animal 0.2
	 * further); the passenger turned with the boat on the side that moves it, kept within 105 degrees of it.
	 */
	public void positionRider(Entity passenger) {
		List<Entity> passengers = passengers();
		float offset = singlePassengerOffset();
		if (passengers.size() > 1) {
			offset = passengers.indexOf(passenger) == 0 ? 0.2F : -0.6F;
			if (passenger instanceof EntityAnimal) {
				offset += 0.2F;
			}
		}
		float angle = -rotationYaw * (float) Math.PI / 180.0F;
		double feet = posY + rideHeight() - (passenger instanceof EntityPlayer ? 0.6D : 0.0D);
		passenger.setPosition(posX + offset * MathHelper.sin(angle), feet + passenger.yOffset, posZ + offset * MathHelper.cos(angle));
		if (McFlyConfig.stopBoatRotationLock) {
			return;
		}
		if (movesItself(passenger)) {
			passenger.rotationYaw += deltaRotation;
			if (passenger instanceof EntityLivingBase living) {
				living.rotationYawHead += deltaRotation;
			}
		}
		clampRotation(passenger);
	}

	/** 26.3's `clampRotation`: a mob faces the boat; anything else is kept within 105 degrees of it. */
	protected float clampRotation(Entity passenger) {
		float body = passengerBodyYRot(passenger);
		if (passenger instanceof EntityLivingBase living) {
			living.renderYawOffset = body;
		}
		if (passenger instanceof EntityLiving mob) {
			passenger.rotationYaw = body;
			// clampHeadRotationToBody: 1.7.10's mobs turn their heads 75 degrees from the body
			float head = mob.rotationYawHead;
			float delta = MathHelper.wrapAngleTo180_float(body - head);
			float clamped = MathHelper.clamp_float(delta, -75.0F, 75.0F);
			mob.rotationYawHead = head + delta - clamped;
			return MathHelper.wrapAngleTo180_float(body - passenger.rotationYaw);
		}
		float delta = MathHelper.wrapAngleTo180_float(passenger.rotationYaw - body);
		float target = MathHelper.clamp_float(delta, -105.0F, 105.0F);
		passenger.rotationYaw += target - delta;
		return target - delta;
	}

	private float passengerBodyYRot(Entity passenger) {
		return passenger instanceof EntityAnimal && passengers().size() == maxPassengers()
			? rotationYaw + (passenger.getEntityId() % 2 == 0 ? 90.0F : 270.0F)
			: rotationYaw;
	}

	/** 26.3's `onPassengerTurned` (the client's mouse, `MixinEntityTurn`). */
	public void onPassengerTurned(Entity passenger) {
		if (!McFlyConfig.stopBoatRotationLock) {
			passenger.prevRotationYaw += clampRotation(passenger);
		}
	}

	/** The boat a passenger is in (through its seat for the second); null for anything else. */
	public static McFlyBoat boatOf(Entity vehicle) {
		return vehicle instanceof McFlyBoat boat ? boat : vehicle instanceof BoatSeat seat ? seat.boat() : null;
	}

	/**
	 * 26.3's `getDismountLocationForPassenger` (the server, `MixinBoatDismount`): out to the side the passenger faces,
	 * past the boat's corner, onto a floor there or a block lower that holds it standing; over water, or with no room,
	 * on top of the boat. Null when the vehicle isn't a boat of ours.
	 */
	public static double[] dismountLocation(Entity vehicle, EntityLivingBase passenger) {
		McFlyBoat boat = boatOf(vehicle);
		if (boat == null) {
			if (vehicle instanceof BoatSeat) {
				return new double[]{vehicle.posX, vehicle.posY, vehicle.posZ};
			}
			return null;
		}
		World world = boat.worldObj;
		// getCollisionHorizontalEscapeVector(width * sqrt 2, its width, its facing)
		double distance = (boat.width * Math.sqrt(2.0D) + passenger.width + 1.0E-5F) / 2.0D;
		float dirX = -MathHelper.sin(passenger.rotationYaw * (float) Math.PI / 180.0F);
		float dirZ = MathHelper.cos(passenger.rotationYaw * (float) Math.PI / 180.0F);
		float scale = Math.max(Math.abs(dirX), Math.abs(dirZ));
		double x = boat.posX + dirX * distance / scale;
		double z = boat.posZ + dirZ * distance / scale;
		int bx = MathHelper.floor_double(x);
		int by = MathHelper.floor_double(boat.boundingBox.maxY);
		int bz = MathHelper.floor_double(z);
		if (world.getBlock(bx, by - 1, bz).getMaterial() != Material.water) {
			for (int y = by; y >= by - 1; y--) {
				double floor = floorHeight(world, bx, y, bz);
				if (!Double.isInfinite(floor) && floor < 1.0D) {
					double half = passenger.width / 2.0D;
					double feet = y + floor;
					AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - half, feet, z - half, x + half, feet + passenger.height, z + half);
					if (world.func_147461_a(box).isEmpty()) {
						return new double[]{x, feet, z};
					}
				}
			}
		}
		return new double[]{boat.posX, boat.boundingBox.maxY, boat.posZ};
	}

	/** 26.3's `getBlockFloorHeight`: the top of the block's collision there, or of the one under it less 1 if that's 1 or more. */
	private static double floorHeight(World world, int x, int y, int z) {
		double top = collisionTop(world, x, y, z);
		if (!Double.isInfinite(top)) {
			return top - y;
		}
		double below = collisionTop(world, x, y - 1, z) - (y - 1);
		return below >= 1.0D ? below - 1.0D : Double.NEGATIVE_INFINITY;
	}

	private static double collisionTop(World world, int x, int y, int z) {
		List<AxisAlignedBB> boxes = new ArrayList<>();
		AxisAlignedBB cell = AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 3, z + 2);
		world.getBlock(x, y, z).addCollisionBoxesToList(world, x, y, z, cell, boxes, null);
		double top = Double.NEGATIVE_INFINITY;
		for (AxisAlignedBB box : boxes) {
			top = Math.max(top, box.maxY);
		}
		return top;
	}

	@Override
	public boolean shouldDismountInWater(Entity rider) {
		// 26.3's boats throw their passengers off after 60 ticks under water, not at once
		return false;
	}

	/** Mobs that don't get pushed in (26.3's `#cannot_be_pushed_onto_boats`: players, water animals) or EFR's list leaves out. */
	static boolean mayBePushedIn(Entity entity) {
		if (entity instanceof EntityPlayer || entity instanceof EntityWaterMob) {
			return false;
		}
		String[] list = McFlyConfig.newBoatEntityBlacklist;
		boolean listed = false;
		String id = EntityList.getEntityString(entity);
		for (String entry : list) {
			if (entry.startsWith("classpath:") ? entity.getClass().getName().contains(entry.substring("classpath:".length())) : entry.equals(id)) {
				listed = true;
				break;
			}
		}
		return listed == McFlyConfig.newBoatEntityBlacklistAsWhitelist;
	}

	// ---------------------------------------------------------------- use, hits

	@Override
	public boolean interactFirst(EntityPlayer player) {
		if (player.isSneaking() || outOfControlTicks >= 60.0F) {
			return false;
		}
		return worldObj.isRemote || addPassenger(player);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (worldObj.isRemote || isDead) {
			return true;
		}
		if (isEntityInvulnerable()) {
			return false;
		}
		// 26.3's projectiles don't hit their shooter's vehicle
		if (source instanceof EntityDamageSourceIndirect && source.getEntity() != null && isPassenger(source.getEntity())) {
			return false;
		}
		setHurtDir(-getHurtDir());
		setHurtTime(10);
		setBeenAttacked();
		setDamage(getDamage() + amount * 10.0F);
		GameEvents.post(worldObj, GameEvent.get("entity_damage"), posX, posY, posZ, source.getEntity());
		boolean creative = source.getEntity() instanceof EntityPlayer player && player.capabilities.isCreativeMode;
		if (creative || getDamage() <= 40.0F) {
			if (creative) {
				setDead();
			}
		} else {
			destroy(source);
		}
		return true;
	}

	/** 26.3's `destroy`: gone, its item dropped with its name (1.7.10 has no `doEntityDrops`: always, as its boat). */
	protected void destroy(DamageSource source) {
		setDead();
		Item item = dropItem();
		if (item != null) {
			ItemStack stack = new ItemStack(item);
			if (customName != null) {
				stack.setStackDisplayName(customName);
			}
			entityDropItem(stack, 0.0F);
		}
	}

	@Override
	public void performHurtAnimation() {
		// 26.3's `animateHurt`
		setHurtDir(-getHurtDir());
		setHurtTime(10);
		setDamage(getDamage() * 11.0F);
	}

	@Override
	public void setDead() {
		if (!worldObj.isRemote && !isDead) {
			ejectPassengers();
			if (seat != null) {
				seat.setDead();
				seat = null;
			}
		}
		super.setDead();
	}

	// ---------------------------------------------------------------- collisions

	@Override
	public AxisAlignedBB getBoundingBox() {
		return boundingBox;
	}

	/** 26.3's `canVehicleCollide`: solid or pushable entities, not its own passengers. */
	@Override
	public AxisAlignedBB getCollisionBox(Entity other) {
		if (other instanceof BoatSeat || isPassenger(other) || other == ridingEntity) {
			return null;
		}
		return other.canBePushed() || other.getBoundingBox() != null ? other.boundingBox : null;
	}

	@Override
	public boolean canBePushed() {
		return true;
	}

	@Override
	public boolean canBeCollidedWith() {
		return !isDead;
	}

	@Override
	protected boolean canTriggerWalking() {
		return false;
	}

	@Override
	public float getEyeHeight() {
		return 0.5625F;
	}

	/** 26.3's `AbstractBoat.push`: another boat below its top, anything else whose feet are no higher than its bottom. */
	@Override
	public void applyEntityCollision(Entity entity) {
		if (entity instanceof McFlyBoat) {
			if (entity.boundingBox.minY < boundingBox.maxY) {
				push(entity);
			}
		} else if (entity.boundingBox.minY <= boundingBox.minY) {
			push(entity);
		}
	}

	/** 26.3's `Entity.push(Entity)`: apart by 0.05, a vehicle with passengers not pushed. */
	private void push(Entity entity) {
		if (isPassenger(entity) || entity instanceof BoatSeat || entity.ridingEntity == this || entity.riddenByEntity == this || entity.noClip || noClip) {
			return;
		}
		double dx = entity.posX - posX;
		double dz = entity.posZ - posZ;
		double max = Math.max(Math.abs(dx), Math.abs(dz));
		if (max >= 0.01D) {
			max = Math.sqrt(max);
			dx /= max;
			dz /= max;
			double scale = Math.min(1.0D, 1.0D / max);
			dx *= scale * 0.05D;
			dz *= scale * 0.05D;
			if (!hasPassengers()) {
				addVelocity(-dx, 0.0D, -dz);
			}
			if (entity.riddenByEntity == null && entity.canBePushed()) {
				entity.addVelocity(dx, 0.0D, dz);
			}
		}
	}

	// ---------------------------------------------------------------- the client's view of others' boats

	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
		if (isAuthoritative()) {
			return;
		}
		// 26.3's: three steps there
		lerpX = x;
		lerpY = y;
		lerpZ = z;
		lerpYRot = yaw;
		lerpXRot = pitch;
		lerpSteps = 3;
	}

	private void tickLerp() {
		if (isAuthoritative()) {
			lerpSteps = 0;
			return;
		}
		if (worldObj.isRemote && lerpSteps > 0) {
			double x = posX + (lerpX - posX) / lerpSteps;
			double y = posY + (lerpY - posY) / lerpSteps;
			double z = posZ + (lerpZ - posZ) / lerpSteps;
			rotationYaw += MathHelper.wrapAngleTo180_float(lerpYRot - rotationYaw) / lerpSteps;
			rotationPitch += (lerpXRot - rotationPitch) / lerpSteps;
			lerpSteps--;
			setPosition(x, y, z);
		}
	}

	/** A position the server sent back (its own after refusing the client's). */
	public void snapTo(double x, double y, double z, float yaw, float pitch) {
		setPositionAndRotation(x, y, z, yaw, pitch);
		motionX = motionY = motionZ = 0.0D;
	}

	// ---------------------------------------------------------------- each tick

	@Override
	public void onUpdate() {
		oldStatus = status;
		status = computeStatus();
		if (status != Status.UNDER_WATER && status != Status.UNDER_FLOWING_WATER) {
			outOfControlTicks = 0.0F;
		} else {
			outOfControlTicks++;
		}
		if (!worldObj.isRemote && outOfControlTicks >= 60.0F) {
			ejectPassengers();
		}
		if (getHurtTime() > 0) {
			setHurtTime(getHurtTime() - 1);
		}
		if (getDamage() > 0.0F) {
			setDamage(getDamage() - 1.0F);
		}
		if (!worldObj.isRemote) {
			firstGoodX = posX;
			firstGoodY = posY;
			firstGoodZ = posZ;
			keepSeat();
		}
		super.onUpdate();
		tickLerp();
		if (isAuthoritative()) {
			if (!(riddenByEntity instanceof EntityPlayer)) {
				setPaddleState(false, false);
			}
			floatBoat();
			if (worldObj.isRemote) {
				readInput();
				controlBoat();
			}
			// moving touches the blocks it's in (1.7.10's `moveEntity`), as 26.3's `applyEffectsFromBlocks`
			moveEntity(motionX, motionY, motionZ);
			if (worldObj.isRemote) {
				McFlyNetwork.CHANNEL.sendToServer(new BoatMoveMessage(this));
			}
		} else {
			motionX = motionY = motionZ = 0.0D;
			deltaRotation = 0.0F;
			func_145775_I();
		}
		breakLilyPads();
		tickBubbleColumn();
		tickPaddles();
		pushAndTakeIn();
	}

	/** The seat's upkeep (server): the second passenger moves up when the first has gone; an empty seat goes. */
	private void keepSeat() {
		if (seat != null && (seat.isDead || seat.boat() != this)) {
			seat = null;
		}
		if (seat != null) {
			if (riddenByEntity == null && seat.riddenByEntity != null) {
				seat.riddenByEntity.mountEntity(this);
			}
			if (seat.riddenByEntity == null) {
				seat.setDead();
				seat = null;
			}
		}
	}

	/** The steering player's keys (its client: its strafing and forward, as its movement input set them): 26.3's `setInput`. */
	private void readInput() {
		if (riddenByEntity instanceof EntityPlayer player) {
			inputLeft = player.moveStrafing > 0.0F;
			inputRight = player.moveStrafing < 0.0F;
			inputUp = player.moveForward > 0.0F;
			inputDown = player.moveForward < 0.0F;
		} else {
			inputLeft = inputRight = inputUp = inputDown = false;
		}
	}

	/** 26.3's `LilyPadBlock.entityInside`: a lily pad it's in breaks (the server). */
	private void breakLilyPads() {
		if (worldObj.isRemote) {
			return;
		}
		AxisAlignedBB box = boundingBox;
		double e = 1.0E-5D;
		for (int x = MathHelper.floor_double(box.minX + e); x <= MathHelper.floor_double(box.maxX - e); x++) {
			for (int y = MathHelper.floor_double(box.minY + e); y <= MathHelper.floor_double(box.maxY - e); y++) {
				for (int z = MathHelper.floor_double(box.minZ + e); z <= MathHelper.floor_double(box.maxZ - e); z++) {
					if (worldObj.getBlock(x, y, z) instanceof BlockLilyPad) {
						worldObj.func_147480_a(x, y, z, true);
					}
				}
			}
		}
	}

	private void tickPaddles() {
		for (int i = 0; i <= 1; i++) {
			if (getPaddleState(i)) {
				if (paddlePositions[i] % TWO_PI <= PADDLE_SOUND_TIME && (paddlePositions[i] + PADDLE_SPEED) % TWO_PI >= PADDLE_SOUND_TIME) {
					String sound = paddleSound();
					if (sound != null) {
						// the paddle's side: the look turned a quarter
						float yaw = -rotationYaw * (float) Math.PI / 180.0F;
						float pitch = rotationPitch * (float) Math.PI / 180.0F;
						double viewX = MathHelper.sin(yaw) * MathHelper.cos(pitch);
						double viewZ = MathHelper.cos(yaw) * MathHelper.cos(pitch);
						double dx = i == 1 ? -viewZ : viewZ;
						double dz = i == 1 ? viewX : -viewX;
						// heard from the server (1.7.10's client plays none of its own here)
						worldObj.playSoundEffect(posX + dx, posY, posZ + dz, sound, 1.0F, 0.8F + 0.4F * rand.nextFloat());
					}
				}
				paddlePositions[i] += PADDLE_SPEED;
			} else {
				paddlePositions[i] = 0.0F;
			}
		}
	}

	private String paddleSound() {
		switch (computeStatus()) {
			case IN_WATER:
			case UNDER_WATER:
			case UNDER_FLOWING_WATER:
				return McFly.MC_ASSETS + ":entity.boat.paddle_water";
			case ON_LAND:
				return McFly.MC_ASSETS + ":entity.boat.paddle_land";
			default:
				return null;
		}
	}

	/**
	 * 26.3's end of `tick`: what it touches (inflated 0.2 across) is pushed, or, on the server with no player steering,
	 * gets in if it's a living thing that fits (narrower than the boat). A client pushes only its own player.
	 */
	private void pushAndTakeIn() {
		@SuppressWarnings("unchecked")
		List<Entity> near = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(0.2D, -0.01D, 0.2D));
		if (near.isEmpty()) {
			return;
		}
		boolean takeIn = !worldObj.isRemote && !(controllingPassenger() instanceof EntityPlayer);
		for (Entity entity : near) {
			if (entity == ridingEntity || !entity.canBePushed() || entity.isDead
				|| worldObj.isRemote && !(entity instanceof EntityPlayer player && player.isClientWorld())) {
				continue;
			}
			if (takeIn && passengers().size() < maxPassengers() && entity.ridingEntity == null && entity.width < width && entity instanceof EntityLivingBase
				&& mayBePushedIn(entity)) {
				addPassenger(entity);
			} else {
				applyEntityCollision(entity);
			}
		}
	}

	// ---------------------------------------------------------------- floating and steering

	private Status computeStatus() {
		Status under = underwaterStatus();
		if (under != null) {
			waterLevel = boundingBox.maxY;
			return under;
		}
		if (checkInWater()) {
			return Status.IN_WATER;
		}
		float friction = groundFriction();
		if (friction > 0.0F) {
			landFriction = Math.min(friction, McFlyConfig.newBoatMaxLandSpeed);
			return Status.ON_LAND;
		}
		return Status.IN_AIR;
	}

	/**
	 * Water's height in the block (26.3's `FluidState.getHeight`): 1 under more water, else its level's (a source and
	 * falling water 8/9); a water block that isn't a liquid (kelp, a bubble column) as a source; -1 for none.
	 */
	public static float waterHeight(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.getMaterial() == Material.water ? ownHeight(world, block, x, y, z) : -1.0F;
	}

	/** Any liquid's height in its block, as {@link #waterHeight}; 0 for none. */
	public static float liquidHeight(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.getMaterial().isLiquid() ? ownHeight(world, block, x, y, z) : 0.0F;
	}

	private static float ownHeight(World world, Block block, int x, int y, int z) {
		if (world.getBlock(x, y + 1, z).getMaterial() == block.getMaterial()) {
			return 1.0F;
		}
		if (block instanceof BlockLiquid) {
			return 1.0F - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(x, y, z));
		}
		if (block instanceof IFluidBlock fluid) {
			return Math.min(1.0F, Math.abs(fluid.getFilledPercentage(world, x, y, z)));
		}
		return 8.0F / 9.0F;
	}

	private boolean isWaterSource(int x, int y, int z) {
		Block block = worldObj.getBlock(x, y, z);
		return !(block instanceof BlockLiquid || block instanceof IFluidBlock) || worldObj.getBlockMetadata(x, y, z) == 0;
	}

	private Status underwaterStatus() {
		AxisAlignedBB box = boundingBox;
		double maxY = box.maxY + 0.001D;
		int x0 = MathHelper.floor_double(box.minX);
		int x1 = MathHelper.ceiling_double_int(box.maxX);
		int y0 = MathHelper.floor_double(box.maxY);
		int y1 = MathHelper.ceiling_double_int(maxY);
		int z0 = MathHelper.floor_double(box.minZ);
		int z1 = MathHelper.ceiling_double_int(box.maxZ);
		boolean under = false;
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				for (int z = z0; z < z1; z++) {
					float height = waterHeight(worldObj, x, y, z);
					if (height >= 0.0F && maxY < y + height) {
						if (!isWaterSource(x, y, z)) {
							return Status.UNDER_FLOWING_WATER;
						}
						under = true;
					}
				}
			}
		}
		return under ? Status.UNDER_WATER : null;
	}

	private boolean checkInWater() {
		AxisAlignedBB box = boundingBox;
		int x0 = MathHelper.floor_double(box.minX);
		int x1 = MathHelper.ceiling_double_int(box.maxX);
		int y0 = MathHelper.floor_double(box.minY);
		int y1 = MathHelper.ceiling_double_int(box.minY + 0.001D);
		int z0 = MathHelper.floor_double(box.minZ);
		int z1 = MathHelper.ceiling_double_int(box.maxZ);
		boolean in = false;
		waterLevel = -Double.MAX_VALUE;
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				for (int z = z0; z < z1; z++) {
					float height = waterHeight(worldObj, x, y, z);
					if (height >= 0.0F) {
						float top = y + height;
						waterLevel = Math.max(top, waterLevel);
						in |= box.minY < top;
					}
				}
			}
		}
		return in;
	}

	/** 26.3's `getGroundFriction`: the friction of the blocks whose collision meets a sliver under it (lily pads aside). */
	public float groundFriction() {
		AxisAlignedBB bb = boundingBox;
		AxisAlignedBB under = AxisAlignedBB.getBoundingBox(bb.minX, bb.minY - 0.001D, bb.minZ, bb.maxX, bb.minY, bb.maxZ);
		int x0 = MathHelper.floor_double(under.minX) - 1;
		int x1 = MathHelper.ceiling_double_int(under.maxX) + 1;
		int y0 = MathHelper.floor_double(under.minY) - 1;
		int y1 = MathHelper.ceiling_double_int(under.maxY) + 1;
		int z0 = MathHelper.floor_double(under.minZ) - 1;
		int z1 = MathHelper.ceiling_double_int(under.maxZ) + 1;
		float friction = 0.0F;
		int count = 0;
		List<AxisAlignedBB> boxes = new ArrayList<>();
		for (int x = x0; x < x1; x++) {
			for (int z = z0; z < z1; z++) {
				int edges = (x != x0 && x != x1 - 1 ? 0 : 1) + (z != z0 && z != z1 - 1 ? 0 : 1);
				if (edges == 2) {
					continue;
				}
				for (int y = y0; y < y1; y++) {
					if (edges <= 0 || y != y0 && y != y1 - 1) {
						Block block = worldObj.getBlock(x, y, z);
						if (block instanceof BlockLilyPad) {
							continue;
						}
						boxes.clear();
						block.addCollisionBoxesToList(worldObj, x, y, z, under, boxes, this);
						if (!boxes.isEmpty()) {
							friction += block.slipperiness;
							count++;
						}
					}
				}
			}
		}
		return count == 0 ? 0.0F : friction / count;
	}

	private float waterLevelAbove() {
		AxisAlignedBB box = boundingBox;
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.ceiling_double_int(box.maxX);
		int minY = MathHelper.floor_double(box.maxY);
		int maxY = MathHelper.ceiling_double_int(box.maxY - lastYd);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.ceiling_double_int(box.maxZ);
		layers:
		for (int y = minY; y < maxY; y++) {
			float blockHeight = 0.0F;
			for (int x = minX; x < maxX; x++) {
				for (int z = minZ; z < maxZ; z++) {
					float height = waterHeight(worldObj, x, y, z);
					if (height >= 0.0F) {
						blockHeight = Math.max(blockHeight, height);
					}
					if (blockHeight >= 1.0F) {
						continue layers;
					}
				}
			}
			if (blockHeight < 1.0F) {
				return y + blockHeight;
			}
		}
		return maxY + 1;
	}

	/** 26.3's `floatBoat`: gravity 0.04; friction and buoyancy by its status; a landing from the air onto water snapped up. */
	private void floatBoat() {
		double vspeed = -0.04D;
		double buoyancy = 0.0D;
		float invFriction = 0.05F;
		if (oldStatus == Status.IN_AIR && status != Status.IN_AIR && status != Status.ON_LAND) {
			waterLevel = posY + height;
			double targetY = waterLevelAbove() - height + 0.101D;
			if (worldObj.getCollidingBoundingBoxes(this, boundingBox.getOffsetBoundingBox(0.0D, targetY - posY, 0.0D)).isEmpty()) {
				setPosition(posX, targetY, posZ);
				motionY = 0.0D;
				lastYd = 0.0D;
			}
			status = Status.IN_WATER;
			return;
		}
		switch (status) {
			case IN_WATER:
				buoyancy = (waterLevel - posY) / height;
				invFriction = 0.9F;
				break;
			case UNDER_FLOWING_WATER:
				vspeed = -7.0E-4D;
				invFriction = 0.9F;
				break;
			case UNDER_WATER:
				buoyancy = 0.01F;
				invFriction = 0.45F;
				break;
			case IN_AIR:
				invFriction = 0.9F;
				break;
			case ON_LAND:
				invFriction = landFriction;
				if (controllingPassenger() instanceof EntityPlayer) {
					landFriction /= 2.0F;
				}
				break;
			default:
				break;
		}
		motionX *= invFriction;
		motionY += vspeed;
		motionZ *= invFriction;
		deltaRotation *= invFriction;
		if (buoyancy > 0.0D) {
			motionY = (motionY + buoyancy * (0.04D / 0.65D)) * 0.75D;
		}
	}

	/** 26.3's `controlBoat` (the steering player's client): turning 1 a tick, forward 0.04, back 0.005. */
	private void controlBoat() {
		if (!hasPassengers()) {
			return;
		}
		float acceleration = 0.0F;
		if (inputLeft) {
			deltaRotation--;
		}
		if (inputRight) {
			deltaRotation++;
		}
		if (inputRight != inputLeft && !inputUp && !inputDown) {
			acceleration += 0.005F;
		}
		rotationYaw += deltaRotation;
		if (inputUp) {
			acceleration += 0.04F;
		}
		if (inputDown) {
			acceleration -= 0.005F;
		}
		if (status == Status.IN_WATER || isUnderWater()) {
			// EFR's option (1 is 26.3's)
			acceleration *= McFlyConfig.newBoatSpeed;
		}
		float yaw = rotationYaw * (float) Math.PI / 180.0F;
		motionX += MathHelper.sin(-yaw) * acceleration;
		motionZ += MathHelper.cos(yaw) * acceleration;
		setPaddleState(inputRight && !inputLeft || inputUp, inputLeft && !inputRight || inputUp);
	}

	/** 26.3's `checkFallDamage`: the fall is kept (over water it isn't), never hurts it or its passengers. */
	@Override
	protected void updateFallState(double distance, boolean onGround) {
		lastYd = motionY;
		if (ridingEntity == null) {
			if (onGround) {
				fallDistance = 0.0F;
			} else if (worldObj.getBlock(MathHelper.floor_double(posX), MathHelper.floor_double(posY) - 1, MathHelper.floor_double(posZ)).getMaterial() != Material.water
				&& distance < 0.0D) {
				fallDistance -= (float) distance;
			}
		}
	}

	// ---------------------------------------------------------------- bubble columns

	/** Over a bubble column's top (EFR's `BlockBubbleColumn`): 26.3's 60-tick timer, a splash now and then. */
	public void onAboveBubbleColumn(boolean dragDown) {
		if (!worldObj.isRemote) {
			isAboveBubbleColumn = true;
			bubbleColumnDirectionIsDown = dragDown;
			if (getBubbleTime() == 0) {
				setBubbleTime(60);
			}
		}
		if (!isUnderWater() && rand.nextInt(100) == 0) {
			if (worldObj.isRemote) {
				worldObj.playSound(posX, posY, posZ, getSplashSound(), 1.0F, 0.8F + 0.4F * rand.nextFloat(), false);
				worldObj.spawnParticle("splash", posX + rand.nextFloat(), posY + 0.7D, posZ + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
			} else {
				GameEvents.post(worldObj, GameEvent.get("splash"), posX, posY, posZ, controllingPassenger());
			}
		}
	}

	private void tickBubbleColumn() {
		if (worldObj.isRemote) {
			if (getBubbleTime() > 0) {
				bubbleMultiplier += 0.05F;
			} else {
				bubbleMultiplier -= 0.1F;
			}
			bubbleMultiplier = MathHelper.clamp_float(bubbleMultiplier, 0.0F, 1.0F);
			bubbleAngleO = bubbleAngle;
			bubbleAngle = 10.0F * (float) Math.sin(0.5D * ticksExisted) * bubbleMultiplier;
			return;
		}
		if (!isAboveBubbleColumn) {
			setBubbleTime(0);
		}
		int bubbleTime = getBubbleTime();
		if (bubbleTime > 0) {
			setBubbleTime(--bubbleTime);
			int diff = 60 - bubbleTime - 1;
			if (diff > 0 && bubbleTime == 0) {
				setBubbleTime(0);
				if (bubbleColumnDirectionIsDown) {
					ejectPassengers();
					worldObj.setEntityState(this, (byte) 71);
				} else {
					worldObj.setEntityState(this, (byte) 72);
				}
				bubbleColumnEffect(bubbleColumnDirectionIsDown);
			}
			isAboveBubbleColumn = false;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleHealthUpdate(byte id) {
		if (id == 71) {
			bubbleColumnEffect(true);
		} else if (id == 72) {
			bubbleColumnEffect(false);
		} else {
			super.handleHealthUpdate(id);
		}
	}

	private void bubbleColumnEffect(boolean dragDown) {
		if (isAuthoritative()) {
			if (dragDown) {
				motionY -= 0.7D;
			} else {
				motionY = 0.6D;
			}
		}
	}

	// ---------------------------------------------------------------- saved

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		String domain = tag.hasKey("Domain", 8) ? tag.getString("Domain") : "minecraft";
		String wood = tag.hasKey("Type", 8) ? tag.getString("Type") : "oak";
		dataWatcher.updateObject(TYPE, domain + ":" + wood);
		// EFR wrote a string and read a list: names were lost
		setCustomName(tag.hasKey("CustomName", 8) ? tag.getString("CustomName") : null);
		// EFR's empty `Seat` is dropped: a seat is made when a second passenger gets in
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		String key = dataWatcher.getWatchableObjectString(TYPE);
		int colon = key.indexOf(':');
		tag.setString("Type", key.substring(colon + 1));
		tag.setString("Domain", colon < 0 ? "minecraft" : key.substring(0, colon));
		if (customName != null) {
			tag.setString("CustomName", customName);
		}
	}
}
