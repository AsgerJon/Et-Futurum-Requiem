package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A fence gate opened or closed posts `block_open` or `_close` with its sound (SPEC-modern-movement.md §90.9, step H1; 26.3's `FenceGateBlock`): by hand, by redstone. */
@Mixin(BlockFenceGate.class)
public abstract class MixinBlockFenceGate {

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
	private void mcfly$used(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, BlockFenceGate.isFenceGateOpen(world.getBlockMetadata(x, y, z)) ? "block_open" : "block_close", x, y, z, player);
	}

	@Inject(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V", ordinal = 0))
	private void mcfly$openedByRedstone(World world, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_open", x, y, z, null);
	}

	@Inject(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V", ordinal = 1))
	private void mcfly$closedByRedstone(World world, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_close", x, y, z, null);
	}
}
