package io.github.asgerjon.mcfly.potion;

import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.world.World;

/** A dispenser shoots a tipped arrow as it shoots an arrow (SPEC-modern-movement.md §82), to be picked up again. */
public class TippedArrowDispense extends BehaviorProjectileDispense {

	private net.minecraft.item.ItemStack shooting;

	@Override
	public net.minecraft.item.ItemStack dispenseStack(IBlockSource source, net.minecraft.item.ItemStack stack) {
		shooting = stack;
		return super.dispenseStack(source, stack);
	}

	@Override
	protected IProjectile getProjectileEntity(World world, IPosition at) {
		TippedArrow arrow = new TippedArrow(world, at.getX(), at.getY(), at.getZ());
		arrow.canBePickedUp = 1;
		arrow.setArrow(shooting);
		return arrow;
	}
}
