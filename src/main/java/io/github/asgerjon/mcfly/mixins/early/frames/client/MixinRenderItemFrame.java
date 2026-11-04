package io.github.asgerjon.mcfly.mixins.early.frames.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.entities.EntityGlowItemFrame;
import ganymedes01.etfuturum.entities.ModernFrames;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item frames drawn as 26.3 (SPEC-modern-movement.md §67.3; `ItemFrameRenderer`). A floor or ceiling frame is drawn as
 * 1.7.10 draws a north wall's, in a GL frame turned up (or down) about X and half round about its face: its back on
 * its block, its item's top to the north on a floor, to the south on a ceiling. The item turns in eighths about its
 * middle (1.7.10's quarter turns' offsets are this turn's: `(-0.16 sin a, 0.16 cos a - 0.16)`); a map in quarters
 * (26.3's `rotation % 4`).
 */
@Mixin(RenderItemFrame.class)
public abstract class MixinRenderItemFrame extends Render {

	@Shadow
	private void func_147915_b(EntityItemFrame frame) {
	}

	@Shadow
	private void renderFrameItemAsBlock(EntityItemFrame frame) {
	}

	@Shadow
	private void func_82402_b(EntityItemFrame frame) {
	}

	@Shadow
	protected abstract void func_147914_a(EntityItemFrame frame, double x, double y, double z);

	@Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItemFrame;DDDFF)V", at = @At("HEAD"), cancellable = true)
	private void etfu$floorOrCeiling(EntityItemFrame frame, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
		int direction = frame.hangingDirection;
		if (!ModernFrames.isFloorOrCeiling(direction)) {
			return;
		}
		ci.cancel();
		GL11.glPushMatrix();
		int[] at = ModernFrames.frameBlock(frame);
		GL11.glTranslated(at[0] - (frame.posX - x - 0.5D), at[1] - (frame.posY - y - 0.5D), at[2] - (frame.posZ - z - 0.5D));
		GL11.glRotatef(direction == ModernFrames.FLOOR ? 90.0F : -90.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		float frameYaw = frame.rotationYaw;
		frame.hangingDirection = 2;
		frame.rotationYaw = 0.0F;
		ModernFrames.setDrawnTurned(frame, direction);
		try {
			if (isMap(frame)) {
				func_147915_b(frame);
			} else {
				renderFrameItemAsBlock(frame);
			}
			func_82402_b(frame);
		} finally {
			frame.hangingDirection = direction;
			frame.rotationYaw = frameYaw;
			ModernFrames.setDrawnTurned(null, 0);
			GL11.glPopMatrix();
		}
		func_147914_a(frame, x, y - 0.25D, z);
	}

	/** A glow frame's item (and map) at full light (26.3's `getLightCoords` for a glow frame); the frame itself as lit. */
	@WrapMethod(method = "func_82402_b")
	private void etfu$glowFrameLight(EntityItemFrame frame, Operation<Void> original) {
		if (!(frame instanceof EntityGlowItemFrame)) {
			original.call(frame);
			return;
		}
		float lastX = OpenGlHelper.lastBrightnessX, lastY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		try {
			original.call(frame);
		} finally {
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
		}
	}

	/** 1.7.10's own turn in the item's drawing: none for an item (this mixin turns it), quarters for a map. */
	@WrapOperation(method = "func_82402_b", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItemFrame;getRotation()I"))
	private int etfu$vanillaTurn(EntityItemFrame frame, Operation<Integer> original) {
		int turn = original.call(frame);
		return isMap(frame) ? turn % 4 : 0;
	}

	/** Before 1.7.10's turn about the face (its second `glRotatef`): an item's eighth turns about its middle. */
	@Inject(method = "func_82402_b", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 1, remap = false))
	private void etfu$eighthTurns(EntityItemFrame frame, CallbackInfo ci) {
		if (frame.getDisplayedItem() == null || isMap(frame)) {
			return;
		}
		float degrees = frame.getRotation() * 45.0F;
		double radians = Math.toRadians(degrees);
		GL11.glRotatef(-degrees, 0.0F, 0.0F, 1.0F);
		GL11.glTranslatef((float) (-0.16D * Math.sin(radians)), (float) (0.16D * Math.cos(radians) - 0.16D), 0.0F);
	}

	private static boolean isMap(EntityItemFrame frame) {
		return frame.getDisplayedItem() != null && frame.getDisplayedItem().getItem() == Items.filled_map;
	}
}
