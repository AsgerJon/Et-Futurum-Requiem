package ganymedes01.etfuturum.mixins.early.hunger;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Breaking a block costs 0.005 exhaustion, as in modern; 1.7.10 charged 0.025. */
@Mixin(Block.class)
public class MixinBlock {

	@ModifyConstant(method = "harvestBlock", constant = @Constant(floatValue = 0.025F))
	private float etfu$miningCost(float original) {
		return 0.005F;
	}
}
