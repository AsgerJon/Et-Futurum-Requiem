package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shearing an entity posts `shear` at it, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `Shearable.shear`). */
@Mixin(ItemShears.class)
public abstract class MixinItemShears {

	@Inject(method = "itemInteractionForEntity", at = @At("RETURN"))
	private void mcfly$sheared(ItemStack stack, EntityPlayer player, EntityLivingBase target, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() && !target.worldObj.isRemote) {
			GameEvents.post(target.worldObj, io.github.asgerjon.mcfly.gameevent.GameEvent.get("shear"), target.posX, target.boundingBox.minY, target.posZ, player,
				null, 0);
		}
	}
}
