package io.github.asgerjon.mcfly.mixins.early.wood;

import io.github.asgerjon.mcfly.wood.SignPlacing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSign;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's oak sign placed as McFly's are (SPEC-modern-movement.md 91.8; 26.3's `StandingAndWallBlockItem`). */
@Mixin(ItemSign.class)
public abstract class MixinItemSign {

	@Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
	private void mcfly$placeAs26_3(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
								   CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(SignPlacing.place(stack, player, world, x, y, z, side, Blocks.standing_sign, Blocks.wall_sign));
	}
}
