package io.github.asgerjon.mcfly.mixins.early.frames;

import ganymedes01.etfuturum.entities.ModernFrames;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHangingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An item frame on the top or bottom of a block (SPEC-modern-movement.md §67.3; 26.3's `ItemFrameItem.mayPlace`: any
 * face inside the build height); 1.7.10's item does nothing on them. Paintings stay on walls.
 */
@Mixin(ItemHangingEntity.class)
public abstract class MixinItemHangingEntity {

	@Shadow
	@Final
	private Class<?> hangingEntityClass;

	@Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
	private void etfu$onFloorsAndCeilings(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
		CallbackInfoReturnable<Boolean> cir) {
		if ((side != 0 && side != 1) || hangingEntityClass != EntityItemFrame.class) {
			return;
		}
		cir.setReturnValue(ModernFrames.place(stack, player, world, x, y, z, side, false));
	}
}
