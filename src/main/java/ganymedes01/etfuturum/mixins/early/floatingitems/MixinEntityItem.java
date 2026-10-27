package ganymedes01.etfuturum.mixins.early.floatingitems;

import ganymedes01.etfuturum.movement.ModernWater;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Items float (§30): more than 0.1 blocks deep in water or lava an item has no gravity, a slight lift up to 0.06 per
 * tick and extra horizontal drag (0.99 in water, 0.95 in lava), as since 1.13. The gravity line of the item's tick is
 * the place: the lift and drag replace the 0.04 drop.
 */
@Mixin(EntityItem.class)
public abstract class MixinEntityItem extends Entity {

	public MixinEntityItem(World world) {
		super(world);
	}

	@ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 0.03999999910593033D))
	private double etfu$floatInsteadOfFalling(double gravity) {
		double drag = etfu$fluidDrag();
		if (drag == 0.0D) {
			return gravity;
		}
		this.motionX *= drag;
		this.motionZ *= drag;
		if (this.motionY < 0.06D) {
			this.motionY += 5.0E-4D;
		}
		return 0.0D;
	}

	@Unique
	private double etfu$fluidDrag() {
		if (ModernWater.getFluidHeight(this.worldObj, this.boundingBox, Material.water) > 0.1D) {
			return 0.99D;
		}
		if (ModernWater.getFluidHeight(this.worldObj, this.boundingBox, Material.lava) > 0.1D) {
			return 0.95D;
		}
		return 0.0D;
	}
}
