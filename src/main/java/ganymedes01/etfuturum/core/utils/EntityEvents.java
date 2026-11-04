package ganymedes01.etfuturum.core.utils;

/** Entity statuses EFR sends (World.setEntityState) beyond 1.7.10's, numbered as modern's entity events. */
public final class EntityEvents {

	/** A totem of undying saved the entity (modern's event 35; SPEC-modern-movement.md §55.2). */
	public static final byte TOTEM_USED = 35;

	/** A glow squid squirts its ink (EFR's own; 26.3's server sends the particles; SPEC-modern-movement.md §67.1). */
	public static final byte GLOW_SQUID_INK = 60;

	private EntityEvents() {
	}
}
