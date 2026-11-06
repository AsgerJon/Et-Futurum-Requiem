package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;

/**
 * A pressure plate of ours (SPEC-modern-movement.md §88.2), 26.3's `PressurePlateBlock` on 1.7.10's: pressed by what
 * its `BlockSetType` senses (every entity, or mobs: 1.7.10's `Sensitivity`), strength 0.5, drawn as a block's meta. A
 * plate with no tool rule (26.3's polished blackstone one) drops by hand.
 */
public class McFlyPressurePlate extends BlockPressurePlate implements McFlySetType.Typed {

	private final Block look;
	private final int lookMeta;
	private final boolean anyTool;
	private McFlySetType type;
	private MapColor colour;

	public McFlyPressurePlate(String name, Material material, Sensitivity sensitivity, Block look, int lookMeta, boolean anyTool) {
		super(name, material, sensitivity);
		this.look = look;
		this.lookMeta = lookMeta;
		this.anyTool = anyTool;
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name(name));
		setHardness(0.5F);
		this.blockResistance = 0.5F * 5.0F;
		setStepSound(material == Material.wood ? soundTypeWood : soundTypeStone);
		this.type = material == Material.wood ? McFlySetType.WOOD : McFlySetType.STONE;
	}

	/**
	 * A wooden plate of a set (spec §91.7): pressed by anything, the set's sound and clicks, the axe's (needed for
	 * nothing), the planks' colour.
	 */
	public McFlyPressurePlate(String name, McFlySetType type, Block look, int lookMeta, MapColor colour) {
		this(name, Material.wood, Sensitivity.everything, look, lookMeta, true);
		this.type = type;
		this.colour = colour;
		setStepSound(type.sound);
		setHarvestLevel("axe", 0);
	}

	@Override
	public McFlySetType setType() {
		return type;
	}

	@Override
	public boolean canHarvestBlock(EntityPlayer player, int meta) {
		return anyTool || super.canHarvestBlock(player, meta);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return colour != null ? colour : look.getMapColor(lookMeta);
	}

	/** 26.3's `canSupportCenter`: a sturdy top, or a fence's or a wall's (1.7.10 took a fence's only). */
	@Override
	public boolean canPlaceBlockAt(net.minecraft.world.World world, int x, int y, int z) {
		return super.canPlaceBlockAt(world, x, y, z) || world.getBlock(x, y - 1, z) instanceof net.minecraft.block.BlockWall;
	}

	/** Kept while it stands as it could be placed (on a wall too). */
	@Override
	public void onNeighborBlockChange(net.minecraft.world.World world, int x, int y, int z, Block neighbour) {
		if (!canPlaceBlockAt(world, x, y, z)) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return look.getIcon(1, lookMeta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
	}
}
