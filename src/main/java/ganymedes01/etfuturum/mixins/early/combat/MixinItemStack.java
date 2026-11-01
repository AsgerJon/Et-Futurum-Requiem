package ganymedes01.etfuturum.mixins.early.combat;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.combat.WeaponStats;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Weapons get modern's damage and speed (spec §46.2) from the item's own modifiers; a stack with its own
 * AttributeModifiers tag keeps them, as modern's attribute_modifiers component replaces the item's.
 */
@Mixin(ItemStack.class)
public class MixinItemStack {

	@WrapOperation(method = "getAttributeModifiers", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getAttributeModifiers(Lnet/minecraft/item/ItemStack;)Lcom/google/common/collect/Multimap;", remap = false))
	private Multimap<String, AttributeModifier> etfu$modernWeaponStats(Item item, ItemStack stack, Operation<Multimap<String, AttributeModifier>> original) {
		return WeaponStats.withModernStats(item, original.call(item, stack));
	}
}
