package ganymedes01.etfuturum.skins;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Skins for the players a server does not authenticate (offline mode, a development single-player): the typed name
 * is looked up on Mojang's public API and that account's signed skin data goes on the player's otherwise offline
 * profile. Every client then draws the skin as it would on an online server, the signature being Mojang's own; the
 * player's own client gets the data in an {@code OfflineSkinMessage}. For test servers: any name wears the skin of
 * the account of that name. Answers are kept for a day in etfuturum_offline_skins.json next to the server's world,
 * which also keeps Mojang's limit of one request per profile per minute out of the way across restarts.
 * ConfigMixins.offlineSkins.
 */
public final class OfflineSkins {

	private static final String NAME_TO_ID = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String NAME_TO_ID_FALLBACK = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
	private static final String ID_TO_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
	private static final String CACHE_FILE = "etfuturum_offline_skins.json";
	private static final long FRESH_FOR = 24L * 60 * 60 * 1000;
	private static final int TIMEOUT = 4000;

	private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "EFR offline skin lookup");
		thread.setDaemon(true);
		return thread;
	});
	/** Lower-cased name to the answer; guarded by itself. */
	private static final Map<String, Entry> CACHE = new HashMap<>();
	private static boolean cacheLoaded;
	/** The profiles this server put skin data on; server thread only. */
	private static final Set<GameProfile> RESOLVED = Collections.newSetFromMap(new WeakHashMap<>());

	private OfflineSkins() {
	}

	/** Starts looking up a name off the calling thread. */
	public static Future<Property> lookUp(String name) {
		return POOL.submit(() -> fetch(name));
	}

	/** The signed textures property a finished lookup found, or null: no account, no skin, or a failure (logged). */
	public static Property result(Future<Property> lookup, String name) {
		try {
			return lookup.get();
		} catch (ExecutionException e) {
			Logger.warn("Could not look up the skin of " + name + ", the default one stays: " + e.getCause());
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public static void markResolved(GameProfile profile) {
		RESOLVED.add(profile);
	}

	public static boolean wasResolved(GameProfile profile) {
		return RESOLVED.contains(profile);
	}

	/** The signed textures property of a profile, if it carries one. */
	public static Property textures(GameProfile profile) {
		return profile == null ? null : Iterables.getFirst(profile.getProperties().get("textures"), null);
	}

	private static Property fetch(String name) throws IOException {
		String key = name.toLowerCase(Locale.ROOT);
		Entry cached;
		synchronized (CACHE) {
			loadCache();
			cached = CACHE.get(key);
		}
		if (cached != null && cached.isFresh()) {
			return cached.textures;
		}
		try {
			Property found = fetchFromMojang(name);
			synchronized (CACHE) {
				CACHE.put(key, new Entry(found, System.currentTimeMillis()));
				saveCache();
			}
			return found;
		} catch (IOException e) {
			if (cached != null) {
				Logger.warn("Could not refresh the skin of " + name + ", using the one from " + cached.age() + " ago: " + e.getMessage());
				return cached.textures;
			}
			throw e;
		}
	}

	/** Name to account (two endpoints, Mojang has moved this one before), account to its signed skin data. */
	private static Property fetchFromMojang(String name) throws IOException {
		String encoded = URLEncoder.encode(name, "UTF-8");
		JsonObject account;
		try {
			account = getJson(NAME_TO_ID + encoded);
		} catch (IOException e) {
			account = getJson(NAME_TO_ID_FALLBACK + encoded);
		}
		if (account == null || !account.has("id")) {
			Logger.info("No Mojang account is named " + name + ": the default skin stays.");
			return null;
		}
		String id = account.get("id").getAsString();
		JsonObject profile = getJson(ID_TO_PROFILE + id + "?unsigned=false");
		if (profile != null && profile.has("properties")) {
			for (JsonElement element : profile.getAsJsonArray("properties")) {
				JsonObject property = element.getAsJsonObject();
				if (property.has("name") && "textures".equals(property.get("name").getAsString()) && property.has("value") && property.has("signature")) {
					Logger.info("Offline skin: " + name + " wears the skin of the account " + account.get("name").getAsString() + " (" + id + ").");
					return new Property("textures", property.get("value").getAsString(), property.get("signature").getAsString());
				}
			}
		}
		Logger.info("The Mojang account " + name + " has no skin data: the default skin stays.");
		return null;
	}

	/** The JSON object an endpoint answers, null when it has nothing for the name or id. */
	private static JsonObject getJson(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(proxy());
		connection.setConnectTimeout(TIMEOUT);
		connection.setReadTimeout(TIMEOUT);
		connection.setRequestProperty("User-Agent", "EtFuturumRequiem offline skins");
		connection.setRequestProperty("Accept", "application/json");
		try {
			int code = connection.getResponseCode();
			if (code == 204 || code == 400 || code == 404) {
				return null;
			}
			if (code == 429) {
				throw new IOException(connection.getURL().getHost() + " is rate limiting this address (one request per profile per minute), try again in a minute");
			}
			if (code / 100 != 2) {
				throw new IOException(url + " answered " + code);
			}
			try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
				JsonElement json = new JsonParser().parse(reader);
				return json.isJsonObject() ? json.getAsJsonObject() : null;
			}
		} finally {
			connection.disconnect();
		}
	}

	private static Proxy proxy() {
		MinecraftServer server = MinecraftServer.getServer();
		return server == null ? Proxy.NO_PROXY : server.getServerProxy();
	}

	private static File cacheFile() {
		MinecraftServer server = MinecraftServer.getServer();
		return server == null ? new File(CACHE_FILE) : server.getFile(CACHE_FILE);
	}

	private static void loadCache() {
		if (cacheLoaded) {
			return;
		}
		cacheLoaded = true;
		File file = cacheFile();
		if (!file.isFile()) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			for (Map.Entry<String, JsonElement> entry : new JsonParser().parse(reader).getAsJsonObject().entrySet()) {
				JsonObject answer = entry.getValue().getAsJsonObject();
				Property textures = answer.has("value") && answer.has("signature") ? new Property("textures", answer.get("value").getAsString(), answer.get("signature").getAsString()) : null;
				CACHE.put(entry.getKey(), new Entry(textures, answer.has("time") ? answer.get("time").getAsLong() : 0L));
			}
		} catch (IOException | RuntimeException e) {
			Logger.warn("Could not read " + file + ", looking the skins up again: " + e);
			CACHE.clear();
		}
	}

	private static void saveCache() {
		JsonObject json = new JsonObject();
		for (Map.Entry<String, Entry> entry : CACHE.entrySet()) {
			JsonObject answer = new JsonObject();
			answer.addProperty("time", entry.getValue().time);
			if (entry.getValue().textures != null) {
				answer.addProperty("value", entry.getValue().textures.getValue());
				answer.addProperty("signature", entry.getValue().textures.getSignature());
			}
			json.add(entry.getKey(), answer);
		}
		File file = cacheFile();
		try {
			Files.write(file.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			Logger.warn("Could not write " + file + ": " + e);
		}
	}

	/** An answer: the skin data, or none for a name without an account or skin, and when it was fetched. */
	private static final class Entry {
		final Property textures;
		final long time;

		Entry(Property textures, long time) {
			this.textures = textures;
			this.time = time;
		}

		boolean isFresh() {
			return System.currentTimeMillis() - time < FRESH_FOR;
		}

		String age() {
			long minutes = (System.currentTimeMillis() - time) / 60000L;
			return minutes < 120 ? minutes + " min" : minutes / 60 + " h";
		}
	}
}
