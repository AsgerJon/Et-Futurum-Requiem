package io.github.asgerjon.mcfly.mixins.early.lodestone;

import io.github.asgerjon.mcfly.lodestone.LodestoneCompass;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 ticks every living entity's held and worn items (LivingEntity.aiStep, EntityEquipment.tick); 1.7.10 only a
 * player's inventory. So a lodestone compass a mob, an armor stand or (later) a copper golem holds loses its target too
 * (SPEC-modern-movement.md §57.4). Compasses only: other items tick as in 1.7.10.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void etfu$tickHeldCompasses(CallbackInfo ci) {
		EntityLivingBase self = (EntityLivingBase) (Object) this;
		if (self.worldObj.isRemote || self instanceof EntityPlayer) {
			return;
		}
		ItemStack[] equipment = self.getLastActiveItems();
		if (equipment == null) {
			return;
		}
		for (ItemStack stack : equipment) {
			if (stack != null && stack.getItem() == Items.compass) {
				LodestoneCompass.tick(stack, self.worldObj);
			}
		}
	}
}
