package io.github.asgerjon.mcfly.mixins.early.wood;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's oak gate (and any gate keeping 1.7.10's own use) sounds by its new state (SPEC-modern-movement.md §91.6,
 * step D): level event 1003, a coin between `random.door_open` and `_close`, becomes 26.3's `block.fence_gate.open` or
 * `close` (EFR's `ClientEventHandler` swapped it at random).
 */
@Mixin(BlockFenceGate.class)
public abstract class MixinBlockFenceGateSound {

	@WrapOperation(method = {"onBlockActivated", "onNeighborBlockChange"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
	private void mcfly$byState(World world, EntityPlayer player, int id, int x, int y, int z, int data, Operation<Void> original) {
		if (id != 1003) {
			original.call(world, player, id, x, y, z, data);
			return;
		}
		if (!world.isRemote) {
			boolean open = (world.getBlockMetadata(x, y, z) & 4) != 0;
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.fence_gate." + (open ? "open" : "close"), 1.0F,
				world.rand.nextFloat() * 0.1F + 0.9F);
		}
	}
}
