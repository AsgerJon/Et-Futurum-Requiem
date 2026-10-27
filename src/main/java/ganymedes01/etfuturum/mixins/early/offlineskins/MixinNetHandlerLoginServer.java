package ganymedes01.etfuturum.mixins.early.offlineskins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import ganymedes01.etfuturum.skins.OfflineSkins;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerLoginServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Future;

/**
 * A player the server will not authenticate (offline mode, or the local connection of a development client) is
 * looked up by name on Mojang's API while the login proceeds, and the skin data found goes on the offline profile
 * the player gets. The UUID stays the offline one, so player files, operators and bans are what they were.
 */
@Mixin(NetHandlerLoginServer.class)
public abstract class MixinNetHandlerLoginServer {
	@Shadow
	private GameProfile field_147337_i;
	@Shadow
	@Final
	private MinecraftServer field_147327_f;
	@Shadow
	@Final
	public NetworkManager field_147333_a;
	@Unique
	private Future<Property> etfu$lookup;
	@Unique
	private String etfu$lookedUpName;

	@Shadow
	protected abstract GameProfile func_152506_a(GameProfile profile);

	@Inject(method = "processLoginStart", at = @At("TAIL"))
	private void etfu$lookUpTheName(C00PacketLoginStart packet, CallbackInfo ci) {
		boolean authenticated = field_147327_f.isServerInOnlineMode() && !field_147333_a.isLocalChannel();
		if (!authenticated && field_147337_i != null && !field_147337_i.isComplete() && field_147337_i.getName() != null && !field_147337_i.getName().isEmpty()) {
			etfu$lookedUpName = field_147337_i.getName();
			etfu$lookup = OfflineSkins.lookUp(etfu$lookedUpName);
		}
	}

	/**
	 * Acceptance (vanilla's tryAcceptPlayer, tried again every tick within its 30 s login limit) waits for the lookup,
	 * then the skin data goes on the offline profile, which vanilla keeps since it is complete.
	 */
	@Inject(method = "func_147326_c", at = @At("HEAD"), cancellable = true)
	private void etfu$finishTheLookup(CallbackInfo ci) {
		if (etfu$lookup == null) {
			return;
		}
		if (!etfu$lookup.isDone()) {
			ci.cancel();
			return;
		}
		Property textures = OfflineSkins.result(etfu$lookup, etfu$lookedUpName);
		etfu$lookup = null;
		if (textures != null && !field_147337_i.isComplete()) {
			GameProfile profile = func_152506_a(field_147337_i);
			profile.getProperties().put("textures", textures);
			OfflineSkins.markResolved(profile);
			field_147337_i = profile;
		}
	}
}
