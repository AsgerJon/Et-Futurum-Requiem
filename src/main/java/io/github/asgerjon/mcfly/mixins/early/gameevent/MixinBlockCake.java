package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockCake;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A slice of cake eaten posts `eat` at the cake, by the player; the last one `block_destroy` too (SPEC-modern-movement.md §90.9, step H1; 26.3's `CakeBlock.eat`). */
@Mixin(BlockCake.class)
public abstract class MixinBlockCake {

	@Inject(method = "func_150036_b", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FoodStats;addStats(IF)V", shift = At.Shift.AFTER))
	private void mcfly$ate(World world, int x, int y, int z, EntityPlayer player, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "eat", x, y, z, player);
	}

	@Inject(method = "func_150036_b", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(III)Z", shift = At.Shift.AFTER))
	private void mcfly$eatenUp(World world, int x, int y, int z, EntityPlayer player, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_destroy", x, y, z, player);
	}
}
