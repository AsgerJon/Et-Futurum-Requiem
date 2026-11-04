package ganymedes01.etfuturum.ducks;

/** 1.7.10's skull as a dragon head's (type 5): its jaw's animation (26.3's SkullBlockEntity; spec §76.12, 7d). */
public interface IDragonHead {

	/** 26.3's getAnimation: the ticks it's been powered, plus the partial tick while it still is. */
	float etfu$animation(float partialTicks);
}
