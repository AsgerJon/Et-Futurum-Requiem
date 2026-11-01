package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EffectArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentTranslation;

import java.util.Collections;
import java.util.List;

/** Modern's /effect: give and clear (spec §41.4). */
public final class EffectCommand extends ModernCommand {

	private static final SimpleCommandExceptionType GIVE_FAILED = simple("commands.effect.give.failed");
	private static final SimpleCommandExceptionType CLEAR_EVERYTHING_FAILED = simple("commands.effect.clear.everything.failed");
	private static final SimpleCommandExceptionType CLEAR_SPECIFIC_FAILED = simple("commands.effect.clear.specific.failed");
	private static final SimpleCommandExceptionType REQUIRES_ENTITY = simple("permissions.requires.entity");
	/** Modern's endless effect; 1.7.10 has none, so the longest duration, which it shows as **:**. */
	private static final int INFINITE = -1;

	public EffectCommand() {
		super("effect", 2);
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("effect")
			.requires(requirement())
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("clear")
				.executes(c -> clearAll(c.getSource(), self(c.getSource())))
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.entities())
					.executes(c -> clearAll(c.getSource(), EntityArgument.getEntities(c, "targets")))
					.then(RequiredArgumentBuilder.<ModernCommandSource, Potion>argument("effect", EffectArgument.effect())
						.executes(c -> clearOne(c.getSource(), EntityArgument.getEntities(c, "targets"), EffectArgument.getEffect(c, "effect"))))))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("give")
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.entities())
					.then(RequiredArgumentBuilder.<ModernCommandSource, Potion>argument("effect", EffectArgument.effect())
						.executes(c -> give(c.getSource(), EntityArgument.getEntities(c, "targets"), EffectArgument.getEffect(c, "effect"), null, 0))
						.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("seconds", IntegerArgumentType.integer(1, 1000000))
							.executes(c -> give(c.getSource(), EntityArgument.getEntities(c, "targets"), EffectArgument.getEffect(c, "effect"),
								IntegerArgumentType.getInteger(c, "seconds"), 0))
							.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("amplifier", IntegerArgumentType.integer(0, 255))
								.executes(c -> give(c.getSource(), EntityArgument.getEntities(c, "targets"), EffectArgument.getEffect(c, "effect"),
									IntegerArgumentType.getInteger(c, "seconds"), IntegerArgumentType.getInteger(c, "amplifier")))))
						.then(LiteralArgumentBuilder.<ModernCommandSource>literal("infinite")
							.executes(c -> give(c.getSource(), EntityArgument.getEntities(c, "targets"), EffectArgument.getEffect(c, "effect"), INFINITE, 0))
							.then(RequiredArgumentBuilder.<ModernCommandSource, Integer>argument("amplifier", IntegerArgumentType.integer(0, 255))
								.executes(c -> give(c.getSource(), EntityArgument.getEntities(c, "targets"), EffectArgument.getEffect(c, "effect"),
									INFINITE, IntegerArgumentType.getInteger(c, "amplifier")))))))));
	}

	private static List<Entity> self(ModernCommandSource source) throws CommandSyntaxException {
		Entity entity = source.entity();
		if (entity == null) {
			throw REQUIRES_ENTITY.create();
		}
		return Collections.singletonList(entity);
	}

	/** No time: 30 seconds, or 1 tick for an instant effect, whose given number counts ticks. */
	private static int durationInTicks(Integer seconds, Potion potion) {
		if (seconds == null) {
			return potion.isInstant() ? 1 : 600;
		}
		if (potion.isInstant()) {
			// 1.7.10 applies an instant effect every tick it lasts; an endless one applies once
			return seconds == INFINITE ? 1 : seconds;
		}
		return seconds == INFINITE ? Integer.MAX_VALUE : seconds * 20;
	}

	private int give(ModernCommandSource source, List<Entity> targets, Potion potion, Integer seconds, int amplifier) throws CommandSyntaxException {
		int duration = durationInTicks(seconds, potion);
		int applied = 0;
		EntityLivingBase only = null;
		for (Entity entity : targets) {
			if (entity instanceof EntityLivingBase living && apply(living, new PotionEffect(potion.id, duration, amplifier))) {
				only = ++applied == 1 ? living : null;
			}
		}
		if (applied == 0) {
			throw GIVE_FAILED.create();
		}
		if (applied == 1) {
			source.sendSuccess(legacyView(), true, "commands.effect.give.success.single", name(potion), only.func_145748_c_());
		} else {
			source.sendSuccess(legacyView(), true, "commands.effect.give.success.multiple", name(potion), applied);
		}
		return applied;
	}

	/** Applied unless the target is immune, or already has the effect as strong and as long. */
	private static boolean apply(EntityLivingBase living, PotionEffect effect) {
		if (!living.isPotionApplicable(effect)) {
			return false;
		}
		Potion potion = Potion.potionTypes[effect.getPotionID()];
		PotionEffect before = living.getActivePotionEffect(potion);
		int amplifier = before == null ? -1 : before.getAmplifier();
		int duration = before == null ? -1 : before.getDuration();
		boolean ambient = before != null && before.getIsAmbient();
		living.addPotionEffect(effect);
		PotionEffect after = living.getActivePotionEffect(potion);
		return after != null && (before == null || after.getAmplifier() != amplifier || after.getDuration() != duration || after.getIsAmbient() != ambient);
	}

	private int clearAll(ModernCommandSource source, List<Entity> targets) throws CommandSyntaxException {
		int cleared = 0;
		Entity only = null;
		for (Entity entity : targets) {
			if (entity instanceof EntityLivingBase living && !living.getActivePotionEffects().isEmpty()) {
				living.clearActivePotions();
				only = ++cleared == 1 ? living : null;
			}
		}
		if (cleared == 0) {
			throw CLEAR_EVERYTHING_FAILED.create();
		}
		if (cleared == 1) {
			source.sendSuccess(legacyView(), true, "commands.effect.clear.everything.success.single", only.func_145748_c_());
		} else {
			source.sendSuccess(legacyView(), true, "commands.effect.clear.everything.success.multiple", cleared);
		}
		return cleared;
	}

	private int clearOne(ModernCommandSource source, List<Entity> targets, Potion potion) throws CommandSyntaxException {
		int cleared = 0;
		Entity only = null;
		for (Entity entity : targets) {
			if (entity instanceof EntityLivingBase living && living.isPotionActive(potion)) {
				living.removePotionEffect(potion.id);
				only = ++cleared == 1 ? living : null;
			}
		}
		if (cleared == 0) {
			throw CLEAR_SPECIFIC_FAILED.create();
		}
		if (cleared == 1) {
			source.sendSuccess(legacyView(), true, "commands.effect.clear.specific.success.single", name(potion), only.func_145748_c_());
		} else {
			source.sendSuccess(legacyView(), true, "commands.effect.clear.specific.success.multiple", name(potion), cleared);
		}
		return cleared;
	}

	private static ChatComponentTranslation name(Potion potion) {
		return new ChatComponentTranslation(potion.getName());
	}
}
