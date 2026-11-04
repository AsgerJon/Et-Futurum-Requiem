package io.github.asgerjon.mcfly.shulkerbox;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.dye.DyeColor;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

/**
 * A shulker box as an item (SPEC-modern-movement.md §81): its colour and contents in its tag (`Color`, `Items`, as EFR
 * kept them), one to a stack; placed facing the side it's put on; washed plain in a water cauldron (26.3's
 * `CauldronInteractions.shulkerBoxInteraction`); its tooltip 26.3's (the first 5 stacks, then "and N more...").
 */
public class ShulkerBoxItem extends ItemBlock {

	public ShulkerBoxItem(Block block) {
		super(block);
		setMaxStackSize(1);
		setHasSubtypes(true);
		net.minecraft.block.BlockDispenser.dispenseBehaviorRegistry.putObject(this, new ShulkerBoxDispense());
	}

	public static int colour(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getByte("Color") & 31 : 0;
	}

	/** "tile.etfuturum.red_shulker_box" (EFR's names, in the lang files). */
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		int colour = colour(stack);
		return "tile." + McFly.name(colour > 0 ? DyeColor.byId(colour - 1).getName() + "_shulker_box" : "shulker_box");
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		int level = world.getBlockMetadata(x, y, z);
		if (world.getBlock(x, y, z) == Blocks.cauldron && level > 0 && colour(stack) > 0) {
			if (!world.isRemote) {
				stack.getTagCompound().removeTag("Color");
				if (stack.getTagCompound().hasNoTags()) {
					stack.setTagCompound(null);
				}
				world.setBlockMetadataWithNotify(x, y, z, level - 1, 3);
			}
			return true;
		}
		return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, meta)) {
			return false;
		}
		ShulkerBoxBlock.placed(world, x, y, z, side, stack);
		return true;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
		if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("Items", 9)) {
			return;
		}
		NBTTagList items = stack.getTagCompound().getTagList("Items", 10);
		int shown = 0;
		int more = 0;
		for (int i = 0; i < items.tagCount(); i++) {
			ItemStack inside = ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i));
			if (inside == null) {
				continue;
			}
			if (shown < 5) {
				tooltip.add(inside.getDisplayName() + " x" + inside.stackSize);
				shown++;
			} else {
				more++;
			}
		}
		if (more > 0) {
			tooltip.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocalFormatted("container.shulkerBox.more", more));
		}
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		list.add(new ItemStack(item));
		for (DyeColor colour : DyeColor.values()) {
			ItemStack stack = new ItemStack(item);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setByte("Color", (byte) (colour.id() + 1));
			stack.setTagCompound(tag);
			list.add(stack);
		}
	}
}
