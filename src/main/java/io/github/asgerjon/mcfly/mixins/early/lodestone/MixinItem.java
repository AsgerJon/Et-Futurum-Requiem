package io.github.asgerjon.mcfly.mixins.early.lodestone;

import ganymedes01.etfuturum.items.LodestoneCompass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10's compass is a plain Item, so its lodestone behaviour (SPEC-modern-movement.md §57.2) goes into Item's own
 * methods, for the compass only: locking onto a lodestone, losing it, the name.
 */
@Mixin(Item.class)
public class MixinItem {

	/** Used on a lodestone, sneaking or not (any item's use on a block), as 26.3's CompassItem.useOn. */
	@Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
	private void etfu$lockOntoTheLodestone(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
										   float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == Items.compass && LodestoneCompass.isLodestone(world, x, y, z)) {
			cir.setReturnValue(LodestoneCompass.lock(stack, player, world, x, y, z));
		}
	}

	/** Each tick in a player's inventory (EFR's off-hand included), on the server, as 26.3's inventoryTick. */
	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void etfu$checkTheLodestone(ItemStack stack, World world, Entity holder, int slot, boolean held, CallbackInfo ci) {
		if ((Object) this == Items.compass && !world.isRemote) {
			LodestoneCompass.tick(stack, world);
		}
	}

	/** "Lodestone Compass"; a name given at an anvil still comes first, as 1.7.10's ItemStack.getDisplayName has it. */
	@Inject(method = "getItemStackDisplayName", at = @At("HEAD"), cancellable = true)
	private void etfu$lodestoneCompassName(ItemStack stack, CallbackInfoReturnable<String> cir) {
		if ((Object) this == Items.compass && LodestoneCompass.isLodestoneCompass(stack)) {
			cir.setReturnValue(StatCollector.translateToLocal("item.etfuturum.lodestone_compass.name").trim());
		}
	}
}
