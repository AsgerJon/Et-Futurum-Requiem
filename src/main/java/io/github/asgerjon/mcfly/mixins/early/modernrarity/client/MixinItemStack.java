package io.github.asgerjon.mcfly.mixins.early.modernrarity.client;

import ganymedes01.etfuturum.client.ModernRarity;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An enchanted item's rarity as 26.3's (SPEC-modern-movement.md §61.3): its rarity unenchanted, a step up. 1.7.10 made
 * any enchanted item rare, and items with their own rarity ignored enchanting (an enchanted trident stayed rare).
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {

	@Inject(method = "getRarity", at = @At("RETURN"), cancellable = true)
	private void etfu$enchantedStepUp(CallbackInfoReturnable<EnumRarity> cir) {
		ItemStack self = (ItemStack) (Object) this;
		if (!self.isItemEnchanted() || self.getItem() == null) {
			return;
		}
		ItemStack plain = self.copy();
		NBTTagCompound tag = (NBTTagCompound) plain.getTagCompound().copy();
		tag.removeTag("ench");
		plain.setTagCompound(tag.hasNoTags() ? null : tag);
		cir.setReturnValue(ModernRarity.enchanted(plain.getItem().getRarity(plain)));
	}
}
