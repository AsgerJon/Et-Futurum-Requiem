package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A plant put in a pot posts `block_change` at the pot, by the player (SPEC-modern-movement.md §90.9, step H1; 26.3's `FlowerPotBlock.useItemOn`). McFly's plants go the same way. */
@Mixin(BlockFlowerPot.class)
public abstract class MixinBlockFlowerPotEvent {

	@Inject(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityFlowerPot;markDirty()V", shift = At.Shift.AFTER))
	private void mcfly$planted(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		GameEventHooks.atBlock(world, "block_change", x, y, z, player);
	}
}
