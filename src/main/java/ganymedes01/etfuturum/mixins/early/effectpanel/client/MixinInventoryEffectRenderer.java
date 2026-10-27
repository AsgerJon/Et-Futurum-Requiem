package ganymedes01.etfuturum.mixins.early.effectpanel.client;

import net.minecraft.client.renderer.InventoryEffectRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The inventory's potion effect panel relies on the alpha test being on: the effect sprites in inventory.png have
 * colours hidden under fully transparent pixels (the icon corners, the right end of the background), and with the
 * test off they are drawn as a black box and coloured corner marks on the first effect, until the first text turns the
 * test back on. Vanilla never sets it here, so whatever rendered before can leave it off. This puts it on.
 */
@Mixin(InventoryEffectRenderer.class)
public class MixinInventoryEffectRenderer {

	@Inject(method = "func_147044_g", at = @At("HEAD"))
	private void etfu$alphaTestForEffectPanel(CallbackInfo ci) {
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
	}
}
