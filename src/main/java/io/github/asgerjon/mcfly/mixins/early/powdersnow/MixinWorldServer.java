package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Snowfall fills cauldrons with powder snow (SPEC-modern-movement.md §62.8; 26.3 {@code CauldronBlock} and
 * {@code LayeredCauldronBlock.handlePrecipitation} with snow). 1.7.10's precipitation tick asks the biome only while it
 * rains, and fills only in biomes that rain: where it is cold enough to snow at the cauldron (below 0.15, as 1.7.10's
 * snow layers), an empty cauldron becomes powder snow level 1 and one below 3 goes up a level: 26.3's 10% at a
 * precipitation tick once in 48 (spec §63.3), or 1 in 30 at 1.7.10's once in 16 without the modern weather pace.
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

	@WrapOperation(method = "func_147456_g", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;getBiomeGenForCoords(II)Lnet/minecraft/world/biome/BiomeGenBase;"))
	private BiomeGenBase etfu$snowIntoCauldrons(WorldServer world, int x, int z, Operation<BiomeGenBase> original) {
		BiomeGenBase biome = original.call(world, x, z);
		if (McFlyBlocks.POWDER_SNOW_CAULDRON.isEnabled()) {
			int top = world.getPrecipitationHeight(x, z);
			Block below = world.getBlock(x, top - 1, z);
			Block powderSnow = McFlyBlocks.POWDER_SNOW_CAULDRON.get();
			if ((below == Blocks.cauldron && world.getBlockMetadata(x, top - 1, z) == 0 || below == powderSnow)
					&& biome.getFloatTemperature(x, top, z) < 0.15F && world.rand.nextInt(McFlyConfig.modernWeatherPace ? 10 : 30) == 0) {
				etfu$fill((World) world, x, top - 1, z, below == powderSnow ? world.getBlockMetadata(x, top - 1, z) : 0);
			}
		}
		return biome;
	}

	private static void etfu$fill(World world, int x, int y, int z, int level) {
		if (level < 3) {
			world.setBlock(x, y, z, McFlyBlocks.POWDER_SNOW_CAULDRON.get(), level + 1, 3);
		}
	}
}
