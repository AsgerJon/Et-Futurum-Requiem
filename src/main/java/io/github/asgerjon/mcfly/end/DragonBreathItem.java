package io.github.asgerjon.mcfly.end;

import io.github.asgerjon.mcfly.base.McFlyItem;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

/**
 * 26.3's dragon's breath (`Items.DRAGON_BREATH`; SPEC-modern-movement.md §85): uncommon, the bottle left when it's used.
 * Bottled from a cloud the dragon made (`modernend.MixinItemGlassBottle`); brewed into a splash potion it makes the
 * lingering one (the brewing table, §46). 1.7.10's brewing string marks it an ingredient for EFR's brewing stand.
 */
public class DragonBreathItem extends McFlyItem {

	public DragonBreathItem() {
		super("dragon_breath");
		setContainerItem(Items.glass_bottle);
		setPotionEffect("-14+13");
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return EnumRarity.uncommon;
	}
}
