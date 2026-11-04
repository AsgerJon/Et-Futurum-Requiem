package ganymedes01.etfuturum.client.chat;

import io.github.asgerjon.mcfly.network.McFlyNetwork;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.SuggestionSource;
import io.github.asgerjon.mcfly.network.CommandSuggestionRequestMessage;
import io.github.asgerjon.mcfly.network.CommandSuggestionsMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** The client's command source: what suggestions need to know, and the requests for the server's (spec §40.1). */
@SideOnly(Side.CLIENT)
public final class ClientCommandSource implements SuggestionSource {

	private CompletableFuture<Suggestions> pending;
	private int pendingId;

	/** Asks the server for the suggestions at the end of the input; a newer request ends the wait for the older. */
	public CompletableFuture<Suggestions> askServer(CommandContext<?> context) {
		if (pending != null) {
			pending.cancel(false);
		}
		pending = new CompletableFuture<>();
		int id = ++pendingId;
		McFlyNetwork.CHANNEL.sendToServer(new CommandSuggestionRequestMessage(id, context.getInput()));
		return pending;
	}

	public void completeServerSuggestions(CommandSuggestionsMessage message) {
		if (message.id != pendingId || pending == null) {
			return;
		}
		StringRange range = StringRange.between(message.start, message.start + message.length);
		List<Suggestion> list = new ArrayList<>();
		for (int i = 0; i < message.texts.size(); i++) {
			String tooltip = message.tooltips.get(i);
			list.add(new Suggestion(range, message.texts.get(i), tooltip == null ? null
				: new ComponentMessage(IChatComponent.Serializer.func_150699_a(tooltip))));
		}
		CompletableFuture<Suggestions> answered = pending;
		pending = null;
		answered.complete(new Suggestions(range, list));
	}

	/** The client doesn't know its permission level (1.7.10 never sends it); the server checks when the command runs. */
	@Override
	public boolean allowsSelectors() {
		return true;
	}

	/** The block in the crosshair, as modern's client suggests it for positions; else ~ ~ ~. */
	@Override
	public String[] absoluteCoordinates() {
		MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
		if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			return new String[]{String.valueOf(hit.blockX), String.valueOf(hit.blockY), String.valueOf(hit.blockZ)};
		}
		return SuggestionSource.super.absoluteCoordinates();
	}

	/** The names in the tab list. */
	@Override
	public List<String> onlinePlayerNames() {
		List<String> names = new ArrayList<>();
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer != null) {
			for (GuiPlayerInfo info : mc.thePlayer.sendQueue.playerInfoList) {
				names.add(info.name);
			}
		}
		return names;
	}
}
