package io.github.asgerjon.mcfly.gameevent;

import net.minecraft.world.World;

/**
 * A game event listener (SPEC-modern-movement.md §90.9, step H1), 26.3's `GameEventListener`: where it listens from,
 * how far, and what it does with an event that reaches it.
 */
public interface GameEventListener {

	/** Where it listens from, now (x, y, z), or null when it can't. */
	double[] position(World world);

	/** How far it hears (26.3's `getListenerRadius`). */
	int radius();

	/** An event within its radius and the event's: whether it took it. */
	boolean handle(World world, GameEvent event, GameEvent.Context context, double x, double y, double z);

	/** 26.3's `DeliveryMode.BY_DISTANCE`: after the plain listeners, nearest first (a vibration's); else as visited. */
	default boolean byDistance() {
		return false;
	}
}
