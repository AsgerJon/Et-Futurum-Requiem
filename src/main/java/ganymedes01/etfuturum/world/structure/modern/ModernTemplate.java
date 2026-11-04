package ganymedes01.etfuturum.world.structure.modern;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A 26.3 structure template (`StructureTemplate`; SPEC-modern-movement.md §76.12): its size, palette and blocks as 26.3
 * writes them (`palette` of `id` and `properties`, blocks with an optional block entity), placed as 26.3 places one:
 * positions turned and mirrored about a pivot, states turned with them, clipped to a box, structure blocks (and air,
 * for a piece that doesn't overwrite) left out, block entities after the rest in 26.3's order (by y, x, z). Its data
 * markers (structure blocks in data mode) are handed back for the piece to act on.
 */
public final class ModernTemplate {

	/** One block of the template, at its own position. */
	public static final class BlockInfo {
		public final int x;
		public final int y;
		public final int z;
		public final ModernState state;
		public final NBTTagCompound nbt;

		BlockInfo(int x, int y, int z, ModernState state, NBTTagCompound nbt) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.state = state;
			this.nbt = nbt;
		}
	}

	/** A data marker where the template put it in the world. */
	public static final class Marker {
		public final int x;
		public final int y;
		public final int z;
		public final String metadata;

		Marker(int x, int y, int z, String metadata) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.metadata = metadata;
		}
	}

	/** How a template goes in (26.3's StructurePlaceSettings, the parts structures here use). */
	public static final class Settings {
		public ModernRotation rotation = ModernRotation.NONE;
		public ModernMirror mirror = ModernMirror.NONE;
		public int pivotX;
		public int pivotZ;
		/** 26.3's BlockIgnoreProcessor.STRUCTURE_AND_AIR (else STRUCTURE_BLOCK only). */
		public boolean ignoreAir;
		/** Where blocks may go; null for anywhere. */
		public ModernBox box;

		public Settings rotation(ModernRotation rotation) {
			this.rotation = rotation;
			return this;
		}

		public Settings ignoreAir(boolean ignoreAir) {
			this.ignoreAir = ignoreAir;
			return this;
		}

		public Settings box(ModernBox box) {
			this.box = box;
			return this;
		}
	}

	private static final Map<String, ModernTemplate> CACHE = new HashMap<>();

	public final String name;
	public final int sizeX;
	public final int sizeY;
	public final int sizeZ;
	/** The blocks in 26.3's placing order: those without a block entity, then those with, by y, x, z. */
	public final List<BlockInfo> blocks;

	private ModernTemplate(String name, int sizeX, int sizeY, int sizeZ, List<BlockInfo> blocks) {
		this.name = name;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.blocks = blocks;
	}

	/** A template from the mod's resources ("end_city/ship" is /data/structure/end_city/ship.nbt), read once. */
	public static synchronized ModernTemplate get(String name) {
		return CACHE.computeIfAbsent(name, ModernTemplate::load);
	}

	private static ModernTemplate load(String name) {
		String path = "/data/structure/" + name + ".nbt";
		try (InputStream in = ModernTemplate.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("no template " + path);
			}
			return read(name, CompressedStreamTools.readCompressed(in));
		} catch (IOException e) {
			throw new IllegalStateException("can't read template " + path, e);
		}
	}

	static ModernTemplate read(String name, NBTTagCompound tag) {
		int[] size = ints(tag.getTagList("size", 3));
		NBTTagList paletteTag = tag.hasKey("palette") ? tag.getTagList("palette", 10) : tag.getTagList("palettes", 9).getCompoundTagAt(0) == null ? new NBTTagList() : firstPalette(tag);
		List<ModernState> palette = new ArrayList<>();
		for (int i = 0; i < paletteTag.tagCount(); i++) {
			palette.add(state(paletteTag.getCompoundTagAt(i)));
		}
		NBTTagList blocksTag = tag.getTagList("blocks", 10);
		List<BlockInfo> plain = new ArrayList<>();
		List<BlockInfo> entities = new ArrayList<>();
		for (int i = 0; i < blocksTag.tagCount(); i++) {
			NBTTagCompound block = blocksTag.getCompoundTagAt(i);
			int[] pos = ints(block.getTagList("pos", 3));
			ModernState state = palette.get(block.getInteger("state"));
			NBTTagCompound nbt = block.hasKey("nbt", 10) ? block.getCompoundTag("nbt") : null;
			(nbt != null ? entities : plain).add(new BlockInfo(pos[0], pos[1], pos[2], state, nbt));
		}
		entities.sort((a, b) -> a.y != b.y ? Integer.compare(a.y, b.y) : a.x != b.x ? Integer.compare(a.x, b.x) : Integer.compare(a.z, b.z));
		List<BlockInfo> all = new ArrayList<>(plain);
		all.addAll(entities);
		return new ModernTemplate(name, size[0], size[1], size[2], Collections.unmodifiableList(all));
	}

	private static NBTTagList firstPalette(NBTTagCompound tag) {
		NBTTagList palettes = tag.getTagList("palettes", 9);
		return palettes.tagCount() == 0 ? new NBTTagList() : (NBTTagList) palettes.tagList.get(0);
	}

	private static ModernState state(NBTTagCompound tag) {
		// 26.3 writes "id" and "properties"; older templates "Name" and "Properties"
		String id = tag.hasKey("id") ? tag.getString("id") : tag.getString("Name");
		NBTTagCompound props = tag.hasKey("properties") ? tag.getCompoundTag("properties") : tag.getCompoundTag("Properties");
		Map<String, String> properties = new LinkedHashMap<>();
		for (Object key : props.func_150296_c()) {
			properties.put((String) key, props.getString((String) key));
		}
		return new ModernState(id, properties);
	}

	private static int[] ints(NBTTagList list) {
		int[] out = new int[list.tagCount()];
		for (int i = 0; i < out.length; i++) {
			NBTBase element = (NBTBase) list.tagList.get(i);
			out[i] = element instanceof NBTTagInt number ? number.func_150287_d() : 0;
		}
		return out;
	}

	/** 26.3's StructureTemplate.transform: a template position mirrored, then turned about the pivot. */
	public static int[] transform(int x, int y, int z, ModernMirror mirror, ModernRotation rotation, int pivotX, int pivotZ) {
		boolean mirrored = true;
		switch (mirror) {
			case LEFT_RIGHT -> z = -z;
			case FRONT_BACK -> x = -x;
			default -> mirrored = false;
		}
		return switch (rotation) {
			case COUNTERCLOCKWISE_90 -> new int[] {pivotX - pivotZ + z, y, pivotX + pivotZ - x};
			case CLOCKWISE_90 -> new int[] {pivotX + pivotZ - z, y, pivotZ - pivotX + x};
			case CLOCKWISE_180 -> new int[] {pivotX + pivotX - x, y, pivotZ + pivotZ - z};
			default -> new int[] {x, y, z};
		};
	}

	public static int[] transform(int x, int y, int z, Settings settings) {
		return transform(x, y, z, settings.mirror, settings.rotation, settings.pivotX, settings.pivotZ);
	}

	/** 26.3's getBoundingBox: the template's corners turned, about the place. */
	public ModernBox boundingBox(Settings settings, int x, int y, int z) {
		int[] a = transform(0, 0, 0, settings);
		int[] b = transform(sizeX - 1, sizeY - 1, sizeZ - 1, settings);
		return ModernBox.fromCorners(a[0], a[1], a[2], b[0], b[1], b[2]).move(x, y, z);
	}

	/** 26.3's calculateConnectedPosition: where a child's origin goes, from this one's offset (the child's pivot 0). */
	public static int[] connectedPosition(Settings parent, int offsetX, int offsetY, int offsetZ, Settings child) {
		int[] a = transform(offsetX, offsetY, offsetZ, parent);
		int[] b = transform(0, 0, 0, child);
		return new int[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
	}

	/**
	 * 26.3's placeInWorld: the blocks at (x, y, z) (flag 2, no neighbour updates), a container's loot seed drawn from
	 * the random as 26.3 draws it; the data markers, wherever they fall.
	 */
	public List<Marker> place(World world, int x, int y, int z, Settings settings, Random random) {
		for (BlockInfo info : blocks) {
			if (info.state.id.equals("minecraft:structure_block") || settings.ignoreAir && info.state.isAir()) {
				continue;
			}
			int[] at = transform(info.x, info.y, info.z, settings);
			int bx = at[0] + x;
			int by = at[1] + y;
			int bz = at[2] + z;
			if (settings.box != null && !settings.box.isInside(bx, by, bz)) {
				continue;
			}
			ModernBlockStates.place(world, bx, by, bz, info.state.mirror(settings.mirror).rotate(settings.rotation), info.nbt, random);
		}
		List<Marker> markers = new ArrayList<>();
		for (BlockInfo info : blocks) {
			if (info.state.id.equals("minecraft:structure_block") && info.nbt != null && "DATA".equalsIgnoreCase(info.nbt.getString("mode"))) {
				int[] at = transform(info.x, info.y, info.z, settings);
				markers.add(new Marker(at[0] + x, at[1] + y, at[2] + z, info.nbt.getString("metadata")));
			}
		}
		return markers;
	}
}
