package ganymedes01.etfuturum.enchantment;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.items.equipment.ItemTrident;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * The trident's Riptide (SPEC-modern-movement.md §58.2): levels I-III, weight 2, enchanting cost 17 + 7 a level above I
 * to 50, tridents only. What it does is in trident.Riptide.
 */
public class Riptide extends Enchantment {

	public Riptide() {
		super(McFlyConfig.riptideID, 2, EnumEnchantmentType.all);
		Enchantment.addToBookList(this);
		setName("riptide");
	}

	@Override
	public int getMinEnchantability(int level) {
		return 17 + 7 * (level - 1);
	}

	@Override
	public int getMaxEnchantability(int level) {
		return 50;
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean canApply(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ItemTrident;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		return stack != null && (stack.getItem() instanceof ItemTrident || stack.getItem() == Items.book);
	}

	/** Never with Channeling or Loyalty. */
	@Override
	public boolean canApplyTogether(Enchantment other) {
		return super.canApplyTogether(other) && other != ModEnchantments.channeling && other != ModEnchantments.loyalty;
	}
}
