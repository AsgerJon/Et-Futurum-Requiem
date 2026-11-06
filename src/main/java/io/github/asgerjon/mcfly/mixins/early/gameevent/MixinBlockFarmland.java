package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.block.BlockFarmland;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/** Farmland turned back to dirt posts `block_change` about the dirt, by what trod it (none when it dried) (SPEC-modern-movement.md §90.9, step H1; 26.3's `FarmlandBlock.turnToBaseBlock`). */
@Mixin(BlockFarmland.class)
public abstract class MixinBlockFarmland {

	@Inject(method = "onFallenUpon", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;)Z", shift = At.Shift.AFTER))
	private void mcfly$trodden(World world, int x, int y, int z, Entity entity, float fallDistance, CallbackInfo ci) {
		GameEvents.postAtBlock(world, "block_change", x, y, z, entity, Blocks.dirt, 0);
	}

	@Inject(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;)Z", shift = At.Shift.AFTER))
	private void mcfly$dried(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		GameEvents.postAtBlock(world, "block_change", x, y, z, null, Blocks.dirt, 0);
	}
}
