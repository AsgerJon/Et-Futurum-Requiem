package ganymedes01.etfuturum.skins;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Reading the skin data (Mojang's base64 "textures" payload) a profile carries. */
public final class SkinTextures {

	private SkinTextures() {
	}

	/**
	 * Whether the profile's skin is the slim-armed (Alex) model: null when the profile carries no skin data, false
	 * when the account has no custom skin.
	 */
	public static Boolean slim(GameProfile profile) {
		Property textures = OfflineSkins.textures(profile);
		if (textures == null) {
			return null;
		}
		try {
			JsonObject payload = new JsonParser().parse(new String(Base64.getDecoder().decode(textures.getValue()), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject skin = payload.has("textures") ? payload.getAsJsonObject("textures").getAsJsonObject("SKIN") : null;
			if (skin == null) {
				return Boolean.FALSE;
			}
			JsonObject metadata = skin.getAsJsonObject("metadata");
			return metadata != null && metadata.has("model") && "slim".equals(metadata.get("model").getAsString());
		} catch (RuntimeException e) {
			return null;
		}
	}
}
