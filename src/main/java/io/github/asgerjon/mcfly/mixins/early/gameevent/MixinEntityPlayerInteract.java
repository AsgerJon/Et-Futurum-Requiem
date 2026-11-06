package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** An item used on an entity (shears, a name tag, a saddle, dye) posts `entity_interact` at it, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `Player.interactOn`). */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerInteract {

	@WrapOperation(method = "interactWith", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;interactWithEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/EntityLivingBase;)Z"))
	private boolean mcfly$itemOnEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, Operation<Boolean> original) {
		boolean used = original.call(stack, player, target);
		if (used) {
			GameEventHooks.atEntity(target, "entity_interact", player);
		}
		return used;
	}
}
