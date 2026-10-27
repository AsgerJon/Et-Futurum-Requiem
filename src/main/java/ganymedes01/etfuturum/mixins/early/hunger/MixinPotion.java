package ganymedes01.etfuturum.mixins.early.hunger;

import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The Hunger effect drains 0.005 exhaustion per level per tick, as in modern; 1.7.10 drained 0.025. */
@Mixin(Potion.class)
public class MixinPotion {

	@ModifyConstant(method = "performEffect", constant = @Constant(floatValue = 0.025F))
	private float etfu$hungerEffectCost(float original) {
		return 0.005F;
	}
}
