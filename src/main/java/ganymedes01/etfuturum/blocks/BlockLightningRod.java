package ganymedes01.etfuturum.blocks;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.trident.Lightning;
import ganymedes01.etfuturum.world.LightningRodRecord;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * The lightning rod as 26.3's (SPEC-modern-movement.md §58.3), one block for each of its 8 kinds: the 4 weathering
 * stages, waxed or not. Meta: the way it points (a ForgeDirection, away from the face it was put on), + 8 while
 * powered.
 */
public class BlockLightningRod extends McFlyBlock implements IDegradable {

	/** How long a struck rod's signal lasts. */
	public static final int POWERED_TICKS = 8;

	private static final String[] STAGES = {"", "exposed_", "weathered_", "oxidized_"};

	private final int copperMeta;
	@SideOnly(Side.CLIENT)
	private IIcon poweredIcon;

	public BlockLightningRod(int copperMeta) {
		super(Material.iron);
		this.copperMeta = copperMeta;
		setHardness(3.0F);
		setResistance(6.0F * 5.0F / 3.0F);
		setHarvestLevel("pickaxe", 1);
		setStepSound(McFlySoundType.COPPER);
		setTickRandomly(copperMeta < 3);
		String name = STAGES[copperMeta % 4] + "lightning_rod";
		setBlockName(McFly.name(copperMeta < 8 ? name : "waxed_" + name));
		setBlockTextureName(name);
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
	public int getRenderType() {
		return RenderIDs.LIGHTNING_ROD;
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

	/** The end of the signal (the scheduled tick), else weathering (a random tick: 1.7.10 sends both here). */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		int meta = world.getBlockMetadata(x, y, z);
		if (isPowered(meta) && !LightningRodRecord.get(world).stillPowered(x, y, z, world.getTotalWorldTime())) {
			world.setBlockMetadataWithNotify(x, y, z, meta & 7, 3);
			updateAttached(world, x, y, z, meta);
		} else {
			tickDegradation(world, x, y, z, random);
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
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX, float subY, float subZ) {
		return tryWaxOnWaxOff(world, x, y, z, player);
	}

	@Override
	public int getCopperMeta(int meta) {
		return copperMeta;
	}

	@Override
	public Block getCopperBlockFromMeta(int meta) {
		return switch (meta) {
			case 1 -> McFlyBlocks.EXPOSED_LIGHTNING_ROD.get();
			case 2 -> McFlyBlocks.WEATHERED_LIGHTNING_ROD.get();
			case 3 -> McFlyBlocks.OXIDIZED_LIGHTNING_ROD.get();
			case 8 -> McFlyBlocks.WAXED_LIGHTNING_ROD.get();
			case 9 -> McFlyBlocks.WAXED_EXPOSED_LIGHTNING_ROD.get();
			case 10 -> McFlyBlocks.WAXED_WEATHERED_LIGHTNING_ROD.get();
			case 11 -> McFlyBlocks.WAXED_OXIDIZED_LIGHTNING_ROD.get();
			default -> McFlyBlocks.LIGHTNING_ROD.get();
		};
	}

	/** Facing and power stay as they are whatever the kind becomes. */
	@Override
	public int getFinalCopperMeta(IBlockAccess world, int x, int y, int z, int meta, int worldMeta) {
		return worldMeta;
	}
}
