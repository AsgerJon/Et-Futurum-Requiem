package io.github.asgerjon.mcfly.mixins.early.vehicledrops;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartFurnace;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The furnace minecart's separate furnace, skipped: the cart drops as one item (MixinEntityMinecart, spec §59). */
@Mixin(EntityMinecartFurnace.class)
public abstract class MixinEntityMinecartFurnace {

	@WrapOperation(method = "killMinecart", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityMinecartFurnace;entityDropItem(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/item/EntityItem;"))
	private EntityItem etfu$noSeparateFurnace(EntityMinecartFurnace cart, ItemStack furnace, float offset, Operation<EntityItem> original) {
		return McFlyConfig.dropVehiclesTogether ? null : original.call(cart, furnace, offset);
	}
}
