package ganymedes01.etfuturum.mixins.early.commandblock;

import net.minecraft.block.BlockCommandBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A command block opens only for a player in creative, as in modern (canUseGameMasterBlocks); 1.7.10 opened it in
 * any mode and only refused to save. Outside creative the click falls through to the held item, as in modern.
 * Saving still needs operator rights, which vanilla checks on the server.
 */
@Mixin(BlockCommandBlock.class)
public abstract class MixinBlockCommandBlock {

	@Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
	private void etfu$creativeOnly(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		if (!player.capabilities.isCreativeMode) {
			cir.setReturnValue(false);
		}
	}
}
