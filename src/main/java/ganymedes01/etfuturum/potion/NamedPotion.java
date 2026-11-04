package ganymedes01.etfuturum.potion;

import com.github.bsideup.jabel.Desugar;
import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A potion by name, as 26.3's `Potion` (SPEC-modern-movement.md §69.3): its id (`minecraft:long_swiftness`), the name
 * its lang keys use (`swiftness`: the long and strong ones share their base's), its effects by effect name, and the
 * drinkable one's damage value when 1.7.10 has that potion (-1 when it doesn't).
 */
public final class NamedPotion {

	/** One effect of the potion: 26.3's effect name, its duration in ticks, its amplifier. */
	@Desugar
	public record Effect(String effect, int duration, int amplifier) {
	}

	private final String id;
	private final String baseName;
	private final List<Effect> effects;
	private final int legacyDamage;

	NamedPotion(String id, String baseName, int legacyDamage, List<Effect> effects) {
		this.id = id;
		this.baseName = baseName;
		this.legacyDamage = legacyDamage;
		this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
	}

	public String id() {
		return id;
	}

	/** The last part of its lang keys (`item.minecraft.potion.effect.<baseName>`). */
	public String baseName() {
		return baseName;
	}

	public List<Effect> effects() {
		return effects;
	}

	/** The drinkable one's damage value in 1.7.10, or -1 when 1.7.10 has no such potion. */
	public int legacyDamage() {
		return legacyDamage;
	}

	/** Offered (the creative list, commands, brewing) only when every one of its effects exists. */
	public boolean isAvailable() {
		for (Effect effect : effects) {
			if (EffectIds.byId(effect.effect()) == null) {
				return false;
			}
		}
		return true;
	}

	/** Its effects as 1.7.10's, new instances each time (none for an effect that doesn't exist). */
	public List<PotionEffect> potionEffects() {
		List<PotionEffect> list = new ArrayList<>();
		for (Effect effect : effects) {
			Potion potion = EffectIds.byId(effect.effect());
			if (potion != null) {
				list.add(new PotionEffect(potion.id, effect.duration(), effect.amplifier()));
			}
		}
		return list;
	}

	@Override
	public String toString() {
		return id;
	}
}
