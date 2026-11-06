package io.github.asgerjon.mcfly.base;

/**
 * A block of ours with variants by meta (SPEC-modern-movement.md §88.2): 26.3's name of each, which its item's
 * unlocalised name takes ({@link McFlyMetaItem}).
 */
public interface NamedMetas {

	/** 26.3's name of the variant at this meta (an item's damage). */
	String nameOf(int meta);

	/**
	 * Whether the meta is one of its own variants, not an unused one that shows a variant (EFR's stone 0 shows granite,
	 * whose own meta is 1): a name goes to its own meta first (spec §89, step D).
	 */
	default boolean isVariant(int meta) {
		return true;
	}
}
