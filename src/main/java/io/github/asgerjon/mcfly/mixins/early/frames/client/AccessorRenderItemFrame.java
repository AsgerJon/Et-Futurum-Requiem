package io.github.asgerjon.mcfly.mixins.early.frames.client;

import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The frame's back icon, for the glow frame's own (SPEC-modern-movement.md §67.4). */
@Mixin(RenderItemFrame.class)
public interface AccessorRenderItemFrame {

	@Accessor("field_94147_f")
	void etfu$setBack(IIcon back);
}
