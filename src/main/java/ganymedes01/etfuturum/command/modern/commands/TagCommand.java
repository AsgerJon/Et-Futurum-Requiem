package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.Suggest;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.entity.EntityTags;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Modern's /tag (spec §51.1); 1.7.10 has none. */
public final class TagCommand extends ModernCommand {

	private static final SimpleCommandExceptionType ADD_FAILED = Responses.failure("commands.tag.add.failed");
	private static final SimpleCommandExceptionType REMOVE_FAILED = Responses.failure("commands.tag.remove.failed");

	public TagCommand() {
		super("tag", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("tag")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.entities())
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("add")
					.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("name", StringArgumentType.word())
						.executes(c -> add(c.getSource(), EntityArgument.getEntities(c, "targets"), StringArgumentType.getString(c, "name")))))
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("remove")
					.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("name", StringArgumentType.word())
						.suggests((c, builder) -> Suggest.matching(tagsOf(EntityArgument.getEntities(c, "targets")), builder))
						.executes(c -> remove(c.getSource(), EntityArgument.getEntities(c, "targets"), StringArgumentType.getString(c, "name")))))
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("list")
					.executes(c -> list(c.getSource(), EntityArgument.getEntities(c, "targets"))))));
	}

	private static Set<String> tagsOf(List<Entity> entities) {
		Set<String> tags = new LinkedHashSet<>();
		for (Entity entity : entities) {
			tags.addAll(EntityTags.get(entity));
		}
		return tags;
	}

	private int add(ModernCommandSource source, List<Entity> targets, String name) throws CommandSyntaxException {
		List<Entity> worked = new ArrayList<>();
		for (Entity entity : targets) {
			if (EntityTags.add(entity, name)) {
				worked.add(entity);
			}
		}
		return Responses.send(source, this, worked, ADD_FAILED,
			"commands.tag.add.success.single", e -> new Object[]{name, e.func_145748_c_()},
			"commands.tag.add.success.multiple", n -> new Object[]{name, n});
	}

	private int remove(ModernCommandSource source, List<Entity> targets, String name) throws CommandSyntaxException {
		List<Entity> worked = new ArrayList<>();
		for (Entity entity : targets) {
			if (EntityTags.remove(entity, name)) {
				worked.add(entity);
			}
		}
		return Responses.send(source, this, worked, REMOVE_FAILED,
			"commands.tag.remove.success.single", e -> new Object[]{name, e.func_145748_c_()},
			"commands.tag.remove.success.multiple", n -> new Object[]{name, n});
	}

	private int list(ModernCommandSource source, List<Entity> targets) {
		Set<String> tags = tagsOf(targets);
		String joined = String.join(", ", tags);
		if (targets.size() == 1) {
			Entity entity = targets.get(0);
			if (tags.isEmpty()) {
				source.sendSuccess(legacyView(), false, "commands.tag.list.single.empty", entity.func_145748_c_());
			} else {
				source.sendSuccess(legacyView(), false, "commands.tag.list.single.success", entity.func_145748_c_(), tags.size(), joined);
			}
		} else if (tags.isEmpty()) {
			source.sendSuccess(legacyView(), false, "commands.tag.list.multiple.empty", targets.size());
		} else {
			source.sendSuccess(legacyView(), false, "commands.tag.list.multiple.success", targets.size(), tags.size(), joined);
		}
		return tags.size();
	}
}
