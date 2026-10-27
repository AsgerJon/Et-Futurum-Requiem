package ganymedes01.etfuturum.mixins.early.givecount;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandGive;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * /give takes counts above 64, as in modern: up to 100 stacks of the item, handed over as full stacks (an item
 * entity holds one stack; the save format and the network carry the count as a byte). Each stack is dropped at the
 * player with no pickup delay, as vanilla's single stack is, so it lands in the inventory and what does not fit
 * stays on the ground.
 */
@Mixin(CommandGive.class)
public abstract class MixinCommandGive {

	@ModifyArg(method = "processCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandGive;parseIntBounded(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;II)I"), index = 3)
	private int etfu$noCountCap(int max) {
		return Integer.MAX_VALUE;
	}

	@WrapOperation(method = "processCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;dropPlayerItemWithRandomChoice(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/EntityItem;"))
	private EntityItem etfu$giveInStacks(EntityPlayerMP player, ItemStack stack, boolean randomly, Operation<EntityItem> original) {
		int perStack = Math.max(1, stack.getMaxStackSize());
		int limit = perStack * 100;
		if (stack.stackSize > limit) {
			throw new CommandException("commands.etfuturum.give.tooMany", limit, stack.func_151000_E());
		}
		if (stack.stackSize <= perStack) {
			return original.call(player, stack, randomly);
		}
		EntityItem last = null;
		for (int left = stack.stackSize; left > 0; left -= perStack) {
			ItemStack part = stack.copy();
			part.stackSize = Math.min(left, perStack);
			EntityItem dropped = original.call(player, part, randomly);
			if (dropped != null) {
				dropped.delayBeforeCanPickup = 0;
				dropped.func_145797_a(player.getCommandSenderName());
				last = dropped;
			}
		}
		return last;
	}
}
