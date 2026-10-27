package ganymedes01.etfuturum.mixins.early.hunger;

import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Taking damage costs 0.1 exhaustion, as modern's damage types default to; 1.7.10's default was 0.3. */
@Mixin(DamageSource.class)
public class MixinDamageSource {

	@ModifyConstant(method = "<init>(Ljava/lang/String;)V", constant = @Constant(floatValue = 0.3F))
	private float etfu$modernDamageExhaustion(float original) {
		return 0.1F;
	}
}
