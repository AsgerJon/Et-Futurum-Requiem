package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Bone meal that did something posts `item_interact_finish` at the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `BoneMealItem.useOn`). */
@Mixin(ItemDye.class)
public abstract class MixinItemDye {

	@WrapOperation(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemDye;applyBonemeal(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;)Z"))
	private boolean mcfly$boneMeal(ItemStack stack, World world, int x, int y, int z, EntityPlayer player, Operation<Boolean> original) {
		boolean used = original.call(stack, world, x, y, z, player);
		if (used) {
			GameEventHooks.usedItem(player, false);
		}
		return used;
	}
}
