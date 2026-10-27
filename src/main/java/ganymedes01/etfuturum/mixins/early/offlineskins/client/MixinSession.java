package ganymedes01.etfuturum.mixins.early.offlineskins.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.client.skins.OfflineSkinsClient;
import net.minecraft.util.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every local player entity is made from the session's profile (joining, respawning, changing dimension): the skin
 * data the server sent for this name goes on it, so the player keeps the skin.
 */
@Mixin(Session.class)
public abstract class MixinSession {
	@ModifyReturnValue(method = "func_148256_e", at = @At("RETURN"))
	private GameProfile etfu$withTheOfflineSkin(GameProfile profile) {
		return OfflineSkinsClient.decorate(profile);
	}
}
