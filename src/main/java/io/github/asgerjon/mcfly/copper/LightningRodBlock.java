package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.trident.Lightning;
import ganymedes01.etfuturum.world.LightningRodRecord;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The lightning rod as 26.3's (SPEC-modern-movement.md §58.3, §87), one block for each of its 8 kinds: the 4 weathering
 * stages, waxed or not (26.3's `WeatheringLightningRodBlock`). Meta: the way it points (a ForgeDirection, away from the
 * face it was put on), + 8 while powered. Drawn from 26.3's `lightning_rod` and `lightning_rod_on` models, turned as
 * its blockstate turns them.
 */
public class LightningRodBlock extends McFlyBlock implements WeatheringCopper, RandomTicking, ModelledBlock {

	/** How long a struck rod's signal lasts. */
	public static final int POWERED_TICKS = 8;

	private final int stage;
	private final boolean waxed;

	@SideOnly(Side.CLIENT)
	private IIcon poweredIcon;

	public LightningRodBlock(int stage, boolean waxed) {
		super(Material.iron);
		this.stage = stage;
		this.waxed = waxed;
		setHardness(3.0F);
		this.blockResistance = 6.0F * 5.0F;
		setHarvestLevel("pickaxe", 1);
		setStepSound(McFlySoundType.COPPER);
		setTickRandomly(!waxed);
		setBlockName(McFly.name(Weathering.name("lightning_rod", stage, waxed)));
		setBlockTextureName(Weathering.name("lightning_rod", stage, false));
	}

	public static Block of(int stage, boolean waxed) {
		return McFlyBlocks.valueOf(Weathering.name("lightning_rod", stage, waxed).toUpperCase()).get();
	}

	public static boolean isPowered(int meta) {
		return (meta & 8) != 0;
	}

	public static ForgeDirection facing(int meta) {
		return ForgeDirection.getOrientation(meta & 7);
	}

	/** 26.3's axis numbers, which its spark event takes: x 0, y 1, z 2. */
	private static int axis(int meta) {
		switch (meta & 7) {
			case 2:
			case 3:
				return 2;
			case 4:
			case 5:
				return 0;
			default:
				return 1;
		}
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		return side;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		switch (axis(world.getBlockMetadata(x, y, z))) {
			case 0:
				setBlockBounds(0.0F, 0.375F, 0.375F, 1.0F, 0.625F, 0.625F);
				break;
			case 2:
				setBlockBounds(0.375F, 0.375F, 0.0F, 0.625F, 0.625F, 1.0F);
				break;
			default:
				setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F);
		}
	}

	@Override
	public void setBlockBoundsForItemRender() {
		setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return Weathering.colour(stage);
	}

	// ---------------------------------------------------------------- signal

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
		return isPowered(world.getBlockMetadata(x, y, z)) ? 15 : 0;
	}

	/** Strong only into the block it's on: asked from there, side is the way the rod points. */
	@Override
	public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
		int meta = world.getBlockMetadata(x, y, z);
		return isPowered(meta) && side == (meta & 7) ? 15 : 0;
	}

	/**
	 * Lightning struck it (26.3's onLightningStrike): a signal for 8 ticks (a second strike meanwhile doesn't make it
	 * longer), the block it's on updated, and sparks along it.
	 */
	public void struck(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if (!isPowered(meta)) {
			world.setBlockMetadataWithNotify(x, y, z, meta | 8, 3);
			LightningRodRecord.get(world).powerUntil(x, y, z, world.getTotalWorldTime() + POWERED_TICKS);
		}
		updateAttached(world, x, y, z, meta);
		world.scheduleBlockUpdate(x, y, z, this, POWERED_TICKS);
		world.playAuxSFX(Lightning.SPARKS_EVENT, x, y, z, axis(meta));
	}

	/** The block it's on, which its strong signal powers: its neighbours learn of the change. */
	private void updateAttached(World world, int x, int y, int z, int meta) {
		ForgeDirection facing = facing(meta);
		world.notifyBlocksOfNeighborChange(x - facing.offsetX, y - facing.offsetY, z - facing.offsetZ, this);
	}

	/** The end of the signal (the scheduled tick; the random tick is apart, `randomTick`). */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		int meta = world.getBlockMetadata(x, y, z);
		if (isPowered(meta) && !LightningRodRecord.get(world).stillPowered(x, y, z, world.getTotalWorldTime())) {
			world.setBlockMetadataWithNotify(x, y, z, meta & 7, 3);
			updateAttached(world, x, y, z, meta);
		}
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		if (!world.isRemote) {
			LightningRodRecord.get(world).add(x, y, z);
			// as 26.3's onPlace: a rod that comes in powered (one that weathered or was cleaned mid-signal) ends its own
			if (isPowered(world.getBlockMetadata(x, y, z))) {
				world.scheduleBlockUpdate(x, y, z, this, POWERED_TICKS);
			}
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!world.isRemote) {
			LightningRodRecord.get(world).remove(x, y, z);
			if (isPowered(meta)) {
				updateAttached(world, x, y, z, meta);
			}
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	/** Electric sparks along it now and then in a thunderstorm when it's the highest block of its column. */
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		if (world.isThundering() && world.rand.nextInt(200) <= world.getTotalWorldTime() % 200L
			&& Lightning.isHighestInColumn(world, x, y, z)) {
			McFlyParticlesClient.electricSparksAlongAxis(x, y, z, axis(world.getBlockMetadata(x, y, z)), 1, 2);
		}
	}

	// ---------------------------------------------------------------- copper

	@Override
	public int stage(IBlockAccess world, int x, int y, int z) {
		return stage;
	}

	@Override
	public boolean waxed(IBlockAccess world, int x, int y, int z) {
		return waxed;
	}

	/** Its facing and power kept (a rod mid-signal ends it itself, `onBlockAdded`). */
	@Override
	public void become(World world, int x, int y, int z, int stage, boolean waxed) {
		world.setBlock(x, y, z, of(stage, waxed), world.getBlockMetadata(x, y, z), 3);
	}

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		Weathering.randomTick(this, world, x, y, z, random);
	}

	// ---------------------------------------------------------------- the look

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	/** 26.3's blockstate: up as the model is, down x 180, the sides x 90 and turned by y. */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(part(world.getBlockMetadata(x, y, z)));
	}

	private static ModelPart part(int meta) {
		String model = isPowered(meta) ? "lightning_rod_on" : "lightning_rod";
		switch (facing(meta)) {
			case DOWN:
				return new ModelPart(model, 180, 0, false);
			case NORTH:
				return new ModelPart(model, 90, 0, false);
			case SOUTH:
				return new ModelPart(model, 90, 180, false);
			case WEST:
				return new ModelPart(model, 90, 270, false);
			case EAST:
				return new ModelPart(model, 90, 90, false);
			default:
				return new ModelPart(model);
		}
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(part(ForgeDirection.UP.ordinal()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return texture.endsWith("lightning_rod_on") ? poweredIcon : blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		super.registerBlockIcons(register);
		poweredIcon = register.registerIcon("lightning_rod_on");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return isPowered(meta) ? poweredIcon : blockIcon;
	}
}
