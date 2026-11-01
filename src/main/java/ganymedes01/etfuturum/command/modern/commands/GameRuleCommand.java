package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.world.GameRules;

/**
 * Modern's /gamerule (spec §51.2) for the rules 1.7.10 has, by 26.3's names; any other rule goes to 1.7.10's form.
 */
public final class GameRuleCommand extends ModernCommand {

	/** 26.3's name and 1.7.10's. */
	private static final String[][] RULES = {{"advance_time", "doDaylightCycle"}, {"spawn_mobs", "doMobSpawning"}, {"mob_drops", "doMobLoot"},
		{"block_drops", "doTileDrops"}, {"keep_inventory", "keepInventory"}, {"mob_griefing", "mobGriefing"},
		{"natural_health_regeneration", "naturalRegeneration"}, {"command_block_output", "commandBlockOutput"},
		{"advance_weather", "doWeatherCycle"}};
	/** A number in 26.3, on or off in 1.7.10; on reads as 26.3's default. */
	private static final String FIRE = "fire_spread_radius_around_player";
	private static final int FIRE_ON = 128;
	private static final DynamicCommandExceptionType NO_RULE = new DynamicCommandExceptionType(
		name -> ComponentMessage.translatable("commands.gamerule.norule", name));

	public GameRuleCommand() {
		super("gamerule", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		LiteralArgumentBuilder<ModernCommandSource> gamerule = LiteralArgumentBuilder.<ModernCommandSource>literal("gamerule").requires(requirement());
		for (String[] rule : RULES) {
			gamerule.then(LiteralArgumentBuilder.<ModernCommandSource>literal(rule[0])
				.executes(c -> query(c, rule[0], rule[1]))
				.then(RequiredArgumentBuilder.<ModernCommandSource, Boolean>argument("value", BoolArgumentType.bool())
					.executes(c -> set(c, rule[0], rule[1], String.valueOf(BoolArgumentType.getBool(c, "value")), String.valueOf(BoolArgumentType.getBool(c, "value"))))));
		}
		gamerule.then(LiteralArgumentBuilder.<ModernCommandSource>literal(FIRE)
			.executes(c -> query(c, FIRE, "doFireTick"))
			.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("value", IntegerArgumentType.integer(0))
				.executes(c -> set(c, FIRE, "doFireTick", String.valueOf(IntegerArgumentType.getInteger(c, "value") > 0),
					String.valueOf(IntegerArgumentType.getInteger(c, "value"))))));
		dispatcher.register(gamerule);
	}

	private static GameRules rules(CommandContext<ModernCommandSource> context, String name, String legacy) throws CommandSyntaxException {
		GameRules rules = context.getSource().server().worldServers[0].getGameRules();
		if (!rules.hasRule(legacy)) {
			throw NO_RULE.create(name);
		}
		return rules;
	}

	/** The value as modern shows it: the fire rule as a number. */
	private static String shown(String name, String legacyValue) {
		return name.equals(FIRE) ? String.valueOf("true".equals(legacyValue) ? FIRE_ON : 0) : legacyValue;
	}

	private int query(CommandContext<ModernCommandSource> context, String name, String legacy) throws CommandSyntaxException {
		String value = shown(name, rules(context, name, legacy).getGameRuleStringValue(legacy));
		context.getSource().sendSuccess(legacyView(), false, "commands.gamerule.query", name, value);
		return value.equals("true") ? 1 : value.equals("false") ? 0 : Integer.parseInt(value);
	}

	private int set(CommandContext<ModernCommandSource> context, String name, String legacy, String legacyValue, String shownValue) throws CommandSyntaxException {
		GameRules rules = rules(context, name, legacy);
		for (net.minecraft.world.WorldServer world : context.getSource().server().worldServers) {
			world.getGameRules().setOrCreateGameRule(legacy, legacyValue);
		}
		rules.setOrCreateGameRule(legacy, legacyValue);
		context.getSource().sendSuccess(legacyView(), true, "commands.gamerule.set", name, shownValue);
		return 1;
	}
}
