package ganymedes01.etfuturum.client.renderer.entity;

import io.github.asgerjon.mcfly.mixins.early.frames.client.AccessorRenderItemFrame;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;

/**
 * The glow item frame (SPEC-modern-movement.md §67.4): 1.7.10's frame with 26.3's glow frame back
 * (`blocks/glow_item_frame`); its item at full light (`frames.client.MixinRenderItemFrame`).
 */
public class GlowItemFrameRenderer extends RenderItemFrame {

	@Override
	public void updateIcons(IIconRegister register) {
		super.updateIcons(register);
		((AccessorRenderItemFrame) this).etfu$setBack(register.registerIcon("glow_item_frame"));
	}
}
