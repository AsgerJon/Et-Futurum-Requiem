package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockTNT;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TNT lit posts `prime_fuse` at it, by who lit it (SPEC-modern-movement.md §90.9, step H1; 26.3's `TntBlock.prime`). */
@Mixin(BlockTNT.class)
public abstract class MixinBlockTNT {

	@Inject(method = "func_150114_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z"))
	private void mcfly$primed(World world, int x, int y, int z, int meta, EntityLivingBase igniter, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "prime_fuse", x, y, z, igniter);
	}
}
