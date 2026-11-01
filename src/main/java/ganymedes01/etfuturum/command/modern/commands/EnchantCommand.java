package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EnchantmentArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.List;
import java.util.Map;

/** Modern's /enchant (spec §44.2). */
public final class EnchantCommand extends ModernCommand {

	private static final DynamicCommandExceptionType NOT_LIVING = dynamic("commands.enchant.failed.entity");
	private static final DynamicCommandExceptionType NO_ITEM = dynamic("commands.enchant.failed.itemless");
	private static final DynamicCommandExceptionType INCOMPATIBLE = dynamic("commands.enchant.failed.incompatible");
	private static final Dynamic2CommandExceptionType LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType(
		(level, max) -> ComponentMessage.translatable("commands.enchant.failed.level", level, max));
	private static final SimpleCommandExceptionType NOTHING_CHANGED = new SimpleCommandExceptionType(ComponentMessage.translatable("commands.enchant.failed"));

	public EnchantCommand() {
		super("enchant", 2);
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("enchant")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.entities())
				.then(RequiredArgumentBuilder.<ModernCommandSource, Enchantment>argument("enchantment", EnchantmentArgument.enchantment())
					.executes(c -> enchant(c.getSource(), EntityArgument.getEntities(c, "targets"), EnchantmentArgument.getEnchantment(c, "enchantment"), 1))
					.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("level", IntegerArgumentType.integer(0))
						.executes(c -> enchant(c.getSource(), EntityArgument.getEntities(c, "targets"), EnchantmentArgument.getEnchantment(c, "enchantment"),
							IntegerArgumentType.getInteger(c, "level")))))));
	}

	/** The main-hand item of each target; one target's failure is reported, several targets' are skipped. */
	private int enchant(ModernCommandSource source, List<Entity> targets, Enchantment enchantment, int level) throws CommandSyntaxException {
		if (level > enchantment.getMaxLevel()) {
			throw LEVEL_TOO_HIGH.create(level, enchantment.getMaxLevel());
		}
		int enchanted = 0;
		Entity only = null;
		for (Entity entity : targets) {
			if (entity instanceof EntityLivingBase living) {
				ItemStack item = living.getHeldItem();
				if (item != null) {
					if (accepts(item, enchantment)) {
						// level 0 changes nothing, but counts, as in modern
						if (level > 0) {
							item.addEnchantment(enchantment, level);
						}
						only = ++enchanted == 1 ? entity : null;
					} else if (targets.size() == 1) {
						throw INCOMPATIBLE.create(item.getDisplayName());
					}
				} else if (targets.size() == 1) {
					throw NO_ITEM.create(entity.func_145748_c_().getUnformattedText());
				}
			} else if (targets.size() == 1) {
				throw NOT_LIVING.create(entity.func_145748_c_().getUnformattedText());
			}
		}
		if (enchanted == 0) {
			throw NOTHING_CHANGED.create();
		}
		if (enchanted == 1) {
			source.sendSuccess(legacyView(), true, "commands.enchant.success.single", fullName(enchantment, level), only.func_145748_c_());
		} else {
			source.sendSuccess(legacyView(), true, "commands.enchant.success.multiple", fullName(enchantment, level), enchanted);
		}
		return enchanted;
	}

	/**
	 * The item takes the enchantment (1.7.10's rule) and it goes with every one already on it, asked both ways; the
	 * same one again doesn't.
	 */
	@SuppressWarnings("unchecked")
	private static boolean accepts(ItemStack item, Enchantment enchantment) {
		if (!enchantment.canApply(item)) {
			return false;
		}
		for (Integer id : ((Map<Integer, Integer>) EnchantmentHelper.getEnchantments(item)).keySet()) {
			Enchantment existing = id >= 0 && id < Enchantment.enchantmentsList.length ? Enchantment.enchantmentsList[id] : null;
			if (existing == enchantment || existing != null && (!existing.canApplyTogether(enchantment) || !enchantment.canApplyTogether(existing))) {
				return false;
			}
		}
		return true;
	}

	/** Grey, with the level unless it is level 1 of an enchantment that has only one (1.7.10 has no curses). */
	private static IChatComponent fullName(Enchantment enchantment, int level) {
		IChatComponent name = new ChatComponentTranslation(enchantment.getName());
		if (level != 1 || enchantment.getMaxLevel() != 1) {
			name.appendText(" ").appendSibling(new ChatComponentTranslation("enchantment.level." + level));
		}
		name.getChatStyle().setColor(EnumChatFormatting.GRAY);
		return name;
	}
}
