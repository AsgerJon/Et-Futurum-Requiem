package io.github.asgerjon.mcfly.mixins.early.wood;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.base.McFlySetType;
import io.github.asgerjon.mcfly.wood.Doors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.7.10's doors by 26.3's rules (SPEC-modern-movement.md §91.6, step D): vanilla's wooden and iron doors, and any
 * door that keeps 1.7.10's own use and update.
 * - **Sound:** level event 1003 (a coin between `random.door_open` and `_close`) becomes the set type's sound by the new
 *   state (EFR's `ClientEventHandler` swapped it at random).
 * - **Power:** every neighbour's update asks, not only a redstone part's, and only a change of signal acts, by the upper
 *   half's powered bit ({@link Doors#redstone}).
 */
@Mixin(BlockDoor.class)
public abstract class MixinBlockDoor {

	@WrapOperation(method = {"onBlockActivated", "func_150014_a"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/World;playAuxSFXAtEntity(Lnet/minecraft/entity/player/EntityPlayer;IIIII)V"))
	private void mcfly$setTypeSound(World world, EntityPlayer player, int id, int x, int y, int z, int data, Operation<Void> original) {
		if (id != 1003) {
			original.call(world, player, id, x, y, z, data);
			return;
		}
		if (!world.isRemote) {
			int lowerY = Doors.lowerY(world, x, y, z);
			McFlySetType.of((Block) (Object) this).playDoor(world, x, lowerY, z, Doors.isOpen(world, x, lowerY, z));
		}
	}

	@WrapOperation(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canProvidePower()Z"))
	private boolean mcfly$everyUpdate(Block neighbour, Operation<Boolean> original) {
		return true;
	}

	@Redirect(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockDoor;func_150014_a(Lnet/minecraft/world/World;IIIZ)V"))
	private void mcfly$byPower(BlockDoor door, World world, int x, int y, int z, boolean signal) {
		Doors.redstone(door, world, x, y, z);
	}
}
