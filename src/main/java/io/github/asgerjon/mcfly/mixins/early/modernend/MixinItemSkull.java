package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 26.3's dragon head as 1.7.10's sixth skull (damage 5; SPEC-modern-movement.md §76.12, 7d): in the creative list, named
 * "Dragon Head", placed as any skull (its type the damage). Its icon is the skeleton's (EFR's skull item renderer draws
 * the model).
 */
@Mixin(ItemSkull.class)
public class MixinItemSkull {

	/* client-only in 1.7.10: not there on a dedicated server */
	@Inject(method = "getSubItems", at = @At("TAIL"), require = 0)
	private void etfu$dragonHeadInList(Item item, CreativeTabs tab, List<ItemStack> list, CallbackInfo ci) {
		list.add(new ItemStack(item, 1, 5));
	}

	@Inject(method = "getUnlocalizedName", at = @At("HEAD"), cancellable = true)
	private void etfu$dragonHeadName(ItemStack stack, CallbackInfoReturnable<String> cir) {
		if (stack.getItemDamage() == 5) {
			cir.setReturnValue("item.skull.dragon");
		}
	}

	@Inject(method = "getIconFromDamage", at = @At("HEAD"), cancellable = true, require = 0)
	private void etfu$dragonHeadIcon(int damage, CallbackInfoReturnable<IIcon> cir) {
		if (damage == 5) {
			cir.setReturnValue(((ItemSkull) (Object) this).getIconFromDamage(0));
		}
	}
}
