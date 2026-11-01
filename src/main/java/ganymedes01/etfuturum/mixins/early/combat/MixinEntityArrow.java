package ganymedes01.etfuturum.mixins.early.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A burning arrow whose hit fails (a raised shield, a creative player, the hurt cooldown) leaves the target's fire as
 * it was (spec §46.9): modern's onHitEntity notes the fire before setting the target alight and puts it back when the
 * hit fails. 1.7.10 set it alight either way. On the client the hit always fails, so the client's copy of the target
 * isn't set alight by its own prediction either; the server's burning flag shows the real fire.
 */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

	@Unique
	private Entity etfu$setAlight;

	@Unique
	private int etfu$fireBefore;

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
	private void etfu$noteFire(Entity target, int seconds, Operation<Void> original) {
		etfu$setAlight = target;
		etfu$fireBefore = ((AccessorEntity) target).etfu$getFire();
		original.call(target, seconds);
	}

	@ModifyExpressionValue(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
	private boolean etfu$fireBackIfTheHitFailed(boolean hurt) {
		if (etfu$setAlight != null) {
			if (!hurt) {
				((AccessorEntity) etfu$setAlight).etfu$setFire(etfu$fireBefore);
			}
			etfu$setAlight = null;
		}
		return hurt;
	}
}
