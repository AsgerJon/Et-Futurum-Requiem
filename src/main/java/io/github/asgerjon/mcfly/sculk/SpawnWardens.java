package io.github.asgerjon.mcfly.sculk;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * 26.3's `spawn_wardens` game rule (SPEC-modern-movement.md §90.9, step H4), by its older camel-case name
 * `doWardenSpawning`, as `DoInsomnia`'s: on by default; off, no shrieker responds.
 */
public final class SpawnWardens {

	public static final String GAMERULE_NAME = "doWardenSpawning";

	private SpawnWardens() {
	}

	public static void registerGamerule(World world) {
		if (!world.isRemote && !world.getGameRules().hasRule(GAMERULE_NAME)) {
			world.getGameRules().addGameRule(GAMERULE_NAME, "true");
		}
	}

	public static boolean isOn(World world) {
		GameRules rules = world.getGameRules();
		return !rules.hasRule(GAMERULE_NAME) || rules.getGameRuleBooleanValue(GAMERULE_NAME);
	}
}
