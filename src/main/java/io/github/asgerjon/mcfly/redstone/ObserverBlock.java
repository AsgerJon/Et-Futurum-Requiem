package io.github.asgerjon.mcfly.redstone;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.3's observer (`ObserverBlock`; SPEC-modern-movement.md §86): meta its facing as a piston's (0 down to 5 east: the
 * way its face looks), plus 8 while powered. A change of the block it faces (`observe`, from
 * `MixinWorldObserverUpdates`), while it's unpowered, starts a signal: a tick in 2, unless one is due already; that tick
 * powers it and schedules another in 2, which unpowers it; each change of its own updates the block behind it and
 * that block's other neighbours. It gives 15 out of its back only, strongly; it conducts no redstone itself (26.3's
 * `isRedstoneConductor` never), though its faces are solid. Placed powered with no tick due (a piston's move), it
 * unpowers at once; broken powered with a tick due, the block behind is told. 3 to break, a pickaxe for drops; placed
 * looking where the player looks. Drawn from 26.3's `observer` and `observer_on` models.
 */
public class ObserverBlock extends McFlyBlock implements ModelledBlock {

	private static final int DELAY = 2;

	/** How many chunks this thread is populating (`MixinChunkPopulating`): observers see nothing while it's above 0. */
	public static final ThreadLocal<int[]> POPULATING = ThreadLocal.withInitial(() -> new int[1]);

	public static boolean populating() {
		return POPULATING.get()[0] > 0;
	}

	@SideOnly(Side.CLIENT)
	private IIcon front, side, top, back, backOn;

	public ObserverBlock() {
		super(Material.rock);
		setNames("observer");
		setHardness(3.0F);
		this.blockResistance = 3.0F * 5.0F;
		setStepSound(soundTypePiston);
		setHarvestLevel("pickaxe", 0);
	}

	public static int facing(int meta) {
		return meta & 7;
	}

	public static boolean powered(int meta) {
		return (meta & 8) != 0;
	}

	// ---------------------------------------------------------------- the signal

	/** The block at (x, y, z) changed: each observer next to it that faces it may start a signal. */
	public static void blockChanged(World world, int x, int y, int z) {
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			int ox = x + direction.offsetX;
			int oy = y + direction.offsetY;
			int oz = z + direction.offsetZ;
			if (oy < 0 || oy >= world.getHeight() || !world.blockExists(ox, oy, oz)) {
				continue;
			}
			Block block = world.getBlock(ox, oy, oz);
			// it faces the changed block when its facing points back the way we came
			if (block instanceof ObserverBlock && facing(world.getBlockMetadata(ox, oy, oz)) == direction.getOpposite().ordinal()) {
				((ObserverBlock) block).observe(world, ox, oy, oz);
			}
		}
	}

	/** 26.3's `updateShape` from the faced block: unpowered, start a signal. */
	private void observe(World world, int x, int y, int z) {
		if (!world.isRemote && !powered(world.getBlockMetadata(x, y, z)) && !tickDue(world, x, y, z)) {
			world.scheduleBlockUpdate(x, y, z, this, DELAY);
		}
	}

	private boolean tickDue(World world, int x, int y, int z) {
		return world instanceof WorldServer
			&& ((io.github.asgerjon.mcfly.mixins.early.redstone.AccessorWorldServerTicks) world).mcfly$pendingTicks().contains(new NextTickListEntry(x, y, z, this));
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		int meta = world.getBlockMetadata(x, y, z);
		if (powered(meta)) {
			world.setBlockMetadataWithNotify(x, y, z, meta & 7, 2);
		} else {
			world.setBlockMetadataWithNotify(x, y, z, meta | 8, 2);
			world.scheduleBlockUpdate(x, y, z, this, DELAY);
		}
		updateBehind(world, x, y, z, meta);
	}

	/** 26.3's `updateNeighborsInFront`: the block behind it, then that block's neighbours but this side. */
	private void updateBehind(World world, int x, int y, int z, int meta) {
		ForgeDirection behind = ForgeDirection.getOrientation(facing(meta)).getOpposite();
		int bx = x + behind.offsetX;
		int by = y + behind.offsetY;
		int bz = z + behind.offsetZ;
		world.notifyBlockOfNeighborChange(bx, by, bz, this);
		world.notifyBlocksOfNeighborChange(bx, by, bz, this, facing(meta));
	}

	/** 26.3's `onPlace`: placed powered with no tick due (moved by a piston), it unpowers at once. */
	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if (!world.isRemote && powered(meta) && !tickDue(world, x, y, z)) {
			world.setBlockMetadataWithNotify(x, y, z, meta & 7, 2);
			updateBehind(world, x, y, z, meta);
		}
	}

	/** 26.3's `affectNeighborsAfterRemoval`: broken powered with a tick due, the block behind hears of it. */
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!world.isRemote && powered(meta) && tickDue(world, x, y, z)) {
			updateBehind(world, x, y, z, meta);
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	/** 15 out of its back: asked from behind (the asker's side toward it is its facing), while powered. */
	@Override
	public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
		int meta = world.getBlockMetadata(x, y, z);
		return powered(meta) && facing(meta) == side ? 15 : 0;
	}

	@Override
	public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
		return isProvidingWeakPower(world, x, y, z, side);
	}

	/** 26.3's `isRedstoneConductor` never: power doesn't pass through it. */
	@Override
	public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	/** A full block: torches, levers and such stand on any face. */
	@Override
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
		return true;
	}

	/** Dust joins it only at its back (26.3's `shouldRedstoneWireConnectTo`). */
	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
		// 1.7.10's side: -1 up, 0 north (-z), 1 east, 2 south, 3 west, as the dust sees the block
		int facing = facing(world.getBlockMetadata(x, y, z));
		switch (side) {
			case 0:
				return facing == ForgeDirection.SOUTH.ordinal();
			case 1:
				return facing == ForgeDirection.WEST.ordinal();
			case 2:
				return facing == ForgeDirection.NORTH.ordinal();
			case 3:
				return facing == ForgeDirection.EAST.ordinal();
			default:
				return false;
		}
	}

	/** 26.3's `getNearestLookingDirection`: the axis the player looks along most, the way they look. */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		Vec3 look = placer.getLookVec();
		double ax = Math.abs(look.xCoord);
		double ay = Math.abs(look.yCoord);
		double az = Math.abs(look.zCoord);
		ForgeDirection facing;
		if (ay >= ax && ay >= az) {
			facing = look.yCoord > 0 ? ForgeDirection.UP : ForgeDirection.DOWN;
		} else if (ax >= az) {
			facing = look.xCoord > 0 ? ForgeDirection.EAST : ForgeDirection.WEST;
		} else {
			facing = look.zCoord > 0 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
		}
		world.setBlockMetadataWithNotify(x, y, z, facing.ordinal(), 2);
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.stoneColor;
	}

	// ---------------------------------------------------------------- the look

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	/** 26.3's blockstate: north as the model is, the sides turned by y, down x 90, up x 270. */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(part(world.getBlockMetadata(x, y, z)));
	}

	private static ModelPart part(int meta) {
		String model = powered(meta) ? "observer_on" : "observer";
		switch (ForgeDirection.getOrientation(facing(meta))) {
			case DOWN:
				return new ModelPart(model, 90, 0, false);
			case UP:
				return new ModelPart(model, 270, 0, false);
			case SOUTH:
				return new ModelPart(model, 0, 180, false);
			case WEST:
				return new ModelPart(model, 0, 270, false);
			case EAST:
				return new ModelPart(model, 0, 90, false);
			default:
				return new ModelPart(model);
		}
	}

	/** Its item: facing south, so its face looks at the viewer as 26.3's item model shows it. */
	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(part(ForgeDirection.SOUTH.ordinal()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		if (texture.endsWith("observer_back_on")) {
			return backOn;
		}
		if (texture.endsWith("observer_back")) {
			return back;
		}
		if (texture.endsWith("observer_top")) {
			return top;
		}
		if (texture.endsWith("observer_side")) {
			return side;
		}
		return front;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int sideNumber, int meta) {
		return front;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
		front = icons.registerIcon("observer_front");
		side = icons.registerIcon("observer_side");
		top = icons.registerIcon("observer_top");
		back = icons.registerIcon("observer_back");
		backOn = icons.registerIcon("observer_back_on");
		blockIcon = front;
	}
}
