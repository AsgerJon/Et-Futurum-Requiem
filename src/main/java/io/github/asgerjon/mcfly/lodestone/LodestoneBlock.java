package io.github.asgerjon.mcfly.lodestone;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * The lodestone (SPEC-modern-movement.md §57, moved into McFly's package by §88.8), 26.3's: 3.5 and 3.5, a pickaxe for
 * drops, the metal map colour, its sound, not moved by pistons; each one kept in the world's {@link LodestoneRecord}, so
 * a compass used on it points to it (`LodestoneCompass`) and stops when it's gone.
 */
public class LodestoneBlock extends McFlyBlock {

	@SideOnly(Side.CLIENT)
	private IIcon top;

	public LodestoneBlock() {
		super(Material.iron);
		setNames("lodestone");
		strength(3.5F, 3.5F).tool("pickaxe", 0).sound(McFlySoundType.LODESTONE).colour(MapColor.ironColor);
	}

	@Override
	public int getMobilityFlag() {
		return 2;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		if (!world.isRemote) {
			LodestoneRecord.get(world).add(x, y, z);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!world.isRemote) {
			LodestoneRecord.get(world).remove(x, y, z);
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon("lodestone_side");
		top = register.registerIcon("lodestone_top");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return side > 1 ? blockIcon : top;
	}
}
