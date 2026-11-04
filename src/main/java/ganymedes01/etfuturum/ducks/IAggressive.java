package ganymedes01.etfuturum.ducks;

/**
 * 26.3's {@code Mob.isAggressive} for zombies (SPEC-modern-movement.md §63.9): set on the server by the attack AI, synced
 * on the zombie's DataWatcher, read by the model to raise the arms higher.
 */
public interface IAggressive {

	boolean etfu$isAggressive();

	void etfu$setAggressive(boolean aggressive);
}
