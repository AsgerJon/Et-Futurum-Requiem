package io.github.asgerjon.mcfly.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A block model in 26.3's JSON form (SPEC-modern-movement.md §84): its elements (boxes in 0 to 16, each face with its
 * texture, UVs and cull face) and its texture names, a parent's taken where the model has none. Read from our
 * resources (`assets/<domain>/models/block/<name>.json`, copied from the 26.3 jar). Element rotations are (spec §88.5,
 * the chain's); face rotations and display transforms aren't read: the models drawn so far have none (a face rotation fails the load, so one is
 * noticed).
 */
public final class BlockModel {

	private static final Map<String, BlockModel> LOADED = new HashMap<>();

	private final List<Element> elements;
	private final Map<String, String> textures;

	private BlockModel(List<Element> elements, Map<String, String> textures) {
		this.elements = elements;
		this.textures = textures;
	}

	/** The model by its name ("end_rod", "block/end_rod" or "minecraft:block/end_rod"), loaded once. */
	public static synchronized BlockModel get(String name) {
		String key = shortName(name);
		BlockModel model = LOADED.get(key);
		if (model == null) {
			model = load(key);
			LOADED.put(key, model);
		}
		return model;
	}

	private static String shortName(String name) {
		String path = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
		return path.startsWith("block/") ? path.substring("block/".length()) : path;
	}

	private static BlockModel load(String name) {
		String resource = "/assets/" + io.github.asgerjon.mcfly.McFly.DOMAIN + "/models/block/" + name + ".json";
		JsonObject json;
		try (InputStream in = BlockModel.class.getResourceAsStream(resource)) {
			if (in == null) {
				// a parent we don't carry (`block/block`: display transforms only) adds nothing
				return new BlockModel(Collections.<Element>emptyList(), Collections.<String, String>emptyMap());
			}
			json = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("can't read " + resource, e);
		}
		BlockModel parent = json.has("parent") ? get(json.get("parent").getAsString()) : null;
		Map<String, String> textures = new HashMap<>();
		if (parent != null) {
			textures.putAll(parent.textures);
		}
		if (json.has("textures")) {
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
				textures.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		List<Element> elements;
		if (json.has("elements")) {
			elements = new ArrayList<>();
			for (JsonElement element : json.getAsJsonArray("elements")) {
				elements.add(Element.read(element.getAsJsonObject(), resource));
			}
		} else {
			elements = parent != null ? parent.elements : Collections.<Element>emptyList();
		}
		return new BlockModel(Collections.unmodifiableList(elements), Collections.unmodifiableMap(textures));
	}

	public List<Element> elements() {
		return elements;
	}

	/** A face's texture reference ("#texture") followed to its name ("block/chorus_plant"), or null when it's left open. */
	public String texture(String reference) {
		String name = reference;
		for (int depth = 0; depth < 16 && name != null && name.startsWith("#"); depth++) {
			name = textures.get(name.substring(1));
		}
		return name == null || name.startsWith("#") ? null : name;
	}

	/** Every texture its faces use, followed to their names (those left open, as a template's "#wall", not among them). */
	public Set<String> textureNames() {
		Set<String> names = new LinkedHashSet<>();
		for (Element element : elements) {
			for (Face face : element.faces.values()) {
				String name = texture(face.texture);
				if (name != null) {
					names.add(name);
				}
			}
		}
		return names;
	}

	/**
	 * A box of the model: from and to in 0 to 16, its faces by direction; 26.3's element rotation (an axis, 0 x, 1 y, 2 z,
	 * or -1 for none; the angle in degrees about the origin; `rescale` stretching the other two axes by 1 / cos(angle),
	 * spec §88.5), and its `shade_direction_override` (the side its faces are shaded as, or null).
	 */
	public static final class Element {
		public final float[] from;
		public final float[] to;
		public final Map<Side, Face> faces;
		public final int rotationAxis;
		public final float rotationAngle;
		public final float[] rotationOrigin;
		public final boolean rescale;
		public final Side shadeSide;

		private Element(float[] from, float[] to, Map<Side, Face> faces, int rotationAxis, float rotationAngle, float[] rotationOrigin, boolean rescale,
			Side shadeSide) {
			this.from = from;
			this.to = to;
			this.faces = faces;
			this.rotationAxis = rotationAxis;
			this.rotationAngle = rotationAngle;
			this.rotationOrigin = rotationOrigin;
			this.rescale = rescale;
			this.shadeSide = shadeSide;
		}

		private static Element read(JsonObject json, String resource) {
			int axis = -1;
			float angle = 0.0F;
			float[] origin = {8.0F, 8.0F, 8.0F};
			boolean rescale = false;
			if (json.has("rotation")) {
				JsonObject rotation = json.getAsJsonObject("rotation");
				axis = "x|y|z".indexOf(rotation.get("axis").getAsString()) / 2;
				angle = rotation.get("angle").getAsFloat();
				origin = vector(rotation.getAsJsonArray("origin"));
				rescale = rotation.has("rescale") && rotation.get("rescale").getAsBoolean();
			}
			Side shadeSide = json.has("shade_direction_override") ? Side.byName(json.get("shade_direction_override").getAsString()) : null;
			Map<Side, Face> faces = new EnumMap<>(Side.class);
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("faces").entrySet()) {
				JsonObject face = entry.getValue().getAsJsonObject();
				// 26.3's face rotation: its UVs turned a quarter a step (`Quadrant`), 0, 90, 180 or 270
				int faceRotation = face.has("rotation") ? Math.floorMod(face.get("rotation").getAsInt(), 360) : 0;
				if (faceRotation % 90 != 0) {
					throw new IllegalStateException(resource + ": a face rotation not a quarter turn");
				}
				float[] uv = null;
				if (face.has("uv")) {
					JsonArray array = face.getAsJsonArray("uv");
					uv = new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()};
				}
				Side cull = face.has("cullface") ? Side.byName(face.get("cullface").getAsString()) : null;
				faces.put(Side.byName(entry.getKey()), new Face(face.get("texture").getAsString(), uv, cull, faceRotation));
			}
			return new Element(vector(json.getAsJsonArray("from")), vector(json.getAsJsonArray("to")), faces, axis, angle, origin, rescale, shadeSide);
		}

		private static float[] vector(JsonArray array) {
			return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
		}
	}

	/**
	 * A face: its texture reference, its UVs (null: worked out from the box, 26.3's `defaultFaceUV`), its cull face, its
	 * UV rotation (0, 90, 180, 270).
	 */
	public static final class Face {
		public final String texture;
		public final float[] uv;
		public final Side cull;
		public final int rotation;

		private Face(String texture, float[] uv, Side cull, int rotation) {
			this.texture = texture;
			this.uv = uv;
			this.cull = cull;
			this.rotation = rotation;
		}
	}

	/** The six directions as 26.3's models name them, with their unit steps and 1.7.10's side numbers. */
	public enum Side {
		DOWN(0, -1, 0, 0), UP(0, 1, 0, 1), NORTH(0, 0, -1, 2), SOUTH(0, 0, 1, 3), WEST(-1, 0, 0, 4), EAST(1, 0, 0, 5);

		public final int dx;
		public final int dy;
		public final int dz;
		/** 1.7.10's side number (0 down, 1 up, 2 north, 3 south, 4 west, 5 east), as ForgeDirection's. */
		public final int side;

		Side(int dx, int dy, int dz, int side) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.side = side;
		}

		public static Side byName(String name) {
			return valueOf(name.toUpperCase(java.util.Locale.ROOT));
		}

		public static Side byStep(int dx, int dy, int dz) {
			for (Side side : values()) {
				if (side.dx == dx && side.dy == dy && side.dz == dz) {
					return side;
				}
			}
			throw new IllegalArgumentException(dx + " " + dy + " " + dz);
		}
	}
}
