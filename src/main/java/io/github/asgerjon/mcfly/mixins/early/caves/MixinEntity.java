package io.github.asgerjon.mcfly.mixins.early.caves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

/**
 * The crystal chime (SPEC-modern-movement.md §90.4), 26.3's `Entity.playAmethystStepSound`: a step on amethyst or
 * budding amethyst (`#crystal_sound_blocks`) chimes at most every 20 ticks, its intensity fading by 0.997 a tick and
 * rising 0.07 a chime; pitch 0.5 + intensity x random x 1.2, volume 0.1 + intensity x 1.2. After 1.7.10's step sound,
 * which every entity's own step sound overrides.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	public int ticksExisted;
	@Shadow
	protected Random rand;
	@Unique
	private float etfu$crystalIntensity;
	@Unique
	private int etfu$lastCrystalTick;

	@Shadow
	public abstract void playSound(String name, float volume, float pitch);

	@WrapOperation(method = "moveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;func_145780_a(IIILnet/minecraft/block/Block;)V"))
	private void etfu$crystalChime(Entity self, int x, int y, int z, Block block, Operation<Void> original) {
		original.call(self, x, y, z, block);
		if (block != McFlyBlocks.AMETHYST_BLOCK.get() && block != McFlyBlocks.BUDDING_AMETHYST.get() || ticksExisted < etfu$lastCrystalTick + 20) {
			return;
		}
		etfu$crystalIntensity = Math.min(1.0F, etfu$crystalIntensity * (float) Math.pow(0.997D, ticksExisted - etfu$lastCrystalTick) + 0.07F);
		float pitch = 0.5F + etfu$crystalIntensity * rand.nextFloat() * 1.2F;
		playSound(McFly.MC_ASSETS + ":block.amethyst_block.chime", 0.1F + etfu$crystalIntensity * 1.2F, pitch);
		etfu$lastCrystalTick = ticksExisted;
	}
}
