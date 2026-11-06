package io.github.asgerjon.mcfly.base;

import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** The item of a block of variants (SPEC-modern-movement.md §88.2): one a damage, placing that meta, named as 26.3's. */
public class McFlyMetaItem extends ItemBlock {

	public McFlyMetaItem(Block block) {
		super(block);
		setHasSubtypes(true);
		setMaxDamage(0);
	}

	@Override
	public int getMetadata(int meta) {
		return meta;
	}

	/** The block's picture at the damage (1.7.10's `ItemBlock` shows meta 0's for every damage: a flat item's would all look alike). */
	@Override
	@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	public net.minecraft.util.IIcon getIconFromDamage(int damage) {
		return field_150939_a.getIcon(2, damage);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return "tile." + McFly.name(((NamedMetas) field_150939_a).nameOf(stack.getItemDamage()));
	}
}
