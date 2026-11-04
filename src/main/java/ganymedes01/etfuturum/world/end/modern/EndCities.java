package ganymedes01.etfuturum.world.end.modern;

import ganymedes01.etfuturum.world.structure.modern.ModernBox;
import ganymedes01.etfuturum.world.structure.modern.ModernRotation;
import ganymedes01.etfuturum.world.structure.modern.ModernTemplate;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The End's cities as 26.3's (`EndCityStructure`, `EndCityPieces`, the `end_cities` set; SPEC-modern-movement.md
 * §76.12, stage 7b): where they start, how their pieces grow from the start's random, and placing them a chunk at a
 * time (each piece clipped to the chunk). The pieces are a function of the seed and the start chunk, so they're made
 * again when needed (kept in a small cache) rather than saved.
 */
public final class EndCities {

	/** The `end_cities` set's random spread. */
	public static final int SPACING = 20;
	public static final int SEPARATION = 11;
	public static final int SALT = 10387313;
	/** The city's index among 26.3's step-4 (surface structures) structures, which seeds its placing random. */
	public static final int STEP_INDEX = 20;
	/** 26.3 looks 8 chunks around a chunk for starts reaching into it. */
	private static final int REACH = 8;

	private EndCities() {
	}

	/** A piece: a template at a place, turned, overwriting or not, and its group tag (26.3's genDepth). */
	public static final class Piece {
		public final String name;
		public final ModernTemplate template;
		public final ModernTemplate.Settings settings;
		public final boolean overwrite;
		public int x;
		public int y;
		public int z;
		public ModernBox box;
		int genDepth;

		Piece(String name, int x, int y, int z, ModernRotation rotation, boolean overwrite) {
			this.name = name;
			this.template = ModernTemplate.get("end_city/" + name);
			this.settings = new ModernTemplate.Settings().rotation(rotation).ignoreAir(!overwrite);
			this.overwrite = overwrite;
			this.x = x;
			this.y = y;
			this.z = z;
			this.box = template.boundingBox(settings, x, y, z);
		}

		void move(int dx, int dy, int dz) {
			x += dx;
			y += dy;
			z += dz;
			box = box.move(dx, dy, dz);
		}

		public ModernRotation rotation() {
			return settings.rotation;
		}
	}

	/** A city: its start chunk, its pieces (the first the base) and the box around them all. */
	public static final class Start {
		public final int chunkX;
		public final int chunkZ;
		public final List<Piece> pieces;
		public final ModernBox box;

		Start(int chunkX, int chunkZ, List<Piece> pieces) {
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			this.pieces = Collections.unmodifiableList(pieces);
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
			for (Piece piece : pieces) {
				minX = Math.min(minX, piece.box.minX);
				minY = Math.min(minY, piece.box.minY);
				minZ = Math.min(minZ, piece.box.minZ);
				maxX = Math.max(maxX, piece.box.maxX);
				maxY = Math.max(maxY, piece.box.maxY);
				maxZ = Math.max(maxZ, piece.box.maxZ);
			}
			this.box = new ModernBox(minX, minY, minZ, maxX, maxY, maxZ);
		}
	}

	/** Starts by seed and chunk (null kept too: most chunks aren't one). */
	private static final Map<String, Start> CACHE = Collections.synchronizedMap(new LinkedHashMap<String, Start>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Start> eldest) {
			return size() > 256;
		}
	});

	/** 26.3's RandomSpreadStructurePlacement.getPotentialStructureChunk: the one chunk of a 20 by 20 region a city may start in. */
	public static int[] potentialChunk(long seed, int chunkX, int chunkZ) {
		int regionX = Math.floorDiv(chunkX, SPACING);
		int regionZ = Math.floorDiv(chunkZ, SPACING);
		Random random = new Random(regionX * 341873128712L + regionZ * 132897987541L + seed + SALT);
		int limit = SPACING - SEPARATION;
		int spreadX = (random.nextInt(limit) + random.nextInt(limit)) / 2;
		int spreadZ = (random.nextInt(limit) + random.nextInt(limit)) / 2;
		return new int[] {regionX * SPACING + spreadX, regionZ * SPACING + spreadZ};
	}

	/** 26.3's WorldgenRandom.setLargeFeatureSeed on a legacy random (java.util.Random's own). */
	private static Random largeFeatureRandom(long seed, int chunkX, int chunkZ) {
		Random random = new Random(seed);
		long xScale = random.nextLong();
		long zScale = random.nextLong();
		random.setSeed(chunkX * xScale ^ chunkZ * zScale ^ seed);
		return random;
	}

	private static boolean cityBiome(BiomeGenBase biome) {
		return biome == ModernEndBiomes.highlands || biome == ModernEndBiomes.midlands;
	}

	/** The city starting in the chunk, or null. */
	public static Start startAt(long seed, ModernEndTerrain terrain, int chunkX, int chunkZ) {
		int[] potential = potentialChunk(seed, chunkX, chunkZ);
		if (potential[0] != chunkX || potential[1] != chunkZ) {
			return null;
		}
		String key = seed + " " + chunkX + " " + chunkZ;
		synchronized (CACHE) {
			if (CACHE.containsKey(key)) {
				return CACHE.get(key);
			}
		}
		Start start = makeStart(seed, terrain, chunkX, chunkZ);
		CACHE.put(key, start);
		return start;
	}

	/** 26.3's EndCityStructure.findGenerationPoint and generatePieces. */
	private static Start makeStart(long seed, ModernEndTerrain terrain, int chunkX, int chunkZ) {
		Random random = largeFeatureRandom(seed, chunkX, chunkZ);
		int blockX = chunkX * 16 + 7;
		int blockZ = chunkZ * 16 + 7;
		if (!cityBiome(ModernEndBiomes.at(terrain, blockX, blockZ))) {
			return null;
		}
		ModernRotation rotation = ModernRotation.getRandom(random);
		int offsetX = 5;
		int offsetZ = 5;
		if (rotation == ModernRotation.CLOCKWISE_90) {
			offsetX = -5;
		} else if (rotation == ModernRotation.CLOCKWISE_180) {
			offsetX = -5;
			offsetZ = -5;
		} else if (rotation == ModernRotation.COUNTERCLOCKWISE_90) {
			offsetZ = -5;
		}
		int y = Math.min(Math.min(firstOccupiedHeight(terrain, blockX, blockZ), firstOccupiedHeight(terrain, blockX, blockZ + offsetZ)),
			Math.min(firstOccupiedHeight(terrain, blockX + offsetX, blockZ), firstOccupiedHeight(terrain, blockX + offsetX, blockZ + offsetZ)));
		if (y < 60) {
			return null;
		}
		List<Piece> pieces = new ArrayList<>();
		new Generator().startHouseTower(blockX, y, blockZ, rotation, pieces, random);
		return new Start(chunkX, chunkZ, pieces);
	}

	/** 26.3's getFirstOccupiedHeight (WORLD_SURFACE_WG) on the terrain: the column's top block, or -1. */
	static int firstOccupiedHeight(ModernEndTerrain terrain, int x, int z) {
		boolean[] solid = terrain.fillChunk(x >> 4, z >> 4);
		for (int y = ModernEndTerrain.HEIGHT - 1; y >= 0; y--) {
			if (solid[ModernEndTerrain.index(x & 15, y, z & 15)]) {
				return y;
			}
		}
		return -1;
	}

	/**
	 * 26.3's StructureStart.placeInChunk for the cities reaching into chunk (cx, cz): every piece meeting the chunk,
	 * clipped to it, then its data markers. Off when the world has no map features (26.3's generateStructures).
	 */
	static void placeInChunk(World world, ModernEndTerrain terrain, int cx, int cz, Random random) {
		if (!world.getWorldInfo().isMapFeaturesEnabled()) {
			return;
		}
		ModernBox chunkBox = ModernBox.chunk(cx, cz, world.getHeight());
		long seed = world.getSeed();
		for (int regionX = Math.floorDiv(cx - REACH, SPACING); regionX <= Math.floorDiv(cx + REACH, SPACING); regionX++) {
			for (int regionZ = Math.floorDiv(cz - REACH, SPACING); regionZ <= Math.floorDiv(cz + REACH, SPACING); regionZ++) {
				int[] chunk = potentialChunk(seed, regionX * SPACING, regionZ * SPACING);
				if (Math.abs(chunk[0] - cx) > REACH || Math.abs(chunk[1] - cz) > REACH) {
					continue;
				}
				Start start = startAt(seed, terrain, chunk[0], chunk[1]);
				if (start == null || !start.box.intersects(chunkBox)) {
					continue;
				}
				for (Piece piece : start.pieces) {
					if (piece.box.intersects(chunkBox)) {
						ModernTemplate.Settings settings = new ModernTemplate.Settings().rotation(piece.rotation()).ignoreAir(!piece.overwrite).box(chunkBox);
						List<ModernTemplate.Marker> markers = piece.template.place(world, piece.x, piece.y, piece.z, settings, random);
						for (ModernTemplate.Marker marker : markers) {
							EndCityMarkers.handle(world, piece, marker, chunkBox, random);
						}
					}
				}
			}
		}
	}

	/** 26.3's EndCityPieces: the four section generators and their recursion. */
	private static final class Generator {

		private static final Object[][] TOWER_BRIDGES = {
			{ModernRotation.NONE, new int[] {1, -1, 0}},
			{ModernRotation.CLOCKWISE_90, new int[] {6, -1, 1}},
			{ModernRotation.COUNTERCLOCKWISE_90, new int[] {0, -1, 5}},
			{ModernRotation.CLOCKWISE_180, new int[] {5, -1, 6}}};
		private static final Object[][] FAT_TOWER_BRIDGES = {
			{ModernRotation.NONE, new int[] {4, -1, 0}},
			{ModernRotation.CLOCKWISE_90, new int[] {12, -1, 4}},
			{ModernRotation.COUNTERCLOCKWISE_90, new int[] {0, -1, 8}},
			{ModernRotation.CLOCKWISE_180, new int[] {8, -1, 12}}};

		private static final int HOUSE_TOWER = 0;
		private static final int TOWER = 1;
		private static final int TOWER_BRIDGE = 2;
		private static final int FAT_TOWER = 3;

		/** TOWER_BRIDGE_GENERATOR's one ship a city. */
		private boolean shipCreated;

		void startHouseTower(int x, int y, int z, ModernRotation rotation, List<Piece> pieces, Random random) {
			shipCreated = false;
			Piece last = add(pieces, new Piece("base_floor", x, y, z, rotation, true));
			last = add(pieces, addPiece(last, -1, 0, -1, "second_floor_1", rotation, false));
			last = add(pieces, addPiece(last, -1, 4, -1, "third_floor_1", rotation, false));
			last = add(pieces, addPiece(last, -1, 8, -1, "third_roof", rotation, true));
			recursiveChildren(TOWER, 1, last, null, pieces, random);
		}

		private static Piece add(List<Piece> pieces, Piece piece) {
			pieces.add(piece);
			return piece;
		}

		private static Piece addPiece(Piece parent, int ox, int oy, int oz, String name, ModernRotation rotation, boolean overwrite) {
			Piece child = new Piece(name, parent.x, parent.y, parent.z, rotation, overwrite);
			int[] origin = ModernTemplate.connectedPosition(parent.settings, ox, oy, oz, child.settings);
			child.move(origin[0], origin[1], origin[2]);
			return child;
		}

		private static Piece findCollisionPiece(List<Piece> pieces, ModernBox box) {
			for (Piece piece : pieces) {
				if (piece.box.intersects(box)) {
					return piece;
				}
			}
			return null;
		}

		private boolean recursiveChildren(int generator, int genDepth, Piece parent, int[] offset, List<Piece> pieces, Random random) {
			if (genDepth > 8) {
				return false;
			}
			List<Piece> childPieces = new ArrayList<>();
			if (generate(generator, genDepth, parent, offset, childPieces, random)) {
				boolean collision = false;
				int childTag = random.nextInt();
				for (Piece child : childPieces) {
					child.genDepth = childTag;
					Piece collisionPiece = findCollisionPiece(pieces, child.box);
					if (collisionPiece != null && collisionPiece.genDepth != parent.genDepth) {
						collision = true;
						break;
					}
				}
				if (!collision) {
					pieces.addAll(childPieces);
					return true;
				}
			}
			return false;
		}

		private boolean generate(int generator, int genDepth, Piece parent, int[] offset, List<Piece> pieces, Random random) {
			return switch (generator) {
				case HOUSE_TOWER -> houseTower(genDepth, parent, offset, pieces, random);
				case TOWER -> tower(genDepth, parent, pieces, random);
				case TOWER_BRIDGE -> towerBridge(genDepth, parent, pieces, random);
				default -> fatTower(genDepth, parent, pieces, random);
			};
		}

		private boolean houseTower(int genDepth, Piece parent, int[] offset, List<Piece> pieces, Random random) {
			if (genDepth > 8) {
				return false;
			}
			ModernRotation rotation = parent.rotation();
			Piece last = add(pieces, addPiece(parent, offset[0], offset[1], offset[2], "base_floor", rotation, true));
			int numFloors = random.nextInt(3);
			if (numFloors == 0) {
				last = add(pieces, addPiece(last, -1, 4, -1, "base_roof", rotation, true));
			} else if (numFloors == 1) {
				last = add(pieces, addPiece(last, -1, 0, -1, "second_floor_2", rotation, false));
				last = add(pieces, addPiece(last, -1, 8, -1, "second_roof", rotation, false));
				recursiveChildren(TOWER, genDepth + 1, last, null, pieces, random);
			} else if (numFloors == 2) {
				last = add(pieces, addPiece(last, -1, 0, -1, "second_floor_2", rotation, false));
				last = add(pieces, addPiece(last, -1, 4, -1, "third_floor_2", rotation, false));
				last = add(pieces, addPiece(last, -1, 8, -1, "third_roof", rotation, true));
				recursiveChildren(TOWER, genDepth + 1, last, null, pieces, random);
			}
			return true;
		}

		private boolean tower(int genDepth, Piece parent, List<Piece> pieces, Random random) {
			ModernRotation rotation = parent.rotation();
			Piece last = parent;
			int baseX = 3 + random.nextInt(2);
			int baseZ = 3 + random.nextInt(2);
			last = add(pieces, addPiece(last, baseX, -3, baseZ, "tower_base", rotation, true));
			last = add(pieces, addPiece(last, 0, 7, 0, "tower_piece", rotation, true));
			Piece bridgePiece = random.nextInt(3) == 0 ? last : null;
			int towerHeight = 1 + random.nextInt(3);
			for (int i = 0; i < towerHeight; i++) {
				last = add(pieces, addPiece(last, 0, 4, 0, "tower_piece", rotation, true));
				if (i < towerHeight - 1 && random.nextBoolean()) {
					bridgePiece = last;
				}
			}
			if (bridgePiece != null) {
				for (Object[] bridge : TOWER_BRIDGES) {
					if (random.nextBoolean()) {
						int[] at = (int[]) bridge[1];
						Piece bridgeStart = add(pieces, addPiece(bridgePiece, at[0], at[1], at[2], "bridge_end", rotation.getRotated((ModernRotation) bridge[0]), true));
						recursiveChildren(TOWER_BRIDGE, genDepth + 1, bridgeStart, null, pieces, random);
					}
				}
				add(pieces, addPiece(last, -1, 4, -1, "tower_top", rotation, true));
			} else {
				if (genDepth != 7) {
					return recursiveChildren(FAT_TOWER, genDepth + 1, last, null, pieces, random);
				}
				add(pieces, addPiece(last, -1, 4, -1, "tower_top", rotation, true));
			}
			return true;
		}

		private boolean towerBridge(int genDepth, Piece parent, List<Piece> pieces, Random random) {
			ModernRotation rotation = parent.rotation();
			int bridgeLength = random.nextInt(4) + 1;
			Piece last = add(pieces, addPiece(parent, 0, 0, -4, "bridge_piece", rotation, true));
			last.genDepth = -1;
			int nextY = 0;
			for (int i = 0; i < bridgeLength; i++) {
				if (random.nextBoolean()) {
					last = add(pieces, addPiece(last, 0, nextY, -4, "bridge_piece", rotation, true));
					nextY = 0;
				} else {
					if (random.nextBoolean()) {
						last = add(pieces, addPiece(last, 0, nextY, -4, "bridge_steep_stairs", rotation, true));
					} else {
						last = add(pieces, addPiece(last, 0, nextY, -8, "bridge_gentle_stairs", rotation, true));
					}
					nextY = 4;
				}
			}
			if (!shipCreated && random.nextInt(10 - genDepth) == 0) {
				int shipX = -8 + random.nextInt(8);
				int shipZ = -70 + random.nextInt(10);
				add(pieces, addPiece(last, shipX, nextY, shipZ, "ship", rotation, true));
				shipCreated = true;
			} else if (!recursiveChildren(HOUSE_TOWER, genDepth + 1, last, new int[] {-3, nextY + 1, -11}, pieces, random)) {
				return false;
			}
			last = add(pieces, addPiece(last, 4, nextY, 0, "bridge_end", rotation.getRotated(ModernRotation.CLOCKWISE_180), true));
			last.genDepth = -1;
			return true;
		}

		private boolean fatTower(int genDepth, Piece parent, List<Piece> pieces, Random random) {
			ModernRotation rotation = parent.rotation();
			Piece last = add(pieces, addPiece(parent, -3, 4, -3, "fat_tower_base", rotation, true));
			last = add(pieces, addPiece(last, 0, 4, 0, "fat_tower_middle", rotation, true));
			for (int i = 0; i < 2 && random.nextInt(3) != 0; i++) {
				last = add(pieces, addPiece(last, 0, 8, 0, "fat_tower_middle", rotation, true));
				for (Object[] bridge : FAT_TOWER_BRIDGES) {
					if (random.nextBoolean()) {
						int[] at = (int[]) bridge[1];
						Piece bridgeStart = add(pieces, addPiece(last, at[0], at[1], at[2], "bridge_end", rotation.getRotated((ModernRotation) bridge[0]), true));
						recursiveChildren(TOWER_BRIDGE, genDepth + 1, bridgeStart, null, pieces, random);
					}
				}
			}
			add(pieces, addPiece(last, -2, 8, -2, "fat_tower_top", rotation, true));
			return true;
		}
	}
}
