package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyItem;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bamboo, the plant (SPEC-modern-movement.md 91.11, step I; 26.3's `BambooStalkBlock`, `BambooSaplingBlock`), in EFR's ids
 * and metas: the stalk's bit 0 `age` (thick), bits 1 and 2 `leaves` (0 none, 1 small, 2 large), bit 3 `stage` (done
 * growing); the shoot's 0; the item `etfuturum:bamboo`. Held by `#supports_bamboo`; grown by random ticks and bonemeal
 * to 16; both offset by their place (26.3's `OffsetType.XZ`); swords break them at once.
 */
public final class Bamboo {

	public static final int NONE = 0, SMALL = 1, LARGE = 2;
	private static final int MAX_HEIGHT = 16;

	private Bamboo() {
	}

	public static int age(int meta) {
		return meta & 1;
	}

	public static int leaves(int meta) {
		return meta >> 1 & 3;
	}

	public static int stage(int meta) {
		return meta >> 3 & 1;
	}

	public static int meta(int age, int leaves, int stage) {
		return age & 1 | (leaves & 3) << 1 | (stage & 1) << 3;
	}

	/** 26.3's offset for `OffsetType.XZ` (`Block.getOffset`), up to its default 0.25 a way. */
	public static double[] offset(int x, int z) {
		long seed = (long) (x * 3129871) ^ (long) z * 116129781L;
		seed = seed * seed * 42317861L + seed * 11L;
		seed >>= 16;
		double ox = MathHelper.clamp_double(((seed & 15L) / 15.0F - 0.5D) * 0.5D, -0.25D, 0.25D);
		double oz = MathHelper.clamp_double(((seed >> 8 & 15L) / 15.0F - 0.5D) * 0.5D, -0.25D, 0.25D);
		return new double[]{ox, oz};
	}

	/**
	 * 26.3's `#supports_bamboo`: sand (red too), gravel, `#substrate_overworld` (every dirt, grass, podzol, mycelium, mud,
	 * muddy mangrove roots, moss, rooted dirt), bamboo and the shoot.
	 */
	public static boolean supports(Block block) {
		return block == Blocks.sand || block == Blocks.gravel || block == Blocks.dirt || block == Blocks.grass || block == Blocks.mycelium
			|| is(block, McFlyBlocks.MUD) || is(block, McFlyBlocks.MUDDY_MANGROVE_ROOTS) || is(block, McFlyBlocks.MOSS_BLOCK) || is(block, McFlyBlocks.ROOTED_DIRT)
			|| block instanceof Stalk || block instanceof Shoot;
	}

	private static boolean is(Block block, McFlyBlocks entry) {
		return entry.isEnabled() && block == entry.get();
	}

	private static Block stalk() {
		return McFlyBlocks.BAMBOO.get();
	}

	private static Block shoot() {
		return McFlyBlocks.BAMBOO_SAPLING.get();
	}

	private static Item item() {
		return McFlyItems.BAMBOO.get();
	}

	/** 26.3's `getRawBrightness(pos.above(), 0) >= 9`, with air above. */
	private static boolean canGrowAbove(World world, int x, int y, int z) {
		return world.isAirBlock(x, y + 1, z) && world.getFullBlockLightValue(x, y + 1, z) >= 9;
	}

	/** Swords break bamboo at once (26.3's `#sword_instantly_mines`). */
	private static boolean sword(EntityPlayer player) {
		ItemStack held = player.getCurrentEquippedItem();
		return held != null && held.getItem() instanceof ItemSword;
	}

	/** Shared: the model renderer on the client, the icons by texture name. */
	private abstract static class Plant extends Block implements ModelledBlock, IGrowable {

		private final String[] textures;
		private IIcon[] icons;

		Plant(String name, String... textures) {
			super(Material.plants);
			this.textures = textures;
			setBlockName(McFly.name(name));
			setHardness(1.0F);
			this.blockResistance = 1.0F * 5.0F;
			setTickRandomly(true);
			setHarvestLevel("axe", 0);
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
			return FMLCommonHandler.instance().getEffectiveSide().isClient() ? McFlyRenderIds.MODEL : 1;
		}

		@Override
		public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
			return sword(player) ? 1.0F : super.getPlayerRelativeBlockHardness(player, world, x, y, z);
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return item();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public Item getItem(World world, int x, int y, int z) {
			return item();
		}

		@Override
		public double[] modelOffset(IBlockAccess world, int x, int y, int z) {
			return offset(x, z);
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return new ArrayList<>();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			String name = texture.substring(texture.lastIndexOf('/') + 1);
			for (int i = 0; i < textures.length; i++) {
				if (textures[i].equals(name)) {
					return icons[i];
				}
			}
			return icons[0];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return icons[0];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			icons = new IIcon[textures.length];
			for (int i = 0; i < textures.length; i++) {
				icons[i] = register.registerIcon("minecraft:" + textures[i]);
			}
			blockIcon = icons[0];
		}

		/** 26.3's `canSurvive`: what's below supports bamboo. */
		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			return supports(world.getBlock(x, y - 1, z));
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}
	}

	// ---------------------------------------------------------------- the stalk

	/** 26.3's `BambooStalkBlock`. */
	public static class Stalk extends Plant {

		public Stalk() {
			super("bamboo", "bamboo_stalk", "bamboo_small_leaves", "bamboo_large_leaves");
			setStepSound(McFlySoundType.BAMBOO);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.foliageColor;
		}

		/** 26.3's shape: a column 6 across (10 with large leaves), the block's height, offset. */
		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			double[] o = offset(x, z);
			float half = (leaves(world.getBlockMetadata(x, y, z)) == LARGE ? 5.0F : 3.0F) / 16.0F;
			setBlockBounds((float) (0.5D - half + o[0]), 0.0F, (float) (0.5D - half + o[1]), (float) (0.5D + half + o[0]), 1.0F, (float) (0.5D + half + o[1]));
		}

		/** 26.3's collision: a column 3 across, offset. */
		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			double[] o = offset(x, z);
			double half = 1.5D / 16.0D;
			return AxisAlignedBB.getBoundingBox(x + 0.5D - half + o[0], y, z + 0.5D - half + o[1], x + 0.5D + half + o[0], y + 1, z + 0.5D + half + o[1]);
		}

		/** 26.3's `updateShape`: a tick to break when unheld; thick when the stalk above is thicker. */
		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!canBlockStay(world, x, y, z)) {
				world.scheduleBlockUpdate(x, y, z, this, 1);
			}
			int meta = world.getBlockMetadata(x, y, z);
			if (world.getBlock(x, y + 1, z) == this && age(world.getBlockMetadata(x, y + 1, z)) > age(meta)) {
				world.setBlockMetadataWithNotify(x, y, z, meta | 1, 3);
			}
		}

		/**
		 * 26.3's `tick` (broken when unheld) and `randomTick` (1.7.10 calls both this): growing while not done, 1 in 3,
		 * air and light 9 above, under 16 tall.
		 */
		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (!canBlockStay(world, x, y, z)) {
				world.func_147480_a(x, y, z, true);
				return;
			}
			int meta = world.getBlockMetadata(x, y, z);
			if (stage(meta) == 0 && random.nextInt(3) == 0 && canGrowAbove(world, x, y, z)) {
				int height = heightBelow(world, x, y, z) + 1;
				if (height < MAX_HEIGHT) {
					grow(world, x, y, z, meta, random, height);
				}
			}
		}

		/** 26.3's `growBamboo`: a stalk above, its leaves, thickness and stage by the stalk below. */
		void grow(World world, int x, int y, int z, int meta, Random random, int height) {
			Block below = world.getBlock(x, y - 1, z);
			int belowMeta = world.getBlockMetadata(x, y - 1, z);
			Block twoBelow = world.getBlock(x, y - 2, z);
			int twoBelowMeta = world.getBlockMetadata(x, y - 2, z);
			int leaves = NONE;
			if (height >= 1) {
				if (below != this || leaves(belowMeta) == NONE) {
					leaves = SMALL;
				} else {
					leaves = LARGE;
					if (twoBelow == this) {
						world.setBlock(x, y - 1, z, this, belowMeta & ~6 | SMALL << 1, 3);
						world.setBlock(x, y - 2, z, this, twoBelowMeta & ~6, 3);
					}
				}
			}
			int age = age(meta) != 1 && twoBelow != this ? 0 : 1;
			int stage = (height < 11 || !(random.nextFloat() < 0.25F)) && height != 15 ? 0 : 1;
			world.setBlock(x, y + 1, z, this, meta(age, leaves, stage), 3);
		}

		int heightAbove(World world, int x, int y, int z) {
			int height = 0;
			while (height < MAX_HEIGHT && world.getBlock(x, y + height + 1, z) == this) {
				height++;
			}
			return height;
		}

		int heightBelow(World world, int x, int y, int z) {
			int height = 0;
			while (height < MAX_HEIGHT && world.getBlock(x, y - height - 1, z) == this) {
				height++;
			}
			return height;
		}

		/** 26.3's `isValidBonemealTarget`: under 16 tall, the top not done growing, air above it. */
		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			int above = heightAbove(world, x, y, z), below = heightBelow(world, x, y, z);
			int growth = y + above + 1;
			return above + below + 1 < MAX_HEIGHT && stage(world.getBlockMetadata(x, y + above, z)) != 1 && growth < world.getHeight()
				&& world.isAirBlock(x, growth, z);
		}

		/** 26.3's `performBonemeal`: 1 or 2 more on top while it may. */
		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			int above = heightAbove(world, x, y, z);
			int total = above + heightBelow(world, x, y, z) + 1;
			int count = 1 + random.nextInt(2);
			for (int i = 0; i < count; i++) {
				int top = y + above;
				int topMeta = world.getBlockMetadata(x, top, z);
				if (total >= MAX_HEIGHT || stage(topMeta) == 1 || top + 1 >= world.getHeight() || !world.isAirBlock(x, top + 1, z)) {
					return;
				}
				grow(world, x, top, z, topMeta, random, total);
				above++;
				total++;
			}
		}

		/** 26.3's multipart: one of the four stalks by the place, by thickness; the leaves over it. */
		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			List<ModelPart> parts = new ArrayList<>();
			long seed = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
			seed = seed * seed * 42317861L + seed * 11L;
			int variant = (int) ((seed >> 16 & Long.MAX_VALUE) % 4);
			parts.add(new ModelPart("bamboo" + (variant + 1) + "_age" + age(meta)));
			if (leaves(meta) == SMALL) {
				parts.add(new ModelPart("bamboo_small_leaves"));
			} else if (leaves(meta) == LARGE) {
				parts.add(new ModelPart("bamboo_large_leaves"));
			}
			return parts;
		}
	}

	// ---------------------------------------------------------------- the shoot

	/** 26.3's `BambooSaplingBlock`. */
	public static class Shoot extends Plant {

		public Shoot() {
			super("bamboo_sapling", "bamboo_stage0");
			setStepSound(McFlySoundType.BAMBOO_SAPLING);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.woodColor;
		}

		/** 26.3's shape: a column 8 across and 12 high, offset; no collision. */
		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			double[] o = offset(x, z);
			float half = 4.0F / 16.0F;
			setBlockBounds((float) (0.5D - half + o[0]), 0.0F, (float) (0.5D - half + o[1]), (float) (0.5D + half + o[0]), 0.75F, (float) (0.5D + half + o[1]));
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			return null;
		}

		/** 26.3's `updateShape`: gone (dropping bamboo) when unheld; a stalk when a stalk is above. */
		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!canBlockStay(world, x, y, z)) {
				world.func_147480_a(x, y, z, true);
			} else if (world.getBlock(x, y + 1, z) == stalk()) {
				world.setBlock(x, y, z, stalk(), 0, 3);
			}
		}

		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (random.nextInt(3) == 0 && canGrowAbove(world, x, y, z)) {
				growAbove(world, x, y, z);
			}
		}

		/** 26.3's `growBamboo`: a stalk with small leaves above. */
		private void growAbove(World world, int x, int y, int z) {
			world.setBlock(x, y + 1, z, stalk(), meta(0, SMALL, 0), 3);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return y + 1 < world.getHeight() && world.isAirBlock(x, y + 1, z);
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			growAbove(world, x, y, z);
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			List<ModelPart> parts = new ArrayList<>();
			parts.add(new ModelPart("bamboo_sapling"));
			return parts;
		}
	}

	// ---------------------------------------------------------------- the item

	/** The bamboo item, `etfuturum:bamboo` (26.3's `BlockItem` of the stalk: `getStateForPlacement`). Fuel 50. */
	public static class BambooItem extends McFlyItem {

		public BambooItem() {
			super("bamboo");
			setFull3D();
		}

		@Override
		public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
			Block clicked = world.getBlock(x, y, z);
			boolean replaceClicked = clicked == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1 || clicked.isReplaceable(world, x, y, z);
			if (!replaceClicked) {
				int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
				x += offsets[side][0];
				y += offsets[side][1];
				z += offsets[side][2];
			}
			Block there = world.getBlock(x, y, z);
			if (stack.stackSize <= 0 || !player.canPlayerEdit(x, y, z, side, stack) || y < 1 || y >= world.getHeight() || !there.isReplaceable(world, x, y, z)
				|| there.getMaterial().isLiquid()) {
				return false;
			}
			Block below = world.getBlock(x, y - 1, z);
			if (!supports(below)) {
				return false;
			}
			Block block;
			int meta = 0;
			if (below == shoot()) {
				block = stalk();
			} else if (below == stalk()) {
				block = stalk();
				meta = age(world.getBlockMetadata(x, y - 1, z)) > 0 ? 1 : 0;
			} else if (world.getBlock(x, y + 1, z) == stalk()) {
				block = stalk();
				meta = age(world.getBlockMetadata(x, y + 1, z));
			} else {
				block = shoot();
			}
			if (!world.canPlaceEntityOnSide(block, x, y, z, false, side, player, stack)) {
				return false;
			}
			if (world.isRemote) {
				return true;
			}
			if (!world.setBlock(x, y, z, block, meta, 3)) {
				return false;
			}
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, block.stepSound.func_150496_b(), (block.stepSound.getVolume() + 1.0F) / 2.0F,
				block.stepSound.getPitch() * 0.8F);
			stack.stackSize--;
			return true;
		}
	}

	/** Fire 60/60 for the stalk (26.3's `FireBlock`; the shoot none). */
	public static void register() {
		if (McFlyBlocks.BAMBOO.isEnabled()) {
			Blocks.fire.setFireInfo(stalk(), 60, 60);
		}
	}
}
