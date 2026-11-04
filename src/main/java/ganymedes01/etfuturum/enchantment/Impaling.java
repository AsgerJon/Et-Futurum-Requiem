package ganymedes01.etfuturum.enchantment;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.items.equipment.ItemTrident;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentDamage;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * The trident's Impaling (SPEC-modern-movement.md §58.4): levels I-V, weight 2, enchanting cost 1 + 8 a level above I,
 * up to 20 more; tridents only; never with Sharpness, Smite or Bane of Arthropods (they refuse it too, through
 * trident.MixinEnchantmentDamage). What it does is in trident.Impaling.
 */
public class Impaling extends Enchantment {

	public Impaling() {
		super(McFlyConfig.impalingID, 2, EnumEnchantmentType.all);
		Enchantment.addToBookList(this);
		setName("impaling");
	}

	@Override
	public int getMinEnchantability(int level) {
		return 1 + 8 * (level - 1);
	}

	@Override
	public int getMaxEnchantability(int level) {
		return getMinEnchantability(level) + 20;
	}

	@Override
	public int getMaxLevel() {
		return 5;
	}

	@Override
	public boolean canApply(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ItemTrident;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		return stack != null && (stack.getItem() instanceof ItemTrident || stack.getItem() == Items.book);
	}

	@Override
	public boolean canApplyTogether(Enchantment other) {
		return super.canApplyTogether(other) && !(other instanceof EnchantmentDamage);
	}
}
