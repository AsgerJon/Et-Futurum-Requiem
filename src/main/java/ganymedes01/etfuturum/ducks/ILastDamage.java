package ganymedes01.etfuturum.ducks;

import net.minecraft.util.DamageSource;

/** 26.3's {@code getLastDamageSource}: the last damage taken, if within the timeout (ticks), else null (spec §63.4). */
public interface ILastDamage {

	DamageSource etfu$lastDamage(int timeout);
}
