package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.base.McFlyMultiface;
import io.github.asgerjon.mcfly.network.LevelEventMessage;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 26.3's `SculkSpreader` (SPEC-modern-movement.md §90.9, step H3): a catalyst's charge cursors, at most 32 of at most
 * 1000 each, with the level spreader's numbers (growth cost 10, no growth within 4, decay 1 in 10, more decay 1 in 5).
 * Saved as 26.3's `cursors`.
 */
public class SculkSpreader {

	public static final int MAX_CHARGE = 1000;
	private static final int MAX_CURSORS = 32;
	private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

	private final int growthSpawnCost;
	private final int noGrowthRadius;
	private final int chargeDecayRate;
	private final int additionalDecayRate;
	private List<ChargeCursor> cursors = new ArrayList<>();

	public SculkSpreader(int growthSpawnCost, int noGrowthRadius, int chargeDecayRate, int additionalDecayRate) {
		this.growthSpawnCost = growthSpawnCost;
		this.noGrowthRadius = noGrowthRadius;
		this.chargeDecayRate = chargeDecayRate;
		this.additionalDecayRate = additionalDecayRate;
	}

	/** 26.3's `createLevelSpreader`. */
	public static SculkSpreader createLevelSpreader() {
		return new SculkSpreader(10, 4, 10, 5);
	}

	public int growthSpawnCost() {
		return growthSpawnCost;
	}

	public int noGrowthRadius() {
		return noGrowthRadius;
	}

	public int chargeDecayRate() {
		return chargeDecayRate;
	}

	public int additionalDecayRate() {
		return additionalDecayRate;
	}

	public List<ChargeCursor> cursors() {
		return cursors;
	}

	void setCursors(List<ChargeCursor> cursors) {
		this.cursors = cursors;
	}

	/** 26.3's `addCursors`: the charge in cursors of at most 1000 at the place. */
	public void addCursors(int x, int y, int z, int charge) {
		while (charge > 0) {
			int part = Math.min(charge, MAX_CHARGE);
			addCursor(new ChargeCursor(x, y, z, part));
			charge -= part;
		}
	}

	private void addCursor(ChargeCursor cursor) {
		if (cursors.size() < MAX_CURSORS) {
			cursors.add(cursor);
		}
	}

	/**
	 * 26.3's `updateCursors`: each cursor (not too far off) updates; one out of charge posts level event 3006 (the
	 * client's pop); the rest on the same spot merge up to 1000, and each spot with charge posts 3006 with its faces (the
	 * client's charge particles).
	 */
	public void updateCursors(World world, int ox, int oy, int oz, Random random, boolean spreadVeins) {
		if (cursors.isEmpty()) {
			return;
		}
		List<ChargeCursor> processed = new ArrayList<>();
		Map<Long, ChargeCursor> mergeable = new HashMap<>();
		Map<Long, Integer> chargeAt = new LinkedHashMap<>();
		for (ChargeCursor cursor : cursors) {
			if (cursor.isPosUnreasonable(ox, oy, oz)) {
				continue;
			}
			cursor.update(world, ox, oy, oz, random, this, spreadVeins);
			if (cursor.charge <= 0) {
				chargeEvent(world, cursor.x, cursor.y, cursor.z, 0);
				continue;
			}
			long key = key(cursor.x, cursor.y, cursor.z);
			chargeAt.merge(key, cursor.charge, Integer::sum);
			ChargeCursor existing = mergeable.get(key);
			if (existing == null) {
				mergeable.put(key, cursor);
				processed.add(cursor);
			} else if (cursor.charge + existing.charge <= MAX_CHARGE) {
				existing.mergeWith(cursor);
			} else {
				processed.add(cursor);
				if (cursor.charge < existing.charge) {
					mergeable.put(key, cursor);
				}
			}
		}
		for (Map.Entry<Long, Integer> entry : chargeAt.entrySet()) {
			ChargeCursor cursor = mergeable.get(entry.getKey());
			int charge = entry.getValue();
			if (charge > 0 && cursor != null && cursor.facings >= 0) {
				int particles = (int) (Math.log1p(charge) / 2.3F) + 1;
				chargeEvent(world, cursor.x, cursor.y, cursor.z, (particles << 6) + cursor.facings);
			}
		}
		cursors = processed;
	}

	private static void chargeEvent(World world, int x, int y, int z, int data) {
		if (world instanceof WorldServer) {
			LevelEventMessage.sendAround((WorldServer) world, LevelEventMessage.SCULK_CHARGE, x, y, z, data);
		}
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | (long) z & 0x3FFFFFFL;
	}

	public void readFromNBT(NBTTagCompound tag) {
		cursors.clear();
		NBTTagList list = tag.getTagList("cursors", 10);
		for (int i = 0; i < list.tagCount() && i < MAX_CURSORS; i++) {
			addCursor(ChargeCursor.read(list.getCompoundTagAt(i)));
		}
	}

	public void writeToNBT(NBTTagCompound tag) {
		NBTTagList list = new NBTTagList();
		for (ChargeCursor cursor : cursors) {
			list.appendTag(cursor.write());
		}
		tag.setTag("cursors", list);
	}

	/** 26.3's `ChargeCursor`: a place, its charge, its delays, and the faces it last moved over (-1 none known). */
	public static class ChargeCursor {
		int x, y, z;
		int charge;
		int updateDelay;
		int decayDelay = 1;
		int facings = -1;

		public ChargeCursor(int x, int y, int z, int charge) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.charge = charge;
		}

		public int x() {
			return x;
		}

		public int y() {
			return y;
		}

		public int z() {
			return z;
		}

		public int charge() {
			return charge;
		}

		public int decayDelay() {
			return decayDelay;
		}

		private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		/** 26.3's `NON_CORNER_NEIGHBOURS`: the 18 of the 3 x 3 x 3 with a 0 on some axis, but the middle, in its order (x, then y, then z). */
		private static final List<int[]> NON_CORNER_NEIGHBOURS = new ArrayList<>(18);

		static {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if ((dx == 0 || dy == 0 || dz == 0) && !(dx == 0 && dy == 0 && dz == 0)) {
							NON_CORNER_NEIGHBOURS.add(new int[]{dx, dy, dz});
						}
					}
				}
			}
		}

		private boolean isPosUnreasonable(int ox, int oy, int oz) {
			return Math.max(Math.abs(x - ox), Math.max(Math.abs(y - oy), Math.abs(z - oz))) > 1024;
		}

		/** 26.3's `shouldUpdate`: charge left, and its spot loaded (26.3's `shouldTickBlocksAt`). */
		private boolean shouldUpdate(World world, int ox, int oy, int oz) {
			return charge > 0 && world.blockExists(ox, oy, oz);
		}

		/** 26.3's `ChargeCursor.update`. */
		void update(World world, int ox, int oy, int oz, Random random, SculkSpreader spreader, boolean spreadVeins) {
			if (!shouldUpdate(world, ox, oy, oz)) {
				return;
			}
			if (updateDelay > 0) {
				updateDelay--;
				return;
			}
			Block block = world.getBlock(x, y, z);
			SculkBehaviour behaviour = SculkBehaviour.of(block);
			if (spreadVeins && behaviour.attemptSpreadVein(world, x, y, z, block, facings, false)) {
				if (behaviour.canChangeBlockStateOnSpread()) {
					block = world.getBlock(x, y, z);
					behaviour = SculkBehaviour.of(block);
				}
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, io.github.asgerjon.mcfly.McFly.MC_ASSETS + ":block.sculk.spread", 1.0F, 1.0F);
			}
			charge = behaviour.attemptUseCharge(this, world, ox, oy, oz, random, spreader, spreadVeins);
			if (charge <= 0) {
				behaviour.onDischarged(world, x, y, z, random);
				return;
			}
			int[] transfer = validMovementPos(world, x, y, z, random);
			if (transfer != null) {
				behaviour.onDischarged(world, x, y, z, random);
				x = transfer[0];
				y = transfer[1];
				z = transfer[2];
				block = world.getBlock(x, y, z);
			}
			if (block instanceof SculkBehaviour) {
				// 26.3's `MultifaceBlock.availableFaces`: a multiface's faces, else all six
				facings = block instanceof McFlyMultiface ? McFlyMultiface.faces(world, x, y, z) : 63;
			}
			decayDelay = behaviour.updateDecayDelay(decayDelay);
			updateDelay = behaviour.spreadDelay();
		}

		void mergeWith(ChargeCursor other) {
			charge += other.charge;
			other.charge = 0;
			updateDelay = Math.min(updateDelay, other.updateDelay);
		}

		/** 26.3's `getValidMovementPos`: the shuffled non-corner neighbours, the last sculk or vein reached unobstructed, or the first with a vein on a replaceable block. */
		private static int[] validMovementPos(World world, int x, int y, int z, Random random) {
			List<int[]> offsets = new ArrayList<>(NON_CORNER_NEIGHBOURS);
			Collections.shuffle(offsets, random);
			int[] found = null;
			for (int[] offset : offsets) {
				int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
				Block there = world.getBlock(nx, ny, nz);
				if (there instanceof SculkBehaviour && movementUnobstructed(world, x, y, z, nx, ny, nz)) {
					found = new int[]{nx, ny, nz};
					if (SculkVeinBlock.hasSubstrateAccess(world, nx, ny, nz)) {
						break;
					}
				}
			}
			return found;
		}

		/** 26.3's `isMovementUnobstructed`: next to it, or a diagonal with one of the two ways round not sturdy. */
		private static boolean movementUnobstructed(World world, int x, int y, int z, int nx, int ny, int nz) {
			int dx = nx - x, dy = ny - y, dz = nz - z;
			if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
				return true;
			}
			int faceX = dx < 0 ? 4 : 5, faceY = dy < 0 ? 0 : 1, faceZ = dz < 0 ? 2 : 3;
			if (dx == 0) {
				return unobstructed(world, x, y, z, faceY) || unobstructed(world, x, y, z, faceZ);
			}
			if (dy == 0) {
				return unobstructed(world, x, y, z, faceX) || unobstructed(world, x, y, z, faceZ);
			}
			return unobstructed(world, x, y, z, faceX) || unobstructed(world, x, y, z, faceY);
		}

		/** 26.3's `isUnobstructed`: the block that way's facing side not sturdy. */
		private static boolean unobstructed(World world, int x, int y, int z, int face) {
			int tx = x + OFFSET[face][0], ty = y + OFFSET[face][1], tz = z + OFFSET[face][2];
			return !world.getBlock(tx, ty, tz).isSideSolid(world, tx, ty, tz, ForgeDirection.getOrientation(face ^ 1));
		}

		NBTTagCompound write() {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setIntArray("pos", new int[]{x, y, z});
			tag.setInteger("charge", charge);
			tag.setInteger("decay_delay", decayDelay);
			tag.setInteger("update_delay", updateDelay);
			if (facings >= 0) {
				NBTTagList faces = new NBTTagList();
				for (int face = 0; face < 6; face++) {
					if ((facings & 1 << face) != 0) {
						faces.appendTag(new NBTTagString(DIRECTIONS[face]));
					}
				}
				tag.setTag("facings", faces);
			}
			return tag;
		}

		static ChargeCursor read(NBTTagCompound tag) {
			int[] pos = tag.getIntArray("pos");
			ChargeCursor cursor = new ChargeCursor(pos.length > 0 ? pos[0] : 0, pos.length > 1 ? pos[1] : 0, pos.length > 2 ? pos[2] : 0,
				Math.max(0, Math.min(MAX_CHARGE, tag.getInteger("charge"))));
			cursor.decayDelay = tag.hasKey("decay_delay") ? Math.max(0, Math.min(1, tag.getInteger("decay_delay"))) : 1;
			cursor.updateDelay = Math.max(0, tag.getInteger("update_delay"));
			if (tag.hasKey("facings")) {
				NBTTagList faces = tag.getTagList("facings", 8);
				int set = 0;
				for (int i = 0; i < faces.tagCount(); i++) {
					for (int face = 0; face < 6; face++) {
						if (DIRECTIONS[face].equals(faces.getStringTagAt(i))) {
							set |= 1 << face;
						}
					}
				}
				cursor.facings = set;
			}
			return cursor;
		}

		/** The direction names of 26.3's `Direction`, by `ForgeDirection` order. */
		static String direction(int face) {
			return DIRECTIONS[face];
		}

		static ForgeDirection forge(int face) {
			return ForgeDirection.getOrientation(face);
		}
	}
}
