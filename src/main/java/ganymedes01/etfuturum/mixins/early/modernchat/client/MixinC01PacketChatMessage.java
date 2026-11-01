package ganymedes01.etfuturum.mixins.early.modernchat.client;

import ganymedes01.etfuturum.client.chat.ClientCommands;
import net.minecraft.network.play.client.C01PacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * A chat message is cut at modern's 256 characters instead of 100, but only for a server that sent a command tree:
 * one without EFR reads at most 100 and would drop the connection.
 */
@Mixin(C01PacketChatMessage.class)
public class MixinC01PacketChatMessage {

	@ModifyConstant(method = "<init>(Ljava/lang/String;)V", constant = @Constant(intValue = 100))
	private int etfu$modernLength(int length) {
		return ClientCommands.active() ? 256 : length;
	}
}
