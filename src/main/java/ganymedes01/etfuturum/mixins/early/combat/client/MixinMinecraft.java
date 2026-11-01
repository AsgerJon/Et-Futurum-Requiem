package ganymedes01.etfuturum.mixins.early.combat.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.combat.ICombatPlayer;
import ganymedes01.etfuturum.network.PunchMessage;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Left click in modern's order (spec §46.4): the attack, block click or miss first, then the swing and the punch
 * notice to the server. A miss restarts the charge.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

	@Shadow
	public EntityClientPlayerMP thePlayer;

	@Shadow
	public MovingObjectPosition objectMouseOver;

	@Shadow
	public WorldClient theWorld;

	@Unique
	private boolean etfu$clicked;

	@WrapWithCondition(method = "func_147116_af", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;swingItem()V"))
	private boolean etfu$swingAfterwards(EntityClientPlayerMP player) {
		etfu$clicked = true;
		return false;
	}

	@Inject(method = "func_147116_af", at = @At("TAIL"))
	private void etfu$swingAndPunch(CallbackInfo ci) {
		if (!etfu$clicked) {
			return;
		}
		etfu$clicked = false;
		if (etfu$missed()) {
			((ICombatPlayer) thePlayer).etfu$resetAttackStrength();
		}
		thePlayer.swingItem();
		EtFuturum.networkWrapper.sendToServer(new PunchMessage());
	}

	/** Nothing hit, or a block that is air: modern's miss. */
	@Unique
	private boolean etfu$missed() {
		MovingObjectPosition hit = objectMouseOver;
		if (hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
			return true;
		}
		return hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && theWorld.getBlock(hit.blockX, hit.blockY, hit.blockZ).getMaterial() == Material.air;
	}
}
