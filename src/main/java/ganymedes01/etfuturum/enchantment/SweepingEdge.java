package ganymedes01.etfuturum.enchantment;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;

/**
 * Stronger sweeps (spec §46.3): a sweep's damage is 1 + level / (level + 1) × the attack's damage. The effect lives in
 * the combat update's attack (combat.MixinEntityPlayer). Swords only, as 1.7.10's weapon type is.
 */
public class SweepingEdge extends Enchantment {

	public SweepingEdge() {
		super(McFlyConfig.sweepingEdgeID, 2, EnumEnchantmentType.weapon);
		Enchantment.addToBookList(this);
		setName("sweeping_edge");
	}

	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		return 5 + (enchantmentLevel - 1) * 9;
	}

	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		return 20 + (enchantmentLevel - 1) * 9;
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	/** The share of the attack's damage a sweep adds: 1/2, 2/3, 3/4. */
	public static float damageRatio(int level) {
		return level <= 0 ? 0.0F : level / (level + 1.0F);
	}
}
