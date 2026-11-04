package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.ducks.ILastDamage;
import ganymedes01.etfuturum.entities.PanicCauses;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3's {@code PanicGoal} (SPEC-modern-movement.md §63.4): panic when the last damage, within 40 ticks, is a panic
 * cause (not only from an attacker or while burning, as 1.7.10); a burning mob first heads for water within 5 blocks.
 */
@Mixin(EntityAIPanic.class)
public abstract class MixinEntityAIPanic {

	@Shadow
	private EntityCreature theEntityCreature;
	@Shadow
	private double randPosX;
	@Shadow
	private double randPosY;
	@Shadow
	private double randPosZ;

	@Inject(method = "shouldExecute", at = @At("HEAD"), cancellable = true)
	private void etfu$modernPanic(CallbackInfoReturnable<Boolean> cir) {
		if (!McFlyConfig.modernPanic || !(theEntityCreature instanceof ILastDamage)) {
			return;
		}
		if (!PanicCauses.isPanicCause(((ILastDamage) theEntityCreature).etfu$lastDamage(40))) {
			cir.setReturnValue(false);
			return;
		}
		if (theEntityCreature.isBurning()) {
			int[] water = etfu$lookForWater(theEntityCreature, 5);
			if (water != null) {
				randPosX = water[0];
				randPosY = water[1];
				randPosZ = water[2];
				cir.setReturnValue(true);
				return;
			}
		}
		Vec3 spot = RandomPositionGenerator.findRandomTarget(theEntityCreature, 5, 4);
		if (spot == null) {
			cir.setReturnValue(false);
			return;
		}
		randPosX = spot.xCoord;
		randPosY = spot.yCoord;
		randPosZ = spot.zCoord;
		cir.setReturnValue(true);
	}

	/**
	 * 26.3 {@code lookForWater}: none if the mob stands in a block with collision; else the nearest water, 5 sideways and
	 * 1 up or down, by walking distance ({@code BlockPos.findClosestMatch} goes by Manhattan rings).
	 */
	@Unique
	private static int[] etfu$lookForWater(EntityCreature mob, int range) {
		World world = mob.worldObj;
		int x = MathHelper.floor_double(mob.posX), y = MathHelper.floor_double(mob.posY), z = MathHelper.floor_double(mob.posZ);
		if (world.getBlock(x, y, z).getCollisionBoundingBoxFromPool(world, x, y, z) != null) {
			return null;
		}
		for (int distance = 0; distance <= range * 2 + 1; distance++) {
			for (int dy = -1; dy <= 1; dy++) {
				int rest = distance - Math.abs(dy);
				if (rest < 0) {
					continue;
				}
				for (int dx = -Math.min(rest, range); dx <= Math.min(rest, range); dx++) {
					int dz = rest - Math.abs(dx);
					if (dz > range) {
						continue;
					}
					for (int sign : dz == 0 ? new int[]{1} : new int[]{1, -1}) {
						if (world.getBlock(x + dx, y + dy, z + sign * dz).getMaterial() == Material.water) {
							return new int[]{x + dx, y + dy, z + sign * dz};
						}
					}
				}
			}
		}
		return null;
	}
}
