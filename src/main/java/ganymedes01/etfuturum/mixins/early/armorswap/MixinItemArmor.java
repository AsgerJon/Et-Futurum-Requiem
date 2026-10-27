package ganymedes01.etfuturum.mixins.early.armorswap;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-clicking with armour swaps it with the worn piece (SPEC-modern-movement.md §31, modern since 1.19.4). 1.7.10
 * only puts it on when the slot is empty, and that case is left to vanilla. Armour that overrides the right-click
 * itself keeps its own behaviour, since this only runs through ItemArmor's.
 */
@Mixin(ItemArmor.class)
public class MixinItemArmor {

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void etfu$swapWithWornArmour(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		int slot = EntityLiving.getArmorPosition(stack) - 1;
		ItemStack worn = player.getCurrentArmor(slot);
		if (worn == null || ItemStack.areItemStacksEqual(stack, worn)) {
			return;
		}
		if (!world.isRemote) {
			StatBase stat = StatList.objectUseStats[Item.getIdFromItem(stack.getItem())];
			if (stat != null) {
				player.addStat(stat, 1);
			}
		}
		if (stack.stackSize <= 1) {
			player.setCurrentItemOrArmor(slot + 1, stack.copy());
			cir.setReturnValue(worn);
			return;
		}
		player.setCurrentItemOrArmor(slot + 1, stack.splitStack(1));
		if (!player.inventory.addItemStackToInventory(worn)) {
			player.dropPlayerItemWithRandomChoice(worn, false);
		}
		cir.setReturnValue(stack);
	}
}
