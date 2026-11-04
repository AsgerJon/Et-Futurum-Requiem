package ganymedes01.etfuturum.ducks;

/** An effect packet's effect number, whole (spec §70.3, stage C): 1.7.10 keeps it in a byte. */
public interface IWideEffectPacket {

	int etfu$effect();
}
