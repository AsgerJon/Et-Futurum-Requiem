package io.github.asgerjon.mcfly.gameevent;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A game event of McFly's (SPEC-modern-movement.md §90.9, step H1), 26.3's `GameEvent`: its name ("minecraft:step"),
 * how far it's heard (26.3's notification radius: 16, `shriek` 32, the jukebox's 10) and its vibration frequency (1 to
 * 15, 26.3's `VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT`, `resonate_n` n; 0 for those that make none); 26.3's game
 * event tags.
 */
public final class GameEvent {

	private static final Map<String, GameEvent> BY_NAME = new LinkedHashMap<>();
	private static final Map<String, Set<String>> TAGS = new HashMap<>();

	/** 26.3's, in its order; the frequency table's. */
	private static final String[] NAMES = {"block_activate", "block_attach", "block_change", "block_close", "block_deactivate", "block_destroy",
		"block_detach", "block_open", "block_place", "bounce", "container_close", "container_open", "drink", "eat", "elytra_glide", "entity_damage",
		"entity_die", "entity_dismount", "entity_interact", "entity_mount", "entity_place", "entity_action", "equip", "explode", "flap", "fluid_pickup",
		"fluid_place", "hit_ground", "instrument_play", "item_interact_finish", "item_interact_start", "jukebox_play", "jukebox_stop_play",
		"lightning_strike", "note_block_play", "prime_fuse", "projectile_land", "projectile_shoot", "sculk_sensor_tendrils_clicking", "shear", "shriek",
		"splash", "step", "swim", "teleport", "unequip"};
	private static final Map<String, Integer> FREQUENCY = new HashMap<>();

	static {
		Object[][] frequencies = {{"step", 1}, {"swim", 1}, {"flap", 1}, {"projectile_land", 2}, {"hit_ground", 2}, {"splash", 2}, {"bounce", 2},
			{"item_interact_finish", 3}, {"projectile_shoot", 3}, {"instrument_play", 3}, {"entity_action", 4}, {"elytra_glide", 4}, {"unequip", 4},
			{"entity_dismount", 5}, {"equip", 5}, {"entity_interact", 6}, {"shear", 6}, {"entity_mount", 6}, {"entity_damage", 7}, {"drink", 8},
			{"eat", 8}, {"container_close", 9}, {"block_close", 9}, {"block_deactivate", 9}, {"block_detach", 9}, {"container_open", 10},
			{"block_open", 10}, {"block_activate", 10}, {"block_attach", 10}, {"prime_fuse", 10}, {"note_block_play", 10}, {"block_change", 11},
			{"block_destroy", 12}, {"fluid_pickup", 12}, {"block_place", 13}, {"fluid_place", 13}, {"entity_place", 14}, {"lightning_strike", 14},
			{"teleport", 14}, {"entity_die", 15}, {"explode", 15}};
		for (Object[] entry : frequencies) {
			FREQUENCY.put((String) entry[0], (Integer) entry[1]);
		}
		for (String name : NAMES) {
			int radius = name.equals("shriek") ? 32 : name.startsWith("jukebox_") ? 10 : 16;
			add(name, radius, FREQUENCY.getOrDefault(name, 0));
		}
		for (int i = 1; i <= 15; i++) {
			add("resonate_" + i, 16, i);
		}
		Set<String> vibrations = new HashSet<>(Arrays.asList("block_attach", "block_change", "block_close", "block_destroy", "block_detach", "block_open",
			"block_place", "block_activate", "block_deactivate", "bounce", "container_close", "container_open", "drink", "eat", "elytra_glide",
			"entity_damage", "entity_die", "entity_dismount", "entity_interact", "entity_mount", "entity_place", "entity_action", "equip", "explode",
			"fluid_pickup", "fluid_place", "hit_ground", "instrument_play", "item_interact_finish", "lightning_strike", "note_block_play", "prime_fuse",
			"projectile_land", "projectile_shoot", "shear", "splash", "step", "swim", "teleport", "unequip", "flap"));
		for (int i = 1; i <= 15; i++) {
			vibrations.add("resonate_" + i);
		}
		TAGS.put("vibrations", vibrations);
		TAGS.put("ignore_vibrations_sneaking", new HashSet<>(Arrays.asList("hit_ground", "projectile_shoot", "step", "swim", "item_interact_start",
			"item_interact_finish")));
		TAGS.put("shrieker_can_listen", Collections.singleton("sculk_sensor_tendrils_clicking"));
		TAGS.put("allay_can_listen", Collections.singleton("note_block_play"));
		// 26.3's: the vibrations but flap, with the shriek and the shrieker's
		Set<String> warden = new HashSet<>(vibrations);
		warden.remove("flap");
		warden.add("shriek");
		warden.add("sculk_sensor_tendrils_clicking");
		TAGS.put("warden_can_listen", warden);
	}

	private static void add(String name, int radius, int frequency) {
		BY_NAME.put(name, new GameEvent("minecraft:" + name, radius, frequency));
	}

	public final String name;
	public final int radius;
	public final int frequency;

	private GameEvent(String name, int radius, int frequency) {
		this.name = name;
		this.radius = radius;
		this.frequency = frequency;
	}

	/** The event by its name ("minecraft:step" or "step"), or null. */
	public static GameEvent get(String name) {
		return name == null ? null : BY_NAME.get(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
	}

	/** Every event, in 26.3's order. */
	public static Collection<GameEvent> all() {
		return Collections.unmodifiableCollection(BY_NAME.values());
	}

	/** Whether it's in a game event tag of 26.3's ("minecraft:vibrations", "minecraft:ignore_vibrations_sneaking", ...). */
	public boolean is(String tag) {
		Set<String> events = TAGS.get(tag.startsWith("minecraft:") ? tag.substring("minecraft:".length()) : tag);
		return events != null && events.contains(name.substring("minecraft:".length()));
	}

	/** 26.3's `getResonanceEventByFrequency`. */
	public static GameEvent resonance(int frequency) {
		return get("resonate_" + frequency);
	}

	@Override
	public String toString() {
		return name;
	}

	/** 26.3's `GameEvent.Context`: the entity that caused it and the block state it's about (either may be null). */
	public static final class Context {
		public final Entity source;
		public final Block affected;
		public final int affectedMeta;

		public Context(Entity source, Block affected, int affectedMeta) {
			this.source = source;
			this.affected = affected;
			this.affectedMeta = affectedMeta;
		}
	}
}
