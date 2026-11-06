package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * `equip` and `unequip` (SPEC-modern-movement.md §90.9, step H1; 26.3's `LivingEntity.onEquipItem`) from 1.7.10's
 * per-tick look at the equipment on the server (`onUpdate`, which compares each slot against the last and sends the
 * change): each slot as it's read, against what the last look saw. Not on an entity's first look (26.3's first tick).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

	@Shadow
	@Final
	private ItemStack[] previousEquipment;

	@Unique
	private boolean mcfly$equipmentKnown;

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getEquipmentInSlot(I)Lnet/minecraft/item/ItemStack;"))
	private ItemStack mcfly$equipmentLook(EntityLivingBase self, int slot, Operation<ItemStack> original) {
		ItemStack now = original.call(self, slot);
		if (mcfly$equipmentKnown && slot >= 0 && slot < previousEquipment.length) {
			GameEventHooks.equipmentChanged(self, slot, previousEquipment[slot], now);
		}
		return now;
	}

	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;onLivingUpdate()V"))
	private void mcfly$equipmentLooked(CallbackInfo ci) {
		World world = ((EntityLivingBase) (Object) this).worldObj;
		if (!world.isRemote) {
			mcfly$equipmentKnown = true;
		}
	}
}
