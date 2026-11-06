package io.github.asgerjon.mcfly.elytra;

/**
 * A player that can glide (SPEC-modern-movement.md §17, §85), mixed into 1.7.10's player by
 * `mixins.early.elytra.MixinEntityPlayer`: 26.3's fall flying flag (a shared entity flag, synced, saved as
 * `FallFlying`), the ticks spent gliding, and on the client the wings' animation. For other mods too:
 * `((GlidingPlayer) player).etfu$isElytraFlying()`.
 */
public interface GlidingPlayer {

	/** Whether it's gliding (26.3's `isFallFlying`). */
	boolean etfu$isElytraFlying();

	void etfu$setElytraFlying(boolean flag);

	/** Ticks spent gliding so far, 0 while not gliding. */
	int etfu$getFallFlyingTicks();

	/** The wing angles and the velocity history used to draw this player (client only). */
	ElytraAnimationState etfu$getElytraAnimation();
}
