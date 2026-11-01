package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.TimeArgument;
import net.minecraft.world.WorldServer;

/** The classic part of modern's /time (spec §51.1): set, add and query, for every world as 1.7.10's does. */
public final class TimeCommand extends ModernCommand {

	public TimeCommand() {
		super("time", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		LiteralArgumentBuilder<ModernCommandSource> set = LiteralArgumentBuilder.<ModernCommandSource>literal("set")
			.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("time", TimeArgument.time(0))
				.executes(c -> set(c.getSource(), TimeArgument.getTime(c, "time"))));
		String[][] markers = {{"day", "1000"}, {"noon", "6000"}, {"night", "13000"}, {"midnight", "18000"}};
		for (String[] marker : markers) {
			int time = Integer.parseInt(marker[1]);
			set.then(LiteralArgumentBuilder.<ModernCommandSource>literal(marker[0]).executes(c -> set(c.getSource(), time)));
		}
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("time")
			.requires(requirement())
			.then(set)
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("add")
				.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("time", TimeArgument.time(Integer.MIN_VALUE))
					.executes(c -> add(c.getSource(), TimeArgument.getTime(c, "time")))))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("query")
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("daytime").executes(c -> query(c.getSource(),
					(int) (c.getSource().world().getWorldTime() % 24000L), "commands.time.query")))
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("gametime").executes(c -> query(c.getSource(),
					(int) (c.getSource().world().getTotalWorldTime() % Integer.MAX_VALUE), "commands.time.query.gametime")))
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("day").executes(c -> query(c.getSource(),
					(int) (c.getSource().world().getWorldTime() / 24000L % Integer.MAX_VALUE), "commands.time.query")))));
	}

	private int set(ModernCommandSource source, int time) {
		for (WorldServer world : source.server().worldServers) {
			world.setWorldTime(time);
		}
		source.sendSuccess(legacyView(), true, "commands.etfuturum.time.set", time);
		return (int) (source.world().getWorldTime() % 24000L);
	}

	private int add(ModernCommandSource source, int time) {
		for (WorldServer world : source.server().worldServers) {
			world.setWorldTime(world.getWorldTime() + time);
		}
		int daytime = (int) (source.world().getWorldTime() % 24000L);
		source.sendSuccess(legacyView(), true, "commands.etfuturum.time.set", daytime);
		return daytime;
	}

	private int query(ModernCommandSource source, int value, String key) {
		source.sendSuccess(legacyView(), false, key, value);
		return value;
	}
}
