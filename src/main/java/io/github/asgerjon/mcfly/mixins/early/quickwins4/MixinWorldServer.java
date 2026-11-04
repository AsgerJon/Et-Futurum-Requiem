package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Weather at 26.3's pace (SPEC-modern-movement.md §63.3): a chunk's precipitation tick (ice, a snow layer, a cauldron's
 * rain or snow) once in 48 ticks ({@code ServerLevel.tickChunk}), not 1.7.10's 16. The tick's only roll of 16.
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

	@ModifyArg(method = "func_147456_g", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
	private int etfu$precipitationOnceIn48(int bound) {
		return bound == 16 && McFlyConfig.modernWeatherPace ? 48 : bound;
	}
}
