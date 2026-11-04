package ganymedes01.etfuturum.world.end.modern.dragon;

/** 26.3's dragon phases, by their ids (SPEC-modern-movement.md §76.7). */
public enum DragonPhase {
	HOLDING_PATTERN, STRAFE_PLAYER, LANDING_APPROACH, LANDING, TAKEOFF, SITTING_FLAMING, SITTING_SCANNING, SITTING_ATTACKING,
	CHARGING_PLAYER, DYING, HOVER;

	public static DragonPhase byId(int id) {
		return id >= 0 && id < values().length ? values()[id] : HOLDING_PATTERN;
	}

	public boolean isSitting() {
		return this == SITTING_FLAMING || this == SITTING_SCANNING || this == SITTING_ATTACKING || this == HOVER;
	}
}
