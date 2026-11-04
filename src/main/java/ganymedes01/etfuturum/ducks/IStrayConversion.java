package ganymedes01.etfuturum.ducks;

/** A skeleton turning into a stray in powder snow (spec §62.7), added by {@code powdersnow.MixinEntitySkeleton}. */
public interface IStrayConversion {

	boolean etfu$isConverting();

	/** Client side: what the server said. */
	void etfu$setConvertingSeen(boolean converting);
}
