package io.github.asgerjon.mcfly.smithing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * The smithing table (SPEC-modern-movement.md §88.4, step C2), 26.3's `SmithingTableBlock`: 2.5, an axe's, wood's
 * sounds, burning in lava; a right click opens its menu ({@link SmithingMenu}, through the mod's GUI handler). Its
 * faces 26.3's model's: top, bottom, the front on north and south, the side on east and west.
 */
public class SmithingTableBlock extends McFlyBlock {

	@SideOnly(Side.CLIENT)
	private IIcon top, bottom, front;

	public SmithingTableBlock() {
		super(Material.wood);
		setNames("smithing_table");
		strength(2.5F, 2.5F).tool("axe", 0).sound(soundTypeWood).colour(MapColor.woodColor);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {
			io.github.asgerjon.mcfly.base.McFlyGuis.open(player, io.github.asgerjon.mcfly.base.McFlyGuis.SMITHING_TABLE, world, x, y, z);
		}
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon("smithing_table_side");
		top = register.registerIcon("smithing_table_top");
		bottom = register.registerIcon("smithing_table_bottom");
		front = register.registerIcon("smithing_table_front");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return side == 0 ? bottom : side == 1 ? top : side == 2 || side == 3 ? front : blockIcon;
	}
}
