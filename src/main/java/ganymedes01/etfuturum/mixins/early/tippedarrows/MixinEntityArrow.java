package ganymedes01.etfuturum.mixins.early.tippedarrows;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.entities.EntityTippedArrow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A tipped arrow's effects once its hit is done (spec §46.10), as modern's doPostHurtEffects: only for a hit that
 * landed, and after it, a totem it used included (EntityTippedArrow.applyEffectsTo).
 */
@Mixin(EntityArrow.class)
public class MixinEntityArrow {

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
	private boolean etfu$effectsAfterTheHit(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		boolean hurt = original.call(target, source, amount);
		if (hurt && (Object) this instanceof EntityTippedArrow tipped && target instanceof EntityLivingBase living && !target.worldObj.isRemote) {
			tipped.applyEffectsTo(living);
		}
		return hurt;
	}
}
