package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Modern's answer to a command with targets (spec §51.1): one target that worked, the single message by its name; more,
 * the multiple message with their count; none, the failure.
 */
final class Responses {

	private Responses() {
	}

	static SimpleCommandExceptionType failure(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	/** Returns how many worked. */
	static int send(ModernCommandSource source, ModernCommand command, List<? extends Entity> worked, SimpleCommandExceptionType failure,
					String singleKey, Function<Entity, Object[]> single, String multipleKey, IntFunction<Object[]> multiple) throws CommandSyntaxException {
		if (worked.isEmpty()) {
			if (failure == null) {
				return 0;
			}
			throw failure.create();
		}
		if (worked.size() == 1) {
			source.sendSuccess(command.legacyView(), true, singleKey, single.apply(worked.get(0)));
		} else {
			source.sendSuccess(command.legacyView(), true, multipleKey, multiple.apply(worked.size()));
		}
		return worked.size();
	}
}
