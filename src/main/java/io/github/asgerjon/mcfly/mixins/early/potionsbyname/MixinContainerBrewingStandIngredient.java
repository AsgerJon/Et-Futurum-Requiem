package io.github.asgerjon.mcfly.mixins.early.potionsbyname;

import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotions;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7.10's brewing stand's reagent slot takes 26.3's reagents too (the phantom membrane, ...; spec §69.3). */
@Mixin(targets = "net.minecraft.inventory.ContainerBrewingStand$Ingredient")
public abstract class MixinContainerBrewingStandIngredient {

	@Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
	private void etfu$tableReagents(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (NamedPotions.isReady()) {
			cir.setReturnValue(BrewingTable.isIngredient(stack));
		}
	}
}
