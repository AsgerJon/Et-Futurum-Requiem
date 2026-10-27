package ganymedes01.etfuturum.offhand;

import net.minecraft.item.ItemStack;

/**
 * The off-hand stack kept by the player inventory (SPEC-modern-movement.md §36.1), implemented by the InventoryPlayer
 * mixin. Reached from vanilla code as inventory index {@link Offhand#INVENTORY_INDEX}.
 */
public interface IOffhandInventory {

	ItemStack etfu$getOffhand();

	void etfu$setOffhand(ItemStack stack);
}
