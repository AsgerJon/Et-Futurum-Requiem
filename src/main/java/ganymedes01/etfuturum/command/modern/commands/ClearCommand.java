package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.arguments.ItemPredicateArgument;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Modern's /clear [targets] [item] [maxCount] (spec §51.1): maxCount 0 only counts. */
public final class ClearCommand extends ModernCommand {

	private static final DynamicCommandExceptionType FAILED_SINGLE = new DynamicCommandExceptionType(
		name -> ComponentMessage.translatable("clear.failed.single", name));
	private static final DynamicCommandExceptionType FAILED_MULTIPLE = new DynamicCommandExceptionType(
		count -> ComponentMessage.translatable("clear.failed.multiple", count));

	public ClearCommand() {
		super("clear", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		Predicate<ItemStack> any = stack -> true;
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("clear")
			.requires(requirement())
			.executes(c -> clear(c.getSource(), Collections.singletonList(Commands.requirePlayer(c.getSource())), any, -1))
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.players())
				.executes(c -> clear(c.getSource(), EntityArgument.getPlayers(c, "targets"), any, -1))
				.then(RequiredArgumentBuilder.<ModernCommandSource, Predicate<ItemStack>>argument("item", ItemPredicateArgument.itemPredicate())
					.executes(c -> clear(c.getSource(), EntityArgument.getPlayers(c, "targets"), ItemPredicateArgument.getItemPredicate(c, "item"), -1))
					.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("maxCount", IntegerArgumentType.integer(0))
						.executes(c -> clear(c.getSource(), EntityArgument.getPlayers(c, "targets"), ItemPredicateArgument.getItemPredicate(c, "item"),
							IntegerArgumentType.getInteger(c, "maxCount")))))));
	}

	/** Up to maxCount matching items from each player (all for -1); 0 only counts them. */
	private int clear(ModernCommandSource source, List<EntityPlayerMP> players, Predicate<ItemStack> item, int maxCount) throws CommandSyntaxException {
		int total = 0;
		for (EntityPlayerMP player : players) {
			total += clear(player.inventory.mainInventory, item, maxCount);
			total += clear(player.inventory.armorInventory, item, maxCount < 0 ? -1 : Math.max(0, maxCount - total));
			player.inventoryContainer.detectAndSendChanges();
		}
		if (total == 0) {
			throw players.size() == 1 ? FAILED_SINGLE.create(players.get(0).func_145748_c_()) : FAILED_MULTIPLE.create(players.size());
		}
		String kind = maxCount == 0 ? "test" : "success";
		if (players.size() == 1) {
			source.sendSuccess(legacyView(), true, "commands.clear." + kind + ".single", total, players.get(0).func_145748_c_());
		} else {
			source.sendSuccess(legacyView(), true, "commands.clear." + kind + ".multiple", total, players.size());
		}
		return total;
	}

	private static int clear(ItemStack[] slots, Predicate<ItemStack> item, int maxCount) {
		int count = 0;
		for (int slot = 0; slot < slots.length; slot++) {
			ItemStack stack = slots[slot];
			if (stack == null || !item.test(stack)) {
				continue;
			}
			if (maxCount == 0) {
				count += stack.stackSize;
				continue;
			}
			int take = maxCount < 0 ? stack.stackSize : Math.min(stack.stackSize, maxCount - count);
			if (take <= 0) {
				break;
			}
			stack.stackSize -= take;
			count += take;
			if (stack.stackSize <= 0) {
				slots[slot] = null;
			}
		}
		return count;
	}
}
