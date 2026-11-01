package ganymedes01.etfuturum.combat;

/** The attack and swap timers on a player (spec §46.1), on the server and on the player's own client. */
public interface ICombatPlayer {

	/** Ticks since the last attack. */
	int etfu$getAttackTicks();

	/** Ticks since the last swing or item change. */
	int etfu$getSwapTicks();

	/** Restarts both: a swing, a miss, an item change, a dropped item. */
	void etfu$resetAttackStrength();

	/** Restarts the attack timer only: an attack, when its damage is worked out. */
	void etfu$resetAttackTicks();

	/** The player's item cooldowns (§46.9). */
	ItemCooldowns etfu$getCooldowns();
}
