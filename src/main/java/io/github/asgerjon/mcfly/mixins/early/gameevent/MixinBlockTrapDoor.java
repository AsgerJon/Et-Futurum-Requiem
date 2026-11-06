package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A trapdoor opened or closed posts `block_open` or `_close` with its sound (SPEC-modern-movement.md §90.9, step H1; 26.3's `TrapDoorBlock.playSound`). */
@Mixin(BlockTrapDoor.class)
public abstract class MixinBlockTrapDoor {

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
	private void mcfly$used(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, (world.getBlockMetadata(x, y, z) & 4) != 0 ? "block_open" : "block_close", x, y, z, player);
	}

	@Inject(method = "func_150120_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
	private void mcfly$byRedstone(World world, int x, int y, int z, boolean open, CallbackInfo ci) {
		GameEventHooks.atBlock(world, open ? "block_open" : "block_close", x, y, z, null);
	}
}
