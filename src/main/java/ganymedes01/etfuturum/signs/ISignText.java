package ganymedes01.etfuturum.signs;

/**
 * A sign's text look as 26.3 has it (SPEC-modern-movement.md §67.2; `SignText`'s colour and glow, `SignBlockEntity`'s
 * wax), added to every `TileEntitySign` (EFR's wood signs too) by `mixins.early.signs.MixinTileEntitySign`.
 */
public interface ISignText {

	/** The dye's name, 26.3's (`black` unless dyed). */
	String etfu$getColor();

	void etfu$setColor(String color);

	boolean etfu$isGlowing();

	void etfu$setGlowing(boolean glowing);

	boolean etfu$isWaxed();

	void etfu$setWaxed(boolean waxed);
}
