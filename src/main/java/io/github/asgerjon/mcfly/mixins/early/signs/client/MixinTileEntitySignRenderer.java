package io.github.asgerjon.mcfly.mixins.early.signs.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.client.renderer.tileentity.SignTextDrawing;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A sign's lines drawn with their colour, glow and outline (SPEC-modern-movement.md §67.2; `SignTextDrawing`). */
@Mixin(TileEntitySignRenderer.class)
public abstract class MixinTileEntitySignRenderer {

	@WrapOperation(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySign;DDDF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
	private int etfu$drawLine(FontRenderer font, String line, int x, int y, int color, Operation<Integer> original, @Local(argsOnly = true) TileEntitySign sign) {
		SignTextDrawing.draw(font, line, x, y, color, sign);
		return 0;
	}
}
