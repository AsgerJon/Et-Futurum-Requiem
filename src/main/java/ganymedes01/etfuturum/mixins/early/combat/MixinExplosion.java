package ganymedes01.etfuturum.mixins.early.combat;

import ganymedes01.etfuturum.combat.DamageRules;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Blast Protection cuts a player's push too (spec §46.10). A player is pushed by the vector the explosion sends to
 * their client, which 1.7.10 made from the push before Blast Protection's cut (only the server-side motion, which the
 * client's own movement replaces, was cut), so it never cut a player's push. Modern's one push is cut for everyone.
 */
@Mixin(Explosion.class)
public class MixinExplosion {

	@Shadow
	private Map<EntityPlayer, Vec3> field_77288_k;

	@Inject(method = "doExplosionA", at = @At("TAIL"))
	private void etfu$blastProtectionForPlayers(CallbackInfo ci) {
		for (Map.Entry<EntityPlayer, Vec3> push : field_77288_k.entrySet()) {
			double share = DamageRules.explosionKnockback(push.getKey(), 1.0D);
			Vec3 vector = push.getValue();
			vector.xCoord *= share;
			vector.yCoord *= share;
			vector.zCoord *= share;
		}
	}
}
