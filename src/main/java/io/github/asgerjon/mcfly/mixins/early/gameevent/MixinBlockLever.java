package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockLever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A lever pulled posts `block_activate` or `_deactivate` with its click (SPEC-modern-movement.md §90.9, step H1; 26.3's `LeverBlock.pull`). */
@Mixin(BlockLever.class)
public abstract class MixinBlockLever {

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"))
	private void mcfly$pulled(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		boolean on = (world.getBlockMetadata(x, y, z) & 8) != 0;
		GameEventHooks.atBlock(world, on ? "block_activate" : "block_deactivate", x, y, z, player);
	}
}
