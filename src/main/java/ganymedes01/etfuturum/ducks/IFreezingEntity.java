package ganymedes01.etfuturum.ducks;

/**
 * Powder snow's state on every entity (SPEC-modern-movement.md §62), added by {@code powdersnow.MixinEntity}: 26.3's
 * {@code TicksFrozen}, {@code isInPowderSnow} / {@code wasInPowderSnow}, the stuck multiplier of
 * {@code makeStuckInBlock}, and what the server last told the clients.
 */
public interface IFreezingEntity {

	int etfu$getTicksFrozen();

	void etfu$setTicksFrozen(int ticks);

	boolean etfu$isInPowderSnow();

	void etfu$setInPowderSnow(boolean in);

	boolean etfu$wasInPowderSnow();

	/** Movement this tick times (x, y, z), then motion 0 (26.3 {@code makeStuckInBlock}). */
	void etfu$makeStuck(double x, double y, double z);

	/** The sideways part of the last move asked for, for the snowflakes (26.3's known movement). */
	boolean etfu$movedSideways();

	/** Fire is put out once every block the entity is in has had its turn (26.3 runs EXTINGUISH after them all). */
	void etfu$extinguishAfterCollisions();

	/** The climb-out goal asks to climb this tick (1.7.10's mobs jump through their own jump helpers, not always). */
	void etfu$requestClimb();

	boolean etfu$takeClimbRequest();

	int etfu$lastSentTicks();

	void etfu$setLastSentTicks(int ticks);
}
