package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

/**
 * A button's press and release post `block_activate` and `_deactivate` with its clicks (SPEC-modern-movement.md §90.9,
 * step H1; 26.3's `ButtonBlock.press` and `checkPressed`): pressed by the player, let go by its tick, pressed by an
 * arrow (that one) or let go when the arrow's gone.
 */
@Mixin(BlockButton.class)
public abstract class MixinBlockButton {

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"))
	private void mcfly$pressed(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, "block_activate", x, y, z, player);
	}

	@Inject(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"))
	private void mcfly$letGo(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_deactivate", x, y, z, null);
	}

	@Inject(method = "func_150046_n", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V", ordinal = 0))
	private void mcfly$arrowPressed(World world, int x, int y, int z, CallbackInfo ci) {
		Block block = (Block) (Object) this;
		List<?> arrows = world.getEntitiesWithinAABB(EntityArrow.class, AxisAlignedBB.getBoundingBox(x + block.getBlockBoundsMinX(), y + block.getBlockBoundsMinY(),
			z + block.getBlockBoundsMinZ(), x + block.getBlockBoundsMaxX(), y + block.getBlockBoundsMaxY(), z + block.getBlockBoundsMaxZ()));
		GameEventHooks.atBlock(world, "block_activate", x, y, z, arrows.isEmpty() ? null : (Entity) arrows.get(0));
	}

	@Inject(method = "func_150046_n", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V", ordinal = 1))
	private void mcfly$arrowGone(World world, int x, int y, int z, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_deactivate", x, y, z, null);
	}
}
