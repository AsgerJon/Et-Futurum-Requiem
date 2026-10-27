package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The client side of the sneak rule (SPEC-modern-movement.md §36.2): either hand's item keeps a sneaking click off the block. */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

	@ModifyExpressionValue(method = "onPlayerRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;getHeldItem()Lnet/minecraft/item/ItemStack;"))
	private ItemStack etfu$eitherHandForSneaking(ItemStack held, @Local(argsOnly = true) EntityPlayer player) {
		return held != null || !Offhand.isEnabled() ? held : Offhand.getOffhand(player);
	}
}
