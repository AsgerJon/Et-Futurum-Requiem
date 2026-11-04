package io.github.asgerjon.mcfly.mixins.early.modernrarity.client;

import ganymedes01.etfuturum.client.ModernRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The held item's name above the hotbar in its rarity's colour, italic when renamed (26.3's; SPEC-modern-movement.md §61.3). */
@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForge extends GuiIngame {

	private MixinGuiIngameForge(Minecraft mc) {
		super(mc);
	}

	@ModifyVariable(method = "renderToolHightlight", at = @At("STORE"), ordinal = 0, remap = false)
	private String etfu$rarityColour(String name) {
		return highlightingItemStack == null ? name : ModernRarity.heldName(highlightingItemStack);
	}
}
