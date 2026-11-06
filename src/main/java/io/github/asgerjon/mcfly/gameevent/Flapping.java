package io.github.asgerjon.mcfly.gameevent;

/**
 * An entity that flaps (SPEC-modern-movement.md §90.9, step H1; 26.3's `Entity.isFlapping` and `onFlap`), mixed into
 * 1.7.10's: after a move of this length (x 0.6, 26.3's `flyDist`) with air under it, whether it flapped now.
 */
public interface Flapping {

	boolean mcfly$flapped(float moved);
}
