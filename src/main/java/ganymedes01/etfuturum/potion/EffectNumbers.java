package ganymedes01.etfuturum.potion;

import ganymedes01.etfuturum.ducks.IWideEffectPacket;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.Potion;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Effect numbers on the wire (SPEC-modern-movement.md §70.3, stage C). A number is only an effect's place in this
 * game's table, so the server sends its whole table by name when a player joins ({@link #serverTable}, the
 * `EffectTableMessage`), as 26.3 sends its registries' numbers, and the client maps the server's numbers to its own
 * ({@link #toClient}) and its own back ({@link #toServer}). Without a table (a server that sends none) a number is
 * taken as it is.
 */
public final class EffectNumbers {

	private static volatile Map<Integer, Integer> serverToClient = Collections.emptyMap();
	private static volatile Map<Integer, Integer> clientToServer = Collections.emptyMap();
	private static volatile boolean hasTable;

	private EffectNumbers() {
	}

	/** The effect number a packet that gives an effect carries, whole (`MixinS1DPacketEntityEffect`). */
	public static int effectOf(S1DPacketEntityEffect packet) {
		return ((IWideEffectPacket) packet).etfu$effect();
	}

	/** This game's effects by name and number, for a joining player. */
	public static Map<String, Integer> serverTable() {
		Map<String, Integer> table = new LinkedHashMap<>();
		for (String name : EffectRegistry.names()) {
			Potion potion = EffectRegistry.get(name);
			if (potion != null) {
				table.put(name, potion.id);
			}
		}
		return table;
	}

	/** The client takes the server's table: each server number to the number this client's effect of that name has. */
	public static void applyServerTable(Map<String, Integer> table) {
		Map<Integer, Integer> toClient = new HashMap<>();
		Map<Integer, Integer> toServer = new HashMap<>();
		for (Map.Entry<String, Integer> entry : table.entrySet()) {
			Potion potion = EffectRegistry.get(entry.getKey());
			if (potion != null) {
				toClient.put(entry.getValue(), potion.id);
				toServer.put(potion.id, entry.getValue());
			}
		}
		serverToClient = toClient;
		clientToServer = toServer;
		hasTable = true;
	}

	public static boolean hasServerTable() {
		return hasTable;
	}

	/** Leaving a server: its table goes. */
	public static void forget() {
		serverToClient = Collections.emptyMap();
		clientToServer = Collections.emptyMap();
		hasTable = false;
	}

	public static int toClient(int server) {
		Integer client = serverToClient.get(server);
		return client == null ? server : client;
	}

	public static int toServer(int client) {
		Integer server = clientToServer.get(client);
		return server == null ? client : server;
	}
}
