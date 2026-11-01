package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

/** Modern's /experience and its alias /xp (spec §51.1): add, set and query, in levels or points. */
public final class ExperienceCommand extends ModernCommand {

	private static final SimpleCommandExceptionType POINTS_INVALID = Responses.failure("commands.experience.set.points.invalid");

	public ExperienceCommand() {
		super("experience", 2);
	}

	@Override
	public List<String> aliases() {
		return java.util.Collections.singletonList("xp");
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		LiteralCommandNode<ModernCommandSource> experience = dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("experience")
			.requires(requirement())
			.then(change("add", true))
			.then(change("set", false))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("query")
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("target", EntityArgument.of(true, true))
					.then(LiteralArgumentBuilder.<ModernCommandSource>literal("levels").executes(c -> query(c.getSource(), EntityArgument.getPlayers(c, "target").get(0), true)))
					.then(LiteralArgumentBuilder.<ModernCommandSource>literal("points").executes(c -> query(c.getSource(), EntityArgument.getPlayers(c, "target").get(0), false))))));
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("xp").requires(requirement()).redirect(experience));
	}

	private LiteralArgumentBuilder<ModernCommandSource> change(String name, boolean add) {
		return LiteralArgumentBuilder.<ModernCommandSource>literal(name)
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("target", EntityArgument.players())
				.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("amount", add ? IntegerArgumentType.integer() : IntegerArgumentType.integer(0))
					.executes(c -> change(c.getSource(), EntityArgument.getPlayers(c, "target"), IntegerArgumentType.getInteger(c, "amount"), add, false))
					.then(LiteralArgumentBuilder.<ModernCommandSource>literal("levels")
						.executes(c -> change(c.getSource(), EntityArgument.getPlayers(c, "target"), IntegerArgumentType.getInteger(c, "amount"), add, true)))
					.then(LiteralArgumentBuilder.<ModernCommandSource>literal("points")
						.executes(c -> change(c.getSource(), EntityArgument.getPlayers(c, "target"), IntegerArgumentType.getInteger(c, "amount"), add, false)))));
	}

	/** The points into the current level, as modern counts them. */
	private static int points(EntityPlayerMP player) {
		return Math.round(player.experience * player.xpBarCap());
	}

	private int change(ModernCommandSource source, List<EntityPlayerMP> players, int amount, boolean add, boolean levels) throws CommandSyntaxException {
		List<EntityPlayerMP> worked = new ArrayList<>();
		for (EntityPlayerMP player : players) {
			if (levels) {
				// 1.7.10's player sends its experience again after a level change
				player.addExperienceLevel(add ? amount : amount - player.experienceLevel);
			} else if (add) {
				addPoints(player, amount);
			} else {
				if (amount > player.xpBarCap()) {
					continue;
				}
				player.experience = (float) amount / player.xpBarCap();
				player.addExperienceLevel(0);
			}
			worked.add(player);
		}
		String key = "commands.experience." + (add ? "add." : "set.") + (levels ? "levels" : "points") + ".success.";
		return Responses.send(source, this, worked, POINTS_INVALID,
			key + "single", p -> new Object[]{amount, p.func_145748_c_()},
			key + "multiple", n -> new Object[]{amount, n});
	}

	/** Points up or down across levels, as modern's giveExperiencePoints does. */
	private static void addPoints(EntityPlayerMP player, int amount) {
		if (amount >= 0) {
			player.addExperience(amount);
			return;
		}
		int left = -amount;
		while (left > 0) {
			int inLevel = points(player);
			if (left <= inLevel) {
				player.experience = (float) (inLevel - left) / player.xpBarCap();
				left = 0;
			} else if (player.experienceLevel > 0) {
				// down to the top of the level below; what is left comes off that
				left -= inLevel;
				player.experienceLevel--;
				player.experience = 1.0F;
			} else {
				player.experience = 0.0F;
				left = 0;
			}
		}
		player.experienceTotal = Math.max(0, player.experienceTotal + amount);
		player.addExperienceLevel(0);
	}

	private int query(ModernCommandSource source, EntityPlayerMP player, boolean levels) {
		int value = levels ? player.experienceLevel : points(player);
		source.sendSuccess(legacyView(), false, "commands.experience.query." + (levels ? "levels" : "points"), player.func_145748_c_(), value);
		return value;
	}
}
