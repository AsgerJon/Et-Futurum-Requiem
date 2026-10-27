package ganymedes01.etfuturum.elytra;

public interface IElytraPlayer extends ganymedes01.etfuturum.api.elytra.IElytraPlayer {
	void etfu$setElytraFlying(boolean flag);

	/**
	 * Ticks spent gliding so far, 0 while not gliding (§17).
	 */
	int etfu$getFallFlyingTicks();

	/**
	 * The wing angles and the velocity history used to draw this player (client only, §21).
	 */
	ElytraAnimationState etfu$getElytraAnimation();
}
