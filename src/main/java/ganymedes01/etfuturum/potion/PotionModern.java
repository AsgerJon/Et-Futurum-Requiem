package ganymedes01.etfuturum.potion;

/**
 * One of 26.3's effects whose work is elsewhere (SPEC-modern-movement.md §69.11; `ModernEffects`): luck and bad luck,
 * wind charged, weaving, oozing, infested. Named and drawn by its 26.3 name (`textures/mob_effect/<name>.png`).
 */
public class PotionModern extends ModPotions {

	protected PotionModern(String name, int id, boolean isBad, int color) {
		super(name, id, isBad, color);
		setPotionName("potion." + name);
	}
}
