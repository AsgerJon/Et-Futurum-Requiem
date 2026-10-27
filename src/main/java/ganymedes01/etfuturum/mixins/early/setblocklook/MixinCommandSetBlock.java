package ganymedes01.etfuturum.mixins.early.setblocklook;

import ganymedes01.etfuturum.command.LookedAtBlock;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSetBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** /setblock from a player may leave the coordinates out and set the block they look at (LookedAtBlock). */
@Mixin(CommandSetBlock.class)
public class MixinCommandSetBlock {

	@ModifyVariable(method = "processCommand", at = @At("HEAD"), argsOnly = true)
	private String[] etfu$defaultToTheLookedAtBlock(String[] args, ICommandSender sender, String[] ignored) {
		return LookedAtBlock.fillInCoordinates(sender, args);
	}

	/** Block names complete as the first argument too, since that is where they go without coordinates. */
	@Inject(method = "addTabCompletionOptions", at = @At("HEAD"), cancellable = true)
	private void etfu$blockNamesFirst(ICommandSender sender, String[] args, CallbackInfoReturnable<List<String>> cir) {
		if (args.length == 1 && !LookedAtBlock.isCoordinate(args[0]) && sender instanceof EntityPlayerMP) {
			cir.setReturnValue(CommandBase.getListOfStringsFromIterableMatchingLastWord(args, Block.blockRegistry.getKeys()));
		}
	}
}
