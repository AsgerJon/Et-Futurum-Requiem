package ganymedes01.etfuturum.command.modern;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** A command EFR has in modern form (spec §41.1): its Brigadier tree, and how 1.7.10 sees it. */
public abstract class ModernCommand {

	private final String name;
	private final int permissionLevel;
	private final ICommand legacyView;

	protected ModernCommand(String name, int permissionLevel) {
		this.name = name;
		this.permissionLevel = permissionLevel;
		legacyView = new LegacyView(name, permissionLevel);
	}

	public String name() {
		return name;
	}

	/** The command as 1.7.10 code expects one: for feedback to operators and for Forge's CommandEvent. */
	public ICommand legacyView() {
		return legacyView;
	}

	protected Predicate<ModernCommandSource> requirement() {
		return source -> source.hasPermission(permissionLevel, name);
	}

	public abstract void register(CommandDispatcher<ModernCommandSource> dispatcher);

	/** Other names the command answers to (`tp` for teleport); they redirect to it in the tree. */
	public List<String> aliases() {
		return Collections.emptyList();
	}

	/**
	 * Whether 1.7.10's form of the command could take these words (the ones after the name). When the modern parse
	 * fails and it couldn't, the modern error shows at once instead of 1.7.10's own complaint.
	 */
	public boolean legacyCouldRun(String[] args) {
		return true;
	}

	private static final class LegacyView extends CommandBase {
		private final String name;
		private final int permissionLevel;

		private LegacyView(String name, int permissionLevel) {
			this.name = name;
			this.permissionLevel = permissionLevel;
		}

		@Override
		public String getCommandName() {
			return name;
		}

		@Override
		public int getRequiredPermissionLevel() {
			return permissionLevel;
		}

		@Override
		public String getCommandUsage(ICommandSender sender) {
			return "/" + name;
		}

		/** Never: ModernCommands runs the modern form itself. */
		@Override
		public void processCommand(ICommandSender sender, String[] args) {
			throw new UnsupportedOperationException("/" + name + " runs through Brigadier");
		}
	}
}
