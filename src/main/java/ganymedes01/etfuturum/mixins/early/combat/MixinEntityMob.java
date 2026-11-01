package ganymedes01.etfuturum.mixins.early.combat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.combat.DamageRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A mob's Knockback as modern's knockback (spec §46.10), as for players' hits (MixinEntityPlayer). */
@Mixin(EntityMob.class)
public class MixinEntityMob {

	@WrapOperation(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
	private void etfu$modernExtraKnockback(Entity target, double x, double y, double z, Operation<Void> original) {
		if (target instanceof EntityLivingBase living) {
			DamageRules.knockback(living, Math.sqrt(x * x + z * z), -x, -z);
		} else {
			original.call(target, x, y, z);
		}
	}
}
