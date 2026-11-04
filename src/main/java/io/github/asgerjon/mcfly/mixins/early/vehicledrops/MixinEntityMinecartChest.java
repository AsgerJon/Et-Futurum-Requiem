package io.github.asgerjon.mcfly.mixins.early.vehicledrops;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The chest minecart's separate chest, skipped: the cart drops as one item (MixinEntityMinecart, spec §59). */
@Mixin(EntityMinecartChest.class)
public abstract class MixinEntityMinecartChest {

	@WrapOperation(method = "killMinecart", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityMinecartChest;func_145778_a(Lnet/minecraft/item/Item;IF)Lnet/minecraft/entity/item/EntityItem;"))
	private EntityItem etfu$noSeparateBlock(EntityMinecartChest cart, Item block, int count, float offset, Operation<EntityItem> original) {
		return McFlyConfig.dropVehiclesTogether ? null : original.call(cart, block, count, offset);
	}
}
