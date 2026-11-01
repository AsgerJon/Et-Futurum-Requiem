package ganymedes01.etfuturum.mixins.early.modernchat;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.command.modern.ModernCommands;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player's commands change with their permission: the tree goes again, as modern sends it with the level. */
@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManager {

	@Shadow
	@Final
	private MinecraftServer mcServer;

	@Shadow
	public abstract EntityPlayerMP func_152612_a(String username);

	/** Made operator. */
	@Inject(method = "func_152605_a", at = @At("TAIL"))
	private void etfu$commandsAfterOp(GameProfile profile, CallbackInfo ci) {
		etfu$resend(profile);
	}

	/** No longer operator. */
	@Inject(method = "func_152610_b", at = @At("TAIL"))
	private void etfu$commandsAfterDeop(GameProfile profile, CallbackInfo ci) {
		etfu$resend(profile);
	}

	/** Cheats turned on or off for a LAN world: everyone's commands may change. */
	@Inject(method = "setCommandsAllowedForAll", at = @At("TAIL"))
	private void etfu$commandsAfterCheats(boolean allowed, CallbackInfo ci) {
		ModernCommands.sendCommandsToAll(mcServer);
	}

	@Unique
	private void etfu$resend(GameProfile profile) {
		EntityPlayerMP player = func_152612_a(profile.getName());
		if (player != null) {
			ModernCommands.sendCommands(player);
		}
	}
}
