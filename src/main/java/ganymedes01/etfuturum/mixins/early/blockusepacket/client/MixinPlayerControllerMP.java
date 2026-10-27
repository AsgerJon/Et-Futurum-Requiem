package ganymedes01.etfuturum.mixins.early.blockusepacket.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client sends every right-click on a block to the server before trying it itself (SPEC-modern-movement.md §35,
 * modern since 1.13). 1.7.10 first asked whether the held block could go there and sent nothing when it couldn't, so
 * a click could never reach the server in the tick that freed the spot. The server decides either way; the local
 * attempt still runs and fails as before, so there is no swing for a click that places nothing.
 */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

	@ModifyExpressionValue(method = "onPlayerRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemBlock;func_150936_a(Lnet/minecraft/world/World;IIIILnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)Z"))
	private boolean etfu$sendTheClickAnyway(boolean clientThinksItFits) {
		return true;
	}
}
