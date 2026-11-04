package io.github.asgerjon.mcfly.mixins.early.modernchat.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.CommandDispatcher;
import ganymedes01.etfuturum.client.chat.ChatSuggestions;
import ganymedes01.etfuturum.client.chat.IChatScreen;
import ganymedes01.etfuturum.client.chat.ClientCommandSource;
import ganymedes01.etfuturum.client.chat.ClientCommands;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modern's chat input in 1.7.10's chat screen (spec §40.3, ChatSuggestions), once the server has sent its command
 * tree; without one the screen is 1.7.10's. GuiSleepMP is a chat screen too.
 */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen implements IChatScreen {

	@Shadow
	protected GuiTextField inputField;

	@Unique
	private ChatSuggestions etfu$suggestions;
	@Unique
	private String etfu$textBefore;

	@Override
	public ChatSuggestions etfu$suggestions() {
		return etfu$suggestions;
	}

	@Override
	public GuiTextField etfu$input() {
		return inputField;
	}

	/** Modern's 256 characters, when the server reads that many (it sent a tree, so it has EFR). */
	@ModifyConstant(method = "initGui", constant = @Constant(intValue = 100))
	private int etfu$modernLength(int length) {
		return ClientCommands.active() ? 256 : length;
	}

	/** The list opens on its own only after the first edit; the usage shows at once. */
	@Inject(method = "initGui", at = @At("TAIL"))
	private void etfu$attachSuggestions(CallbackInfo ci) {
		CommandDispatcher<ClientCommandSource> commands = ClientCommands.dispatcher();
		etfu$suggestions = commands == null ? null
			: new ChatSuggestions(commands, this, inputField, fontRendererObj, (lines, x, y) -> func_146283_a(lines, x, y));
		if (etfu$suggestions != null) {
			etfu$suggestions.setAllowSuggestions(false);
			etfu$suggestions.update();
		}
	}

	/** The list takes its keys first, and Tab always (1.7.10's own completion never runs). */
	@Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
	private void etfu$suggestionsFirst(char typedChar, int keyCode, CallbackInfo ci) {
		etfu$textBefore = null;
		if (etfu$suggestions == null) {
			return;
		}
		if (etfu$suggestions.keyPressed(keyCode)) {
			ci.cancel();
			return;
		}
		etfu$textBefore = inputField.getText();
	}

	/** A changed text updates everything; a value from the history (Up, Down) doesn't open the list. */
	@Inject(method = "keyTyped", at = @At("TAIL"))
	private void etfu$edited(char typedChar, int keyCode, CallbackInfo ci) {
		if (etfu$suggestions != null && etfu$textBefore != null && !etfu$textBefore.equals(inputField.getText())) {
			etfu$suggestions.edited();
			if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
				etfu$suggestions.setAllowSuggestions(false);
			}
		}
		etfu$textBefore = null;
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void etfu$clickOnList(int mouseX, int mouseY, int button, CallbackInfo ci) {
		if (etfu$suggestions != null && etfu$suggestions.mouseClicked(mouseX, mouseY)) {
			ci.cancel();
		}
	}

	/** The wheel over the list scrolls the list, not the chat. */
	@ModifyExpressionValue(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int etfu$wheelOnList(int wheel) {
		if (wheel != 0 && etfu$suggestions != null) {
			int mouseX = Mouse.getEventX() * width / mc.displayWidth;
			int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
			if (etfu$suggestions.mouseScrolled(Integer.signum(wheel), mouseX, mouseY)) {
				return 0;
			}
		}
		return wheel;
	}

	@Inject(method = "drawScreen", at = @At("TAIL"))
	private void etfu$drawSuggestions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if (etfu$suggestions != null) {
			etfu$suggestions.render(mouseX, mouseY);
		}
	}
}
