package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.block.BlockCauldron;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A cauldron's game events (SPEC-modern-movement.md §90.9, step H1; 26.3's `CauldronInteractions` and
 * `LayeredCauldronBlock`): a lowered level posts `block_change` (`lowerFillLevel`), so does rain's filling
 * (`handlePrecipitation`); a bucket emptied into it `fluid_place`, by the player; a bottle filled from it `fluid_pickup`
 * after the lowering, by no one. McFly's modern bucket and creative bottle post in its own mixin.
 */
@Mixin(BlockCauldron.class)
public abstract class MixinBlockCauldron {

	@Inject(method = "func_150024_a", at = @At("HEAD"))
	private void mcfly$level(World world, int x, int y, int z, int level, CallbackInfo ci) {
		if (level < world.getBlockMetadata(x, y, z)) {
			GameEvents.postAtBlock(world, "block_change", x, y, z, null, Blocks.cauldron, Math.max(0, level));
		}
	}

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockCauldron;func_150024_a(Lnet/minecraft/world/World;IIII)V", ordinal = 0, shift = At.Shift.AFTER))
	private void mcfly$bucketEmptied(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, "fluid_place", x, y, z, player);
	}

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockCauldron;func_150024_a(Lnet/minecraft/world/World;IIII)V", ordinal = 1, shift = At.Shift.AFTER))
	private void mcfly$bottleFilled(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, "fluid_pickup", x, y, z, null);
	}

	@Inject(method = "fillWithRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z", shift = At.Shift.AFTER))
	private void mcfly$rained(World world, int x, int y, int z, CallbackInfo ci) {
		GameEvents.postAtBlock(world, "block_change", x, y, z, null, Blocks.cauldron, world.getBlockMetadata(x, y, z));
	}
}
