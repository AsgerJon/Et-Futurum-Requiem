package ganymedes01.etfuturum.world.structure.modern;

/** 26.3's BoundingBox: inclusive block corners. */
public final class ModernBox {

	public final int minX;
	public final int minY;
	public final int minZ;
	public final int maxX;
	public final int maxY;
	public final int maxZ;

	public ModernBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public static ModernBox fromCorners(int x1, int y1, int z1, int x2, int y2, int z2) {
		return new ModernBox(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
	}

	/** A chunk's whole column (26.3's ChunkGenerator.getWritableArea). */
	public static ModernBox chunk(int cx, int cz, int height) {
		return new ModernBox(cx * 16, 0, cz * 16, cx * 16 + 15, height - 1, cz * 16 + 15);
	}

	public ModernBox move(int dx, int dy, int dz) {
		return new ModernBox(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
	}

	public boolean intersects(ModernBox other) {
		return maxX >= other.minX && minX <= other.maxX && maxZ >= other.minZ && minZ <= other.maxZ && maxY >= other.minY && minY <= other.maxY;
	}

	public boolean isInside(int x, int y, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
	}

	@Override
	public String toString() {
		return minX + " " + minY + " " + minZ + " " + maxX + " " + maxY + " " + maxZ;
	}
}
