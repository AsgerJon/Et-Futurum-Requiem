package io.github.asgerjon.mcfly.mixins.early.modernchat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.command.modern.ModernCommands;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Commands in modern form run first, with the 1.7.10 form as the fallback (spec §41.1, ModernCommands.beginRun).
 * Only the server's handler: Forge's client commands have their own. The method is never cancelled, since EFR's
 * doWeatherCycle (and other mods) mark its start and end.
 */
@Mixin(CommandHandler.class)
public abstract class MixinCommandHandler {

	@Shadow
	public abstract Map<String, ICommand> getCommands();

	@Unique
	private boolean etfu$isServerHandler() {
		return (Object) this instanceof ServerCommandManager;
	}

	@Inject(method = "executeCommand", at = @At("HEAD"))
	private void etfu$modernFirst(ICommandSender sender, String text, CallbackInfoReturnable<Integer> cir) {
		if (etfu$isServerHandler()) {
			ModernCommands.beginRun(sender, text, getCommands());
		}
	}

	/** After a modern run 1.7.10 finds no command of the name, and replies only with "unknown command", dropped below. */
	@WrapOperation(method = "executeCommand", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
	private Object etfu$noLegacyCommandAfterModern(Map<String, ICommand> commands, Object name, Operation<Object> original) {
		return etfu$isServerHandler() && ModernCommands.ranModern() ? null : original.call(commands, name);
	}

	@WrapOperation(method = "executeCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/ICommandSender;addChatMessage(Lnet/minecraft/util/IChatComponent;)V"))
	private void etfu$modernError(ICommandSender sender, IChatComponent message, Operation<Void> original) {
		if (!etfu$isServerHandler() || !ModernCommands.replaceLegacyMessage(sender, message)) {
			original.call(sender, message);
		}
	}

	@ModifyReturnValue(method = "executeCommand", at = @At("RETURN"))
	private int etfu$modernResult(int result) {
		return etfu$isServerHandler() ? ModernCommands.endRun(result) : result;
	}
}
