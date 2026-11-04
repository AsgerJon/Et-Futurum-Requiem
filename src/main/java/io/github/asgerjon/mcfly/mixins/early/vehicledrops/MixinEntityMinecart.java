package io.github.asgerjon.mcfly.mixins.early.vehicledrops;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartFurnace;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A minecart with a chest, a furnace, a hopper or TNT drops as one (SPEC-modern-movement.md §59, 1.19+): the plain
 * minecart 1.7.10 drops for every cart becomes the cart's own item, named as before. The subclasses' mixins skip the
 * separate block; a TNT cart that goes off drops nothing (MixinEntityMinecartTNT). Spawner and command block carts keep
 * the plain minecart, as in 26.3.
 */
@Mixin(EntityMinecart.class)
public abstract class MixinEntityMinecart {

	@ModifyArg(method = "killMinecart", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/Item;I)V"), index = 0)
	private Item etfu$dropAsOne(Item minecart) {
		if (!McFlyConfig.dropVehiclesTogether) {
			return minecart;
		}
		Object cart = this;
		if (cart instanceof EntityMinecartChest) {
			return Items.chest_minecart;
		}
		if (cart instanceof EntityMinecartFurnace) {
			return Items.furnace_minecart;
		}
		if (cart instanceof EntityMinecartHopper) {
			return Items.hopper_minecart;
		}
		if (cart instanceof EntityMinecartTNT) {
			return Items.tnt_minecart;
		}
		return minecart;
	}
}
