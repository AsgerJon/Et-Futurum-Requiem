package ganymedes01.etfuturum.mixins.early.totem;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.core.handlers.ServerEventHandler;
import ganymedes01.etfuturum.entities.ITotemUser;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The totem of undying, where 1.7.10 sees a hit's death: after the hit is done (armour, protection, absorption), just
 * before the death sound and onDeath, as modern's checkTotemDeathProtection. A totem that saves the holder turns the
 * death into a hurt (its health is back above 0 when the check reads it). The tick it did is kept, for the Harming
 * arrow fix (EntityTippedArrow.applyEffectsTo).
 */
@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase implements ITotemUser {

	@Unique
	private long etfu$totemTick = -1L;

	@ModifyExpressionValue(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getHealth()F", ordinal = 2))
	private float etfu$totemBeforeDeath(float health, @Local(argsOnly = true) DamageSource source) {
		EntityLivingBase self = (EntityLivingBase) (Object) this;
		if (health <= 0.0F && !self.worldObj.isRemote && ServerEventHandler.useTotem(self, source)) {
			etfu$totemTick = self.worldObj.getTotalWorldTime();
			return self.getHealth();
		}
		return health;
	}

	@Override
	public boolean etfu$usedTotemThisTick() {
		return etfu$totemTick == ((EntityLivingBase) (Object) this).worldObj.getTotalWorldTime();
	}
}
