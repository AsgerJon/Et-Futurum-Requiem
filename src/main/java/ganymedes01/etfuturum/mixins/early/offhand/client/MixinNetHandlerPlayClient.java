package ganymedes01.etfuturum.mixins.early.offhand.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S0BPacketAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An off-hand swing of another entity (SPEC-modern-movement.md §36.4): the animation type 6, which 1.7.10 otherwise
 * ignores, plays the swing on that entity's other arm. Modern's 3 can't be used: 1.7.10's server sends 3 to the
 * player and their trackers whenever eating starts. A slot packet for the off-hand pops the item in the HUD like one
 * for the hotbar (§36.1).
 */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {
	@Shadow
	private WorldClient clientWorldController;

	@Inject(method = "handleAnimation", at = @At("HEAD"), cancellable = true)
	private void etfu$offhandSwing(S0BPacketAnimation packet, CallbackInfo ci) {
		if (packet.func_148977_d() != Offhand.OFFHAND_SWING_ANIMATION) {
			return;
		}
		Entity entity = clientWorldController.getEntityByID(packet.func_148978_c());
		if (entity instanceof EntityLivingBase) {
			Offhand.swing((EntityLivingBase) entity, Hand.OFF);
		}
		ci.cancel();
	}

	/** The slot packet's hotbar range, 36 to 44, takes in container slot 45, as modern's isHotbarSlot does. */
	@ModifyExpressionValue(method = "handleSetSlot", at = @At(value = "CONSTANT", args = "intValue=45"))
	private int etfu$offhandSlotPopsToo(int hotbarEnd) {
		return Offhand.isEnabled() ? hotbarEnd + 1 : hotbarEnd;
	}
}
