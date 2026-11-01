package ganymedes01.etfuturum.command.modern;

import com.github.bsideup.jabel.Desugar;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * The command tree in its network form (spec §40.1): every node once, breadth first from the root, each with its
 * kind, whether input ending there runs, its children and redirect by index, a literal's text or an argument's name
 * and type, whether the argument's suggestions come from the server, and (ours) the usage text of a wrapped 1.7.10
 * command.
 */
public final class CommandTree {

	private static final int KIND_MASK = 3;
	private static final int ROOT = 0;
	private static final int LITERAL = 1;
	private static final int ARGUMENT = 2;
	private static final int EXECUTABLE = 4;
	private static final int REDIRECT = 8;
	private static final int ASK_SERVER = 16;
	private static final int USAGE = 64;

	private CommandTree() {
	}

	public static <S> void write(RootCommandNode<S> root, Map<CommandNode<S>, String> usage, ByteBuf buf) {
		Map<CommandNode<S>, Integer> index = new IdentityHashMap<>();
		List<CommandNode<S>> nodes = new ArrayList<>();
		Queue<CommandNode<S>> queue = new ArrayDeque<>();
		queue.add(root);
		CommandNode<S> node;
		while ((node = queue.poll()) != null) {
			if (!index.containsKey(node)) {
				index.put(node, nodes.size());
				nodes.add(node);
				queue.addAll(node.getChildren());
				if (node.getRedirect() != null) {
					queue.add(node.getRedirect());
				}
			}
		}

		ByteBufUtils.writeVarInt(buf, nodes.size(), 5);
		for (CommandNode<S> n : nodes) {
			String usageText = usage.get(n);
			int flags = n instanceof RootCommandNode ? ROOT : n instanceof LiteralCommandNode ? LITERAL : ARGUMENT;
			if (n.getCommand() != null) flags |= EXECUTABLE;
			if (n.getRedirect() != null) flags |= REDIRECT;
			if (n instanceof ArgumentCommandNode<?, ?> argument && argument.getCustomSuggestions() != null) flags |= ASK_SERVER;
			if (usageText != null) flags |= USAGE;
			buf.writeByte(flags);
			ByteBufUtils.writeVarInt(buf, n.getChildren().size(), 5);
			for (CommandNode<S> child : n.getChildren()) {
				ByteBufUtils.writeVarInt(buf, index.get(child), 5);
			}
			if (n.getRedirect() != null) {
				ByteBufUtils.writeVarInt(buf, index.get(n.getRedirect()), 5);
			}
			if (n instanceof LiteralCommandNode<?> literal) {
				ByteBufUtils.writeUTF8String(buf, literal.getLiteral());
			} else if (n instanceof ArgumentCommandNode<?, ?> argument) {
				ByteBufUtils.writeUTF8String(buf, argument.getName());
				ArgumentTypes.write(argument.getType(), buf);
			}
			if (usageText != null) {
				ByteBufUtils.writeUTF8String(buf, usageText);
			}
		}
		ByteBufUtils.writeVarInt(buf, index.get(root), 5);
	}

	/** A tree read back: its root, and the usage texts of the wrapped commands by their literal. */
	@Desugar
	public record Decoded<S>(RootCommandNode<S> root, Map<CommandNode<S>, String> usage) {
	}

	/**
	 * Builds the tree for the client's source type: nodes that run get {@code executable} (the server runs the
	 * command), arguments with server suggestions get {@code askServer}.
	 */
	public static <S> Decoded<S> read(ByteBuf buf, Command<S> executable, SuggestionProvider<S> askServer) {
		int count = ByteBufUtils.readVarInt(buf, 5);
		Entry[] entries = new Entry[count];
		for (int i = 0; i < count; i++) {
			Entry e = new Entry();
			e.flags = buf.readByte();
			e.children = new int[ByteBufUtils.readVarInt(buf, 5)];
			for (int c = 0; c < e.children.length; c++) {
				e.children[c] = ByteBufUtils.readVarInt(buf, 5);
			}
			e.redirect = (e.flags & REDIRECT) != 0 ? ByteBufUtils.readVarInt(buf, 5) : -1;
			int kind = e.flags & KIND_MASK;
			if (kind == LITERAL || kind == ARGUMENT) {
				e.name = ByteBufUtils.readUTF8String(buf);
			}
			if (kind == ARGUMENT) {
				e.type = ArgumentTypes.read(buf);
			}
			if ((e.flags & USAGE) != 0) {
				e.usage = ByteBufUtils.readUTF8String(buf);
			}
			entries[i] = e;
		}
		int rootIndex = ByteBufUtils.readVarInt(buf, 5);

		// A redirect is fixed when its node is built, so a node waits for its redirect's target; children come after
		List<CommandNode<S>> nodes = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			nodes.add(null);
		}
		int left = count;
		while (left > 0) {
			boolean built = false;
			for (int i = 0; i < count; i++) {
				Entry e = entries[i];
				if (nodes.get(i) == null && (e.redirect < 0 || nodes.get(e.redirect) != null)) {
					nodes.set(i, build(e, e.redirect < 0 ? null : nodes.get(e.redirect), executable, askServer));
					left--;
					built = true;
				}
			}
			if (!built) {
				throw new IllegalStateException("The server sent an impossible command tree");
			}
		}

		Map<CommandNode<S>, String> usage = new IdentityHashMap<>();
		for (int i = 0; i < count; i++) {
			for (int child : entries[i].children) {
				nodes.get(i).addChild(nodes.get(child));
			}
			if (entries[i].usage != null) {
				usage.put(nodes.get(i), entries[i].usage);
			}
		}
		// a pattern with <S> isn't allowed at the build's Java 8 language level
		CommandNode<S> root = nodes.get(rootIndex);
		if (!(root instanceof RootCommandNode)) {
			throw new IllegalStateException("The server's command tree has no root");
		}
		return new Decoded<>((RootCommandNode<S>) root, usage);
	}

	private static <S> CommandNode<S> build(Entry e, CommandNode<S> redirect, Command<S> executable, SuggestionProvider<S> askServer) {
		ArgumentBuilder<S, ?> builder;
		switch (e.flags & KIND_MASK) {
			case ROOT:
				return new RootCommandNode<>();
			case LITERAL:
				builder = LiteralArgumentBuilder.literal(e.name);
				break;
			default:
				RequiredArgumentBuilder<S, ?> argument = RequiredArgumentBuilder.argument(e.name, e.type);
				if ((e.flags & ASK_SERVER) != 0) {
					argument.suggests(askServer);
				}
				builder = argument;
		}
		if ((e.flags & EXECUTABLE) != 0) {
			builder.executes(executable);
		}
		if (redirect != null) {
			builder.redirect(redirect);
		}
		return builder.build();
	}

	private static final class Entry {
		int flags;
		int[] children;
		int redirect;
		String name;
		ArgumentType<?> type;
		String usage;
	}
}
