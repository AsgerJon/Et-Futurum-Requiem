package ganymedes01.etfuturum.enchantment;

import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;

/**
 * Faster movement in water. The effect lives in the modern water movement (modernmovement.MixinEntityLivingBase):
 * water movement efficiency = level / 3, halved while not on the ground.
 */
public class DepthStrider extends Enchantment {

	public DepthStrider() {
		super(ConfigEnchantsPotions.depthStriderID, 2, EnumEnchantmentType.armor_feet);
		Enchantment.addToBookList(this);
		setName("depth_strider");
	}

	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		return enchantmentLevel * 10;
	}

	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		return this.getMinEnchantability(enchantmentLevel) + 15;
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean canApplyTogether(Enchantment other) {
		return super.canApplyTogether(other) && (ModEnchantments.frostWalker == null || other != ModEnchantments.frostWalker);
	}
}
