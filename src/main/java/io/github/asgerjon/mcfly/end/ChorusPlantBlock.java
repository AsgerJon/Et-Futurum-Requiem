package io.github.asgerjon.mcfly.end;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.3's chorus plant (`ChorusPlantBlock`, `PipeBlock` of size 10; SPEC-modern-movement.md §84): its six arms worked out
 * from its neighbours (another plant or a flower; down also end stone, 26.3's `supports_chorus_plant`), not stored;
 * standing as 26.3's `canSurvive` says, else broken a tick later dropping 0 or 1 chorus fruit; 0.4 to break, wood
 * sounds, popped by pistons, quicker with an axe. Drawn by 26.3's multipart blockstate.
 */
public class ChorusPlantBlock extends McFlyBlock implements ModelledBlock {

	/** 1.7.10's directions in 26.3's blockstate order: north, east, south, west, up, down. */
	private static final ForgeDirection[] PARTS = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST,
		ForgeDirection.UP, ForgeDirection.DOWN};
	/** Each part's turn (x, y) in 26.3's blockstate: north as the models are, east y 90, south 180, west 270, up x 270, down x 90. */
	private static final int[][] TURNS = {{0, 0}, {0, 90}, {0, 180}, {0, 270}, {270, 0}, {90, 0}};
	/** The models for a side without an arm, per part in 26.3's order, with their weights. */
	private static final String[][] NO_SIDE = {
		{"chorus_plant_noside", "chorus_plant_noside1", "chorus_plant_noside2", "chorus_plant_noside3"},
		{"chorus_plant_noside1", "chorus_plant_noside2", "chorus_plant_noside3", "chorus_plant_noside"},
		{"chorus_plant_noside2", "chorus_plant_noside3", "chorus_plant_noside", "chorus_plant_noside1"},
		{"chorus_plant_noside3", "chorus_plant_noside", "chorus_plant_noside1", "chorus_plant_noside2"},
		{"chorus_plant_noside", "chorus_plant_noside3", "chorus_plant_noside1", "chorus_plant_noside2"},
		{"chorus_plant_noside3", "chorus_plant_noside2", "chorus_plant_noside1", "chorus_plant_noside"}};

	public ChorusPlantBlock() {
		super(Material.plants);
		setNames("chorus_plant");
		setHardness(0.4F);
		this.blockResistance = 0.4F * 5.0F;
		setStepSound(soundTypeWood);
		setHarvestLevel("axe", 0);
	}

	// ---------------------------------------------------------------- arms and footing

	/** Whether the plant at (x, y, z) has its arm toward that side (26.3's `getStateWithConnections`). */
	public boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
		Block block = world.getBlock(x + side.offsetX, y + side.offsetY, z + side.offsetZ);
		return block == this || block == McFlyBlocks.CHORUS_FLOWER.get() || side == ForgeDirection.DOWN && supportsPlant(block);
	}

	/** 26.3's `supports_chorus_plant` (and `supports_chorus_flower`): end stone. */
	static boolean supportsPlant(Block block) {
		return block == Blocks.end_stone;
	}

	/** 26.3's `canSurvive`. */
	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		Block below = world.getBlock(x, y - 1, z);
		boolean aboveAndBelow = !world.isAirBlock(x, y + 1, z) && !world.isAirBlock(x, y - 1, z);
		for (ForgeDirection side : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST}) {
			int nx = x + side.offsetX;
			int nz = z + side.offsetZ;
			if (world.getBlock(nx, y, nz) == this) {
				if (aboveAndBelow) {
					return false;
				}
				Block underNeighbour = world.getBlock(nx, y - 1, nz);
				if (underNeighbour == this || supportsPlant(underNeighbour)) {
					return true;
				}
			}
		}
		return below == this || supportsPlant(below);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
	}

	/** 26.3's `updateShape`: a plant that can't stand is broken a tick later. */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (!canBlockStay(world, x, y, z)) {
			world.scheduleBlockUpdate(x, y, z, this, 1);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (!canBlockStay(world, x, y, z)) {
			world.func_147480_a(x, y, z, true);
		}
	}

	/** 26.3's loot: 0 or 1 chorus fruit. */
	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return McFlyItems.CHORUS_FRUIT.get();
	}

	@Override
	public int quantityDropped(Random random) {
		return random.nextInt(2);
	}

	// ---------------------------------------------------------------- shape

	/** 26.3's `PipeBlock` of size 10 in pixels: the core 3 to 13, each arm 3 to 13 across from its side to the middle. */
	private List<float[]> parts(IBlockAccess world, int x, int y, int z) {
		List<float[]> parts = new ArrayList<>();
		parts.add(new float[]{3, 3, 3, 13, 13, 13});
		for (ForgeDirection side : PARTS) {
			if (!connects(world, x, y, z, side)) {
				continue;
			}
			switch (side) {
				case NORTH:
					parts.add(new float[]{3, 3, 0, 13, 13, 8});
					break;
				case SOUTH:
					parts.add(new float[]{3, 3, 8, 13, 13, 16});
					break;
				case WEST:
					parts.add(new float[]{0, 3, 3, 8, 13, 13});
					break;
				case EAST:
					parts.add(new float[]{8, 3, 3, 16, 13, 13});
					break;
				case UP:
					parts.add(new float[]{3, 8, 3, 13, 16, 13});
					break;
				default:
					parts.add(new float[]{3, 0, 3, 13, 8, 13});
					break;
			}
		}
		return parts;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		float[] all = {16, 16, 16, 0, 0, 0};
		for (float[] part : parts(world, x, y, z)) {
			for (int i = 0; i < 3; i++) {
				all[i] = Math.min(all[i], part[i]);
				all[i + 3] = Math.max(all[i + 3], part[i + 3]);
			}
		}
		setBlockBounds(all[0] / 16, all[1] / 16, all[2] / 16, all[3] / 16, all[4] / 16, all[5] / 16);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		for (float[] p : parts(world, x, y, z)) {
			AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + p[0] / 16, y + p[1] / 16, z + p[2] / 16, x + p[3] / 16, y + p[4] / 16, z + p[5] / 16);
			if (mask.intersectsWith(box)) {
				list.add(box);
			}
		}
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
	public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	// ---------------------------------------------------------------- the look

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	/**
	 * 26.3's multipart blockstate: an arm model toward each neighbour it joins, one of the four armless models toward each
	 * other side (weights 2, 1, 1, 1 for `noside`, `noside1` to `3`), chosen by the place as `ChorusVariants` does.
	 */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		List<ModelPart> parts = new ArrayList<>();
		for (int i = 0; i < PARTS.length; i++) {
			boolean uvlock = i != 0;
			if (connects(world, x, y, z, PARTS[i])) {
				parts.add(new ModelPart("chorus_plant_side", TURNS[i][0], TURNS[i][1], uvlock));
			} else {
				parts.add(new ModelPart(ChorusVariants.pick(NO_SIDE[i], x, y, z), TURNS[i][0], TURNS[i][1], true));
			}
		}
		return parts;
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart("chorus_plant"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return blockIcon;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.purpleColor;
	}
}
