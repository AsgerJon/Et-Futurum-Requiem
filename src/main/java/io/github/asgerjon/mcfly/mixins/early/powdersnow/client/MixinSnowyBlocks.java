package io.github.asgerjon.mcfly.mixins.early.powdersnow.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockMycelium;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Grass, mycelium and podzol under powder snow show their snowy sides (SPEC-modern-movement.md §62.11; 26.3's
 * {@code snow} tag, {@code SnowyBlock}): 1.7.10 looks at the block above (or its material), so powder snow reads as a
 * snow block there.
 */
@Mixin({BlockGrass.class, BlockMycelium.class, BlockDirt.class})
public abstract class MixinSnowyBlocks {

	@ModifyExpressionValue(method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlock(III)Lnet/minecraft/block/Block;"))
	private Block etfu$powderSnowIsSnow(Block above) {
		return Freezing.isPowderSnow(above) ? Blocks.snow : above;
	}
}
