package io.github.asgerjon.mcfly.banner;

import io.github.asgerjon.mcfly.dye.DyeColor;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

/**
 * A banner as an item (SPEC-modern-movement.md §80; 26.3's `BannerItem`): its damage its colour (white 0 to black 15),
 * its layers in its tag; 16 to a stack. Placed on the top of a block it stands, turned to face the player in 16 steps;
 * on a side it hangs on that wall; used on a water cauldron with layers it's washed (26.3's
 * `CauldronInteractions.bannerInteraction`, spec §66.7).
 */
public class BannerItem extends ItemBlock {

	public BannerItem(Block block) {
		super(block);
		setMaxDamage(0);
		setMaxStackSize(16);
		setHasSubtypes(true);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		Block clicked = world.getBlock(x, y, z);
		if (clicked == Blocks.cauldron && world.getBlockMetadata(x, y, z) > 0 && Banners.count(stack) > 0) {
			wash(stack, player, world, x, y, z);
			return true;
		}
		if (side == 0 || !clicked.getMaterial().isSolid()) {
			return false;
		}
		switch (side) {
			case 1 -> y++;
			case 2 -> z--;
			case 3 -> z++;
			case 4 -> x--;
			case 5 -> x++;
			default -> {
			}
		}
		if (!player.canPlayerEdit(x, y, z, side, stack) || !field_150939_a.canPlaceBlockAt(world, x, y, z)) {
			return false;
		}
		// standing: 26.3's RotationSegment.convertToSegment(the player's yaw + 180); on a wall: the side it was put on
		int meta = side == 1 ? MathHelper.floor_double((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15 : side;
		world.setBlock(x, y, z, field_150939_a, meta, 3);
		world.playSoundEffect(x + 0.5F, y + 0.5F, z + 0.5F, field_150939_a.stepSound.func_150496_b(),
			(field_150939_a.stepSound.getVolume() + 1.0F) / 2.0F, field_150939_a.stepSound.getPitch() * 0.8F);
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile instanceof BannerBlockEntity banner) {
			banner.setStanding(side == 1);
			banner.setItemValues(stack);
		}
		stack.stackSize--;
		return true;
	}

	/**
	 * 26.3's wash: the top layer off one banner, one level of water used; a stack of more (or any in creative) keeps the
	 * rest, the washed one going into the inventory (dropped if it's full).
	 */
	private static void wash(ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
		if (world.isRemote) {
			return;
		}
		if (stack.stackSize == 1 && !player.capabilities.isCreativeMode) {
			Banners.removeTopLayer(stack);
		} else {
			ItemStack washed = stack.copy();
			washed.stackSize = 1;
			Banners.removeTopLayer(washed);
			if (!player.capabilities.isCreativeMode) {
				stack.stackSize--;
			}
			if (!player.inventory.addItemStackToInventory(washed)) {
				player.dropPlayerItemWithRandomChoice(washed, false);
			}
		}
		world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) - 1, 3);
	}

	/** "Red Banner": `item.banner.<colour>.name`. */
	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return StatCollector.translateToLocal("item.banner." + DyeColor.byId(Banners.base(stack)).getName() + ".name");
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
		Banners.addLayerLines(stack, tooltip);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for (int colour = 0; colour < 16; colour++) {
			list.add(new ItemStack(item, 1, colour));
		}
	}
}
