package io.github.asgerjon.mcfly.mixins.early.powdersnow.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Frozen hearts (SPEC-modern-movement.md §62.5; 26.3 {@code Hud.HeartType.FROZEN}): fully frozen, without Poison or
 * Wither (they come first), the health hearts are 26.3's {@code frozen_*} sprites. Forge draws the plain ones at u 52
 * (full), 61 (half), 70 and 79 (their blinking forms), v 0 or 45 (hardcore); the containers and absorption stay.
 */
@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForge {

	@Unique
	private static final ResourceLocation[] etfu$FROZEN = new ResourceLocation[8];

	static {
		String[] names = {"full", "half", "full_blinking", "half_blinking"};
		for (int i = 0; i < 4; i++) {
			etfu$FROZEN[i] = new ResourceLocation("textures/gui/sprites/hud/heart/frozen_" + names[i] + ".png");
			etfu$FROZEN[i + 4] = new ResourceLocation("textures/gui/sprites/hud/heart/frozen_hardcore_" + names[i] + ".png");
		}
	}

	@WrapOperation(method = "renderHealth", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;drawTexturedModalRect(IIIIII)V", remap = true))
	private void etfu$frozenHeart(GuiIngameForge gui, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
		int kind = u == 52 ? 0 : u == 61 ? 1 : u == 70 ? 2 : u == 79 ? 3 : -1;
		Minecraft mc = Minecraft.getMinecraft();
		if (kind < 0 || v != 0 && v != 45 || !Freezing.isFullyFrozen(mc.thePlayer)
				|| mc.thePlayer.isPotionActive(Potion.poison) || mc.thePlayer.isPotionActive(Potion.wither)) {
			original.call(gui, x, y, u, v, width, height);
			return;
		}
		mc.getTextureManager().bindTexture(etfu$FROZEN[kind + (v == 45 ? 4 : 0)]);
		Gui.func_146110_a(x, y, 0.0F, 0.0F, 9, 9, 9.0F, 9.0F);
		mc.getTextureManager().bindTexture(Gui.icons);
	}
}
