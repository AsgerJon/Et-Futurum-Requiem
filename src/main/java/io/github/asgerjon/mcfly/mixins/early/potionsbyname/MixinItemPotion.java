package io.github.asgerjon.mcfly.mixins.early.potionsbyname;

import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * A potion by name (SPEC-modern-movement.md §69.3) gives its 26.3 effects and has its 26.3 name. 1.7.10 reads a
 * potion's effects from the stack here for drinking, splashing, the tooltip and a witch, so a named 1.7.10 potion
 * gets 26.3's lengths everywhere, splash ones whole (1.7.10 made them 3/4, §69.4). A potion with no name (an odd
 * damage value, or custom effects that replace a potion's) stays 1.7.10's.
 */
@Mixin(ItemPotion.class)
public class MixinItemPotion {

	@Inject(method = "getEffects(Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
	private void etfu$namedEffects(ItemStack stack, CallbackInfoReturnable<List> cir) {
		if (!NamedPotions.isReady()) {
			return;
		}
		PotionContents contents = PotionContents.of(stack);
		if (contents.potion() != null) {
			cir.setReturnValue(contents.allEffects());
		}
	}

	/** 26.3's `item.minecraft.potion.effect.<name>` (or the splash potion's); a potion without such a key keeps 1.7.10's. */
	@Inject(method = "getItemStackDisplayName(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void etfu$namedName(ItemStack stack, CallbackInfoReturnable<String> cir) {
		if (!NamedPotions.isReady()) {
			return;
		}
		PotionContents contents = PotionContents.of(stack);
		if (contents.potion() != null || contents.customName() != null) {
			String key = contents.nameKey(PotionContents.isSplash(stack) ? "splash_potion" : "potion");
			if (StatCollector.canTranslate(key)) {
				cir.setReturnValue(StatCollector.translateToLocal(key).trim());
			}
		}
	}
}
