package io.github.asgerjon.mcfly.end;

import ganymedes01.etfuturum.world.end.modern.EndGateways;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.List;
import java.util.Random;

/**
 * The end gateway as 26.3's (`TheEndGatewayBlockEntity` with `EndGatewayBlock.entityInside` and its portal transition;
 * SPEC-modern-movement.md §76.10, in McFly's package since §84; saved as EFR named it, `etfuturum.end_gateway`): its beam rises over its first 200 ticks; something going in (alive, not riding, not
 * the dragon or the wither) goes to its exit, keeping its turn and motion (an ender pearl stopped), and the gateway
 * cools down 40 ticks; every 2400 ticks it flashes. A gateway with no exit (a kill's) finds one on first use: land out
 * past 1024 blocks along its direction (or a small island made there), a return gateway 10 above the tallest block.
 */
public class EndGatewayBlockEntity extends TileEntity {

	private static final int SPAWN_TIME = 200;
	private static final int COOLDOWN_TIME = 40;
	private static final int ATTENTION_INTERVAL = 2400;
	private static final int EVENT_COOLDOWN = 1;
	private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
	/** How far a moving entity is looked for (its path this tick may cross the gateway: 26.3 checks along it). */
	private static final double REACH = 4.0;

	private long age;
	private int teleportCooldown;
	/** Where it leads, x y z, or null (found on first use). */
	private int[] exitPortal;
	private boolean exactTeleport;

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		saveCustom(tag);
	}

	private NBTTagCompound saveCustom(NBTTagCompound tag) {
		tag.setLong("Age", age);
		if (exitPortal != null) {
			tag.setIntArray("exit_portal", exitPortal.clone());
		}
		if (exactTeleport) {
			tag.setBoolean("ExactTeleport", true);
		}
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		loadCustom(tag);
	}

	private void loadCustom(NBTTagCompound tag) {
		age = tag.getLong("Age");
		exitPortal = null;
		if (tag.hasKey("exit_portal")) {
			int[] exit = tag.getIntArray("exit_portal");
			if (exit.length == 3 && inSpawnableBounds(exit)) {
				exitPortal = exit;
			}
		} else if (tag.hasKey("ExitPortal")) {
			// EFR's 1.9 port saved it as a compound
			NBTTagCompound old = tag.getCompoundTag("ExitPortal");
			exitPortal = new int[] {old.getInteger("X"), old.getInteger("Y"), old.getInteger("Z")};
		}
		exactTeleport = tag.getBoolean("ExactTeleport");
	}

	/** 26.3's Level.isInSpawnableBounds. */
	private static boolean inSpawnableBounds(int[] pos) {
		return pos[0] >= -30000000 && pos[2] >= -30000000 && pos[0] < 30000000 && pos[2] < 30000000 && pos[1] >= -20000000 && pos[1] < 20000000;
	}

	@Override
	public Packet getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 8, saveCustom(new NBTTagCompound()));
	}

	/** The client takes the age and exit (EFR's never read it, so a loaded gateway's beam always rose again). */
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		loadCustom(packet.func_148857_g());
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 256.0 * 256.0;
	}

	@Override
	public void updateEntity() {
		if (worldObj.isRemote) {
			// 26.3's beamAnimationTick
			age++;
			if (isCoolingDown()) {
				teleportCooldown--;
			}
			return;
		}
		// 26.3's entities tick before block entities: what's inside goes through first, then the gateway's tick
		if (!isCoolingDown()) {
			entitiesInside();
		}
		boolean spawning = isSpawning();
		boolean coolingDown = isCoolingDown();
		age++;
		if (coolingDown) {
			teleportCooldown--;
		} else if (age % ATTENTION_INTERVAL == 0L) {
			triggerCooldown();
		}
		if (spawning != isSpawning() || coolingDown != isCoolingDown()) {
			markDirty();
		}
	}

	/** 26.3's EndGatewayBlock.entityInside, for the first thing in the gateway this tick. */
	private void entitiesInside() {
		AxisAlignedBB cell = AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
		@SuppressWarnings("unchecked")
		List<Entity> near = worldObj.getEntitiesWithinAABB(Entity.class, cell.expand(REACH, REACH, REACH));
		for (Entity entity : near) {
			if (!canUsePortal(entity) || !passesThrough(entity, cell)) {
				continue;
			}
			// on its portal cooldown it stays (the cooldown kept up); else through, and its cooldown starts
			int delay = portalDelay(entity);
			if (entity.timeUntilPortal > 0) {
				entity.timeUntilPortal = delay;
			} else {
				entity.timeUntilPortal = delay;
				teleport(entity);
			}
			triggerCooldown();
			return;
		}
	}

	/** 26.3's Entity.canUsePortal(false): alive and not riding; the dragon and the wither never. */
	private static boolean canUsePortal(Entity entity) {
		return entity.isEntityAlive() && entity.ridingEntity == null && !(entity instanceof EntityDragon)
			&& !(entity instanceof EntityDragonPart) && !(entity instanceof EntityWither);
	}

	/** 26.3's getDimensionChangingDelay: a player's (10), a vehicle a player rides the player's, else 300. */
	private static int portalDelay(Entity entity) {
		return entity.riddenByEntity instanceof EntityPlayer rider ? rider.getPortalCooldown() : entity.getPortalCooldown();
	}

	/**
	 * Whether its box is in the gateway, or crossed it this tick (26.3 checks the blocks along an entity's movement, so a
	 * fast pearl doesn't skip it): the box swept from where it was to where it is, against the gateway's cell.
	 */
	private static boolean passesThrough(Entity entity, AxisAlignedBB cell) {
		AxisAlignedBB box = entity.boundingBox;
		if (box.maxX > cell.minX && box.minX < cell.maxX && box.maxY > cell.minY && box.minY < cell.maxY && box.maxZ > cell.minZ && box.minZ < cell.maxZ) {
			return true;
		}
		double dx = entity.posX - entity.prevPosX;
		double dy = entity.posY - entity.prevPosY;
		double dz = entity.posZ - entity.prevPosZ;
		if (dx * dx + dy * dy + dz * dz > 64.0) {
			// a jump, not a move
			return false;
		}
		// the box where it was, moved along (dx, dy, dz) from 0 to 1: does it meet the cell on the way?
		double[] from = {box.minX - dx, box.minY - dy, box.minZ - dz};
		double[] size = {box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ};
		double[] move = {dx, dy, dz};
		double[] low = {cell.minX, cell.minY, cell.minZ};
		double[] high = {cell.maxX, cell.maxY, cell.maxZ};
		double enter = 0.0;
		double leave = 1.0;
		for (int axis = 0; axis < 3; axis++) {
			double lo = low[axis] - size[axis];
			double hi = high[axis];
			if (Math.abs(move[axis]) < 1.0E-9) {
				if (from[axis] <= lo || from[axis] >= hi) {
					return false;
				}
				continue;
			}
			double t1 = (lo - from[axis]) / move[axis];
			double t2 = (hi - from[axis]) / move[axis];
			enter = Math.max(enter, Math.min(t1, t2));
			leave = Math.min(leave, Math.max(t1, t2));
			if (enter >= leave) {
				return false;
			}
		}
		return true;
	}

	/** 26.3's portal transition: to the exit; an ender pearl stopped and turned to 0, anything else keeping both. */
	private void teleport(Entity entity) {
		double[] to = portalPosition((WorldServer) worldObj);
		if (to == null) {
			return;
		}
		if (entity instanceof EntityEnderPearl) {
			entity.setLocationAndAngles(to[0], to[1], to[2], 0.0F, 0.0F);
			entity.motionX = entity.motionY = entity.motionZ = 0.0;
			entity.velocityChanged = true;
			return;
		}
		moveKeeping(entity, to[0], to[1], to[2]);
		if (entity.riddenByEntity != null) {
			// 26.3's vehicle takes its passengers along
			entity.updateRiderPosition();
			Entity rider = entity.riddenByEntity;
			if (rider instanceof EntityPlayerMP player) {
				player.playerNetServerHandler.setPlayerLocation(rider.posX, rider.posY, rider.posZ, rider.rotationYaw, rider.rotationPitch);
			}
		}
	}

	/** The entity's feet to (x, y, z), its turn kept, and its motion (a player's as the client last moved). */
	private static void moveKeeping(Entity entity, double x, double y, double z) {
		if (entity instanceof EntityPlayerMP player) {
			// 1.7.10's position packet stops the client: its motion (its last move) sent back after it
			double mx = player.posX - player.prevPosX;
			double my = player.posY - player.prevPosY;
			double mz = player.posZ - player.prevPosZ;
			player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
			player.playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(player.getEntityId(), mx, my, mz));
			return;
		}
		entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
	}

	/** 26.3's getPortalPosition: the exit (found and its return gateway made the first time, in the End), where to come out. */
	private double[] portalPosition(WorldServer world) {
		if (exitPortal == null && world.provider.dimensionId == 1) {
			int[] found = findOrCreateValidTeleportPos(world, xCoord, zCoord);
			int[] exit = {found[0], found[1] + GATEWAY_HEIGHT_ABOVE_SURFACE, found[2]};
			EndGateways.placeShell(world, exit[0], exit[1], exit[2], new int[] {xCoord, yCoord, zCoord}, false);
			setExitPosition(exit, exactTeleport);
		}
		if (exitPortal == null) {
			return null;
		}
		int[] at = exactTeleport ? exitPortal : findExitPosition(world, exitPortal);
		return new double[] {at[0] + 0.5, at[1], at[2] + 0.5};
	}

	/** One above the tallest full block (not bedrock) within 5 of the exit, its own column aside. */
	private static int[] findExitPosition(World world, int[] exit) {
		int[] at = findTallestBlock(world, exit[0], exit[1] + 2, exit[2], 5, false);
		return new int[] {at[0], at[1] + 1, at[2]};
	}

	private static int[] findOrCreateValidTeleportPos(WorldServer world, int gatewayX, int gatewayZ) {
		double[] tentative = findExitPortalXZPosTentative(world, gatewayX, gatewayZ);
		Chunk chunk = chunkAt(world, tentative[0], tentative[1]);
		int[] exit = findValidSpawnInChunk(world, chunk);
		if (exit == null) {
			exit = new int[] {MathHelper.floor_double(tentative[0] + 0.5), 75, MathHelper.floor_double(tentative[1] + 0.5)};
			EndGateways.smallIsland(world, new Random(EndGateways.asLong(exit[0], exit[1], exit[2])), exit[0], exit[1], exit[2]);
		}
		return findTallestBlock(world, exit[0], exit[1], exit[2], 16, true);
	}

	/** 1024 out along the gateway's direction from (0, 0); back 16 a step past land, then out 16 a step past void. */
	private static double[] findExitPortalXZPosTentative(WorldServer world, int gatewayX, int gatewayZ) {
		double length = Math.sqrt((double) gatewayX * gatewayX + (double) gatewayZ * gatewayZ);
		double dirX = length < 1.0E-5F ? 0.0 : gatewayX / length;
		double dirZ = length < 1.0E-5F ? 0.0 : gatewayZ / length;
		double x = dirX * 1024.0;
		double z = dirZ * 1024.0;
		for (int limit = 16; !isChunkEmpty(world, x, z) && limit-- > 0; ) {
			x += dirX * -16.0;
			z += dirZ * -16.0;
		}
		for (int limit = 16; isChunkEmpty(world, x, z) && limit-- > 0; ) {
			x += dirX * 16.0;
			z += dirZ * 16.0;
		}
		return new double[] {x, z};
	}

	private static Chunk chunkAt(World world, double x, double z) {
		return world.getChunkFromChunkCoords(MathHelper.floor_double(x / 16.0), MathHelper.floor_double(z / 16.0));
	}

	private static boolean isChunkEmpty(World world, double x, double z) {
		return highestSection(chunkAt(world, x, z)) < 0;
	}

	/** The highest of a chunk's 16-block sections with a block in it, or -1. */
	private static int highestSection(Chunk chunk) {
		ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
		for (int i = sections.length - 1; i >= 0; i--) {
			if (sections[i] != null && !sections[i].isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	/** The end stone nearest (0, 0, 0) in the chunk from y 30 up with two blocks above that aren't full. */
	private static int[] findValidSpawnInChunk(World world, Chunk chunk) {
		int section = highestSection(chunk);
		int maxY = (section < 0 ? 0 : section * 16) + 16 - 1;
		int minX = chunk.xPosition * 16;
		int minZ = chunk.zPosition * 16;
		int[] closest = null;
		double closestDist = 0.0;
		for (int z = minZ; z <= minZ + 15; z++) {
			for (int y = 30; y <= maxY; y++) {
				for (int x = minX; x <= minX + 15; x++) {
					if (chunk.getBlock(x & 15, y, z & 15) == Blocks.end_stone && !fullBlock(world, x, y + 1, z) && !fullBlock(world, x, y + 2, z)) {
						double dist = (x + 0.5) * (x + 0.5) + (y + 0.5) * (y + 0.5) + (z + 0.5) * (z + 0.5);
						if (closest == null || dist < closestDist) {
							closest = new int[] {x, y, z};
							closestDist = dist;
						}
					}
				}
			}
		}
		return closest;
	}

	/** The tallest full block within dist columns of the place (bedrock and its own column only if allowed). */
	private static int[] findTallestBlock(World world, int x, int y, int z, int dist, boolean allowBedrock) {
		int[] tallest = null;
		for (int xd = -dist; xd <= dist; xd++) {
			for (int zd = -dist; zd <= dist; zd++) {
				if (xd != 0 || zd != 0 || allowBedrock) {
					for (int at = world.getHeight() - 1; at > (tallest == null ? 0 : tallest[1]); at--) {
						if (fullBlock(world, x + xd, at, z + zd) && (allowBedrock || world.getBlock(x + xd, at, z + zd) != Blocks.bedrock)) {
							tallest = new int[] {x + xd, at, z + zd};
							break;
						}
					}
				}
			}
		}
		return tallest == null ? new int[] {x, y, z} : tallest;
	}

	/** 26.3's isCollisionShapeFullBlock: a block whose collision box is the whole cube. */
	private static boolean fullBlock(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block.getMaterial() == Material.air) {
			return false;
		}
		AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
		double e = 1.0E-7;
		return box != null && box.minX <= x + e && box.minY <= y + e && box.minZ <= z + e && box.maxX >= x + 1 - e && box.maxY >= y + 1 - e && box.maxZ >= z + 1 - e;
	}

	public boolean isSpawning() {
		return age < SPAWN_TIME;
	}

	public boolean isCoolingDown() {
		return teleportCooldown > 0;
	}

	public float getSpawnPercent(float partialTicks) {
		return MathHelper.clamp_float((age + partialTicks) / SPAWN_TIME, 0.0F, 1.0F);
	}

	public float getCooldownPercent(float partialTicks) {
		return 1.0F - MathHelper.clamp_float((teleportCooldown - partialTicks) / COOLDOWN_TIME, 0.0F, 1.0F);
	}

	/** Where it leads (x y z), or null if it hasn't found it yet. */
	public int[] exitPortal() {
		return exitPortal == null ? null : exitPortal.clone();
	}

	public boolean exactTeleport() {
		return exactTeleport;
	}

	public void setExitPosition(int[] exit, boolean exact) {
		exactTeleport = exact;
		exitPortal = exit == null ? null : exit.clone();
		markDirty();
	}

	/** The cooldown (40 ticks), told to clients as block event 1. */
	public void triggerCooldown() {
		if (!worldObj.isRemote) {
			teleportCooldown = COOLDOWN_TIME;
			worldObj.addBlockEvent(xCoord, yCoord, zCoord, getBlockType(), EVENT_COOLDOWN, 0);
			markDirty();
		}
	}

	@Override
	public boolean receiveClientEvent(int id, int type) {
		if (id == EVENT_COOLDOWN) {
			teleportCooldown = COOLDOWN_TIME;
			return true;
		}
		return super.receiveClientEvent(id, type);
	}

	/** 26.3's `shouldRenderFace`: the side (1.7.10's number, 0 down to 5 east) shows unless its neighbour hides it. */
	public boolean shouldRenderFace(int side) {
		net.minecraftforge.common.util.ForgeDirection direction = net.minecraftforge.common.util.ForgeDirection.getOrientation(side);
		return getBlockType().shouldSideBeRendered(worldObj, xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ, side);
	}

	/** 26.3's `getParticleAmount`: one portal particle a display tick for each side that shows. */
	public int getParticleAmount() {
		int count = 0;
		for (int side = 0; side < 6; side++) {
			if (shouldRenderFace(side)) {
				count++;
			}
		}
		return count;
	}
}
