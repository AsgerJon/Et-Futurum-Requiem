package io.github.asgerjon.mcfly.base;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * An item (or a block, for its item) 26.3 makes fire resistant (SPEC-modern-movement.md §88.4; `Item.Properties
 * .fireResistant`, the `damage_resistant` component of `#is_fire`): its item entity isn't hurt by fire or lava, and
 * fire doesn't wear it as armour. A fixed set, netherite's; not chosen by name.
 */
public interface FireResistant {

	static boolean is(ItemStack stack) {
		if (stack == null || stack.getItem() == null) {
			return false;
		}
		if (stack.getItem() instanceof FireResistant) {
			return true;
		}
		return stack.getItem() instanceof ItemBlock && Block.getBlockFromItem(stack.getItem()) instanceof FireResistant;
	}
}
