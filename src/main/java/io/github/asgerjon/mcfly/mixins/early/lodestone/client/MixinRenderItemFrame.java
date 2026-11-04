package io.github.asgerjon.mcfly.mixins.early.lodestone.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.client.LodestoneNeedle;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.entity.item.EntityItemFrame;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A lodestone compass in an item frame points by the frame (SPEC-modern-movement.md §57.2). The frame draws its item as
 * a made-up dropped item, so the frame is named for the time it draws it. Its own compass code only touches 1.7.10's
 * animated compass sprite, which a lodestone compass doesn't use. Wrapped whole, so the frame stops being named however
 * the drawing ends: the shield's renderer cancels it (combat.client.MixinRenderItemFrame), which skipped a RETURN hook
 * and left a dead frame named for every compass shown afterwards.
 */
@Mixin(RenderItemFrame.class)
public class MixinRenderItemFrame {

	@WrapMethod(method = "func_82402_b")
	private void etfu$shownByTheFrame(EntityItemFrame frame, Operation<Void> original) {
		boolean showing = LodestoneNeedle.startShowing(frame);
		try {
			original.call(frame);
		} finally {
			if (showing) {
				LodestoneNeedle.stopShowing(frame);
			}
		}
	}
}
