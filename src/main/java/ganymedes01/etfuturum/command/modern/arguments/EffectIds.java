package ganymedes01.etfuturum.command.modern.arguments;

import ganymedes01.etfuturum.potion.EffectRegistry;
import net.minecraft.potion.Potion;

import java.util.Collection;

/**
 * Modern's ids for 1.7.10's effects (spec §41.3): now the registry of effects by name (spec §70,
 * {@link EffectRegistry}), kept under this name for the commands and potions that use it.
 */
public final class EffectIds {

	private EffectIds() {
	}

	public static Potion byId(String id) {
		return EffectRegistry.get(id);
	}

	public static String idOf(Potion potion) {
		return EffectRegistry.key(potion);
	}

	/** Every id, sorted. */
	public static Collection<String> ids() {
		return EffectRegistry.names();
	}
}
