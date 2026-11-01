package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import java.util.Collections;
import java.util.List;

/** Modern's /kill [targets] (spec §51.1): living things die (creative players too), the rest are removed. */
public final class KillCommand extends ModernCommand {

	public KillCommand() {
		super("kill", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("kill")
			.requires(requirement())
			.executes(c -> kill(c.getSource(), Collections.singletonList(Commands.requireEntity(c.getSource()))))
			.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.entities())
				.executes(c -> kill(c.getSource(), EntityArgument.getEntities(c, "targets")))));
	}

	private int kill(ModernCommandSource source, List<? extends Entity> targets) throws CommandSyntaxException {
		for (Entity entity : targets) {
			if (entity instanceof EntityLivingBase) {
				entity.attackEntityFrom(DamageSource.outOfWorld, Float.MAX_VALUE);
			} else {
				entity.setDead();
			}
		}
		return Responses.send(source, this, targets, null,
			"commands.kill.success.single", e -> new Object[]{e.func_145748_c_()},
			"commands.kill.success.multiple", n -> new Object[]{n});
	}
}
