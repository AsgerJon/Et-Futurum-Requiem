package ganymedes01.etfuturum.world;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.copper.LightningRodBlock;
import ganymedes01.etfuturum.trident.Lightning;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Where a dimension's lightning rods are, saved with it (SPEC-modern-movement.md §58.5). 26.3 finds the rod natural
 * lightning goes to in its point-of-interest store; 1.7.10 has none, so the rod keeps this one up as it's placed and
 * broken, as the lodestone keeps LodestoneRecord.
 */
public class LightningRodRecord extends WorldSavedData {

	private static final String NAME = McFly.DOMAIN + "_lightning_rods";

	private final Set<ChunkCoordinates> positions = new HashSet<>();
	/**
	 * When each struck rod's signal ends, in world time; not saved. 1.7.10 sends random ticks (the rod's weathering) to
	 * the same method as the scheduled one that ends the signal, and this tells them apart.
	 */
	private final Map<ChunkCoordinates, Long> poweredUntil = new HashMap<>();

	/** For MapStorage, which makes it by reflection. */
	public LightningRodRecord(String name) {
		super(name);
	}

	/** The world's record, one per dimension. */
	public static LightningRodRecord get(World world) {
		LightningRodRecord record = (LightningRodRecord) world.perWorldStorage.loadData(LightningRodRecord.class, NAME);
		if (record == null) {
			record = new LightningRodRecord(NAME);
			world.perWorldStorage.setData(NAME, record);
		}
		return record;
	}

	public void add(int x, int y, int z) {
		if (positions.add(new ChunkCoordinates(x, y, z))) {
			markDirty();
		}
	}

	public void remove(int x, int y, int z) {
		ChunkCoordinates pos = new ChunkCoordinates(x, y, z);
		poweredUntil.remove(pos);
		if (positions.remove(pos)) {
			markDirty();
		}
	}

	public void powerUntil(int x, int y, int z, long time) {
		poweredUntil.put(new ChunkCoordinates(x, y, z), time);
	}

	/** Whether the rod's signal has ticks left at this world time; forgets it once it hasn't. */
	public boolean stillPowered(int x, int y, int z, long time) {
		ChunkCoordinates pos = new ChunkCoordinates(x, y, z);
		Long until = poweredUntil.get(pos);
		if (until != null && time < until) {
			return true;
		}
		poweredUntil.remove(pos);
		return false;
	}

	/**
	 * The closest rod within range of x y z (straight-line, block to block, as 26.3's) that is the highest block of its
	 * column; null if none. Rods in chunks that aren't loaded are passed over: 26.3 would load them to look.
	 */
	public ChunkCoordinates closestOnTop(World world, int x, int y, int z, int range) {
		ChunkCoordinates closest = null;
		float closestDistance = (float) range * range;
		for (ChunkCoordinates pos : positions) {
			float distance = pos.getDistanceSquared(x, y, z);
			if (distance <= closestDistance && world.blockExists(pos.posX, pos.posY, pos.posZ)
				&& world.getBlock(pos.posX, pos.posY, pos.posZ) instanceof LightningRodBlock
				&& Lightning.isHighestInColumn(world, pos.posX, pos.posY, pos.posZ)) {
				closest = pos;
				closestDistance = distance;
			}
		}
		return closest;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		positions.clear();
		int[] xyz = tag.getIntArray("Positions");
		for (int i = 0; i + 2 < xyz.length; i += 3) {
			positions.add(new ChunkCoordinates(xyz[i], xyz[i + 1], xyz[i + 2]));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		int[] xyz = new int[positions.size() * 3];
		int i = 0;
		for (ChunkCoordinates pos : positions) {
			xyz[i++] = pos.posX;
			xyz[i++] = pos.posY;
			xyz[i++] = pos.posZ;
		}
		tag.setIntArray("Positions", xyz);
	}
}
