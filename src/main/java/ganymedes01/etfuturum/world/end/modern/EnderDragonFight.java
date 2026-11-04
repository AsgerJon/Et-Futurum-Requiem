package ganymedes01.etfuturum.world.end.modern;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import ganymedes01.etfuturum.EtFuturum;
import io.github.asgerjon.mcfly.network.EndBossBarMessage;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The dragon fight as 26.3's (SPEC-modern-movement.md §76.6, `EnderDragonFight`), saved with the End: the first scan
 * (the exit portal made inactive, an old world's state read), the dragon found or made, its crystals counted, its bar
 * shown to players within 192 of (0, 128, 0), its death (the portal activated, the egg the first time).
 */
public class EnderDragonFight extends WorldSavedData {

	public static final String NAME = "etfuturum_ender_dragon_fight";
	private static final int DRAGON_Y = 128;

	private WorldServer world;
	private boolean needsStateScanning = true;
	private boolean dragonKilled;
	private boolean previouslyKilled;
	private UUID dragonUUID;
	private ChunkCoordinates exitPortal;
	/** The gateways still to open, in the order they open (from the back), shuffled by the seed (stage 5 opens them). */
	private final List<Integer> gateways = new ArrayList<>();
	/** The respawn's stage and time, and its four crystals (stage 4). */
	String respawnStage;
	int respawnTime;
	final List<UUID> respawnCrystals = new ArrayList<>();

	private int ticksSinceDragonSeen;
	private int aliveCrystals;
	private int ticksSinceCrystalsScanned;
	private int ticksSinceLastPlayerScan = 21;
	private final Set<EntityPlayerMP> barPlayers = new HashSet<>();
	private float lastProgress = -1.0F;
	private boolean lastVisible;

	public EnderDragonFight(String name) {
		super(name);
	}

	/** The End's fight (made and saved with it). */
	public static EnderDragonFight of(World world) {
		EnderDragonFight fight = (EnderDragonFight) world.perWorldStorage.loadData(EnderDragonFight.class, NAME);
		if (fight == null) {
			fight = new EnderDragonFight(NAME);
			world.perWorldStorage.setData(NAME, fight);
		}
		fight.init((WorldServer) world);
		return fight;
	}

	/** The fight where the world is 26.3's End, else null. */
	public static EnderDragonFight ifThere(World world) {
		return !world.isRemote && world.provider instanceof WorldProviderModernEnd ? of(world) : null;
	}

	private void init(WorldServer world) {
		if (this.world == world) {
			return;
		}
		this.world = world;
		if (gateways.isEmpty()) {
			for (int i = 0; i < 20; i++) {
				gateways.add(i);
			}
			Collections.shuffle(gateways, new Random(world.getSeed()));
			markDirty();
		}
	}

	// ---------------------------------------------------------------- the tick (the end of the End's world tick)

	public void tick() {
		boolean visible = !dragonKilled;
		if (++ticksSinceLastPlayerScan >= 20) {
			updatePlayers();
			ticksSinceLastPlayerScan = 0;
		}
		if (barPlayers.isEmpty()) {
			return;
		}
		if (!arenaLoaded()) {
			return;
		}
		if (needsStateScanning) {
			scanState();
			needsStateScanning = false;
			markDirty();
		}
		if (respawnStage != null) {
			DragonRespawn.tick(world, this);
		}
		if (!dragonKilled) {
			if (dragonUUID == null || ++ticksSinceDragonSeen >= 1200) {
				findOrCreateDragon();
				ticksSinceDragonSeen = 0;
			}
			if (++ticksSinceCrystalsScanned >= 100) {
				updateCrystalCount();
				ticksSinceCrystalsScanned = 0;
			}
			EntityDragon dragon = dragon();
			if (dragon != null) {
				updateDragon(dragon);
			}
		}
		if (visible != lastVisible) {
			lastVisible = visible;
			sendBar();
		}
	}

	/**
	 * 26.3 keeps the arena loaded by a ticket while players are there (not ported: spec §76.6) and checks that it's
	 * ticking; before the first scan the chunks it reads are loaded.
	 */
	private boolean arenaLoaded() {
		if (needsStateScanning) {
			for (int cx = -8; cx <= 8; cx++) {
				for (int cz = -8; cz <= 8; cz++) {
					world.theChunkProviderServer.loadChunk(cx, cz);
				}
			}
		}
		return world.theChunkProviderServer.chunkExists(0, 0);
	}

	private void updatePlayers() {
		Set<EntityPlayerMP> inRange = new HashSet<>();
		for (Object entity : world.playerEntities) {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			if (player.isEntityAlive() && player.getDistanceSq(0.0, DRAGON_Y, 0.0) <= 192.0 * 192.0) {
				inRange.add(player);
			}
		}
		for (EntityPlayerMP player : new ArrayList<>(barPlayers)) {
			if (!inRange.contains(player)) {
				barPlayers.remove(player);
				McFlyNetwork.CHANNEL.sendTo(new EndBossBarMessage(false, 0.0F), player);
			}
		}
		for (EntityPlayerMP player : inRange) {
			if (barPlayers.add(player)) {
				McFlyNetwork.CHANNEL.sendTo(new EndBossBarMessage(!dragonKilled && lastProgress >= 0.0F, Math.max(0.0F, lastProgress)), player);
			}
		}
	}

	private void sendBar() {
		for (EntityPlayerMP player : barPlayers) {
			McFlyNetwork.CHANNEL.sendTo(new EndBossBarMessage(!dragonKilled && lastProgress >= 0.0F, Math.max(0.0F, lastProgress)), player);
		}
	}

	/** 26.3's updateDragon (the dragon's tick calls it there): the bar's health. */
	public void updateDragon(EntityDragon dragon) {
		if (dragon.getUniqueID().equals(dragonUUID)) {
			ticksSinceDragonSeen = 0;
			float progress = dragon.getHealth() / dragon.getMaxHealth();
			if (progress != lastProgress) {
				lastProgress = progress;
				sendBar();
			}
		}
	}

	// ---------------------------------------------------------------- the state

	private void scanState() {
		boolean activePortal = hasActiveExitPortal();
		if (activePortal) {
			previouslyKilled = true;
		} else {
			previouslyKilled = false;
			if (findExitPortal() == null) {
				spawnExitPortal(false);
			}
		}
		List<EntityDragon> dragons = dragons();
		if (dragons.isEmpty()) {
			dragonKilled = true;
		} else {
			EntityDragon dragon = dragons.get(0);
			dragonUUID = dragon.getUniqueID();
			dragonKilled = false;
			if (!activePortal) {
				// an old world's dragon with no portal made for it: 26.3 removes it and makes its own
				dragon.setDead();
				dragonUUID = null;
			}
		}
		if (!previouslyKilled && dragonKilled) {
			dragonKilled = false;
		}
		markDirty();
	}

	private boolean hasActiveExitPortal() {
		for (int cx = -8; cx <= 8; cx++) {
			for (int cz = -8; cz <= 8; cz++) {
				Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
				for (Object tile : chunk.chunkTileEntityMap.values()) {
					if (tile instanceof TileEntityEndPortal) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** 26.3's exit portal pattern (its bedrock rim and pillar) found at the top of (0, 0): its place, else null. */
	ChunkCoordinates findExitPortal() {
		if (exitPortal != null) {
			return exitPortal;
		}
		ChunkCoordinates found = scanForPodium();
		if (found != null) {
			exitPortal = found;
			markDirty();
		}
		return found;
	}

	/** 26.3's pattern search: a podium's middle among the portal blocks within 8 chunks, else down the column at (0, 0). */
	private ChunkCoordinates scanForPodium() {
		for (int cx = -8; cx <= 8; cx++) {
			for (int cz = -8; cz <= 8; cz++) {
				for (Object tile : world.getChunkFromChunkCoords(cx, cz).chunkTileEntityMap.values()) {
					if (tile instanceof TileEntityEndPortal portal) {
						ChunkCoordinates found = matchPodium(portal.xCoord, portal.yCoord, portal.zCoord);
						if (found != null) {
							return found;
						}
					}
				}
			}
		}
		for (int y = world.getHeightValue(0, 0); y >= 1; y--) {
			if (isPodiumAt(0, y, 0)) {
				return new ChunkCoordinates(0, y, 0);
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- the respawn (spec §76.9)

	/** A crystal placed: with the dragon dead and none running, a crystal in each of the four starts the respawn. */
	public void tryRespawn() {
		if (!dragonKilled || respawnStage != null) {
			return;
		}
		ChunkCoordinates location = exitPortal;
		if (location == null) {
			if (findExitPortal() == null) {
				spawnExitPortal(true);
			}
			location = exitPortal;
		}
		List<EntityEnderCrystal> crystals = new ArrayList<>();
		int[][] sides = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
		for (int[] side : sides) {
			int x = location.posX + side[0] * 3;
			int y = location.posY + 1;
			int z = location.posZ + side[1] * 3;
			@SuppressWarnings("unchecked")
			List<EntityEnderCrystal> found = world.getEntitiesWithinAABB(EntityEnderCrystal.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1));
			if (found.isEmpty()) {
				return;
			}
			crystals.addAll(found);
		}
		// every podium found: its bedrock and portal blocks to end stone, then it's made again, inactive
		for (ChunkCoordinates podium = scanForPodium(); podium != null; podium = scanForPodium()) {
			for (int x = podium.posX - 3; x <= podium.posX + 3; x++) {
				for (int y = podium.posY - 1; y <= podium.posY + 3; y++) {
					for (int z = podium.posZ - 3; z <= podium.posZ + 3; z++) {
						if (world.getBlock(x, y, z) == Blocks.bedrock || world.getBlock(x, y, z) == Blocks.end_portal) {
							world.setBlock(x, y, z, Blocks.end_stone);
						}
					}
				}
			}
		}
		DragonRespawn.begin(world, this, crystals);
		spawnExitPortal(false);
	}

	/** The respawn's stage, or null (tests and the views). */
	public String respawnStage() {
		return respawnStage;
	}

	/** A portal block inside 26.3's rim: the podium's middle near it, if the bedrock pattern is there. */
	private ChunkCoordinates matchPodium(int x, int y, int z) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (isPodiumAt(x + dx, y, z + dz)) {
					return new ChunkCoordinates(x + dx, y, z + dz);
				}
			}
		}
		return null;
	}

	/** The podium's bedrock at (x, y, z): its pillar 3 up, the rim 3 out on each side and the floor under the middle. */
	private boolean isPodiumAt(int x, int y, int z) {
		for (int up = 0; up < 3; up++) {
			if (world.getBlock(x, y + up, z) != Blocks.bedrock) {
				return false;
			}
		}
		return world.getBlock(x + 3, y, z) == Blocks.bedrock && world.getBlock(x - 3, y, z) == Blocks.bedrock
			&& world.getBlock(x, y, z + 3) == Blocks.bedrock && world.getBlock(x, y, z - 3) == Blocks.bedrock
			&& world.getBlock(x + 1, y - 1, z) == Blocks.bedrock;
	}

	/** At the top block of (0, 0), down through bedrock while above y 63 (26.3's spawnExitPortal). */
	void spawnExitPortal(boolean active) {
		if (exitPortal == null) {
			int y = world.getHeightValue(0, 0) - 1;
			while (world.getBlock(0, y, 0) == Blocks.bedrock && y > 63) {
				y--;
			}
			exitPortal = new ChunkCoordinates(0, Math.max(1, y), 0);
			markDirty();
		}
		ModernEndPodium.place(world, exitPortal.posX, exitPortal.posY, exitPortal.posZ, active);
	}

	private List<EntityDragon> dragons() {
		List<EntityDragon> found = new ArrayList<>();
		for (Object entity : world.loadedEntityList) {
			if (entity instanceof EntityDragon dragon && dragon.isEntityAlive()) {
				found.add(dragon);
			}
		}
		return found;
	}

	private EntityDragon dragon() {
		if (dragonUUID == null) {
			return null;
		}
		for (EntityDragon dragon : dragons()) {
			if (dragon.getUniqueID().equals(dragonUUID)) {
				return dragon;
			}
		}
		return null;
	}

	/** 26.3's findOrCreateDragon finding a dragon that's there: the fight's dragon from now on (tests use it too). */
	public void useDragon(EntityDragon dragon) {
		dragonUUID = dragon.getUniqueID();
		dragonKilled = false;
		markDirty();
	}

	private void findOrCreateDragon() {
		List<EntityDragon> dragons = dragons();
		if (dragons.isEmpty()) {
			createNewDragon();
		} else {
			dragonUUID = dragons.get(0).getUniqueID();
			markDirty();
		}
	}

	EntityDragon createNewDragon() {
		world.getChunkFromBlockCoords(0, 0);
		EntityDragon dragon = new EntityDragon(world);
		dragon.setLocationAndAngles(0.0, DRAGON_Y, 0.0, world.rand.nextFloat() * 360.0F, 0.0F);
		if (ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon.of(dragon) != null) {
			ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon.of(dragon).setPhase(ganymedes01.etfuturum.world.end.modern.dragon.DragonPhase.HOLDING_PATTERN);
		}
		world.spawnEntityInWorld(dragon);
		dragonUUID = dragon.getUniqueID();
		lastProgress = 1.0F;
		sendBar();
		markDirty();
		return dragon;
	}

	private void updateCrystalCount() {
		aliveCrystals = 0;
		for (EndSpikes.Spike spike : EndSpikes.forSeed(world.getSeed())) {
			aliveCrystals += world.getEntitiesWithinAABB(EntityEnderCrystal.class, spike.column()).size();
		}
	}

	public int aliveCrystals() {
		return aliveCrystals;
	}

	public boolean hasPreviouslyKilledDragon() {
		return previouslyKilled;
	}

	public boolean dragonKilled() {
		return dragonKilled;
	}

	/** The dragon died (26.3's setDragonKilled): the bar hidden, the portal activated, a gateway, the egg the first time. */
	public void setDragonKilled(EntityDragon dragon) {
		if (dragonUUID == null || dragon.getUniqueID().equals(dragonUUID)) {
			lastProgress = 0.0F;
			dragonKilled = true;
			sendBar();
			spawnExitPortal(true);
			EndGateways.spawnNext(world, this);
			if (!previouslyKilled) {
				world.setBlock(0, world.getHeightValue(0, 0), 0, Blocks.dragon_egg);
			}
			previouslyKilled = true;
			markDirty();
		}
	}

	/** How many of the 20 gateways are still to open. */
	public int gatewaysLeft() {
		return gateways.size();
	}

	/** The next of the 20 gateways to open (from the back of the seed's order), or -1 when all are open. */
	int takeGateway() {
		if (gateways.isEmpty()) {
			return -1;
		}
		markDirty();
		return gateways.remove(gateways.size() - 1);
	}

	public void onCrystalDestroyed(EntityEnderCrystal crystal, DamageSource source) {
		if (respawnStage != null && respawnCrystals.contains(crystal.getUniqueID())) {
			DragonRespawn.abort(world, this);
		} else {
			updateCrystalCount();
			EntityDragon dragon = dragon();
			if (dragon != null && ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon.of(dragon) != null) {
				ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon.of(dragon).onCrystalDestroyed(crystal, source);
			}
		}
	}

	// ---------------------------------------------------------------- saved

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		needsStateScanning = !tag.hasKey("needs_state_scanning") || tag.getBoolean("needs_state_scanning");
		dragonKilled = tag.getBoolean("dragon_killed");
		previouslyKilled = tag.getBoolean("previously_killed");
		respawnStage = tag.hasKey("respawn_stage") ? tag.getString("respawn_stage") : null;
		respawnTime = tag.getInteger("respawn_time");
		dragonUUID = tag.hasKey("dragon_uuid") ? UUID.fromString(tag.getString("dragon_uuid")) : null;
		int[] portal = tag.getIntArray("exit_portal_location");
		exitPortal = portal.length == 3 ? new ChunkCoordinates(portal[0], portal[1], portal[2]) : null;
		gateways.clear();
		for (int gateway : tag.getIntArray("gateways")) {
			gateways.add(gateway);
		}
		respawnCrystals.clear();
		NBTTagList crystals = tag.getTagList("respawn_crystals", 8);
		for (int i = 0; i < crystals.tagCount(); i++) {
			respawnCrystals.add(UUID.fromString(crystals.getStringTagAt(i)));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		tag.setBoolean("needs_state_scanning", needsStateScanning);
		tag.setBoolean("dragon_killed", dragonKilled);
		tag.setBoolean("previously_killed", previouslyKilled);
		if (respawnStage != null) {
			tag.setString("respawn_stage", respawnStage);
		}
		tag.setInteger("respawn_time", respawnTime);
		if (dragonUUID != null) {
			tag.setString("dragon_uuid", dragonUUID.toString());
		}
		if (exitPortal != null) {
			tag.setTag("exit_portal_location", new NBTTagIntArray(new int[] {exitPortal.posX, exitPortal.posY, exitPortal.posZ}));
		}
		int[] open = new int[gateways.size()];
		for (int i = 0; i < open.length; i++) {
			open[i] = gateways.get(i);
		}
		tag.setIntArray("gateways", open);
		NBTTagList crystals = new NBTTagList();
		for (UUID crystal : respawnCrystals) {
			crystals.appendTag(new NBTTagString(crystal.toString()));
		}
		tag.setTag("respawn_crystals", crystals);
	}

	/** For the respawn (stage 4) and tests: the dragon's id. */
	public UUID dragonUUID() {
		return dragonUUID;
	}

	ChunkCoordinates exitPortal() {
		return exitPortal;
	}

	void setDragonKilledFlag(boolean killed) {
		dragonKilled = killed;
		markDirty();
	}

	WorldServer world() {
		return world;
	}
}
