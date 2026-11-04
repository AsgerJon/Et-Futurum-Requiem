package ganymedes01.etfuturum.trident;

/** A player's Riptide spin (modern's auto spin attack), kept on each side; on EntityPlayer by trident.MixinEntityPlayer. */
public interface IRiptidePlayer {

	/** Ticks of spin left, counted down on each side. */
	int etfu$getSpinTicks();

	void etfu$setSpinTicks(int ticks);
}
