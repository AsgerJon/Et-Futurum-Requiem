package io.github.asgerjon.mcfly.mixins.early.stepsounds;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Footsteps on thin blocks (SPEC-modern-movement.md §56). In moveEntity, once the walked distance is counted and
 * before 1.7.10's own step check: when 26.3's player rules (modernStepSounds) or the MC-8820 fix
 * (thinBlockStepSoundFix) apply, the step is played here and nextStepDistance moved on, so 1.7.10's step from the
 * block 0.2 below the feet doesn't play as well. onEntityWalking still goes to that block, as in 1.7.10 and 26.3.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	public World worldObj;
	@Shadow
	public double posX;
	@Shadow
	public double posY;
	@Shadow
	public double posZ;
	@Shadow
	public double motionX;
	@Shadow
	public double motionY;
	@Shadow
	public double motionZ;
	@Shadow
	public float yOffset;
	@Shadow
	@Final
	public AxisAlignedBB boundingBox;
	@Shadow
	public float distanceWalkedOnStepModified;
	@Shadow
	private int nextStepDistance;
	@Shadow
	protected Random rand;

	@Shadow
	public abstract boolean isInWater();

	@Shadow
	protected abstract String getSwimSound();

	@Shadow
	protected abstract void func_145780_a(int x, int y, int z, Block block);

	@Shadow
	public abstract void playSound(String name, float volume, float pitch);

	@Inject(method = "moveEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;distanceWalkedOnStepModified:F", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
	private void etfu$stepOnThinBlocks(double x, double y, double z, CallbackInfo ci) {
		if (distanceWalkedOnStepModified <= nextStepDistance) {
			return;
		}
		// 1.7.10's block 0.2 below the feet, with a fence, wall or gate under it standing in for it
		int bx = MathHelper.floor_double(posX);
		int by = MathHelper.floor_double(posY - 0.20000000298023224D - yOffset);
		int bz = MathHelper.floor_double(posZ);
		Block below = worldObj.getBlock(bx, by, bz);
		int renderType = worldObj.getBlock(bx, by - 1, bz).getRenderType();
		if (renderType == 11 || renderType == 32 || renderType == 21) {
			below = worldObj.getBlock(bx, by - 1, bz);
		}
		boolean onSomething = below.getMaterial() != Material.air;
		Block above = worldObj.getBlock(bx, by + 1, bz);
		boolean modern = McFlyConfig.modernStepSounds && onSomething && (Object) this instanceof EntityPlayer;
		boolean combination = modern && etfu$combinationStepSound(above);
		boolean inside = modern && !combination && etfu$insideStepSound(above, worldObj.getBlockMetadata(bx, by + 1, bz));
		int thin = combination || inside || !McFlyConfig.thinBlockStepSoundFix ? by : etfu$thinBlockUnderFeet(bx, by, bz);
		if (!combination && !inside && thin == by) {
			return;
		}

		nextStepDistance = (int) distanceWalkedOnStepModified + 1;
		if (isInWater()) {
			float volume = Math.min(1.0F, MathHelper.sqrt_double(motionX * motionX * 0.20000000298023224D + motionY * motionY + motionZ * motionZ * 0.20000000298023224D) * 0.35F);
			playSound(getSwimSound(), volume, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
		}
		if (combination) {
			// the block on top as a plain step, the one under it muffled (26.3's Entity.playMuffledStepSound)
			etfu$step(above, 0.15F, 1.0F);
			if (!below.getMaterial().isLiquid()) {
				etfu$step(below, 0.05F, 0.8F);
			}
		} else if (inside) {
			etfu$step(above, 0.15F, 1.0F);
		} else {
			func_145780_a(bx, thin, bz, worldObj.getBlock(bx, thin, bz));
		}
		if (onSomething) {
			below.onEntityWalking(worldObj, bx, by, bz, (Entity) (Object) this);
		}
	}

	private void etfu$step(Block block, float volume, float pitch) {
		Block.SoundType sound = block.stepSound;
		playSound(sound.getStepResourcePath(), sound.getVolume() * volume, sound.getPitch() * pitch);
	}

	/** The y of the block the feet stand on when its top is less than 0.2 above its base (a bottom-half trapdoor), else by. */
	private int etfu$thinBlockUnderFeet(int bx, int by, int bz) {
		double feet = boundingBox.minY;
		int y = MathHelper.floor_double(feet - 1.0E-5D);
		if (y == by) {
			return by;
		}
		AxisAlignedBB box = worldObj.getBlock(bx, y, bz).getCollisionBoundingBoxFromPool(worldObj, bx, y, bz);
		return box != null && Math.abs(box.maxY - feet) < 1.0E-3D ? y : by;
	}

	/** 26.3's combination_step_sound_blocks, as far as EFR has them. */
	private static boolean etfu$combinationStepSound(Block block) {
		return block == Blocks.carpet || block == Blocks.snow_layer || block == ModBlocks.MOSS_CARPET.get()
				|| block == ModBlocks.NETHER_SPROUTS.get() || block == ModBlocks.NETHER_ROOTS.get();
	}

	/** 26.3's inside_step_sound_blocks, as far as EFR has them; the small amethyst bud is cluster 1's meta 0 to 5. */
	private static boolean etfu$insideStepSound(Block block, int meta) {
		return block == Blocks.waterlily || block == ModBlocks.GLOW_LICHEN.get() || block == ModBlocks.PINK_PETALS.get()
				|| block == ModBlocks.AMETHYST_CLUSTER_1.get() && meta < 6 || ganymedes01.etfuturum.entities.Freezing.isPowderSnow(block);
	}
}
