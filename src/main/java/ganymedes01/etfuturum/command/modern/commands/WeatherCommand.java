package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.TimeArgument;
import net.minecraft.world.storage.WorldInfo;

import java.util.Random;

/** Modern's /weather clear|rain|thunder [duration] (spec §51.1); without one, 1.7.10's 5 to 15 minutes. */
public final class WeatherCommand extends ModernCommand {

	private static final Random RANDOM = new Random();

	public WeatherCommand() {
		super("weather", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		LiteralArgumentBuilder<ModernCommandSource> weather = LiteralArgumentBuilder.<ModernCommandSource>literal("weather").requires(requirement());
		for (String kind : new String[]{"clear", "rain", "thunder"}) {
			weather.then(LiteralArgumentBuilder.<ModernCommandSource>literal(kind)
				.executes(c -> set(c.getSource(), kind, (300 + RANDOM.nextInt(600)) * 20))
				.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("duration", TimeArgument.time(1))
					.executes(c -> set(c.getSource(), kind, TimeArgument.getTime(c, "duration")))));
		}
		dispatcher.register(weather);
	}

	private int set(ModernCommandSource source, String kind, int duration) {
		WorldInfo info = source.server().worldServers[0].getWorldInfo();
		boolean rain = !kind.equals("clear");
		boolean thunder = kind.equals("thunder");
		info.setRaining(rain);
		info.setThundering(thunder);
		info.setRainTime(duration);
		info.setThunderTime(duration);
		source.sendSuccess(legacyView(), true, "commands.weather.set." + kind);
		return duration;
	}
}
