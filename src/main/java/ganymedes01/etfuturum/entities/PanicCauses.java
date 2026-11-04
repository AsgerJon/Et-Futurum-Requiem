package ganymedes01.etfuturum.entities;

import net.minecraft.util.DamageSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 26.3's damage type tags {@code panic_causes} and {@code panic_environmental_causes}, as 1.7.10 and EFR name their
 * damage (SPEC-modern-movement.md §63.4; the polar bear's too, §60). 1.7.10 has no lightning damage of its own (it
 * burns: {@code inFire}); fireworks, wind charges, sonic booms and dragon breath have no damage here.
 */
public final class PanicCauses {

	/** {@code panic_environmental_causes}: cactus, freeze, hot floor, in fire, lava, lightning, on fire. */
	public static final Set<String> ENVIRONMENTAL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"cactus", "freeze", "hotFloor", "inFire", "lava", "lightningBolt", "onFire")));

	/** {@code panic_causes}: the environmental ones and attacks. */
	public static final Set<String> ALL;

	static {
		Set<String> all = new HashSet<>(ENVIRONMENTAL);
		all.addAll(Arrays.asList("arrow", "explosion", "explosion.player", "fireball", "fireworks", "indirectMagic", "magic",
				"mob", "player", "sting", "thrown", "trident", "wither", "witherSkull"));
		ALL = Collections.unmodifiableSet(all);
	}

	private PanicCauses() {
	}

	public static boolean isPanicCause(DamageSource source) {
		return source != null && ALL.contains(source.damageType);
	}
}
