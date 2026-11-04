package io.github.asgerjon.mcfly.potion;

import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** A dispenser throws a lingering potion as it throws a splash one (SPEC-modern-movement.md §82). */
public class PotionDispense implements IBehaviorDispenseItem {

	public PotionDispense(boolean unused) {
	}

	@Override
	public ItemStack dispense(IBlockSource source, ItemStack stack) {
		ItemStack one = stack.copy();
		one.stackSize = 1;
		return new BehaviorProjectileDispense() {
			@Override
			protected IProjectile getProjectileEntity(World world, IPosition at) {
				return new ThrownLingeringPotion(world, at.getX(), at.getY(), at.getZ(), one);
			}

			@Override
			protected float func_82498_a() {
				return super.func_82498_a() * 0.5F;
			}

			@Override
			protected float func_82500_b() {
				return super.func_82500_b() * 1.25F;
			}
		}.dispense(source, stack);
	}
}
