package io.github.asgerjon.mcfly.mixins.early.potionsbyname.client;

import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * The client side of potions by name (SPEC-modern-movement.md §69.3): 26.3's colour for a named potion (1.7.10 takes
 * it from the damage value alone), and the creative list as 26.3's: every offered potion in its order, the drinkable
 * ones then the splash ones, then whatever 1.7.10 (or a mod's bit patterns) lists that has no name.
 */
@Mixin(ItemPotion.class)
public class MixinItemPotion {

	@Unique
	private int etfu$listStart;

	@Inject(method = "getColorFromItemStack(Lnet/minecraft/item/ItemStack;I)I", at = @At("HEAD"), cancellable = true)
	private void etfu$namedColour(ItemStack stack, int pass, CallbackInfoReturnable<Integer> cir) {
		if (pass == 0 && NamedPotions.isReady()) {
			PotionContents contents = PotionContents.of(stack);
			if (contents.isNamed()) {
				cir.setReturnValue(contents.color());
			}
		}
	}

	@Inject(method = "getSubItems(Lnet/minecraft/item/Item;Lnet/minecraft/creativetab/CreativeTabs;Ljava/util/List;)V", at = @At("HEAD"))
	private void etfu$listStart(Item item, CreativeTabs tab, List list, CallbackInfo ci) {
		etfu$listStart = list.size();
	}

	@SuppressWarnings("unchecked")
	@Inject(method = "getSubItems(Lnet/minecraft/item/Item;Lnet/minecraft/creativetab/CreativeTabs;Ljava/util/List;)V", at = @At("RETURN"))
	private void etfu$namedList(Item item, CreativeTabs tab, List list, CallbackInfo ci) {
		if (!NamedPotions.isReady()) {
			return;
		}
		int start = Math.min(etfu$listStart, list.size());
		List<ItemStack> listed = new ArrayList<>(list.subList(start, list.size()));
		list.subList(start, list.size()).clear();
		for (boolean splash : new boolean[]{false, true}) {
			for (NamedPotion potion : NamedPotions.available()) {
				list.add(PotionContents.create(item, splash, potion));
			}
		}
		for (ItemStack stack : listed) {
			if (PotionContents.of(stack).potion() == null) {
				list.add(stack);
			}
		}
	}
}
