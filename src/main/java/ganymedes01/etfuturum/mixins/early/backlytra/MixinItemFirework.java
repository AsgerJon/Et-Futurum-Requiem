package ganymedes01.etfuturum.mixins.early.backlytra;

import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFirework;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A rocket used on a block while gliding boosts instead of launching from the block (SPEC-modern-movement.md §20):
 * modern's firework item passes its block use while fall-flying, so the click goes on to the air use. 1.7.10's item
 * launched a plain rocket from the block; declining here lets the client's right click fall through to the air use,
 * where EFR's boost handler takes it, and the server's block use does nothing.
 */
@Mixin(ItemFirework.class)
public class MixinItemFirework {

	@Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
	private void etfu$noRocketFromBlocksWhileGliding(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		if (FallFlying.isFallFlying(player)) {
			cir.setReturnValue(false);
		}
	}
}
