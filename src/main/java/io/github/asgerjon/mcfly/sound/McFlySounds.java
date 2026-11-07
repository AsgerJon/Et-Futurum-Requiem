package io.github.asgerjon.mcfly.sound;

import io.github.asgerjon.mcfly.McFly;

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
		// berries picked from cave vines (spec §90.3); shears on a growing plant's head (the Nether vines' too, §88.7)
		EVENTS.put("block.cave_vines.pick_berries", "block");
		EVENTS.put("block.growing_plant.crop", "block");
		// pointed dripstone's landing and drips (spec §90.7)
		for (String event : new String[]{"land", "drip_water", "drip_lava", "drip_water_into_cauldron", "drip_lava_into_cauldron"}) {
			EVENTS.put("block.pointed_dripstone." + event, "block");
		}
		// the big dripleaf's tilt (spec §90.8)
		EVENTS.put("block.big_dripleaf.tilt_down", "block");
		EVENTS.put("block.big_dripleaf.tilt_up", "block");
		// the sculk sensors' clicking and amethyst's resonance (spec §90.9, step H2)
		EVENTS.put("block.sculk_sensor.clicking", "block");
		EVENTS.put("block.sculk_sensor.clicking_stop", "block");
		EVENTS.put("block.amethyst_block.resonate", "block");
		// the catalyst's bloom (spec §90.9, step H3)
		EVENTS.put("block.sculk_catalyst.bloom", "block");
		EVENTS.put("block.sculk.spread", "block");
		EVENTS.put("block.sculk.charge", "block");
		// the set types' clicks EFR never played (spec §91.7): the stone set's, bamboo's plate's
		for (String event : new String[]{"block.stone_button", "block.stone_pressure_plate", "block.bamboo_wood_pressure_plate"}) {
			EVENTS.put(event + ".click_on", "block");
			EVENTS.put(event + ".click_off", "block");
		}
		// a waxed sign refusing a use (spec 91.8)
		EVENTS.put("block.sign.waxed_interact_fail", "block");
		EVENTS.put("block.hanging_sign.waxed_interact_fail", "block");
		// the boats' paddles (spec 91.12; EFR listed them as players')
		EVENTS.put("entity.boat.paddle_land", "neutral");
		EVENTS.put("entity.boat.paddle_water", "neutral");
		// the smoker's and blast furnace's (spec 92.3; EFR's list had them)
		EVENTS.put("block.smoker.smoke", "block");
		EVENTS.put("block.blastfurnace.fire_crackle", "block");
		// the barrel's (spec 92.4)
		EVENTS.put("block.barrel.open", "block");
		EVENTS.put("block.barrel.close", "block");
		// the shelves' (spec 91.10)
		for (String event : new String[]{"activate", "deactivate", "place_item", "take_item", "single_swap", "multi_swap"}) {
			EVENTS.put("block.shelf." + event, "block");
		}
		// the shriek and the warden's replies (spec §90.9, step H4)
		EVENTS.put("block.sculk_shrieker.shriek", "block");
		EVENTS.put("entity.warden.nearby_close", "hostile");
		EVENTS.put("entity.warden.nearby_closer", "hostile");
		EVENTS.put("entity.warden.nearby_closest", "hostile");
		EVENTS.put("entity.warden.listening_angry", "hostile");
		// the amethyst buds' own (their step the cluster's), the chime (a step on amethyst, a projectile's hit) (spec §90.4)
		for (String bud : new String[]{"small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud"}) {
			EVENTS.put("block." + bud + ".break", "block");
			EVENTS.put("block." + bud + ".place", "block");
		}
		EVENTS.put("block.amethyst_block.chime", "block");
		// a water bottle on dirt (spec §90, step C)
		EVENTS.put("entity.generic.splash", "block");
		// glow lichen's step (26.3's GLOW_LICHEN steps as a vine)
		EVENTS.put("block.vine.step", "neutral");
		// copper's (spec §87): the bulb, the door and trapdoor, honeycomb's wax and the axe's
		EVENTS.put("block.copper_bulb.turn_on", "block");
		EVENTS.put("block.copper_bulb.turn_off", "block");
		EVENTS.put("block.copper_door.open", "block");
		EVENTS.put("block.copper_door.close", "block");
		EVENTS.put("block.copper_trapdoor.open", "block");
		EVENTS.put("block.copper_trapdoor.close", "block");
		EVENTS.put("item.honeycomb.wax_on", "player");
		EVENTS.put("item.axe.wax_off", "player");
		EVENTS.put("item.axe.scrape", "player");
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
		// a lingering potion thrown (spec §82)
		EVENTS.put("entity.lingering_potion.throw", "neutral");
		// our blocks' sound types (spec §79.4): 26.3's break, step and place for each
		// each type's own paths (the bamboo shoot steps as the stalk: spec 91.11)
		String prefix = McFly.MC_ASSETS + ":";
		for (McFlySoundType type : McFlySoundType.VALUES) {
			EVENTS.put(type.getBreakSound().replace(prefix, ""), "block");
			EVENTS.put(type.getStepResourcePath().replace(prefix, ""), "neutral");
			EVENTS.put(type.func_150496_b().replace(prefix, ""), "block");
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
