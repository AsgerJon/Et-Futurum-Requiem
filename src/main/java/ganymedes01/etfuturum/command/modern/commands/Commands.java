package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

/** What several commands need of their source. */
final class Commands {

	private static final SimpleCommandExceptionType NEEDS_ENTITY = new SimpleCommandExceptionType(ComponentMessage.translatable("permissions.requires.entity"));
	private static final SimpleCommandExceptionType NEEDS_PLAYER = new SimpleCommandExceptionType(ComponentMessage.translatable("permissions.requires.player"));

	private Commands() {
	}

	/** The entity running it ("An entity is required to run this command here" for the console). */
	static Entity requireEntity(ModernCommandSource source) throws CommandSyntaxException {
		Entity entity = source.entity();
		if (entity == null) {
			throw NEEDS_ENTITY.create();
		}
		return entity;
	}

	static EntityPlayerMP requirePlayer(ModernCommandSource source) throws CommandSyntaxException {
		if (!(source.entity() instanceof EntityPlayerMP player)) {
			throw NEEDS_PLAYER.create();
		}
		return player;
	}
}
