package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyItem;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;

/** Raw copper, iron and gold (SPEC-modern-movement.md §87), EFR's `raw_ore`: damage 0 copper, 1 iron, 2 gold. */
public class RawOreItem extends McFlyItem {

	public static final int COPPER = 0, IRON = 1, GOLD = 2;
	private static final String[] NAMES = {"raw_copper", "raw_iron", "raw_gold"};

	@SideOnly(Side.CLIENT)
	private IIcon[] icons;

	public RawOreItem() {
		setHasSubtypes(true);
		setMaxDamage(0);
		setUnlocalizedName(McFly.name("raw_ore"));
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return "item." + McFly.name(NAMES[Math.min(stack.getItemDamage(), 2)]);
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for (int damage = 0; damage < NAMES.length; damage++) {
			list.add(new ItemStack(item, 1, damage));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		icons = new IIcon[NAMES.length];
		for (int i = 0; i < NAMES.length; i++) {
			icons[i] = register.registerIcon(NAMES[i]);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int damage) {
		return icons[Math.min(damage, 2)];
	}
}
