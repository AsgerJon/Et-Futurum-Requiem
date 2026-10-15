package ganymedes01.etfuturum.mixins.early.modernmovement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.movement.ModernMovement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Modern sneaking at edges (§7.2): stop at drops deeper than 0.6 instead of 1.7.10's full block.
 */
@Mixin(Entity.class)
public class MixinEntitySneakEdge {

	@Shadow
	public boolean noClip;

	@Shadow
	protected boolean isInWeb;

	/**
	 * Backs the movement off before the move, like modern. A cobweb scales the movement by 0.25 inside moveEntity,
	 * before modern's edge check, so the check works on the scaled movement and the result is scaled back up.
	 */
	@WrapMethod(method = "moveEntity")
	private void etfu$backOffFromEdge(double x, double y, double z, Operation<Void> original) {
		if ((Object) this instanceof EntityPlayer && !this.noClip) {
			double webScale = this.isInWeb ? 0.25D : 1.0D;
			double[] backedOff = ModernMovement.backOffFromEdge((EntityPlayer) (Object) this, x * webScale, y, z * webScale);
			x = backedOff[0] / webScale;
			z = backedOff[1] / webScale;
		}
		original.call(x, y, z);
	}

	/**
	 * Turns off 1.7.10's own edge loops for players (the first three "would I fall" checks in moveEntity). The sneak
	 * flag itself stays, since 1.7.10 also uses it for step-ups and walking effects.
	 */
	@ModifyExpressionValue(method = "moveEntity", at = {
			@At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0),
			@At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 1),
			@At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 2)
	})
	private boolean etfu$skipVanillaEdgeLoops(boolean wouldFall) {
		return wouldFall && !((Object) this instanceof EntityPlayer);
	}
}
