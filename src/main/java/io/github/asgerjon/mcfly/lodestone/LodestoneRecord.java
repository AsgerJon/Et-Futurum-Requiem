package io.github.asgerjon.mcfly.lodestone;

import io.github.asgerjon.mcfly.McFly;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Where a dimension's lodestones are, saved with it (SPEC-modern-movement.md §57.2). 26.3 asks its point-of-interest
 * store whether a compass's lodestone is still there, and that store also knows unloaded chunks; 1.7.10 has none, so
 * the lodestone keeps this one up as it's placed and broken.
 */
public class LodestoneRecord extends WorldSavedData {

	private static final String NAME = McFly.DOMAIN + "_lodestones";

	/** Each x, y, z exactly, whatever the height: a taller world's lodestones too (§57.2). */
	private final Set<ChunkCoordinates> positions = new HashSet<>();

	/** For MapStorage, which makes it by reflection. */
	public LodestoneRecord(String name) {
		super(name);
	}

	/** The world's record, one per dimension. */
	public static LodestoneRecord get(World world) {
		LodestoneRecord record = (LodestoneRecord) world.perWorldStorage.loadData(LodestoneRecord.class, NAME);
		if (record == null) {
			record = new LodestoneRecord(NAME);
			world.perWorldStorage.setData(NAME, record);
		}
		return record;
	}

	public boolean contains(int x, int y, int z) {
		return positions.contains(new ChunkCoordinates(x, y, z));
	}

	public void add(int x, int y, int z) {
		if (positions.add(new ChunkCoordinates(x, y, z))) {
			markDirty();
		}
	}

	public void remove(int x, int y, int z) {
		if (positions.remove(new ChunkCoordinates(x, y, z))) {
			markDirty();
		}
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
