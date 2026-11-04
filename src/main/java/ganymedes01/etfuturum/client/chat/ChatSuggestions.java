package ganymedes01.etfuturum.client.chat;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.Suggest;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modern's chat input behaviour for 1.7.10's chat screen (spec §40.3): the coloured command, the suggestion list as
 * you type with its grey preview, the usage and error lines, and the keys and mouse on the list.
 */
@SideOnly(Side.CLIENT)
public final class ChatSuggestions {

	private static final Pattern WHITESPACE = Pattern.compile("(\\s+)");
	private static final String LITERAL = EnumChatFormatting.GRAY.toString();
	private static final String UNPARSED = EnumChatFormatting.RED.toString();
	private static final String[] ARGUMENT_COLOURS = {EnumChatFormatting.AQUA.toString(), EnumChatFormatting.YELLOW.toString(),
		EnumChatFormatting.GREEN.toString(), EnumChatFormatting.LIGHT_PURPLE.toString(), EnumChatFormatting.GOLD.toString()};
	private static final int LINE_HEIGHT = 12;
	private static final int USAGE_OFFSET_FROM_BOTTOM = 27;
	private static final int LINE_LIMIT = 10;
	/** How many rows the view keeps below the selection when it follows the selection down. */
	private static final int LINE_START_OFFSET = 1;
	private static final int FILL = 0xD0000000;
	private static final int SELECTED = 0xFFFFFF00;
	private static final int UNSELECTED = 0xFFAAAAAA;

	public interface TooltipDrawer {
		void draw(List<String> lines, int x, int y);
	}

	/** The tree when the screen opened: a disconnect may drop the current one while the screen still draws. */
	private final CommandDispatcher<ClientCommandSource> commands;
	private final GuiScreen screen;
	private final GuiTextField input;
	private final FontRenderer font;
	private final TooltipDrawer tooltips;
	private final List<String> usage = new ArrayList<>();
	private int usageX;
	private int usageWidth;
	private ParseResults<ClientCommandSource> parse;
	private CompletableFuture<Suggestions> pending;
	private SuggestionsList list;
	private boolean allowSuggestions;
	private boolean keepSuggestions;

	public ChatSuggestions(CommandDispatcher<ClientCommandSource> commands, GuiScreen screen, GuiTextField input, FontRenderer font, TooltipDrawer tooltips) {
		this.commands = commands;
		this.screen = screen;
		this.input = input;
		this.font = font;
		this.tooltips = tooltips;
		((IChatInputField) input).etfu$setFormatter(this::format);
	}

	public void setAllowSuggestions(boolean allowSuggestions) {
		this.allowSuggestions = allowSuggestions;
		if (!allowSuggestions) {
			list = null;
		}
	}

	/** The text changed: modern's screen lets the list open from then on. */
	public void edited() {
		setAllowSuggestions(true);
		update();
	}

	/** Keys for the list first; Tab opens it when it is closed. True when the key was used. */
	public boolean keyPressed(int keyCode) {
		if (list != null && list.keyPressed(keyCode)) {
			return true;
		}
		if (keyCode != Keyboard.KEY_TAB) {
			return false;
		}
		showSuggestions();
		return true;
	}

	public boolean mouseScrolled(int direction, int mouseX, int mouseY) {
		return list != null && list.mouseScrolled(direction, mouseX, mouseY);
	}

	public boolean mouseClicked(int mouseX, int mouseY) {
		return list != null && list.mouseClicked(mouseX, mouseY);
	}

	public void showSuggestions() {
		if (pending == null || !pending.isDone()) {
			return;
		}
		Suggestions suggestions;
		try {
			suggestions = pending.join();
		} catch (RuntimeException e) {
			return;
		}
		if (!suggestions.isEmpty()) {
			int width = 0;
			for (Suggestion suggestion : suggestions.getList()) {
				width = Math.max(width, font.getStringWidth(suggestion.getText()));
			}
			int x = MathHelper.clamp_int(screenX(suggestions.getRange().getStart()), 0, screenX(0) + input.getWidth() - width);
			list = new SuggestionsList(x, screen.height - LINE_HEIGHT, width, sorted(suggestions));
		}
	}

	/** The open list's entries in their shown order, empty while it is closed (unitTAS reads it). */
	public List<String> shownSuggestions() {
		List<String> shown = new ArrayList<>();
		if (list != null) {
			for (Suggestion suggestion : list.suggestions) {
				shown.add(suggestion.getText());
			}
		}
		return shown;
	}

	/** The usage or error lines above the input, without formatting codes (unitTAS reads them). */
	public List<String> usageLines() {
		List<String> lines = new ArrayList<>();
		for (String line : usage) {
			lines.add(EnumChatFormatting.getTextWithoutFormattingCodes(line));
		}
		return lines;
	}

	/** Entries starting with the word at the cursor first, the rest after, each group in Brigadier's order. */
	private List<Suggestion> sorted(Suggestions suggestions) {
		String partial = input.getText().substring(0, input.getCursorPosition());
		String lastWord = partial.substring(lastWordIndex(partial)).toLowerCase(Locale.ROOT);
		List<Suggestion> matching = new ArrayList<>();
		List<Suggestion> others = new ArrayList<>();
		for (Suggestion suggestion : suggestions.getList()) {
			if (suggestion.getText().startsWith(lastWord) || suggestion.getText().startsWith("minecraft:" + lastWord)) {
				matching.add(suggestion);
			} else {
				others.add(suggestion);
			}
		}
		matching.addAll(others);
		return matching;
	}

	/** Works out the parse, the suggestions and the usage for the text and cursor (modern runs it on every edit). */
	public void update() {
		String text = input.getText();
		if (parse != null && !parse.getReader().getString().equals(text)) {
			parse = null;
		}
		if (!keepSuggestions) {
			((IChatInputField) input).etfu$setSuggestion(null);
			list = null;
		}
		usage.clear();
		StringReader reader = new StringReader(text);
		boolean command = reader.canRead() && reader.peek() == '/';
		if (command) {
			reader.skip();
		}
		int cursor = input.getCursorPosition();
		if (command) {
			if (parse == null) {
				parse = commands.parse(reader, ClientCommands.source());
			}
			if (cursor >= 1 && (list == null || !keepSuggestions)) {
				CompletableFuture<Suggestions> future = commands.getCompletionSuggestions(parse, cursor);
				pending = future;
				future.thenAccept(result -> {
					if (pending == future && parse != null) {
						updateUsage(parse, result);
					}
				});
			}
		} else if (!text.trim().isEmpty()) {
			String partial = text.substring(0, cursor);
			pending = Suggest.matching(ClientCommands.source().onlinePlayerNames(), new SuggestionsBuilder(partial, lastWordIndex(partial)));
			recomputeUsageWidth();
			usageX = 0;
		} else {
			pending = null;
		}
	}

	private void updateUsage(ParseResults<ClientCommandSource> parse, Suggestions suggestions) {
		boolean trailingCharacters = false;
		if (input.getCursorPosition() == input.getText().length()) {
			if (suggestions.isEmpty() && !parse.getExceptions().isEmpty()) {
				int literals = 0;
				for (CommandSyntaxException exception : parse.getExceptions().values()) {
					if (exception.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
						literals++;
					} else {
						usage.add(exceptionMessage(exception));
					}
				}
				if (literals > 0) {
					usage.add(exceptionMessage(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader())));
				}
			} else if (parse.getReader().canRead()) {
				trailingCharacters = true;
			}
		}

		SuggestionContext<ClientCommandSource> context = parse.getContext().findSuggestionContext(input.getCursorPosition());
		if (usage.isEmpty()) {
			List<String> entries = nodeUsage(context.parent);
			if (entries.isEmpty() && trailingCharacters) {
				CommandSyntaxException exception = parseException(parse);
				if (exception != null) {
					usage.add(exceptionMessage(exception));
				}
			}
			usage.addAll(entries);
		}
		recomputeUsageWidth();
		usageX = usage.isEmpty() ? 0 : MathHelper.clamp_int(screenX(context.startPos), 0, screenX(0) + input.getWidth() - usageWidth);
		list = null;
		if (allowSuggestions && McFlyConfig.commandSuggestions) {
			showSuggestions();
		}
	}

	/** One grey line per argument below the node: a wrapped 1.7.10 command shows its own usage instead. */
	private List<String> nodeUsage(CommandNode<ClientCommandSource> parent) {
		List<String> lines = new ArrayList<>();
		String legacy = ClientCommands.usage(parent);
		if (legacy != null) {
			lines.add(LITERAL + legacy);
			return lines;
		}
		Map<CommandNode<ClientCommandSource>, String> smart = commands.getSmartUsage(parent, ClientCommands.source());
		for (Map.Entry<CommandNode<ClientCommandSource>, String> entry : smart.entrySet()) {
			if (!(entry.getKey() instanceof LiteralCommandNode)) {
				lines.add(LITERAL + entry.getValue());
			}
		}
		return lines;
	}

	/** Why the whole input doesn't parse, as modern words it; null when it does. */
	private static CommandSyntaxException parseException(ParseResults<ClientCommandSource> parse) {
		if (!parse.getReader().canRead()) {
			return null;
		}
		if (parse.getExceptions().size() == 1) {
			return parse.getExceptions().values().iterator().next();
		}
		return parse.getContext().getRange().isEmpty()
			? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader())
			: CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader());
	}

	private static String exceptionMessage(CommandSyntaxException e) {
		IChatComponent message = ComponentMessage.toComponent(e.getRawMessage());
		String context = e.getContext();
		return context == null ? message.getFormattedText()
			: new ChatComponentTranslation("command.context.parse_error", message, e.getCursor(), context).getFormattedText();
	}

	private void recomputeUsageWidth() {
		int longest = 0;
		for (String line : usage) {
			longest = Math.max(longest, font.getStringWidth(line));
		}
		usageWidth = longest;
	}

	/** Where character {@code index} of the text sits on screen, ignoring the scroll, as modern measures it. */
	private int screenX(int index) {
		String text = input.getText();
		return index > text.length() ? input.xPosition : input.xPosition + font.getStringWidth(text.substring(0, index));
	}

	private static int lastWordIndex(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		int result = 0;
		Matcher matcher = WHITESPACE.matcher(text);
		while (matcher.find()) {
			result = matcher.end();
		}
		return result;
	}

	/** The command coloured: literals grey, each argument in turn in the five colours, unread text red; null otherwise. */
	private String format(String text, int offset) {
		if (parse == null || !parse.getReader().getString().equals(input.getText())) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		int unformattedStart = 0;
		int nextColour = -1;
		CommandContextBuilder<ClientCommandSource> context = parse.getContext().getLastChild();
		for (ParsedArgument<ClientCommandSource, ?> argument : context.getArguments().values()) {
			if (++nextColour >= ARGUMENT_COLOURS.length) {
				nextColour = 0;
			}
			int start = Math.max(argument.getRange().getStart() - offset, 0);
			if (start >= text.length()) {
				break;
			}
			int end = Math.min(argument.getRange().getEnd() - offset, text.length());
			if (end > 0 && start >= unformattedStart) {
				out.append(LITERAL).append(text, unformattedStart, start);
				out.append(ARGUMENT_COLOURS[nextColour]).append(text, start, end);
				unformattedStart = end;
			}
		}
		if (parse.getReader().canRead()) {
			int start = Math.max(parse.getReader().getCursor() - offset, 0);
			if (start < text.length() && start >= unformattedStart) {
				int end = Math.min(start + parse.getReader().getRemainingLength(), text.length());
				out.append(LITERAL).append(text, unformattedStart, start);
				out.append(UNPARSED).append(text, start, end);
				unformattedStart = end;
			}
		}
		out.append(LITERAL).append(text.substring(unformattedStart));
		return out.toString();
	}

	/** After the screen: the list when it is open, else the usage and error lines. */
	public void render(int mouseX, int mouseY) {
		if (list != null) {
			list.render(mouseX, mouseY);
			return;
		}
		int line = 0;
		for (String text : usage) {
			int y = screen.height - USAGE_OFFSET_FROM_BOTTOM - LINE_HEIGHT * line;
			Gui.drawRect(usageX - 1, y, usageX + usageWidth + 1, y + LINE_HEIGHT, FILL);
			font.drawStringWithShadow(text, usageX, y + 2, -1);
			line++;
		}
	}

	private final class SuggestionsList {
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final String originalContents;
		private final List<Suggestion> suggestions;
		private int offset;
		private int current;
		private int lastMouseX = Integer.MIN_VALUE;
		private int lastMouseY = Integer.MIN_VALUE;
		private boolean tabCycles;

		private SuggestionsList(int x, int bottom, int width, List<Suggestion> suggestions) {
			int rows = Math.min(suggestions.size(), LINE_LIMIT);
			this.x = x - 1;
			y = bottom - 3 - rows * LINE_HEIGHT;
			this.width = width + 1;
			height = rows * LINE_HEIGHT;
			originalContents = input.getText();
			this.suggestions = suggestions;
			select(0);
		}

		private boolean contains(int mouseX, int mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
		}

		private void render(int mouseX, int mouseY) {
			int rows = Math.min(suggestions.size(), LINE_LIMIT);
			boolean hasPrevious = offset > 0;
			boolean hasNext = suggestions.size() > offset + rows;
			boolean mouseMoved = lastMouseX != mouseX || lastMouseY != mouseY;
			if (mouseMoved) {
				lastMouseX = mouseX;
				lastMouseY = mouseY;
			}
			if (hasPrevious || hasNext) {
				Gui.drawRect(x, y - 1, x + width, y, FILL);
				Gui.drawRect(x, y + height, x + width, y + height + 1, FILL);
				for (int i = 0; i < width; i += 2) {
					if (hasPrevious) {
						Gui.drawRect(x + i, y - 1, x + i + 1, y, -1);
					}
					if (hasNext) {
						Gui.drawRect(x + i, y + height, x + i + 1, y + height + 1, -1);
					}
				}
			}
			boolean hovered = false;
			for (int i = 0; i < rows; i++) {
				Suggestion suggestion = suggestions.get(i + offset);
				int rowY = y + LINE_HEIGHT * i;
				Gui.drawRect(x, rowY, x + width, rowY + LINE_HEIGHT, FILL);
				if (mouseX > x && mouseX < x + width && mouseY > rowY && mouseY < rowY + LINE_HEIGHT) {
					if (mouseMoved) {
						select(i + offset);
					}
					hovered = true;
				}
				font.drawStringWithShadow(suggestion.getText(), x + 1, rowY + 2, i + offset == current ? SELECTED : UNSELECTED);
			}
			if (hovered) {
				Message tooltip = suggestions.get(current).getTooltip();
				if (tooltip != null) {
					tooltips.draw(Collections.singletonList(ComponentMessage.toComponent(tooltip).getFormattedText()), mouseX, mouseY);
				}
			}
		}

		private boolean mouseClicked(int mouseX, int mouseY) {
			if (!contains(mouseX, mouseY)) {
				return false;
			}
			int row = (mouseY - y) / LINE_HEIGHT + offset;
			if (row >= 0 && row < suggestions.size()) {
				select(row);
				use();
			}
			return true;
		}

		private boolean mouseScrolled(int direction, int mouseX, int mouseY) {
			if (!contains(mouseX, mouseY)) {
				return false;
			}
			offset = MathHelper.clamp_int(offset - direction, 0, Math.max(suggestions.size() - LINE_LIMIT, 0));
			return true;
		}

		private boolean keyPressed(int keyCode) {
			switch (keyCode) {
				case Keyboard.KEY_UP:
					cycle(-1);
					tabCycles = false;
					return true;
				case Keyboard.KEY_DOWN:
					cycle(1);
					tabCycles = false;
					return true;
				case Keyboard.KEY_TAB:
					if (tabCycles) {
						cycle(GuiScreen.isShiftKeyDown() ? -1 : 1);
					}
					use();
					return true;
				case Keyboard.KEY_ESCAPE:
					list = null;
					((IChatInputField) input).etfu$setSuggestion(null);
					return true;
				default:
					return false;
			}
		}

		private void cycle(int direction) {
			select(current + direction);
			int first = offset;
			int last = offset + LINE_LIMIT - 1;
			int maxOffset = Math.max(suggestions.size() - LINE_LIMIT, 0);
			if (current < first) {
				offset = MathHelper.clamp_int(current, 0, maxOffset);
			} else if (current > last) {
				offset = MathHelper.clamp_int(current + LINE_START_OFFSET - LINE_LIMIT, 0, maxOffset);
			}
		}

		private void select(int index) {
			current = index;
			if (current < 0) {
				current += suggestions.size();
			}
			if (current >= suggestions.size()) {
				current -= suggestions.size();
			}
			String applied = suggestions.get(current).apply(originalContents);
			String typed = input.getText();
			((IChatInputField) input).etfu$setSuggestion(applied.startsWith(typed) ? applied.substring(typed.length()) : null);
		}

		/** Takes the selected entry; the same list stays for the next Tab. */
		private void use() {
			Suggestion suggestion = suggestions.get(current);
			keepSuggestions = true;
			input.setText(suggestion.apply(originalContents));
			input.setCursorPosition(suggestion.getRange().getStart() + suggestion.getText().length());
			edited();
			select(current);
			keepSuggestions = false;
			tabCycles = true;
		}
	}
}
