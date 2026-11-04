package ganymedes01.etfuturum.world.structure.modern;

import java.util.Map;
import java.util.TreeMap;

/**
 * A 26.3 block state as a template holds it: the block's id and its properties (SPEC-modern-movement.md §76.12). Turned
 * and mirrored as 26.3's blocks turn their own (facing, axis, rotation); made into 1.7.10's blocks by
 * {@link ModernBlockStates}.
 */
public final class ModernState {

	public final String id;
	/** Sorted by name, as 26.3 writes a state. */
	public final Map<String, String> properties;

	public ModernState(String id, Map<String, String> properties) {
		this.id = id.contains(":") ? id : "minecraft:" + id;
		this.properties = new TreeMap<>(properties);
	}

	public String get(String property) {
		return properties.get(property);
	}

	public String get(String property, String fallback) {
		return properties.getOrDefault(property, fallback);
	}

	public boolean isAir() {
		return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air");
	}

	private ModernState with(String property, String value) {
		Map<String, String> changed = new TreeMap<>(properties);
		changed.put(property, value);
		return new ModernState(id, changed);
	}

	/** 26.3's BlockState.rotate: horizontal facings turned, axes x and z swapped by a quarter, standing rotations turned. */
	public ModernState rotate(ModernRotation rotation) {
		if (rotation == ModernRotation.NONE) {
			return this;
		}
		ModernState state = this;
		String facing = get("facing");
		if (facing != null && ModernRotation.isHorizontal(facing)) {
			state = state.with("facing", rotation.rotate(facing));
		}
		String axis = get("axis");
		if (axis != null && (rotation == ModernRotation.CLOCKWISE_90 || rotation == ModernRotation.COUNTERCLOCKWISE_90)) {
			if (axis.equals("x")) {
				state = state.with("axis", "z");
			} else if (axis.equals("z")) {
				state = state.with("axis", "x");
			}
		}
		String turn = get("rotation");
		if (turn != null) {
			state = state.with("rotation", Integer.toString(rotation.rotate(Integer.parseInt(turn), 16)));
		}
		return state;
	}

	/** 26.3's BlockState.mirror: facings across the mirror's line. */
	public ModernState mirror(ModernMirror mirror) {
		if (mirror == ModernMirror.NONE) {
			return this;
		}
		ModernState state = this;
		String facing = get("facing");
		if (facing != null && ModernRotation.isHorizontal(facing)) {
			state = state.with("facing", mirror.mirror(facing));
		}
		String turn = get("rotation");
		if (turn != null) {
			state = state.with("rotation", Integer.toString(mirror.mirror(Integer.parseInt(turn), 16)));
		}
		return state;
	}

	@Override
	public String toString() {
		if (properties.isEmpty()) {
			return id;
		}
		StringBuilder out = new StringBuilder(id).append('[');
		boolean first = true;
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			out.append(first ? "" : ",").append(entry.getKey()).append('=').append(entry.getValue());
			first = false;
		}
		return out.append(']').toString();
	}
}
