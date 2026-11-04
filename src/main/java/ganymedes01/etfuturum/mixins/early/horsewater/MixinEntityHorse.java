package ganymedes01.etfuturum.mixins.early.horsewater;

import ganymedes01.etfuturum.entities.ModernHorses;
import ganymedes01.etfuturum.movement.ModernWater;
import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ridden horses float (`riddenHorsesInWater`; SPEC-modern-movement.md §64.6, 26.3's `floatInLiquidWhileRidden`): with
 * any rider, the types in `can_float_while_ridden` (horse, donkey, mule, zombie horse; not the skeleton horse) go up
 * 0.04 a tick, after the move, in water (`entity_floatable` is water only) deeper than 0.4. Upstream EFR's backport
 * (#95) gave every type 0.03 in any liquid the box touched, lava too.
 */
@Mixin(EntityHorse.class)
public abstract class MixinEntityHorse extends EntityAnimal {

	protected MixinEntityHorse(World world) {
		super(world);
	}

	@Shadow
	public abstract int getHorseType();

	@Inject(method = "onLivingUpdate", at = @At("TAIL"))
	private void floatWithRider(CallbackInfo ci) {
		if (this.riddenByEntity == null || getHorseType() == ModernHorses.SKELETON) {
			return;
		}
		if (ModernWater.getFluidHeight(this.worldObj, this.boundingBox, Material.water) > ModernHorses.FLOAT_DEPTH) {
			this.motionY += ModernHorses.RIDDEN_FLOAT;
		}
	}
}
