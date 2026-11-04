package io.github.asgerjon.mcfly.registry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * McFly's creative tab (SPEC-modern-movement.md §79.4, B9): our blocks and items (each one that names it as its tab),
 * then our spawn eggs.
 */
public final class McFlyTab extends CreativeTabs {

	public static final McFlyTab TAB = new McFlyTab();

	private McFlyTab() {
		super("mcfly");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getTabIconItem() {
		return McFlyItems.TRIDENT.isEnabled() ? McFlyItems.TRIDENT.get()
			: McFlyItems.SPYGLASS.isEnabled() ? McFlyItems.SPYGLASS.get()
			: Items.ender_eye;
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void displayAllReleventItems(List list) {
		super.displayAllReleventItems(list);
		for (int egg : McFlyEntities.eggIds()) {
			list.add(new ItemStack(Items.spawn_egg, 1, egg));
		}
	}
}
