package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.block.BlockJukebox;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A jukebox's record coming out posts `block_change` at it (SPEC-modern-movement.md §90.9, step H1; 26.3's `JukeboxBlockEntity.notifyItemChangedInJukebox`). */
@Mixin(BlockJukebox.class)
public abstract class MixinBlockJukebox {

	@Inject(method = "func_149925_e", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z", shift = At.Shift.AFTER))
	private void mcfly$ejected(World world, int x, int y, int z, CallbackInfo ci) {
		GameEventHooks.atBlock(world, "block_change", x, y, z, null);
	}
}
