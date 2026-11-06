package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockTripWire;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tripwire cut with shears (disarmed) posts `shear` at it, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `TripWireBlock.playerWillDestroy`). */
@Mixin(BlockTripWire.class)
public abstract class MixinBlockTripWire {

	@Inject(method = "onBlockHarvested", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z", shift = At.Shift.AFTER))
	private void mcfly$disarmed(World world, int x, int y, int z, int meta, EntityPlayer player, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "shear", x, y, z, player);
	}
}
