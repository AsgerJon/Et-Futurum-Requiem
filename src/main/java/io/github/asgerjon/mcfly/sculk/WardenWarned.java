package io.github.asgerjon.mcfly.sculk;

/** A player's warden warning (26.3's `ServerPlayer.getWardenSpawnTracker`), mixed into 1.7.10's player by `mixins.early.sculk.MixinEntityPlayer`. */
public interface WardenWarned {

	WardenSpawnTracker mcfly$wardenSpawnTracker();
}
