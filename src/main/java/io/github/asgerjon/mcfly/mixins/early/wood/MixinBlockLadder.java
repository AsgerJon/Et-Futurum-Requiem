package io.github.asgerjon.mcfly.mixins.early.wood;

import net.minecraft.block.BlockLadder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** 26.3's ladder is 3/16 deep (`LadderBlock`'s shapes; 1.7.10's 2/16), so a trapdoor over it lines up (SPEC-modern-movement.md §91.6). */
@Mixin(BlockLadder.class)
public abstract class MixinBlockLadder {

	@ModifyConstant(method = "func_149797_b", constant = @Constant(floatValue = 0.125F, ordinal = 0))
	private float mcfly$depth(float depth) {
		return 0.1875F;
	}
}
