package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Copper doors (SPEC-modern-movement.md §87), EFR's 8 blocks (`copper_door` ... `waxed_oxidized_copper_door`), their
 * metas 1.7.10's doors'. 26.3's `BlockSetType.COPPER`: opened by hand as by redstone, `block.copper_door.open` and
 * `close`; 3 and 6, dropping by hand too (26.3's door has no tool rule), broken by a piston; it ages from its lower
 * half, and both halves change.
 */
public class CopperDoorBlock extends BlockDoor implements WeatheringCopper, RandomTicking, io.github.asgerjon.mcfly.base.McFlySetType.Typed {

	/** Not 1.7.10's iron (which no hand opens), and needing no tool. */
	private static final Material COPPER = new Material(MapColor.adobeColor);

	private final int stage;
	private final boolean waxed;

	public CopperDoorBlock(int stage, boolean waxed) {
		super(COPPER);
		this.stage = stage;
		this.waxed = waxed;
		setBlockName(McFly.name(Weathering.name("copper_door", stage, waxed)));
		setBlockTextureName(Weathering.name("copper_door", stage, false));
		setHardness(3.0F);
		this.blockResistance = 6.0F * 5.0F;
		setStepSound(McFlySoundType.COPPER);
		setHarvestLevel("pickaxe", 1);
		setTickRandomly(!waxed);
		setCreativeTab(io.github.asgerjon.mcfly.registry.McFlyTab.TAB);
	}

	/** 26.3's `BlockSetType.COPPER` (spec §91.6: its redstone by the powered bit, its wind burst, by these). */
	@Override
	public io.github.asgerjon.mcfly.base.McFlySetType setType() {
		return io.github.asgerjon.mcfly.base.McFlySetType.COPPER;
	}

	/** The item's texture (`items/copper_door` and the stages'). */
	public String itemTexture() {
		return Weathering.name("copper_door", stage, false);
	}

	public static Block of(int stage, boolean waxed) {
		return McFlyBlocks.valueOf(Weathering.name("copper_door", stage, waxed).toUpperCase()).get();
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		int full = func_150012_g(world, x, y, z);
		toggle(world, x, y, z, full);
		return true;
	}

	/** The redstone's open and close. */
	@Override
	public void func_150014_a(World world, int x, int y, int z, boolean open) {
		int full = func_150012_g(world, x, y, z);
		if (((full & 4) != 0) != open) {
			toggle(world, x, y, z, full);
		}
	}

	private void toggle(World world, int x, int y, int z, int full) {
		int lower = (full & 7) ^ 4;
		int lowerY = (full & 8) == 0 ? y : y - 1;
		world.setBlockMetadataWithNotify(x, lowerY, z, lower, 2);
		world.markBlockRangeForRenderUpdate(x, lowerY, z, x, lowerY + 1, z);
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ((lower & 4) != 0 ? ":block.copper_door.open" : ":block.copper_door.close"), 1.0F, 1.0F);
	}

	@Override
	public int stage(IBlockAccess world, int x, int y, int z) {
		return stage;
	}

	@Override
	public boolean waxed(IBlockAccess world, int x, int y, int z) {
		return waxed;
	}

	@Override
	public boolean agesHere(IBlockAccess world, int x, int y, int z) {
		return world.getBlockMetadata(x, y, z) < 8;
	}

	/** Both halves, neither told of the other till both are (a half whose other isn't this door breaks). */
	@Override
	public void become(World world, int x, int y, int z, int stage, boolean waxed) {
		Block door = of(stage, waxed);
		int lowerY = world.getBlockMetadata(x, y, z) >= 8 ? y - 1 : y;
		int lowerMeta = world.getBlockMetadata(x, lowerY, z);
		int upperMeta = world.getBlockMetadata(x, lowerY + 1, z);
		boolean whole = world.getBlock(x, lowerY + 1, z) == this && world.getBlock(x, lowerY, z) == this;
		world.setBlock(x, lowerY, z, door, lowerMeta, 2);
		if (whole) {
			world.setBlock(x, lowerY + 1, z, door, upperMeta, 2);
		}
		world.notifyBlocksOfNeighborChange(x, lowerY, z, door);
		world.notifyBlocksOfNeighborChange(x, lowerY + 1, z, door);
	}

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		Weathering.randomTick(this, world, x, y, z, random);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return (meta & 8) != 0 ? null : Item.getItemFromBlock(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(this);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return Weathering.colour(stage);
	}

	/** 1.7.10's door asks for `<texture>_upper` and `_lower`; 26.3's are `_top` and `_bottom`. */
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		super.registerBlockIcons(name -> register.registerIcon(name.replace("_upper", "_top").replace("_lower", "_bottom")));
	}
}
