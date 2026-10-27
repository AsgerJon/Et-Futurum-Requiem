package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Kelp (SPEC-modern-movement.md §29): a plant that stands in for a water source. Meta 8 is a stalk, 9 to 15 the tip
 * with its remaining growth (0 to 6 steps of four blocks). It grows into the water above, flowing or not, and leaves
 * a water source wherever it is removed: the way to fill a column with sources for a bubble column.
 * <p>
 * The meta stays 8 or above on purpose: 1.7.10's water code reads such a cell as a full source, and the bucket only
 * takes meta 0.
 */
public class BlockKelp extends BaseBlock implements IGrowable {

	public static final int STALK = 8;
	private static final int TIP = 9;
	private static final int MAX_REMAINING = 6;
	private static final double GROWTH_CHANCE = 0.14D;

	@SideOnly(Side.CLIENT)
	private IIcon stalkIcon;

	public BlockKelp() {
		super(Material.water);
		setNames("kelp");
		setHardness(0.0F);
		setLightOpacity(Blocks.water.getLightOpacity());
		setTickRandomly(true);
		setMapColorBaseBlock(Blocks.water);
		Utils.setBlockSound(this, ModSounds.soundWetGrass);
	}

	public static boolean isTip(int meta) {
		return meta != STALK;
	}

	/**
	 * How many more four-block steps this tip may grow; 0 when it is done.
	 */
	private static int remainingGrowth(int meta) {
		return isTip(meta) ? MathHelper.clamp_int(meta - TIP, 0, MAX_REMAINING) : 0;
	}

	private static int tipMeta(int remaining) {
		return TIP + MathHelper.clamp_int(remaining, 0, MAX_REMAINING);
	}

	/**
	 * A fresh tip, placed or uncovered, may grow 1 to 6 more steps (modern: 1 to 25 more blocks).
	 */
	private static int freshTipMeta(Random random) {
		return tipMeta(1 + random.nextInt(MAX_REMAINING));
	}

	/**
	 * Vanilla water, still or flowing: what a tip grows into.
	 */
	public static boolean isVanillaWater(Block block) {
		return block == Blocks.water || block == Blocks.flowing_water;
	}

	/**
	 * Where kelp can be put by hand: a water source, falling water or a bubble column cell (modern: a full water
	 * fluid).
	 */
	private static boolean canBePlacedIn(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block instanceof BlockBubbleColumn) {
			return true;
		}
		if (!isVanillaWater(block)) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		return meta == 0 || meta >= 8;
	}

	/**
	 * Kelp stands on kelp or on a block with a solid top, but never on magma.
	 */
	public boolean canBlockStay(World world, int x, int y, int z) {
		Block below = world.getBlock(x, y - 1, z);
		if (below == this) {
			return true;
		}
		if (below == ModBlocks.MAGMA.get()) {
			return false;
		}
		return world.isSideSolid(x, y - 1, z, ForgeDirection.UP);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return canBePlacedIn(world, x, y, z) && canBlockStay(world, x, y, z);
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		return freshTipMeta(world.rand);
	}

	/**
	 * Without support the kelp drops and leaves water, which takes the kelp above with it one block at a time. A tip
	 * with kelp above it becomes a stalk; a stalk with none becomes a fresh tip.
	 */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!canBlockStay(world, x, y, z)) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlock(x, y, z, Blocks.water, 0, 3);
			return;
		}
		boolean kelpAbove = world.getBlock(x, y + 1, z) == this;
		int meta = world.getBlockMetadata(x, y, z);
		if (kelpAbove && isTip(meta)) {
			world.setBlockMetadataWithNotify(x, y, z, STALK, 2);
		} else if (!kelpAbove && !isTip(meta)) {
			world.setBlockMetadataWithNotify(x, y, z, freshTipMeta(world.rand), 2);
		}
	}

	/**
	 * A tip with growth left grows with chance 0.14 per random tick when there is vanilla water above it.
	 */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		int meta = world.getBlockMetadata(x, y, z);
		if (isTip(meta) && remainingGrowth(meta) > 0 && random.nextDouble() < GROWTH_CHANCE
				&& isVanillaWater(world.getBlock(x, y + 1, z))) {
			grow(world, x, y, z, meta);
		}
	}

	/**
	 * Puts the next tip in the water above; this block turns into a stalk when it notices it. The remaining growth
	 * drops by one whenever the strand's height reaches a multiple of four.
	 */
	private void grow(World world, int x, int y, int z, int meta) {
		int height = 1;
		while (world.getBlock(x, y - height, z) == this) {
			height++;
		}
		int remaining = remainingGrowth(meta);
		if ((height + 1) % 4 == 0) {
			remaining--;
		}
		world.setBlock(x, y + 1, z, this, tipMeta(remaining), 3);
	}

	/**
	 * The tip of the strand this block belongs to.
	 */
	private int tipY(World world, int x, int y, int z) {
		while (world.getBlock(x, y + 1, z) == this) {
			y++;
		}
		return y;
	}

	/**
	 * MCP name: {@code canFertilize}. Bone meal on any part grows the tip by one block if there is water above it,
	 * however much it has grown already, as in modern.
	 */
	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		return isVanillaWater(world.getBlock(x, tipY(world, x, y, z) + 1, z));
	}

	/**
	 * MCP name: {@code shouldFertilize}
	 */
	@Override
	public boolean func_149852_a(World world, Random random, int x, int y, int z) {
		return true;
	}

	/**
	 * MCP name: {@code fertilize}
	 */
	@Override
	public void func_149853_b(World world, Random random, int x, int y, int z) {
		int tipY = tipY(world, x, y, z);
		if (isVanillaWater(world.getBlock(x, tipY + 1, z))) {
			grow(world, x, tipY, z, world.getBlockMetadata(x, tipY, z));
		}
	}

	/**
	 * Removing kelp leaves the water source it stood in for.
	 */
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return world.setBlock(x, y, z, Blocks.water, 0, 3);
	}

	@Override
	public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
		world.setBlock(x, y, z, Blocks.water, 0, 3);
		onBlockDestroyedByExplosion(world, x, y, z, explosion);
	}

	/**
	 * Not replaceable like water: placing a block against kelp goes to the block next to it, so kelp stacks by
	 * clicking its top.
	 */
	@Override
	public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
		return false;
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
		return RenderIDs.KELP;
	}

	@Override
	public int getRenderBlockPass() {
		return 1;
	}

	@Override
	public boolean canRenderInPass(int pass) {
		return true;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	/**
	 * The tip is 9/16 tall to the cursor, the stalk a full block, as modern's shapes.
	 */
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, isTip(world.getBlockMetadata(x, y, z)) ? 0.5625F : 1.0F, 1.0F);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon(getTextureName());
		stalkIcon = register.registerIcon(getTextureName() + "_plant");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return isTip(meta) ? blockIcon : stalkIcon;
	}

	/**
	 * The item uses the item sprite, not the block texture.
	 */
	@Override
	public String getItemIconName() {
		return getTextureName();
	}
}
