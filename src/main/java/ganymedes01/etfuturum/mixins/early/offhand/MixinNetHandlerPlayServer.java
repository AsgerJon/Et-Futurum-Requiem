package ganymedes01.etfuturum.mixins.early.offhand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C0APacketAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creative mode writes inventory slots through its own packet, which the server accepts for the 36 + 9 slots of the
 * player's container (SPEC-modern-movement.md §36.1); the off-hand slot is one more, so the creative survival tab can
 * put an item into it. A swing request of type 2 is the off-hand's (§36.4; vanilla only acts on type 1).
 */
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {
	@Shadow
	public EntityPlayerMP playerEntity;

	@Inject(method = "processAnimation", at = @At("HEAD"))
	private void etfu$offhandSwingRequest(C0APacketAnimation packet, CallbackInfo ci) {
		if (Offhand.isEnabled() && packet.func_149421_d() == Offhand.OFFHAND_SWING_REQUEST) {
			Offhand.swing(playerEntity, Hand.OFF);
		}
	}

	@ModifyExpressionValue(method = "processCreativeInventoryAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getHotbarSize()I"))
	private int etfu$oneMoreCreativeSlot(int hotbarSize) {
		return Offhand.isEnabled() ? hotbarSize + 1 : hotbarSize;
	}
}
