package io.github.asgerjon.mcfly.base;

/**
 * A plant that goes in 1.7.10's flower pot (SPEC-modern-movement.md §90, step D2), as 26.3's `FlowerPotBlock` pots it:
 * the pot holds the plant's item and damage (1.7.10's own tile entity), and is drawn as 26.3's potted block's model.
 * See {@link McFlyPots}.
 */
public interface McFlyPottable {

	/** 26.3's model of the plant potted at this damage (`potted_azalea_bush`), or null when it doesn't go in a pot. */
	String pottedModel(int meta);
}
