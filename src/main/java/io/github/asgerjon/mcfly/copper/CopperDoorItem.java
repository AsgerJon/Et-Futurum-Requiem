package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/** A copper door's item (SPEC-modern-movement.md §87): put on a block's top, both halves, facing as 1.7.10's doors. */
public class CopperDoorItem extends ItemBlock {

	@SideOnly(Side.CLIENT)
	private IIcon icon;

	public CopperDoorItem(Block block) {
		super(block);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		if (side != 1) {
			return false;
		}
		y++;
		if (!player.canPlayerEdit(x, y, z, side, stack) || !player.canPlayerEdit(x, y + 1, z, side, stack) || !field_150939_a.canPlaceBlockAt(world, x, y, z)) {
			return false;
		}
		int facing = MathHelper.floor_double((player.rotationYaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
		ItemDoor.placeDoorBlock(world, x, y, z, facing, field_150939_a);
		stack.stackSize--;
		return true;
	}

	/** 26.3's door items: drawn by the item texture of the stage (waxed as unwaxed). */
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		icon = register.registerIcon(((CopperDoorBlock) field_150939_a).itemTexture());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int damage) {
		return icon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getSpriteNumber() {
		return 1;
	}
}
