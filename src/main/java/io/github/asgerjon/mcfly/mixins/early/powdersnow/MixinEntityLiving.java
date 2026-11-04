package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 {@code NaturalSpawner.isValidEmptySpawnBlock} (SPEC-modern-movement.md §62.11): no mob spawns with its feet or
 * head in powder snow, unless it is immune to it (stray, polar bear, snow golem). 1.7.10 only asks for a block that
 * isn't a normal cube.
 */
@Mixin(EntityLiving.class)
public abstract class MixinEntityLiving extends Entity {

	public MixinEntityLiving(World world) {
		super(world);
	}

	@ModifyReturnValue(method = "getCanSpawnHere", at = @At("RETURN"))
	private boolean etfu$notInPowderSnow(boolean canSpawn) {
		if (!canSpawn || Freezing.maySpawnInPowderSnow(this)) {
			return canSpawn;
		}
		int x = MathHelper.floor_double(posX);
		int y = MathHelper.floor_double(boundingBox.minY);
		int z = MathHelper.floor_double(posZ);
		return !Freezing.isPowderSnow(worldObj.getBlock(x, y, z)) && !Freezing.isPowderSnow(worldObj.getBlock(x, y + 1, z));
	}
}
