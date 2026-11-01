package ganymedes01.etfuturum.mixins.early.modernchat.client;

import ganymedes01.etfuturum.client.chat.IChatInputField;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

/**
 * The chat's text field draws the command coloured and the selected suggestion's rest in grey after the cursor
 * (spec §40.3). Formatting codes are inserted for drawing only; they take no width, and the section sign can't be
 * typed into chat. Other text fields have no formatter and draw as before.
 */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField implements IChatInputField {

	@Unique
	private static final int ETFU_SUGGESTION_COLOUR = 0xFF808080;

	@Shadow
	@Final
	private FontRenderer field_146211_a;
	@Shadow
	public int xPosition;
	@Shadow
	public int yPosition;
	@Shadow
	public int height;
	@Shadow
	private String text;
	@Shadow
	private boolean enableBackgroundDrawing;
	@Shadow
	private int lineScrollOffset;
	@Shadow
	private int cursorPosition;

	@Shadow
	public abstract int getMaxStringLength();

	@Shadow
	public abstract boolean getVisible();

	@Unique
	private BiFunction<String, Integer, String> etfu$formatter;
	@Unique
	private String etfu$suggestion;
	@Unique
	private boolean etfu$suggestionDrawn;

	@Override
	public void etfu$setFormatter(BiFunction<String, Integer, String> formatter) {
		etfu$formatter = formatter;
	}

	@Override
	public void etfu$setSuggestion(String suggestion) {
		etfu$suggestion = suggestion;
	}

	@Inject(method = "drawTextBox", at = @At("HEAD"))
	private void etfu$newFrame(CallbackInfo ci) {
		etfu$suggestionDrawn = false;
	}

	/** The text before the cursor (or all of it when the cursor is off screen) starts at the scroll offset. */
	@ModifyArg(method = "drawTextBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I", ordinal = 0), index = 0)
	private String etfu$formatBeforeCursor(String part) {
		return etfu$format(part, lineScrollOffset);
	}

	@ModifyArg(method = "drawTextBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I", ordinal = 1), index = 0)
	private String etfu$formatAfterCursor(String part) {
		return etfu$format(part, cursorPosition);
	}

	/** Before the underscore cursor, so the cursor shows on top as in modern. */
	@Inject(method = "drawTextBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I", ordinal = 2))
	private void etfu$suggestionUnderCursor(CallbackInfo ci) {
		etfu$drawSuggestion();
	}

	/** While the cursor blinks off. */
	@Inject(method = "drawTextBox", at = @At("TAIL"))
	private void etfu$suggestionWithoutCursor(CallbackInfo ci) {
		if (!etfu$suggestionDrawn) {
			etfu$drawSuggestion();
		}
	}

	@Unique
	private String etfu$format(String part, int offset) {
		if (etfu$formatter == null) {
			return part;
		}
		String formatted = etfu$formatter.apply(part, offset);
		return formatted == null ? part : formatted;
	}

	/** Only with the cursor at the end of the text and room left in the field, as modern. */
	@Unique
	private void etfu$drawSuggestion() {
		etfu$suggestionDrawn = true;
		if (etfu$suggestion == null || !getVisible() || cursorPosition != text.length() || text.length() >= getMaxStringLength()) {
			return;
		}
		int x = (enableBackgroundDrawing ? xPosition + 4 : xPosition) + field_146211_a.getStringWidth(text.substring(lineScrollOffset));
		int y = enableBackgroundDrawing ? yPosition + (height - 8) / 2 : yPosition;
		field_146211_a.drawStringWithShadow(etfu$suggestion, x, y, ETFU_SUGGESTION_COLOUR);
	}
}
