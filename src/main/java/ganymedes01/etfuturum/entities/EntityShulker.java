package ganymedes01.etfuturum.entities;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.lib.Reference;
import io.github.asgerjon.mcfly.network.LevelEventMessage;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;

/**
 * The shulker as 26.3's `Shulker`, written anew (SPEC-modern-movement.md §76.13, the rebuild; the user: "Fuck EFR,
 * reimplement from scratch"): a golem in its block, held by one face, peeking out of its shell, shooting homing
 * bullets at what it targets, and teleporting when it can't stay. EFR's class name and id stay for old saves.
 * <p>
 * Faces are 26.3's 3D ids, which are 1.7.10's sides: down, up, north, south, west, east.
 */
public class EntityShulker extends EntityGolem implements IMob {

	/** Synced: the face it holds by, the raw peek (0 to 100), the colour (16: none). EFR's old slots. */
	private static final int DATA_FACE = 12;
	private static final int DATA_PEEK = 13;
	private static final int DATA_COLOR = 17;
	public static final byte NO_COLOR = 16;
	private static final String SOUND = Reference.MCAssetVer + ":entity.shulker.";
	private static final int[][] STEP = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	/**
	 * Its forward, by the face it opens towards: 26.3's `Direction.getRotation()` of that face applied to south (up:
	 * none; down: x by pi; the four sides: x by pi / 2 first, so all point down).
	 */
	private static final float[][] FORWARD = {{0, 0, -1}, {0, 0, 1}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0}};

	private float currentPeekAmountO;
	private float currentPeekAmount;
	/** 26.3's armour modifier "covered" (+20): put on when it closes, taken off when it opens; not there when it's new. */
	private boolean covered;
	/** 26.3's `NoAI`: no goals, no teleports. */
	private boolean noAi;
	/** The client's slide from the block it was in before (its block, and the ticks left of 6), or none. */
	private int[] clientOldAttachPosition;
	private int clientSideTeleportInterpolation;

	public EntityShulker(World world) {
		super(world);
		setSize(1.0F, 1.0F);
		experienceValue = 5;
		lookHelper = new ShulkerLook();
		tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F, 0.02F));
		tasks.addTask(4, new AttackGoal());
		tasks.addTask(7, new PeekGoal());
		tasks.addTask(8, new EntityAILookIdle(this));
		targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
		targetTasks.addTask(2, new NearestTarget(false));
		targetTasks.addTask(3, new NearestTarget(true));
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		dataWatcher.addObject(DATA_FACE, (byte) 0);
		dataWatcher.addObject(DATA_PEEK, (byte) 0);
		dataWatcher.addObject(DATA_COLOR, NO_COLOR);
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0);
	}

	@Override
	public boolean isAIEnabled() {
		return true;
	}

	/** 26.3's NoAI: none of its goals or senses (its face is still checked every tick). */
	@Override
	protected void updateAITasks() {
		if (!noAi) {
			super.updateAITasks();
		}
	}

	// ---------------------------------------------------------------- the data

	public int attachFace() {
		return dataWatcher == null ? 0 : MathHelper.clamp_int(dataWatcher.getWatchableObjectByte(DATA_FACE), 0, 5);
	}

	/** The face's name, as 26.3's `Direction` (tests). */
	public String attachFaceName() {
		return ForgeDirection.getOrientation(attachFace()).name().toLowerCase(java.util.Locale.ROOT);
	}

	private void setAttachFace(int face) {
		dataWatcher.updateObject(DATA_FACE, (byte) face);
	}

	private int rawPeekAmount() {
		return dataWatcher.getWatchableObjectByte(DATA_PEEK);
	}

	/** 26.3's setRawPeekAmount: on the server, its armour and its sound with it. */
	public void setRawPeekAmount(int amount) {
		if (!worldObj.isRemote) {
			covered = amount == 0;
			playSound(SOUND + (amount == 0 ? "close" : "open"), 1.0F, 1.0F);
		}
		dataWatcher.updateObject(DATA_PEEK, (byte) amount);
	}

	private boolean isClosed() {
		return rawPeekAmount() == 0;
	}

	/** How far its lid is up now, 0 to 1 (eased a twentieth a tick). */
	public float getPeek() {
		return currentPeekAmount;
	}

	public float getClientPeekAmount(float partialTick) {
		return currentPeekAmountO + (currentPeekAmount - currentPeekAmountO) * partialTick;
	}

	public byte getColor() {
		return dataWatcher.getWatchableObjectByte(DATA_COLOR);
	}

	public void setColor(byte color) {
		dataWatcher.updateObject(DATA_COLOR, color >= 0 && color <= 15 ? color : NO_COLOR);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		tag.setByte("AttachFace", (byte) attachFace());
		tag.setByte("Peek", (byte) rawPeekAmount());
		tag.setByte("Color", getColor());
		if (noAi) {
			tag.setBoolean("NoAI", true);
		}
		if (covered) {
			// 26.3 keeps it as an armour modifier in its attributes; 1.7.10 has no armour attribute
			tag.setBoolean("etfuturum:covered", true);
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		setAttachFace(MathHelper.clamp_int(tag.getByte("AttachFace"), 0, 5));
		dataWatcher.updateObject(DATA_PEEK, tag.getByte("Peek"));
		setColor(tag.hasKey("Color") ? tag.getByte("Color") : NO_COLOR);
		noAi = tag.getBoolean("NoAI");
		covered = tag.getBoolean("etfuturum:covered");
	}

	// ---------------------------------------------------------------- the tick

	@Override
	public void onUpdate() {
		motionX = motionY = motionZ = 0.0;
		super.onUpdate();
		motionX = motionY = motionZ = 0.0;
		if (!worldObj.isRemote && ridingEntity == null && !canStayAt(blockX(), blockY(), blockZ(), attachFace())) {
			findNewAttachment();
		}
		if (updatePeekAmount()) {
			onPeekAmountChange();
		}
		updateBox();
		if (worldObj.isRemote) {
			if (clientSideTeleportInterpolation > 0) {
				clientSideTeleportInterpolation--;
			} else {
				clientOldAttachPosition = null;
			}
			ignoreFrustumCheck = clientSideTeleportInterpolation > 0;
		}
		// 26.3's body never turns (its body control does nothing), and its look has no pitch: only the head turns
		renderYawOffset = prevRenderYawOffset = 0.0F;
		rotationPitch = prevRotationPitch = 0.0F;
	}

	private int blockX() {
		return MathHelper.floor_double(posX);
	}

	private int blockY() {
		return MathHelper.floor_double(posY);
	}

	private int blockZ() {
		return MathHelper.floor_double(posZ);
	}

	private void findNewAttachment() {
		int face = findAttachableSurface(blockX(), blockY(), blockZ());
		if (face >= 0) {
			setAttachFace(face);
		} else {
			teleportSomewhere();
		}
	}

	private boolean updatePeekAmount() {
		currentPeekAmountO = currentPeekAmount;
		float target = rawPeekAmount() * 0.01F;
		if (currentPeekAmount == target) {
			return false;
		}
		if (currentPeekAmount > target) {
			currentPeekAmount = MathHelper.clamp_float(currentPeekAmount - 0.05F, target, 1.0F);
		} else {
			currentPeekAmount = MathHelper.clamp_float(currentPeekAmount + 0.05F, 0.0F, target);
		}
		return true;
	}

	/** Its box at the new peek, and what's in the way of its growth pushed out by it (not other shulkers). */
	private void onPeekAmountChange() {
		updateBox();
		float now = physicalPeek(currentPeekAmount);
		float before = physicalPeek(currentPeekAmountO);
		int open = opposite(attachFace());
		float push = now - before;
		if (push > 0.0F) {
			AxisAlignedBB growth = progressBox(open, before, now, posX, posY, posZ);
			for (Object object : worldObj.getEntitiesWithinAABBExcludingEntity(this, growth)) {
				Entity entity = (Entity) object;
				if (!(entity instanceof EntityShulker) && !entity.noClip && entity.ridingEntity != this && ridingEntity != entity) {
					entity.moveEntity(push * STEP[open][0], push * STEP[open][1], push * STEP[open][2]);
				}
			}
		}
	}

	/** 26.3's physical peek: how far the box reaches out, 0 to 1. */
	private static float physicalPeek(float amount) {
		return 0.5F - MathHelper.sin((0.5F + amount) * (float) Math.PI) * 0.5F;
	}

	static int opposite(int face) {
		return face ^ 1;
	}

	/**
	 * 26.3's getProgressDeltaAabb: its cube (1 wide, its feet at the bottom) stretched towards the face by the larger
	 * progress, and cut from the other side by one plus the smaller (from -1: the whole cube stretched).
	 */
	static AxisAlignedBB progressBox(int face, float from, float to, double x, double y, double z) {
		double max = Math.max(from, to);
		double min = Math.min(from, to);
		double[] low = {-0.5, 0.0, -0.5};
		double[] high = {0.5, 1.0, 0.5};
		for (int axis = 0; axis < 3; axis++) {
			double expand = STEP[face][axis] * max;
			if (expand < 0.0) {
				low[axis] += expand;
			} else if (expand > 0.0) {
				high[axis] += expand;
			}
			double contract = -STEP[face][axis] * (1.0 + min);
			if (contract < 0.0) {
				low[axis] -= contract;
			} else if (contract > 0.0) {
				high[axis] -= contract;
			}
		}
		return AxisAlignedBB.getBoundingBox(low[0] + x, low[1] + y, low[2] + z, high[0] + x, high[1] + y, high[2] + z);
	}

	private void updateBox() {
		if (dataWatcher == null) {
			boundingBox.setBounds(posX - 0.5, posY, posZ - 0.5, posX + 0.5, posY + 1.0, posZ + 0.5);
			return;
		}
		boundingBox.setBB(progressBox(opposite(attachFace()), -1.0F, physicalPeek(currentPeekAmount), posX, posY, posZ));
	}

	// ---------------------------------------------------------------- where it can be

	/** Put anywhere, it stands in its block's middle unless it's riding; a move to another block closes its peek. */
	@Override
	public void setPosition(double x, double y, double z) {
		int oldX = MathHelper.floor_double(posX);
		int oldY = MathHelper.floor_double(posY);
		int oldZ = MathHelper.floor_double(posZ);
		if (ridingEntity != null) {
			posX = x;
			posY = y;
			posZ = z;
		} else {
			posX = MathHelper.floor_double(x) + 0.5;
			posY = MathHelper.floor_double(y + 0.5);
			posZ = MathHelper.floor_double(z) + 0.5;
		}
		updateBox();
		if (ticksExisted != 0 && dataWatcher != null) {
			int newX = blockX();
			int newY = blockY();
			int newZ = blockZ();
			if (newX != oldX || newY != oldY || newZ != oldZ) {
				dataWatcher.updateObject(DATA_PEEK, (byte) 0);
				if (worldObj.isRemote && ridingEntity == null && (clientOldAttachPosition == null
					|| clientOldAttachPosition[0] != newX || clientOldAttachPosition[1] != newY || clientOldAttachPosition[2] != newZ)) {
					clientOldAttachPosition = new int[] {oldX, oldY, oldZ};
					clientSideTeleportInterpolation = 6;
					lastTickPosX = prevPosX = posX;
					lastTickPosY = prevPosY = posY;
					lastTickPosZ = prevPosZ = posZ;
				}
			}
		}
	}

	/** 26.3 doesn't smooth a shulker's moves (its interpolation does nothing): it's put there. */
	@Override
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
		setPosition(x, y, z);
		setRotation(yaw, pitch);
	}

	/**
	 * Where its client draws it while it slides from its old block (26.3's getRenderPosition): the move back towards
	 * the old block, by ((6 - t) / 6)^2; null when not sliding.
	 */
	public double[] getRenderOffset(float partialTick) {
		if (clientOldAttachPosition == null || clientSideTeleportInterpolation <= 0) {
			return null;
		}
		double scale = (clientSideTeleportInterpolation - partialTick) / 6.0;
		scale *= scale;
		return new double[] {-(blockX() - clientOldAttachPosition[0]) * scale, -(blockY() - clientOldAttachPosition[1]) * scale,
			-(blockZ() - clientOldAttachPosition[2]) * scale};
	}

	/** The first face it can stay on there (26.3's order: down, up, north, south, west, east), or -1. */
	private int findAttachableSurface(int x, int y, int z) {
		for (int face = 0; face < 6; face++) {
			if (canStayAt(x, y, z, face)) {
				return face;
			}
		}
		return -1;
	}

	/**
	 * 26.3's canStayAt: its block free (air, or a moving piston in its own block), the block on that face solid on the
	 * side towards it, and room to open fully.
	 */
	private boolean canStayAt(int x, int y, int z, int face) {
		Block block = worldObj.getBlock(x, y, z);
		if (!block.isAir(worldObj, x, y, z) && !(block == Blocks.piston_extension && x == blockX() && y == blockY() && z == blockZ())) {
			return false;
		}
		int open = opposite(face);
		int fx = x + STEP[face][0];
		int fy = y + STEP[face][1];
		int fz = z + STEP[face][2];
		if (!worldObj.blockExists(fx, fy, fz) || !worldObj.isSideSolid(fx, fy, fz, ForgeDirection.getOrientation(open), false)) {
			return false;
		}
		AxisAlignedBB opened = progressBox(open, -1.0F, 1.0F, x + 0.5, y, z + 0.5).contract(1.0E-6, 1.0E-6, 1.0E-6);
		return worldObj.getCollidingBoundingBoxes(this, opened).isEmpty();
	}

	/**
	 * 26.3's teleportSomewhere (not without AI): 5 tries at random within 8 each way, at air with room and a face to
	 * stay on that isn't bedrock (26.3's tag `shulker_does_not_teleport_to`); its sound, the trail (level event 2016),
	 * its peek closed, no target.
	 */
	protected boolean teleportSomewhere() {
		if (noAi || !isEntityAlive()) {
			return false;
		}
		int oldX = blockX();
		int oldY = blockY();
		int oldZ = blockZ();
		for (int attempt = 0; attempt < 5; attempt++) {
			int x = oldX + rand.nextInt(17) - 8;
			int y = oldY + rand.nextInt(17) - 8;
			int z = oldZ + rand.nextInt(17) - 8;
			if (y > 0 && worldObj.isAirBlock(x, y, z)
				&& worldObj.getCollidingBoundingBoxes(this, AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1).contract(1.0E-6, 1.0E-6, 1.0E-6)).isEmpty()) {
				int face = findAttachableSurface(x, y, z);
				if (face >= 0 && worldObj.getBlock(x + STEP[face][0], y + STEP[face][1], z + STEP[face][2]) != Blocks.bedrock) {
					if (ridingEntity != null) {
						mountEntity(null);
					}
					setAttachFace(face);
					playSound(SOUND + "teleport", 1.0F, 1.0F);
					setPosition(x + 0.5, y, z + 0.5);
					int packed = (blockX() - oldX + 8 & 0xFF) << 16 | (blockY() - oldY + 8 & 0xFF) << 8 | blockZ() - oldZ + 8 & 0xFF;
					LevelEventMessage.sendAround((WorldServer) worldObj, 2016, oldX, oldY, oldZ, packed);
					dataWatcher.updateObject(DATA_PEEK, (byte) 0);
					setAttackTarget(null);
					return true;
				}
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- no motion of its own

	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
	}

	/** Moved (a piston): put at the new place if nothing's in the way, snapping to its block. */
	@Override
	public void moveEntity(double x, double y, double z) {
		if (x == 0.0 && y == 0.0 && z == 0.0) {
			return;
		}
		if (worldObj.getCollidingBoundingBoxes(this, boundingBox.copy().offset(x, y, z)).isEmpty()) {
			setPosition(posX + x, posY + y, posZ + z);
		}
	}

	@Override
	public void addVelocity(double x, double y, double z) {
	}

	@Override
	public void setVelocity(double x, double y, double z) {
	}

	/** It pushes nothing (26.3's push(Entity) does nothing); what touches it pushes itself away. */
	@Override
	protected void collideWithNearbyEntities() {
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return isEntityAlive() ? boundingBox : null;
	}

	@Override
	public boolean canBeCollidedWith() {
		return isEntityAlive();
	}

	@Override
	public void mountEntity(Entity entity) {
		if (entity != null) {
			setAttachFace(0);
		}
		super.mountEntity(entity);
		if (entity == null && worldObj.isRemote) {
			clientOldAttachPosition = new int[] {blockX(), blockY(), blockZ()};
		}
	}

	/** 26.3: its eyes half up its box, higher as it opens on the floor. */
	@Override
	public float getEyeHeight() {
		return 0.5F * (attachFace() == 0 && currentPeekAmount > 0.0F ? 1.0F + currentPeekAmount : 1.0F);
	}

	@Override
	public int getVerticalFaceSpeed() {
		return 180;
	}

	@Override
	public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
		rotationYaw = prevRotationYaw = 0.0F;
		rotationYawHead = prevRotationYawHead = 0.0F;
		return super.onSpawnWithEgg(data);
	}

	@Override
	public ItemStack getPickedResult(MovingObjectPosition target) {
		return io.github.asgerjon.mcfly.registry.McFlyEntities.eggFor(this);
	}

	// ---------------------------------------------------------------- hurt

	/** 26.3's armour: +20 while its "covered" modifier is on. */
	@Override
	public int getTotalArmorValue() {
		return super.getTotalArmorValue() + (covered ? 20 : 0);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float damage) {
		Entity direct = source.getSourceOfDamage();
		// 26.3's AbstractArrow: arrows and tridents bounce off it closed
		if (isClosed() && (direct instanceof EntityArrow || direct instanceof EntityThrownTrident)) {
			return false;
		}
		if (!super.attackEntityFrom(source, damage)) {
			return false;
		}
		if (getHealth() < getMaxHealth() * 0.5 && rand.nextInt(4) == 0) {
			teleportSomewhere();
		} else if (source.isProjectile() && direct instanceof EntityShulkerBullet) {
			hitByShulkerBullet();
		}
		return true;
	}

	/**
	 * 26.3: open, hit by a bullet, it teleports, and a new one of its colour appears where it was, failing (shulkers
	 * alive within its old box grown 8 - 1) / 5 of the time.
	 */
	private void hitByShulkerBullet() {
		double x = posX;
		double y = posY;
		double z = posZ;
		AxisAlignedBB oldBox = boundingBox.copy();
		if (!isClosed() && teleportSomewhere()) {
			int count = 0;
			for (Object object : worldObj.getEntitiesWithinAABB(EntityShulker.class, oldBox.expand(8.0, 8.0, 8.0))) {
				if (((EntityShulker) object).isEntityAlive()) {
					count++;
				}
			}
			float failure = (count - 1) / 5.0F;
			if (!(worldObj.rand.nextFloat() < failure)) {
				EntityShulker baby = new EntityShulker(worldObj);
				baby.setColor(getColor());
				baby.setPosition(x, y, z);
				worldObj.spawnEntityInWorld(baby);
			}
		}
	}

	@Override
	protected String getLivingSound() {
		return isClosed() ? null : SOUND + "ambient";
	}

	@Override
	protected String getHurtSound() {
		return SOUND + (isClosed() ? "hurt_closed" : "hurt");
	}

	@Override
	protected String getDeathSound() {
		return SOUND + "death";
	}

	@Override
	public int getTalkInterval() {
		return 80;
	}

	/** 26.3's loot table: a shell half the time, 0.5625 with Looting I and 0.0625 more each level. */
	@Override
	protected void dropFewItems(boolean recentlyHit, int looting) {
		int shells = rollShells(looting);
		if (shells > 0 && ModItems.SHULKER_SHELL.isEnabled()) {
			entityDropItem(ModItems.SHULKER_SHELL.newItemStack(shells), 0.0F);
		}
	}

	public int rollShells(int looting) {
		float chance = looting > 0 ? 0.5625F + 0.0625F * (looting - 1) : 0.5F;
		return rand.nextFloat() < chance ? 1 : 0;
	}

	// ---------------------------------------------------------------- its AI (26.3's goals)

	/** Open fully while it has a target; every 20 to 110 ticks a bullet at a target within 20, else it loses it. */
	private class AttackGoal extends EntityAIBase {
		private int attackTime;

		AttackGoal() {
			setMutexBits(3);
		}

		@Override
		public boolean shouldExecute() {
			EntityLivingBase target = getAttackTarget();
			return target != null && target.isEntityAlive() && worldObj.difficultySetting != EnumDifficulty.PEACEFUL;
		}

		@Override
		public void startExecuting() {
			attackTime = 20;
			setRawPeekAmount(100);
		}

		@Override
		public void resetTask() {
			setRawPeekAmount(0);
		}

		@Override
		public void updateTask() {
			if (worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
				return;
			}
			attackTime--;
			EntityLivingBase target = getAttackTarget();
			if (target == null) {
				return;
			}
			getLookHelper().setLookPositionWithEntity(target, 180.0F, 180.0F);
			if (getDistanceSqToEntity(target) < 400.0) {
				if (attackTime <= 0) {
					attackTime = 20 + rand.nextInt(10) * 20 / 2;
					worldObj.spawnEntityInWorld(new EntityShulkerBullet(worldObj, EntityShulker.this, target, axis(attachFace())));
					playSound(SOUND + "shoot", 2.0F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
				}
			} else {
				setAttackTarget(null);
			}
		}
	}

	/** A face's axis, 26.3's (x 0, y 1, z 2). */
	static int axis(int face) {
		return face < 2 ? 1 : face < 4 ? 2 : 0;
	}

	/**
	 * With no target, one in 40 a tick where it can stay: open to 30 for 20, 40 or 60 ticks (1.7.10 asks a task that
	 * isn't running every 3 ticks: 3 in 40 each time).
	 */
	private class PeekGoal extends EntityAIBase {
		private int peekTime;

		@Override
		public boolean shouldExecute() {
			return getAttackTarget() == null && rand.nextInt(40) < 3 && canStayAt(blockX(), blockY(), blockZ(), attachFace());
		}

		@Override
		public boolean continueExecuting() {
			return getAttackTarget() == null && peekTime > 0;
		}

		@Override
		public void startExecuting() {
			peekTime = 20 * (1 + rand.nextInt(3));
			setRawPeekAmount(30);
		}

		@Override
		public void resetTask() {
			if (getAttackTarget() == null) {
				setRawPeekAmount(0);
			}
		}

		@Override
		public void updateTask() {
			peekTime--;
		}
	}

	/**
	 * 26.3's ShulkerNearestAttackGoal and ShulkerDefenseAttackGoal: the nearest player seen (not in Peaceful), or, on
	 * a team, the nearest enemy, within its follow range but only 4 along its face's axis; one in 10 a tick (3 in 10
	 * each time 1.7.10 asks).
	 */
	private class NearestTarget extends EntityAITarget {
		private final boolean enemies;
		private EntityLivingBase found;

		NearestTarget(boolean enemies) {
			super(EntityShulker.this, true);
			this.enemies = enemies;
			setMutexBits(1);
		}

		@Override
		public boolean shouldExecute() {
			if (enemies ? getTeam() == null : worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
				return false;
			}
			if (rand.nextInt(10) >= 3) {
				return false;
			}
			double range = getTargetDistance();
			int faceAxis = axis(attachFace());
			AxisAlignedBB area = boundingBox.expand(faceAxis == 0 ? 4.0 : range, faceAxis == 1 ? 4.0 : range, faceAxis == 2 ? 4.0 : range);
			Class<? extends Entity> type = enemies ? EntityLivingBase.class : EntityPlayer.class;
			found = null;
			double nearest = Double.MAX_VALUE;
			double eyeY = posY + getEyeHeight();
			for (Object object : worldObj.getEntitiesWithinAABB(type, area)) {
				EntityLivingBase candidate = (EntityLivingBase) object;
				if ((enemies && !(candidate instanceof IMob)) || !isSuitableTarget(candidate, false)) {
					continue;
				}
				double dx = candidate.posX - posX;
				double dy = candidate.posY - eyeY;
				double dz = candidate.posZ - posZ;
				double distance = dx * dx + dy * dy + dz * dz;
				if (distance < nearest) {
					nearest = distance;
					found = candidate;
				}
			}
			return found != null;
		}

		@Override
		public void startExecuting() {
			setAttackTarget(found);
			super.startExecuting();
		}
	}

	/**
	 * 26.3's ShulkerLookControl: its head's yaw within the plane of its face (its forward and right turned by the face
	 * it opens towards), no pitch; with nothing to look at, back towards its body at 10 a tick.
	 */
	private class ShulkerLook extends EntityLookHelper {
		private double wantedX;
		private double wantedY;
		private double wantedZ;
		private float yMaxRotSpeed;
		private int lookAtCooldown;

		ShulkerLook() {
			super(EntityShulker.this);
		}

		@Override
		public void setLookPositionWithEntity(Entity entity, float yaw, float pitch) {
			setLookPosition(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ, yaw, pitch);
		}

		@Override
		public void setLookPosition(double x, double y, double z, float yaw, float pitch) {
			wantedX = x;
			wantedY = y;
			wantedZ = z;
			yMaxRotSpeed = yaw;
			lookAtCooldown = 2;
		}

		@Override
		public void onUpdateLook() {
			rotationPitch = 0.0F;
			if (lookAtCooldown > 0) {
				lookAtCooldown--;
				Float yaw = wantedYaw();
				if (yaw != null) {
					rotationYawHead = rotateTowards(rotationYawHead, yaw, yMaxRotSpeed);
				}
			} else {
				rotationYawHead = rotateTowards(rotationYawHead, renderYawOffset, 10.0F);
			}
		}

		private Float wantedYaw() {
			int up = opposite(attachFace());
			float[] forward = FORWARD[up];
			int[] u = STEP[up];
			float rightX = u[1] * forward[2] - u[2] * forward[1];
			float rightY = u[2] * forward[0] - u[0] * forward[2];
			float rightZ = u[0] * forward[1] - u[1] * forward[0];
			float x = (float) (wantedX - posX);
			float y = (float) (wantedY - (posY + getEyeHeight()));
			float z = (float) (wantedZ - posZ);
			float deltaRight = rightX * x + rightY * y + rightZ * z;
			float deltaForward = forward[0] * x + forward[1] * y + forward[2] * z;
			if (!(Math.abs(deltaRight) > 1.0E-5F) && !(Math.abs(deltaForward) > 1.0E-5F)) {
				return null;
			}
			return (float) (Math.atan2(-deltaRight, deltaForward) * 180.0F / (float) Math.PI);
		}

		private float rotateTowards(float from, float to, float max) {
			float delta = MathHelper.wrapAngleTo180_float(to - from);
			return from + MathHelper.clamp_float(delta, -max, max);
		}
	}
}
