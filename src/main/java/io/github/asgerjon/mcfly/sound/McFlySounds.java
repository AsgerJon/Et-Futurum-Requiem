package io.github.asgerjon.mcfly.sound;

import makamys.mclib.ext.assetdirector.ADConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * McFly's sound events (SPEC-modern-movement.md §79, B5), by their modern names, each with its sound category: fetched
 * from the launcher's assets by MCLib's asset director (MCLib, Unlicense; under the namespace of the assets' version).
 * MCLib keeps one list per mod id, and this fork's mod is still EFR's, so ours go into the list EFR hands it; McFly
 * registers its own once it has its own mod id.
 */
public final class McFlySounds {

	public static final Map<String, String> EVENTS = new LinkedHashMap<>();

	static {
		EVENTS.put("entity.lightning_bolt.thunder", "weather");
		EVENTS.put("entity.lightning_bolt.impact", "weather");
		EVENTS.put("entity.skeleton_horse.ambient_water", "neutral");
		EVENTS.put("entity.skeleton_horse.swim", "neutral");
		EVENTS.put("entity.skeleton_horse.jump_water", "neutral");
		EVENTS.put("entity.breeze.wind_burst", "neutral");
		EVENTS.put("entity.phantom.ambient", "hostile");
		EVENTS.put("entity.phantom.bite", "hostile");
		EVENTS.put("entity.phantom.death", "hostile");
		EVENTS.put("entity.phantom.flap", "hostile");
		EVENTS.put("entity.phantom.hurt", "hostile");
		EVENTS.put("entity.phantom.swoop", "hostile");
		EVENTS.put("entity.glow_squid.ambient", "neutral");
		EVENTS.put("entity.glow_squid.hurt", "neutral");
		EVENTS.put("entity.glow_squid.death", "neutral");
		EVENTS.put("entity.glow_squid.squirt", "neutral");
		EVENTS.put("entity.glow_item_frame.add_item", "player");
		EVENTS.put("entity.glow_item_frame.break", "player");
		EVENTS.put("entity.glow_item_frame.place", "player");
		EVENTS.put("entity.glow_item_frame.remove_item", "player");
		EVENTS.put("entity.glow_item_frame.rotate_item", "player");
		EVENTS.put("item.lead.tied", "neutral");
		EVENTS.put("item.lead.untied", "neutral");
		EVENTS.put("entity.polar_bear.ambient", "neutral");
		EVENTS.put("entity.polar_bear.ambient_baby", "neutral");
		EVENTS.put("entity.polar_bear.death", "neutral");
		EVENTS.put("entity.polar_bear.hurt", "neutral");
		EVENTS.put("entity.polar_bear.step", "neutral");
		EVENTS.put("entity.polar_bear.warning", "neutral");
		EVENTS.put("entity.player.hurt_freeze", "player");
		EVENTS.put("entity.skeleton.converted_to_stray", "hostile");
		EVENTS.put("item.shield.block", "player");
		EVENTS.put("item.shield.break", "player");
		EVENTS.put("item.trident.throw", "player");
		EVENTS.put("item.trident.hit", "player");
		EVENTS.put("item.trident.hit_ground", "player");
		EVENTS.put("item.trident.return", "player");
		EVENTS.put("item.trident.riptide_1", "player");
		EVENTS.put("item.trident.riptide_2", "player");
		EVENTS.put("item.trident.riptide_3", "player");
		EVENTS.put("item.trident.thunder", "player");
		EVENTS.put("item.dye.use", "block");
		EVENTS.put("item.glow_ink_sac.use", "block");
		EVENTS.put("item.ink_sac.use", "block");
		EVENTS.put("item.lodestone_compass.lock", "player");
		EVENTS.put("item.bucket.fill_powder_snow", "player");
		EVENTS.put("item.bucket.empty_powder_snow", "player");
		EVENTS.put("item.spyglass.use", "player");
		EVENTS.put("item.spyglass.stop_using", "player");
		EVENTS.put("ui.loom.select_pattern", "master");
		EVENTS.put("ui.loom.take_result", "block");
		EVENTS.put("entity.ender_dragon.shoot", "hostile");
		EVENTS.put("entity.dragon_fireball.explode", "hostile");
		EVENTS.put("item.bottle.fill_dragonbreath", "neutral");
		EVENTS.put("block.end_gateway.spawn", "block");
		// our blocks' sound types (spec §79.4): 26.3's break, step and place for each
		for (McFlySoundType type : McFlySoundType.VALUES) {
			EVENTS.put("block." + type.soundName + ".break", "block");
			EVENTS.put("block." + type.soundName + ".step", "neutral");
			EVENTS.put("block." + type.soundName + ".place", "block");
		}
	}

	private McFlySounds() {
	}

	/** Our events into an asset director config, for the assets' version. */
	public static void addTo(ADConfig config, String version) {
		for (Map.Entry<String, String> event : EVENTS.entrySet()) {
			config.addSoundEvent(version, event.getKey(), event.getValue());
		}
	}
}
