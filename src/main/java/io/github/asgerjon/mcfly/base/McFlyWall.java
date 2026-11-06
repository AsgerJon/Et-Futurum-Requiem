package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockWall;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A wall of ours as 26.3's `WallBlock` (SPEC-modern-movement.md §84.1), on 1.7.10's (so 1.7.10's walls and other mods'
 * take it as a wall). 26.3's states are worked out from the world each time, not stored (the meta is the variant, 0):
 * each side none, low or tall, and the post, by 26.3's rules; its outline, collision (24 high) and model from them. Its
 * look is its block's texture in 26.3's wall models.
 */
public class McFlyWall extends BlockWall implements ModelledBlock, NamedMetas {

	public enum WallSide {
		NONE, LOW, TALL;

		@Override
		public String toString() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** A wall's 26.3 state at a place: its post and its four sides. */
	public static final class Shape {
		public final boolean post;
		public final WallSide north;
		public final WallSide east;
		public final WallSide south;
		public final WallSide west;

		Shape(boolean post, WallSide north, WallSide east, WallSide south, WallSide west) {
			this.post = post;
			this.north = north;
			this.east = east;
			this.south = south;
			this.west = west;
		}

		@Override
		public String toString() {
			return (post ? "post" : "-") + " n=" + north + " e=" + east + " s=" + south + " w=" + west;
		}
	}

	// 26.3's test shapes, in pixels as (minX, minZ, maxX, maxZ): the post's 2 by 2 middle, each side's 2-wide strip from
	// its edge to 9 in (`TEST_SHAPE_POST`, `TEST_SHAPES_WALL`)
	private static final float[] TEST_POST = {7, 7, 9, 9};
	private static final float[][] TEST_SIDE = {{7, 0, 9, 9}, {7, 7, 16, 9}, {7, 7, 9, 16}, {0, 7, 9, 9}};
	private static final ForgeDirection[] SIDES = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST};

	private final Block base;
	private Block[] bases;
	private final String[] names;
	private final int[] baseMetas;
	private final float[] hardness;
	private final float[] resistance;

	public McFlyWall(Block base, String name) {
		this(base, new String[]{name}, new int[]{0});
	}

	/**
	 * A wall of variants (spec §88.2): meta i is 26.3's wall `names[i]`, of the block's meta `baseMetas[i]`; its item
	 * {@link McFlyMetaItem}.
	 */
	public McFlyWall(Block base, String[] names, int[] baseMetas) {
		this(fill(base, names.length), names, baseMetas);
	}

	private static Block[] fill(Block base, int count) {
		Block[] bases = new Block[count];
		java.util.Arrays.fill(bases, base);
		return bases;
	}

	/** A wall of variants from several blocks (spec §89: cobbled and polished deepslate's): meta i of `bases[i]`'s `baseMetas[i]`. */
	public McFlyWall(Block[] bases, String[] names, int[] baseMetas) {
		super(bases[0]);
		this.base = bases[0];
		this.bases = bases;
		this.names = names;
		this.baseMetas = baseMetas;
		this.hardness = new float[names.length];
		this.resistance = new float[names.length];
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name(names[0]));
		setHarvestLevel(base.getHarvestTool(baseMetas[0]), base.getHarvestLevel(baseMetas[0]));
		for (int i = 0; i < names.length; i++) {
			hardness[i] = blockHardness;
			resistance[i] = blockResistance / 5.0F;
		}
	}

	/** 26.3's strength, every variant: 1.7.10's explosion resistance is kept as 26.3's number (1.7.10 divides the field by 5). */
	public McFlyWall strength(float hardness, float resistance) {
		setHardness(hardness);
		this.blockResistance = resistance * 5.0F;
		java.util.Arrays.fill(this.hardness, hardness);
		java.util.Arrays.fill(this.resistance, resistance);
		return this;
	}

	/** The tool and its level (a wall of a vanilla block: Forge gives vanilla's theirs only after ours are made). */
	public McFlyWall tool(String tool, int level) {
		setHarvestLevel(tool, level);
		return this;
	}

	/** Its sound (the nether brick wall's nether bricks', 1.7.10's nether bricks being stone). */
	public McFlyWall sound(SoundType sound) {
		setStepSound(sound);
		return this;
	}

	/** 26.3's strength of one variant. */
	public McFlyWall strength(int meta, float hardness, float resistance) {
		this.hardness[meta] = hardness;
		this.resistance[meta] = resistance;
		if (meta == 0) {
			setHardness(hardness);
			this.blockResistance = resistance * 5.0F;
		}
		return this;
	}

	private int variant(int meta) {
		return meta >= 0 && meta < names.length ? meta : 0;
	}

	@Override
	public String nameOf(int meta) {
		return names[variant(meta)];
	}

	@Override
	public boolean isVariant(int meta) {
		return meta >= 0 && meta < names.length;
	}

	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		return hardness[variant(world.getBlockMetadata(x, y, z))];
	}

	@Override
	public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
		return resistance[variant(world.getBlockMetadata(x, y, z))];
	}

	// ---------------------------------------------------------------- 26.3's state

	/** The wall's state at (x, y, z) by 26.3's rules (whatever wall block stands there). */
	public static Shape shapeAt(IBlockAccess world, int x, int y, int z) {
		boolean[] connected = new boolean[4];
		for (int i = 0; i < 4; i++) {
			connected[i] = connectsTo(world, x, y, z, SIDES[i]);
		}
		List<float[]> above = bottomFace(world, x, y + 1, z);
		WallSide[] sides = new WallSide[4];
		for (int i = 0; i < 4; i++) {
			sides[i] = !connected[i] ? WallSide.NONE : covers(above, TEST_SIDE[i]) ? WallSide.TALL : WallSide.LOW;
		}
		return new Shape(raisesPost(world, x, y, z, sides, above), sides[0], sides[1], sides[2], sides[3]);
	}

	/** 26.3's `connectsTo`: a wall, iron bars or a pane, a fence gate across, or a full solid face (bar the exceptions). */
	private static boolean connectsTo(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
		int nx = x + direction.offsetX;
		int nz = z + direction.offsetZ;
		Block block = world.getBlock(nx, y, nz);
		if (block instanceof BlockWall || block instanceof BlockPane) {
			return true;
		}
		if (block instanceof BlockFenceGate) {
			// 1.7.10's gate meta: 0 south, 1 west, 2 north, 3 east; it joins a wall on a side across its facing's axis
			boolean gateAlongZ = (world.getBlockMetadata(nx, y, nz) & 1) == 0;
			boolean sideAlongZ = direction.offsetZ != 0;
			return gateAlongZ != sideAlongZ;
		}
		return !isExceptionForConnection(block) && block.isSideSolid(world, nx, y, nz, direction.getOpposite());
	}

	/** 26.3's `Block.isExceptionForConnection`: leaves, a barrier, a pumpkin, a jack o'lantern, a melon, a shulker box. */
	public static boolean isExceptionForConnection(Block block) {
		return block instanceof BlockLeaves || block == Blocks.pumpkin || block == Blocks.lit_pumpkin || block == Blocks.melon_block
			|| block == McFlyBlocks.SHULKER_BOX.get() || "barrier".equals(idPath(block));
	}

	private static String idPath(Block block) {
		String name = Block.blockRegistry.getNameForObject(block);
		return name == null ? null : name.substring(name.indexOf(':') + 1);
	}

	/** 26.3's `shouldRaisePost`. */
	private static boolean raisesPost(IBlockAccess world, int x, int y, int z, WallSide[] sides, List<float[]> above) {
		Block top = world.getBlock(x, y + 1, z);
		if (top instanceof BlockWall && shapeAt(world, x, y + 1, z).post) {
			return true;
		}
		boolean northNone = sides[0] == WallSide.NONE;
		boolean eastNone = sides[1] == WallSide.NONE;
		boolean southNone = sides[2] == WallSide.NONE;
		boolean westNone = sides[3] == WallSide.NONE;
		if (northNone && southNone && westNone && eastNone || northNone != southNone || westNone != eastNone) {
			return true;
		}
		if (sides[0] == WallSide.TALL && sides[2] == WallSide.TALL || sides[1] == WallSide.TALL && sides[3] == WallSide.TALL) {
			return false;
		}
		return isPostOverride(top) || covers(above, TEST_POST);
	}

	/** 26.3's `wall_post_override`, as 1.7.10 and McFly have its members: torches, tripwire, signs, banners, pressure plates. */
	private static boolean isPostOverride(Block block) {
		return block == Blocks.torch || block == Blocks.redstone_torch || block == Blocks.unlit_redstone_torch || block == Blocks.tripwire
			|| block instanceof BlockSign || block instanceof BlockBasePressurePlate || block instanceof io.github.asgerjon.mcfly.banner.BannerBlock;
	}

	/**
	 * The bottom face of the block's collision at (x, y, z), as pixel rectangles (minX, minZ, maxX, maxZ): its collision
	 * boxes that reach down to its floor (26.3's `getFaceShape(DOWN)`). Without a world (a render cache), its bounds.
	 */
	private static List<float[]> bottomFace(IBlockAccess access, int x, int y, int z) {
		Block block = access.getBlock(x, y, z);
		if (block.getMaterial() == net.minecraft.block.material.Material.air) {
			return Collections.emptyList();
		}
		World world = asWorld(access);
		List<float[]> face = new ArrayList<>();
		if (world != null) {
			List<AxisAlignedBB> boxes = new ArrayList<>();
			block.addCollisionBoxesToList(world, x, y, z, AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1), boxes, null);
			for (AxisAlignedBB box : boxes) {
				if (box.minY <= y + 1.0E-4) {
					face.add(new float[]{(float) (box.minX - x) * 16, (float) (box.minZ - z) * 16, (float) (box.maxX - x) * 16, (float) (box.maxZ - z) * 16});
				}
			}
		} else {
			block.setBlockBoundsBasedOnState(access, x, y, z);
			if (block.getBlockBoundsMinY() <= 1.0E-4) {
				face.add(new float[]{(float) block.getBlockBoundsMinX() * 16, (float) block.getBlockBoundsMinZ() * 16, (float) block.getBlockBoundsMaxX() * 16,
					(float) block.getBlockBoundsMaxZ() * 16});
			}
		}
		return face;
	}

	/** The world behind a block access: itself, or the world a chunk cache (a render cache, a path finder's) is of. */
	private static World asWorld(IBlockAccess access) {
		if (access instanceof World) {
			return (World) access;
		}
		if (access instanceof net.minecraft.world.ChunkCache) {
			return ((io.github.asgerjon.mcfly.mixins.early.base.AccessorChunkCache) access).mcfly$world();
		}
		return null;
	}

	/** Whether the rectangles cover the test rectangle (26.3's `isCovered`), checked at each half pixel inside it. */
	private static boolean covers(List<float[]> rectangles, float[] test) {
		if (rectangles.isEmpty()) {
			return false;
		}
		for (float px = test[0] + 0.5F; px < test[2]; px += 1.0F) {
			for (float pz = test[1] + 0.5F; pz < test[3]; pz += 1.0F) {
				boolean inside = false;
				for (float[] r : rectangles) {
					if (r[0] <= px && px <= r[2] && r[1] <= pz && pz <= r[3]) {
						inside = true;
						break;
					}
				}
				if (!inside) {
					return false;
				}
			}
		}
		return true;
	}

	// ---------------------------------------------------------------- shapes

	/** 26.3's shapes in pixels as (minX, minY, minZ, maxX, maxY, maxZ): the post 4 to 12, a side 5 to 11 out to 11 in. */
	private static List<float[]> parts(Shape shape, float postHeight, float lowHeight, float tallHeight) {
		List<float[]> parts = new ArrayList<>();
		if (shape.post) {
			parts.add(new float[]{4, 0, 4, 12, postHeight, 12});
		}
		WallSide[] sides = {shape.north, shape.east, shape.south, shape.west};
		for (int i = 0; i < 4; i++) {
			if (sides[i] == WallSide.NONE) {
				continue;
			}
			float top = sides[i] == WallSide.TALL ? tallHeight : lowHeight;
			switch (i) {
				case 0:
					parts.add(new float[]{5, 0, 0, 11, top, 11});
					break;
				case 1:
					parts.add(new float[]{5, 0, 5, 16, top, 11});
					break;
				case 2:
					parts.add(new float[]{5, 0, 5, 11, top, 16});
					break;
				default:
					parts.add(new float[]{0, 0, 5, 11, top, 11});
					break;
			}
		}
		return parts;
	}

	private static AxisAlignedBB box(float[] part, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + part[0] / 16.0, y + part[1] / 16.0, z + part[2] / 16.0, x + part[3] / 16.0, y + part[4] / 16.0, z + part[5] / 16.0);
	}

	private static float[] union(List<float[]> parts) {
		float[] all = parts.get(0).clone();
		for (float[] part : parts) {
			for (int i = 0; i < 3; i++) {
				all[i] = Math.min(all[i], part[i]);
				all[i + 3] = Math.max(all[i + 3], part[i + 3]);
			}
		}
		return all;
	}

	/** The outline: 26.3's shape (post 16, low sides 14, tall 16), as one box around its parts. */
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		float[] all = union(parts(shapeAt(world, x, y, z), 16, 14, 16));
		setBlockBounds(all[0] / 16.0F, all[1] / 16.0F, all[2] / 16.0F, all[3] / 16.0F, all[4] / 16.0F, all[5] / 16.0F);
	}

	/** 26.3's collision: the post and each side, all 24 high. */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		for (float[] part : parts(shapeAt(world, x, y, z), 24, 24, 24)) {
			AxisAlignedBB box = box(part, x, y, z);
			if (mask.intersectsWith(box)) {
				list.add(box);
			}
		}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return box(union(parts(shapeAt(world, x, y, z), 24, 24, 24)), x, y, z);
	}

	@Override
	public boolean canConnectWallTo(IBlockAccess world, int x, int y, int z) {
		// 1.7.10's question about the neighbour at (x, y, z); answered for our wall's own rules from either side
		return world.getBlock(x, y, z) instanceof BlockWall || world.getBlock(x, y, z) instanceof BlockPane || super.canConnectWallTo(world, x, y, z);
	}

	@Override
	public boolean canPlaceTorchOnTop(World world, int x, int y, int z) {
		return true;
	}

	// ---------------------------------------------------------------- the look and the item

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		Shape shape = shapeAt(world, x, y, z);
		List<ModelPart> parts = new ArrayList<>();
		if (shape.post) {
			parts.add(new ModelPart("template_wall_post"));
		}
		WallSide[] sides = {shape.north, shape.east, shape.south, shape.west};
		for (int i = 0; i < 4; i++) {
			if (sides[i] != WallSide.NONE) {
				parts.add(new ModelPart(sides[i] == WallSide.TALL ? "template_wall_side_tall" : "template_wall_side", 0, 90 * i, true));
			}
		}
		return parts;
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(new ModelPart("wall_inventory"));
	}

	/** 26.3's wall models take the block's side texture (`#wall`: blackstone's, not its top). */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return modelIcon(texture, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture, int meta) {
		return bases[variant(meta)].getIcon(2, baseMetas[variant(meta)]);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return bases[variant(meta)].getIcon(side, baseMetas[variant(meta)]);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int i = 0; i < names.length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public int damageDropped(int meta) {
		return variant(meta);
	}

	@Override
	public int getDamageValue(World world, int x, int y, int z) {
		return variant(world.getBlockMetadata(x, y, z));
	}

	@Override
	public MapColor getMapColor(int meta) {
		return bases[variant(meta)].getMapColor(baseMetas[variant(meta)]);
	}
}
