package ganymedes01.etfuturum.client.chat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.command.modern.CommandTree;
import ganymedes01.etfuturum.command.modern.LegacyCommands;
import ganymedes01.etfuturum.lib.Reference;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The client's copy of the command tree (spec §40.5): the server's tree for this player, with Forge's client
 * commands in front of it (a client command of the same name shadows the server's, as it runs first). Until a tree
 * arrives (a server without EFR), there is none and the chat stays 1.7.10's.
 */
@SideOnly(Side.CLIENT)
public final class ClientCommands {

	private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
	private static final ClientCommandSource SOURCE = new ClientCommandSource();
	/** The server or Forge runs the command; the client's tree only needs to know that input ending here runs. */
	private static final Command<ClientCommandSource> RUN_ELSEWHERE = context -> 0;

	/** Set and dropped from the network thread too (a disconnect), so read once where it is used. */
	private static volatile CommandDispatcher<ClientCommandSource> dispatcher;
	private static volatile Map<CommandNode<ClientCommandSource>, String> usage = new IdentityHashMap<>();

	private ClientCommands() {
	}

	public static boolean active() {
		return dispatcher != null;
	}

	public static CommandDispatcher<ClientCommandSource> dispatcher() {
		return dispatcher;
	}

	public static ClientCommandSource source() {
		return SOURCE;
	}

	/** The translated usage of a wrapped 1.7.10 command, by its literal; null for any other node. */
	public static String usage(CommandNode<ClientCommandSource> node) {
		String key = usage.get(node);
		return key == null ? null : StatCollector.translateToLocal(key);
	}

	public static void receiveTree(byte[] bytes) {
		try {
			CommandTree.Decoded<ClientCommandSource> tree = CommandTree.read(Unpooled.wrappedBuffer(bytes), RUN_ELSEWHERE,
				(context, builder) -> context.getSource().askServer(context));
			RootCommandNode<ClientCommandSource> root = new RootCommandNode<>();
			Map<CommandNode<ClientCommandSource>, String> usages = new IdentityHashMap<>(tree.usage());
			Map<String, ICommand> clientCommands = ClientCommandHandler.instance.getCommands();
			ICommandSender player = Minecraft.getMinecraft().thePlayer;
			for (Map.Entry<String, ICommand> entry : clientCommands.entrySet()) {
				ICommand command = entry.getValue();
				if (LegacyCommands.isWrappable(entry.getKey())) {
					LiteralCommandNode<ClientCommandSource> literal = LegacyCommands.<ClientCommandSource>wrap(entry.getKey(), command,
						source -> Minecraft.getMinecraft().thePlayer, source -> true, RUN_ELSEWHERE).build();
					root.addChild(literal);
					String text = player == null ? null : LegacyCommands.usage(command, player);
					if (text != null) {
						usages.put(literal, text);
					}
				}
			}
			for (CommandNode<ClientCommandSource> child : tree.root().getChildren()) {
				if (!clientCommands.containsKey(child.getName())) {
					root.addChild(child);
				}
			}
			usage = usages;
			dispatcher = new CommandDispatcher<>(root);
		} catch (RuntimeException e) {
			LOGGER.error("Could not read the server's command tree; the chat keeps 1.7.10's completion", e);
			forget();
		}
	}

	/** The next server sends its own tree. */
	public static void forget() {
		dispatcher = null;
		usage = new IdentityHashMap<>();
	}
}
