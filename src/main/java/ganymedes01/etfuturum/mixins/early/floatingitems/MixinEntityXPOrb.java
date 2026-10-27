package ganymedes01.etfuturum.mixins.early.floatingitems;

import ganymedes01.etfuturum.movement.ModernWater;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Experience orbs float (§30): with the eyes under water an orb has no gravity, drifts up by 0.0005 per tick to at
 * most 0.06, and loses 1% of its horizontal speed, as since 1.13. The orb's eyes are 0.85 of its height up, less
 * modern's 1/9 for the under-water test.
 */
@Mixin(EntityXPOrb.class)
public abstract class MixinEntityXPOrb extends Entity {

	public MixinEntityXPOrb(World world) {
		super(world);
	}

	@ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 0.029999999329447746D))
	private double etfu$floatInsteadOfFalling(double gravity) {
		double eyeY = this.boundingBox.minY + this.height * 0.85F - 0.11111111F;
		if (!ModernWater.isPointInMaterial(this.worldObj, this.posX, eyeY, this.posZ, Material.water)) {
			return gravity;
		}
		this.motionX *= 0.99D;
		this.motionZ *= 0.99D;
		this.motionY = Math.min(this.motionY + 5.0E-4D, 0.06D);
		return 0.0D;
	}
}
