package ganymedes01.etfuturum.movement;

/**
 * The movement an entity is known to make per tick. On the client that is its motion; on the server a player's motion
 * is not tracked, so it is the step the last position packet brought, as modern's server keeps the last known client
 * movement for the projectiles the player launches.
 */
public interface IKnownMovement {

	double etfu$knownMotionX();

	double etfu$knownMotionY();

	double etfu$knownMotionZ();

	void etfu$setKnownMovement(double x, double y, double z);
}
