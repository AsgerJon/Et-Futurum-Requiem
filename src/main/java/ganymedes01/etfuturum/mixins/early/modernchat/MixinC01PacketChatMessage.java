package ganymedes01.etfuturum.mixins.early.modernchat;

import net.minecraft.network.play.client.C01PacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The server reads chat messages of up to modern's 256 characters (1.7.10: 100); spec §40.1. */
@Mixin(C01PacketChatMessage.class)
public class MixinC01PacketChatMessage {

	@ModifyConstant(method = "readPacketData", constant = @Constant(intValue = 100))
	private int etfu$modernLength(int length) {
		return 256;
	}
}
