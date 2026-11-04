package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.FilledResult;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGlassBottle;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bottle filled from water in creative stays, the water bottle added once (26.3; spec §63.1). 1.7.10 used it up. */
@Mixin(ItemGlassBottle.class)
public abstract class MixinItemGlassBottle extends Item {

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void etfu$creativeFilledResult(ItemStack bottle, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		if (!McFlyConfig.modernCreativeFilling || !player.capabilities.isCreativeMode) {
			return;
		}
		MovingObjectPosition target = getMovingObjectPositionFromPlayer(world, player, true);
		if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& world.canMineBlock(player, target.blockX, target.blockY, target.blockZ)
				&& player.canPlayerEdit(target.blockX, target.blockY, target.blockZ, target.sideHit, bottle)
				&& world.getBlock(target.blockX, target.blockY, target.blockZ).getMaterial() == Material.water) {
			FilledResult.fill(player, bottle, new ItemStack(Items.potionitem, 1, 0));
		}
		cir.setReturnValue(bottle);
	}
}
