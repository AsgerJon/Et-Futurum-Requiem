package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.PlayerPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

	@Shadow
	private Minecraft mc;

	/**
	 * The first-person hand is lit by the light at the eyes, like modern. 1.7.10 reads the light at posY, which on the
	 * client is always 1.62 above the feet: while crawling under a 1-block ceiling that's inside the block above, so the
	 * hand and the held item turned black.
	 */
	@ModifyExpressionValue(method = "renderItemInFirstPerson", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;posY:D", ordinal = 0))
	private double etfu$lightHandAtEyes(double posY) {
		EntityClientPlayerMP player = mc.thePlayer;
		if (player instanceof IPosePlayer) {
			PlayerPose pose = ((IPosePlayer) player).etfu$getAppliedPose();
			if (pose != null) {
				return player.boundingBox.minY + pose.eyeHeight;
			}
		}
		return posY;
	}
}
