package ganymedes01.etfuturum.gamerule;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * Phantoms spawn over the sleepless (SPEC-modern-movement.md §68.3): 26.3's `spawn_phantoms` game rule, by its older
 * name `doInsomnia` (as 1.7.10's rules are camel case; EFR's modern /gamerule takes the new name too).
 */
public final class DoInsomnia {

	public static final String GAMERULE_NAME = "doInsomnia";
	public static final String DEFAULT_VALUE = "true";

	private DoInsomnia() {
	}

	public static void registerGamerule(World world) {
		if (!world.isRemote && !world.getGameRules().hasRule(GAMERULE_NAME)) {
			world.getGameRules().addGameRule(GAMERULE_NAME, DEFAULT_VALUE);
		}
	}

	/** A world without the rule (not loaded through the world load event yet) counts as on, the default. */
	public static boolean isOn(World world) {
		GameRules rules = world.getGameRules();
		return !rules.hasRule(GAMERULE_NAME) || rules.getGameRuleBooleanValue(GAMERULE_NAME);
	}
}
