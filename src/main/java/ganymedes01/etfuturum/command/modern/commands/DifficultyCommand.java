package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.EnumDifficulty;

import java.util.Locale;

/** Modern's /difficulty [level] (spec §51.1). */
public final class DifficultyCommand extends ModernCommand {

	private static final DynamicCommandExceptionType SAME = new DynamicCommandExceptionType(
		difficulty -> ComponentMessage.translatable("commands.difficulty.failure", difficulty));

	public DifficultyCommand() {
		super("difficulty", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		LiteralArgumentBuilder<ModernCommandSource> difficulty = LiteralArgumentBuilder.<ModernCommandSource>literal("difficulty")
			.requires(requirement())
			.executes(c -> {
				EnumDifficulty current = c.getSource().world().difficultySetting;
				c.getSource().sendSuccess(legacyView(), false, "commands.difficulty.query", name(current));
				return current.getDifficultyId();
			});
		for (EnumDifficulty level : EnumDifficulty.values()) {
			difficulty.then(LiteralArgumentBuilder.<ModernCommandSource>literal(level.name().toLowerCase(Locale.ROOT)).executes(c -> {
				if (c.getSource().server().worldServers[0].difficultySetting == level) {
					throw SAME.create(name(level));
				}
				c.getSource().server().func_147139_a(level);
				c.getSource().sendSuccess(legacyView(), true, "commands.etfuturum.difficulty.success", name(level));
				return 0;
			}));
		}
		dispatcher.register(difficulty);
	}

	private static ChatComponentTranslation name(EnumDifficulty difficulty) {
		return new ChatComponentTranslation(difficulty.getDifficultyResourceKey());
	}
}
