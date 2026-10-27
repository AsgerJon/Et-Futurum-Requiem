package ganymedes01.etfuturum.mixins.early.containerclose;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.inventory.ContainerClose;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The vanilla containers that drop their contents on closing (the cursor stack in Container, the crafting grids,
 * the anvil's inputs, the enchanting table's item) put them back into the inventory instead (ContainerClose). The
 * mod's own tables call ContainerClose themselves.
 */
@Mixin({Container.class, ContainerWorkbench.class, ContainerPlayer.class, ContainerRepair.class, net.minecraft.inventory.ContainerEnchantment.class})
public class MixinContainerClose {

	@WrapOperation(method = "onContainerClosed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;dropPlayerItemWithRandomChoice(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/EntityItem;"))
	private EntityItem etfu$backIntoTheInventory(EntityPlayer player, ItemStack stack, boolean traceOrigin, Operation<EntityItem> original) {
		return ContainerClose.giveBack(player, stack) ? null : original.call(player, stack, traceOrigin);
	}
}
