package ganymedes01.etfuturum.mixins.early.chat.client;

import ganymedes01.etfuturum.client.ChatHistoryFile;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Leaving a world keeps the chat: the typed-message history (the up arrow) as modern keeps it, and, if wanted, the
 * log itself, which modern clears. 1.7.10 cleared both. The typed history can also outlive the game itself, kept in
 * a file (ChatHistoryFile), which neither version does.
 */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {
	@Shadow
	@Final
	private List sentMessages;
	@Shadow
	@Final
	private List chatLines;
	@Shadow
	@Final
	private List field_146253_i;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void etfu$loadTheHistory(Minecraft mc, CallbackInfo ci) {
		if (ConfigMixins.chatHistoryAcrossRestarts) {
			sentMessages.addAll(ChatHistoryFile.load());
		}
	}

	@Inject(method = "addToSentMessages", at = @At("TAIL"))
	private void etfu$saveTheHistory(String message, CallbackInfo ci) {
		if (ConfigMixins.chatHistoryAcrossRestarts) {
			ChatHistoryFile.save(sentMessages);
		}
	}

	@Inject(method = "clearChatMessages", at = @At("HEAD"), cancellable = true)
	private void etfu$keepTheChat(CallbackInfo ci) {
		if (!ConfigMixins.chatHistoryBetweenWorlds && !ConfigMixins.chatLogBetweenWorlds) {
			return;
		}
		if (!ConfigMixins.chatLogBetweenWorlds) {
			field_146253_i.clear();
			chatLines.clear();
		}
		if (!ConfigMixins.chatHistoryBetweenWorlds) {
			sentMessages.clear();
		}
		ci.cancel();
	}
}
