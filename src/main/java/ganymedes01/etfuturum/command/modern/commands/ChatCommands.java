package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.ComponentArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.arguments.MessageArgument;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

/** Modern's /say, /me, /msg (with /tell and /w) and /tellraw (spec §51.1). */
public final class ChatCommands {

	private ChatCommands() {
	}

	/** `[name] message` to everyone. */
	public static final class Say extends ModernCommand {
		public Say() {
			super("say", 2);
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("say")
				.requires(requirement())
				.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("message", MessageArgument.message())
					.executes(c -> {
						IChatComponent message = MessageArgument.getMessage(c, "message");
						c.getSource().server().getConfigurationManager().sendChatMsg(
							new ChatComponentTranslation("chat.type.announcement", c.getSource().sender().func_145748_c_(), message));
						return 1;
					})));
		}
	}

	/** `* name action` to everyone. */
	public static final class Me extends ModernCommand {
		public Me() {
			super("me", 0);
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("me")
				.requires(requirement())
				.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("action", MessageArgument.message())
					.executes(c -> {
						IChatComponent action = MessageArgument.getMessage(c, "action");
						c.getSource().server().getConfigurationManager().sendChatMsg(
							new ChatComponentTranslation("chat.type.emote", c.getSource().sender().func_145748_c_(), action));
						return 1;
					})));
		}
	}

	/** A whisper to each target, grey and italic, and word of it to the source. */
	public static final class Msg extends ModernCommand {
		public Msg() {
			super("msg", 0);
		}

		@Override
		public java.util.List<String> aliases() {
			return java.util.Arrays.asList("tell", "w");
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			LiteralCommandNode<ModernCommandSource> msg = dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("msg")
				.requires(requirement())
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.players())
					.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("message", MessageArgument.message())
						.executes(c -> {
							IChatComponent message = MessageArgument.getMessage(c, "message");
							int count = 0;
							for (EntityPlayerMP target : EntityArgument.getPlayers(c, "targets")) {
								target.addChatMessage(whisper("commands.message.display.incoming", c.getSource().sender().func_145748_c_(), message));
								c.getSource().sender().addChatMessage(whisper("commands.message.display.outgoing", target.func_145748_c_(), message));
								count++;
							}
							return count;
						}))));
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("tell").requires(requirement()).redirect(msg));
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("w").requires(requirement()).redirect(msg));
		}

		private static IChatComponent whisper(String key, IChatComponent name, IChatComponent message) {
			IChatComponent whisper = new ChatComponentTranslation(key, name, message.createCopy());
			whisper.getChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true);
			return whisper;
		}
	}

	/** A text component to each target. */
	public static final class TellRaw extends ModernCommand {
		public TellRaw() {
			super("tellraw", 2);
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("tellraw")
				.requires(requirement())
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("targets", EntityArgument.players())
					.then(RequiredArgumentBuilder.<ModernCommandSource, IChatComponent>argument("message", ComponentArgument.textComponent())
						.executes(c -> {
							int count = 0;
							for (EntityPlayerMP target : EntityArgument.getPlayers(c, "targets")) {
								target.addChatMessage(ComponentArgument.getComponent(c, "message").createCopy());
								count++;
							}
							return count;
						}))));
		}
	}
}
