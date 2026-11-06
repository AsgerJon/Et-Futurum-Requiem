package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyMultiface;
import io.github.asgerjon.mcfly.base.MultifaceSpreader;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.nether.NetherLightItem;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.3's sculk vein (`SculkVeinBlock`; SPEC-modern-movement.md §90.9, step H3): McFly's multiface (glow lichen's), 0.2,
 * the hoe, no collision, popped by pistons, map colour black. To Silk Touch it drops one a face, else nothing (shears
 * neither). A catalyst's charge on it turns the block under a face to sculk and spreads veins round it; veins over sculk
 * lose those faces.
 */
public class SculkVeinBlock extends McFlyMultiface implements SculkBehaviour, NetherLightItem.Flat {

	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	private static final ModelPart[] PARTS = {new ModelPart("sculk_vein", 90, 0, true), new ModelPart("sculk_vein", 270, 0, true),
		new ModelPart("sculk_vein"), new ModelPart("sculk_vein", 0, 180, true), new ModelPart("sculk_vein", 0, 270, true),
		new ModelPart("sculk_vein", 0, 90, true)};

	private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new VeinConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
	private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(new VeinConfig(MultifaceSpreader.SpreadType.SAME_POSITION));

	/** The faces of the vein being broken now (`onBlockHarvested` to `harvestBlock`: its block entity is gone between). */
	private static int harvested;

	public SculkVeinBlock() {
		super(Material.vine);
		setBlockName(McFly.name("sculk_vein"));
		setBlockTextureName("sculk_vein");
		setHardness(0.2F);
		this.blockResistance = 0.2F * 5.0F;
		setStepSound(McFlySoundType.SCULK_VEIN);
		setHarvestLevel("hoe", 0);
	}

	/** Its faces' block entity, registered as McFly's `sculk_vein`. */
	public static final class Tile extends io.github.asgerjon.mcfly.base.McFlyMultifaceTile {
	}

	@Override
	public net.minecraft.tileentity.TileEntity createNewTileEntity(World world, int meta) {
		return new Tile();
	}

	/** The registered vein, or null. */
	static SculkVeinBlock vein() {
		return McFlyBlocks.SCULK_VEIN.isEnabled() ? (SculkVeinBlock) McFlyBlocks.SCULK_VEIN.get() : null;
	}

	@Override
	public MultifaceSpreader spreader() {
		return veinSpreader;
	}

	public MultifaceSpreader sameSpaceSpreader() {
		return sameSpaceSpreader;
	}

	// ---------------------------------------------------------------- 26.3's SculkVeinBlock

	/** 26.3's `regrow`: a vein in the space on each of these faces that can hold one; false if none can. */
	static boolean regrow(World world, int x, int y, int z, int facings) {
		SculkVeinBlock vein = vein();
		if (vein == null) {
			return false;
		}
		int faces = 0;
		for (int face = 0; face < 6; face++) {
			if ((facings & 1 << face) != 0 && canAttach(world, x, y, z, face)) {
				faces |= 1 << face;
			}
		}
		if (faces == 0) {
			return false;
		}
		world.setBlock(x, y, z, vein, 0, 3);
		vein.setFaces(world, x, y, z, faces);
		return true;
	}

	/** 26.3's `onDischarged`: its faces against sculk go; none left, it's gone. */
	@Override
	public void onDischarged(World world, int x, int y, int z, Random random) {
		if (world.getBlock(x, y, z) != this) {
			return;
		}
		int faces = faces(world, x, y, z);
		Block sculk = McFlyBlocks.SCULK.get();
		for (int face = 0; face < 6; face++) {
			if (has(faces, face) && world.getBlock(x + OFFSET[face][0], y + OFFSET[face][1], z + OFFSET[face][2]) == sculk) {
				faces &= ~(1 << face);
			}
		}
		setFaces(world, x, y, z, faces);
		world.markBlockForUpdate(x, y, z);
	}

	/** 26.3's `attemptUseCharge`: sculk placed under a face (one charge), else on 1 in 10 the charge halved. */
	@Override
	public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, World world, int ox, int oy, int oz, Random random, SculkSpreader spreader, boolean spreadVeins) {
		if (spreadVeins && attemptPlaceSculk(world, cursor.x(), cursor.y(), cursor.z(), random)) {
			return cursor.charge() - 1;
		}
		return random.nextInt(spreader.chargeDecayRate()) == 0 ? (int) Math.floor(cursor.charge() * 0.5F) : cursor.charge();
	}

	/** 26.3's `attemptPlaceSculk`: on a face, in a random order, whose block is replaceable: sculk, veins round it, the veins over it gone. */
	private boolean attemptPlaceSculk(World world, int x, int y, int z, Random random) {
		int faces = faces(world, x, y, z);
		for (int support : shuffled(random)) {
			if (!has(faces, support)) {
				continue;
			}
			int sx = x + OFFSET[support][0], sy = y + OFFSET[support][1], sz = z + OFFSET[support][2];
			if (SculkReplaceable.is(world.getBlock(sx, sy, sz), world.getBlockMetadata(sx, sy, sz))) {
				Block sculk = McFlyBlocks.SCULK.get();
				world.setBlock(sx, sy, sz, sculk, 0, 3);
				world.playSoundEffect(sx + 0.5D, sy + 0.5D, sz + 0.5D, McFly.MC_ASSETS + ":block.sculk.spread", 1.0F, 1.0F);
				veinSpreader.spreadAll(world, sx, sy, sz, sculk, 0, false);
				int skip = support ^ 1;
				for (int direction = 0; direction < 6; direction++) {
					if (direction == skip) {
						continue;
					}
					int vx = sx + OFFSET[direction][0], vy = sy + OFFSET[direction][1], vz = sz + OFFSET[direction][2];
					if (world.getBlock(vx, vy, vz) == this) {
						onDischarged(world, vx, vy, vz, random);
					}
				}
				return true;
			}
		}
		return false;
	}

	/** 26.3's `hasSubstrateAccess`: a vein with a face on a replaceable block. */
	static boolean hasSubstrateAccess(World world, int x, int y, int z) {
		SculkVeinBlock vein = vein();
		if (vein == null || world.getBlock(x, y, z) != vein) {
			return false;
		}
		int faces = faces(world, x, y, z);
		for (int face = 0; face < 6; face++) {
			int nx = x + OFFSET[face][0], ny = y + OFFSET[face][1], nz = z + OFFSET[face][2];
			if (has(faces, face) && SculkReplaceable.is(world.getBlock(nx, ny, nz), world.getBlockMetadata(nx, ny, nz))) {
				return true;
			}
		}
		return false;
	}

	private static List<Integer> shuffled(Random random) {
		List<Integer> order = new ArrayList<>(6);
		for (int face = 0; face < 6; face++) {
			order.add(face);
		}
		Collections.shuffle(order, random);
		return order;
	}

	/** 26.3's `SculkVeinSpreaderConfig`. */
	private final class VeinConfig extends MultifaceSpreader.DefaultConfig {
		private final MultifaceSpreader.SpreadType[] types;

		VeinConfig(MultifaceSpreader.SpreadType... types) {
			super(SculkVeinBlock.this);
			this.types = types;
		}

		/**
		 * Not against sculk or a catalyst; not past a corner with a sturdy side; not into another fluid than water, nor
		 * fire; into what's replaceable, or air or a vein.
		 */
		@Override
		protected boolean stateCanBeReplaced(IBlockAccess world, int sx, int sy, int sz, MultifaceSpreader.SpreadPos spread, Block existing) {
			int ax = spread.x + OFFSET[spread.face][0], ay = spread.y + OFFSET[spread.face][1], az = spread.z + OFFSET[spread.face][2];
			Block against = world.getBlock(ax, ay, az);
			if (against == McFlyBlocks.SCULK.get() || against == McFlyBlocks.SCULK_CATALYST.get() || against == Blocks.piston_extension) {
				return false;
			}
			if (Math.abs(sx - spread.x) + Math.abs(sy - spread.y) + Math.abs(sz - spread.z) == 2) {
				int back = spread.face ^ 1;
				int nx = sx + OFFSET[back][0], ny = sy + OFFSET[back][1], nz = sz + OFFSET[back][2];
				if (world.getBlock(nx, ny, nz).isSideSolid(world, nx, ny, nz, ForgeDirection.getOrientation(spread.face))) {
					return false;
				}
			}
			if (existing.getMaterial().isLiquid() && existing.getMaterial() != Material.water || existing == Blocks.fire) {
				return false;
			}
			return existing.isReplaceable(world, spread.x, spread.y, spread.z) || super.stateCanBeReplaced(world, sx, sy, sz, spread, existing);
		}

		@Override
		public MultifaceSpreader.SpreadType[] spreadTypes() {
			return types;
		}

		@Override
		public boolean isOtherBlockValidAsSource(Block block) {
			return block != SculkVeinBlock.this;
		}
	}

	// ---------------------------------------------------------------- drops, the look

	@Override
	public boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
		harvested = faces(world, x, y, z);
		super.onBlockHarvested(world, x, y, z, meta, player);
	}

	/** 26.3's loot: to Silk Touch, one a face. */
	@Override
	public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
		int faces = harvested;
		harvested = 0;
		if (!world.isRemote && EnchantmentHelper.getSilkTouchModifier(player) && faces != 0) {
			dropBlockAsItem(world, x, y, z, new ItemStack(this, Integer.bitCount(faces)));
		}
	}

	@Override
	public int getMobilityFlag() {
		return 1;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.blackColor;
	}

	@Override
	protected ModelPart[] faceParts() {
		return PARTS;
	}

	@Override
	public String itemTexture() {
		return "sculk_vein";
	}
}
