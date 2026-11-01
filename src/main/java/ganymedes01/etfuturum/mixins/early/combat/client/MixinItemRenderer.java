package ganymedes01.etfuturum.mixins.early.combat.client;

import ganymedes01.etfuturum.client.renderer.item.ItemShieldRenderer;
import ganymedes01.etfuturum.combat.AttackCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;

/** The held item dips after a swing and rises as the weapon charges (spec §46.4): its target height is the swap charge cubed. */
@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

	@Shadow
	private Minecraft mc;

	@Shadow
	private float equippedProgress;

	@Shadow
	private float prevEquippedProgress;

	/**
	 * Where 1.7.10 starts drawing the held item: the hand's space (both mirrors and the sway applied, nothing of the
	 * item's own placement yet) and its raise height, kept for items that place themselves as modern does (the
	 * shield, spec §46.9). The off-hand's pass swaps in its own progress before calling this method.
	 */
	@Inject(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V", ordinal = 3, remap = false))
	private void etfu$keepHandSpace(float partialTicks, CallbackInfo ci) {
		ItemShieldRenderer.FIRST_PERSON_HAND.rewind();
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, ItemShieldRenderer.FIRST_PERSON_HAND);
		ItemShieldRenderer.firstPersonRaise = prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks;
	}

	@ModifyVariable(method = "updateEquippedItem", at = @At("STORE"), ordinal = 1)
	private float etfu$riseWithCharge(float target) {
		float swap = AttackCooldown.swapCharge(mc.thePlayer, 1.0F);
		return target * swap * swap * swap;
	}
}
