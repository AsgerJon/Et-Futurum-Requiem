package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.arguments.ItemArgument;
import ganymedes01.etfuturum.command.modern.arguments.ItemInput;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Random;

/** Modern's /give (spec §45.5): into the inventory, the rest dropped at the player. */
public final class GiveCommand extends ModernCommand {

	private static final Dynamic2CommandExceptionType TOO_MANY_ITEMS = new Dynamic2CommandExceptionType(
		(max, item) -> ComponentMessage.translatable("commands.give.failed.toomanyitems", max, item));
	private static final int MAX_STACKS = 100;

	public GiveCommand() {
		super("give", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("give")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.players())
				.then(RequiredArgumentBuilder.<ModernCommandSource, ItemInput>argument("item", ItemArgument.item())
					.executes(c -> give(c.getSource(), ItemArgument.getItem(c, "item"), EntityArgument.getPlayers(c, "targets"), 1))
					.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("count", IntegerArgumentType.integer(1))
						.executes(c -> give(c.getSource(), ItemArgument.getItem(c, "item"), EntityArgument.getPlayers(c, "targets"),
							IntegerArgumentType.getInteger(c, "count")))))));
	}

	private int give(ModernCommandSource source, ItemInput input, List<EntityPlayerMP> players, int count) throws CommandSyntaxException {
		ItemStack prototype = input.createItemStack(1);
		int stackSize = prototype.getMaxStackSize();
		int maxCount = stackSize * MAX_STACKS;
		if (count > maxCount) {
			throw TOO_MANY_ITEMS.create(maxCount, prototype.func_151000_E());
		}
		for (EntityPlayerMP player : players) {
			int remaining = count;
			while (remaining > 0) {
				int size = Math.min(stackSize, remaining);
				remaining -= size;
				ItemStack stack = prototype.copy();
				stack.stackSize = size;
				boolean added = player.inventory.addItemStackToInventory(stack);
				if (added && stack.stackSize <= 0) {
					// all of it went in: a copy pops up at the player for a moment, as an item being picked up
					EntityItem shown = player.dropPlayerItemWithRandomChoice(prototype.copy(), false);
					if (shown != null) {
						shown.delayBeforeCanPickup = Short.MAX_VALUE;
						shown.age = shown.lifespan - 1;
					}
					Random random = player.getRNG();
					player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "random.pop", 0.2F,
						((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
					player.openContainer.detectAndSendChanges();
				} else {
					EntityItem dropped = player.dropPlayerItemWithRandomChoice(stack, false);
					if (dropped != null) {
						dropped.delayBeforeCanPickup = 0;
						dropped.func_145797_a(player.getCommandSenderName());
					}
				}
			}
		}
		if (players.size() == 1) {
			source.sendSuccess(legacyView(), true, "commands.give.success.single", count, prototype.func_151000_E(), players.get(0).func_145748_c_());
		} else {
			source.sendSuccess(legacyView(), true, "commands.give.success.multiple", count, prototype.func_151000_E(), players.size());
		}
		return players.size();
	}
}
