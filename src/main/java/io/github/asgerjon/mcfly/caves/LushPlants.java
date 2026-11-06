package io.github.asgerjon.mcfly.caves;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticles;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The lush caves' plants (SPEC-modern-movement.md §90.8, step G), as 26.3's, in McFly's new ids (EFR had none): the
 * spore blossom; the big dripleaf (meta & 3 its facing in 1.7.10's south, west, north, east; meta >> 2 its tilt: none,
 * unstable, partial, full) and its stem (meta & 3 the facing); the small dripleaf (meta & 3 the facing, 4 the upper
 * half). No waterlogging.
 */
public final class LushPlants {

	public static final int NONE = 0, UNSTABLE = 1, PARTIAL = 2, FULL = 3;
	public static final String[] FACINGS = {"south", "west", "north", "east"};
	public static final String[] TILTS = {"none", "unstable", "partial", "full"};
	/** 26.3's blockstate turns by facing (north 0, east 90, south 180, west 270). */
	private static final int[] TURN = {180, 270, 0, 90};
	/** The stem's 6-wide column, a quarter block toward its back, by facing (x, z steps of the facing). */
	private static final int[][] STEP = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};

	private LushPlants() {
	}

	/** The facing opposite the placer's horizontal look (26.3's `getHorizontalDirection().getOpposite()`), in 1.7.10's order. */
	static int facingFrom(EntityLivingBase placer) {
		int looking = placer == null ? 2 : MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		return (looking + 2) & 3;
	}

	/** 26.3's `#supports_big_dripleaf` (with `#supports_small_dripleaf`: clay and moss). */
	static boolean supportsBig(Block block) {
		return block == Blocks.clay || block == McFlyBlocks.MOSS_BLOCK.get() || block == Blocks.dirt || block == Blocks.grass || block == Blocks.mycelium
			|| block == Blocks.farmland || block == McFlyBlocks.ROOTED_DIRT.get() || block == McFlyBlocks.MUD.get() || block == McFlyBlocks.MUDDY_MANGROVE_ROOTS.get()
			|| (McFly.DOMAIN + ":coarse_dirt").equals(Block.blockRegistry.getNameForObject(block));
	}

	static boolean isWaterSource(World world, int x, int y, int z) {
		return world.getBlock(x, y, z).getMaterial() == Material.water && world.getBlockMetadata(x, y, z) == 0;
	}

	/** 26.3's `BigDripleafBlock.canGrowInto`: air, water or a small dripleaf. */
	static boolean canGrowInto(World world, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		return block.isAir(world, x, y, z) || block.getMaterial() == Material.water || block == McFlyBlocks.SMALL_DRIPLEAF.get();
	}

	/** 26.3's `placeWithRandomHeight`: 2 to 5 tall where it fits, stems up to the leaf. */
	static void placeWithRandomHeight(World world, Random random, int x, int y, int z, int facing) {
		int desired = 2 + random.nextInt(4);
		int height = 0;
		while (height < desired && canGrowInto(world, x, y + height, z)) {
			height++;
		}
		int leaf = y + height - 1;
		for (int sy = y; sy < leaf; sy++) {
			world.setBlock(x, sy, z, McFlyBlocks.BIG_DRIPLEAF_STEM.get(), facing, 3);
		}
		world.setBlock(x, leaf, z, McFlyBlocks.BIG_DRIPLEAF.get(), facing, 3);
	}

	/** A block of ours: a plant, instabreak or as given, fire 60 and 100, popped by pistons. */
	abstract static class Plant extends Block implements ModelledBlock {
		Plant(String name, float hardness, Block.SoundType sound) {
			super(Material.plants);
			setBlockName(McFly.name(name));
			setHardness(hardness);
			setStepSound(sound);
			setCreativeTab(McFlyTab.TAB);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.foliageColor;
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
		public int getMobilityFlag() {
			return 1;
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return true;
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 100;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 60;
		}

		@Override
		public int damageDropped(int meta) {
			return 0;
		}

		@Override
		public int getRenderType() {
			return McFlyRenderIds.MODEL;
		}

		abstract boolean canStay(World world, int x, int y, int z);

		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			return canStay(world, x, y, z);
		}

		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!canStay(world, x, y, z)) {
				dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
				world.setBlockToAir(x, y, z);
			}
		}

		@SideOnly(Side.CLIENT)
		IIcon[] icons;

		abstract String[] textures();

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			String[] names = textures();
			icons = new IIcon[names.length];
			for (int i = 0; i < names.length; i++) {
				icons[i] = register.registerIcon(names[i]);
			}
			blockIcon = icons[0];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return blockIcon;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			String name = texture.substring(texture.lastIndexOf('/') + 1);
			String[] names = textures();
			for (int i = 0; i < names.length; i++) {
				if (names[i].equals(name)) {
					return icons[i];
				}
			}
			return blockIcon;
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			return null;
		}
	}

	// ---------------------------------------------------------------- the spore blossom

	/** 26.3's spore blossom: under a face that holds its middle, not in water; falling spores and spores in the air. */
	public static final class SporeBlossom extends Plant {
		public SporeBlossom() {
			super("spore_blossom", 0.0F, McFlySoundType.SPORE_BLOSSOM);
			setBlockBounds(2.0F / 16.0F, 13.0F / 16.0F, 2.0F / 16.0F, 14.0F / 16.0F, 1.0F, 14.0F / 16.0F);
		}

		@Override
		boolean canStay(World world, int x, int y, int z) {
			return y + 1 < world.getHeight() && world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN, false) && world.getBlock(x, y, z).getMaterial() != Material.water;
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return world.getBlock(x, y, z).isReplaceable(world, x, y, z) && world.getBlock(x, y, z).getMaterial() != Material.water && y + 1 < world.getHeight()
				&& world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN, false);
		}

		/** 26.3's `animateTick`: a falling spore 0.7 into it, and 14 tries at an air spore within 10 across and 10 below. */
		@Override
		@SideOnly(Side.CLIENT)
		public void randomDisplayTick(World world, int x, int y, int z, Random random) {
			McFlyParticlesClient.add(McFlyParticles.FALLING_SPORE_BLOSSOM, x + random.nextDouble(), y + 0.7D, z + random.nextDouble(), 0.0D, 0.0D, 0.0D);
			for (int i = 0; i < 14; i++) {
				int ax = x + random.nextInt(21) - 10, ay = y - random.nextInt(10), az = z + random.nextInt(21) - 10;
				if (ay < 0 || world.getBlock(ax, ay, az).isNormalCube()) {
					continue;
				}
				McFlyParticlesClient.add(McFlyParticles.SPORE_BLOSSOM_AIR, ax + random.nextDouble(), ay + random.nextDouble(), az + random.nextDouble(), 0.0D, 0.0D,
					0.0D);
			}
		}

		@Override
		String[] textures() {
			return new String[]{"spore_blossom", "spore_blossom_base"};
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			return Collections.singletonList(new ModelPart("spore_blossom"));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart("spore_blossom"));
		}
	}

	// ---------------------------------------------------------------- the big dripleaf

	/** 26.3's big dripleaf: its tilt under an entity, redstone's reset, a projectile's full tilt, bonemeal. */
	public static final class BigDripleaf extends Plant implements IGrowable {
		private static final int[] DELAY = {-1, 10, 10, 100};

		public BigDripleaf() {
			super("big_dripleaf", 0.1F, McFlySoundType.BIG_DRIPLEAF);
			// 26.3's `#mineable/axe` has both dripleaf blocks
			setHarvestLevel("axe", 0);
		}

		private static int tilt(int meta) {
			return meta >> 2 & 3;
		}

		@Override
		boolean canStay(World world, int x, int y, int z) {
			Block below = world.getBlock(x, y - 1, z);
			return below == this || below == McFlyBlocks.BIG_DRIPLEAF_STEM.get() || supportsBig(below);
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return world.getBlock(x, y, z).isReplaceable(world, x, y, z) && canStay(world, x, y, z);
		}

		/** Facing the placer, or as the dripleaf or stem it's put on. */
		@Override
		public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
			Block below = world.getBlock(x, y - 1, z);
			int facing = below == this || below == McFlyBlocks.BIG_DRIPLEAF_STEM.get() ? world.getBlockMetadata(x, y - 1, z) & 3 : facingFrom(placer);
			world.setBlockMetadataWithNotify(x, y, z, facing, 3);
		}

		/** Unheld, gone; with a dripleaf on it, a stem; a redstone signal resets its tilt. */
		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!canStay(world, x, y, z)) {
				super.onNeighborBlockChange(world, x, y, z, neighbour);
				return;
			}
			int meta = world.getBlockMetadata(x, y, z);
			if (world.getBlock(x, y + 1, z) == this) {
				world.setBlock(x, y, z, McFlyBlocks.BIG_DRIPLEAF_STEM.get(), meta & 3, 3);
				return;
			}
			if (!world.isRemote && world.isBlockIndirectlyGettingPowered(x, y, z)) {
				resetTilt(world, x, y, z);
			}
		}

		/** 26.3's `entityInside` (an entity standing on it, above 11 pixels, unpowered: unstable) and `onProjectileHit` (full). */
		@Override
		public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
			if (world.isRemote) {
				return;
			}
			int meta = world.getBlockMetadata(x, y, z);
			if (entity instanceof IProjectile) {
				setTilt(world, x, y, z, FULL, "tilt_down");
				return;
			}
			if (tilt(meta) == NONE && entity.onGround && entity.boundingBox.minY > y + 0.6875D && !world.isBlockIndirectlyGettingPowered(x, y, z)) {
				setTilt(world, x, y, z, UNSTABLE, null);
			}
		}

		/** 26.3's `tick`: unstable, partial, full, back; powered, back at once. */
		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (world.isRemote || world.getBlock(x, y, z) != this) {
				return;
			}
			if (world.isBlockIndirectlyGettingPowered(x, y, z)) {
				resetTilt(world, x, y, z);
				return;
			}
			int tilt = tilt(world.getBlockMetadata(x, y, z));
			if (tilt == UNSTABLE) {
				setTilt(world, x, y, z, PARTIAL, "tilt_down");
			} else if (tilt == PARTIAL) {
				setTilt(world, x, y, z, FULL, "tilt_down");
			} else if (tilt == FULL) {
				resetTilt(world, x, y, z);
			}
		}

		private void setTilt(World world, int x, int y, int z, int tilt, String sound) {
			int meta = world.getBlockMetadata(x, y, z);
			world.setBlockMetadataWithNotify(x, y, z, meta & 3 | tilt << 2, 2);
			if (tilt != UNSTABLE && tilt != tilt(meta)) {
				// 26.3's `Tilt.causesVibration` (§90.9, step H1)
				io.github.asgerjon.mcfly.gameevent.GameEventHooks.atBlock(world, "block_change", x, y, z, null);
			}
			if (sound != null) {
				playTilt(world, x, y, z, sound);
			}
			if (DELAY[tilt] != -1) {
				world.scheduleBlockUpdate(x, y, z, this, DELAY[tilt]);
			}
		}

		private void resetTilt(World world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			world.setBlockMetadataWithNotify(x, y, z, meta & 3, 2);
			if (tilt(meta) != NONE) {
				playTilt(world, x, y, z, "tilt_up");
				io.github.asgerjon.mcfly.gameevent.GameEventHooks.atBlock(world, "block_change", x, y, z, null);
			}
		}

		private static void playTilt(World world, int x, int y, int z, String sound) {
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.big_dripleaf." + sound, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
		}

		/** 26.3's collision: the leaf only, 11 to 15 (13 partial, none full). */
		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
			int tilt = tilt(world.getBlockMetadata(x, y, z));
			if (tilt == FULL) {
				return;
			}
			AxisAlignedBB leaf = AxisAlignedBB.getBoundingBox(x, y + 11.0D / 16.0D, z, x + 1, y + (tilt == PARTIAL ? 13.0D : 15.0D) / 16.0D, z + 1);
			if (mask.intersectsWith(leaf)) {
				list.add(leaf);
			}
		}

		/** Its outline: leaf and stem (the stem's column alone when fully tilted). */
		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			int tilt = tilt(meta);
			if (tilt == FULL) {
				int[] step = STEP[meta & 3];
				float cx = 0.5F - step[0] * 0.25F, cz = 0.5F - step[1] * 0.25F;
				setBlockBounds(cx - 3.0F / 16.0F, 0.0F, cz - 3.0F / 16.0F, cx + 3.0F / 16.0F, 13.0F / 16.0F, cz + 3.0F / 16.0F);
			} else {
				setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, (tilt == PARTIAL ? 13.0F : 15.0F) / 16.0F, 1.0F);
			}
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			int tilt = tilt(world.getBlockMetadata(x, y, z));
			return tilt == FULL ? null : AxisAlignedBB.getBoundingBox(x, y + 11.0D / 16.0D, z, x + 1, y + (tilt == PARTIAL ? 13.0D : 15.0D) / 16.0D, z + 1);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return canGrowInto(world, x, y + 1, z);
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		/** 26.3's `performBonemeal`: a stem here, the leaf a block up. */
		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			if (!canGrowInto(world, x, y + 1, z)) {
				return;
			}
			int facing = world.getBlockMetadata(x, y, z) & 3;
			world.setBlock(x, y + 1, z, this, facing, 3);
			world.setBlock(x, y, z, McFlyBlocks.BIG_DRIPLEAF_STEM.get(), facing, 3);
		}

		@Override
		String[] textures() {
			return new String[]{"big_dripleaf_top", "big_dripleaf_stem", "big_dripleaf_side", "big_dripleaf_tip"};
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			int tilt = tilt(meta);
			String model = tilt == PARTIAL ? "big_dripleaf_partial_tilt" : tilt == FULL ? "big_dripleaf_full_tilt" : "big_dripleaf";
			return Collections.singletonList(new ModelPart(model, 0, TURN[meta & 3], false));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart("big_dripleaf"));
		}
	}

	/** 26.3's big dripleaf stem: held below (its support or a stem) and above (a stem or the leaf); it drops a big dripleaf; bonemeal grows the leaf on top. */
	public static final class Stem extends Plant implements IGrowable {
		public Stem() {
			super("big_dripleaf_stem", 0.1F, McFlySoundType.BIG_DRIPLEAF);
			setCreativeTab(null);
			setHarvestLevel("axe", 0);
		}

		@Override
		boolean canStay(World world, int x, int y, int z) {
			Block below = world.getBlock(x, y - 1, z), above = world.getBlock(x, y + 1, z);
			return (below == this || supportsBig(below)) && (above == this || above == McFlyBlocks.BIG_DRIPLEAF.get());
		}

		/** 26.3's: unheld, it breaks a tick on. */
		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!canStay(world, x, y, z)) {
				world.scheduleBlockUpdate(x, y, z, this, 1);
			}
		}

		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (!world.isRemote && world.getBlock(x, y, z) == this && !canStay(world, x, y, z)) {
				world.func_147480_a(x, y, z, true);
			}
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return Item.getItemFromBlock(McFlyBlocks.BIG_DRIPLEAF.get());
		}

		@Override
		@SideOnly(Side.CLIENT)
		public Item getItem(World world, int x, int y, int z) {
			return Item.getItemFromBlock(McFlyBlocks.BIG_DRIPLEAF.get());
		}

		/** The leaf on top of this stem (through stems), its y, or -1. */
		private int leaf(World world, int x, int y, int z) {
			int top = y + 1;
			while (top < world.getHeight() && world.getBlock(x, top, z) == this) {
				top++;
			}
			return top < world.getHeight() && world.getBlock(x, top, z) == McFlyBlocks.BIG_DRIPLEAF.get() ? top : -1;
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			int leaf = leaf(world, x, y, z);
			return leaf >= 0 && canGrowInto(world, x, leaf + 1, z);
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			int leaf = leaf(world, x, y, z);
			if (leaf >= 0) {
				((BigDripleaf) McFlyBlocks.BIG_DRIPLEAF.get()).func_149853_b(world, random, x, leaf, z);
			}
		}

		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			int[] step = STEP[world.getBlockMetadata(x, y, z) & 3];
			float cx = 0.5F - step[0] * 0.25F, cz = 0.5F - step[1] * 0.25F;
			setBlockBounds(cx - 3.0F / 16.0F, 0.0F, cz - 3.0F / 16.0F, cx + 3.0F / 16.0F, 1.0F, cz + 3.0F / 16.0F);
		}

		@Override
		String[] textures() {
			return new String[]{"big_dripleaf_stem"};
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			return Collections.singletonList(new ModelPart("big_dripleaf_stem", 0, TURN[world.getBlockMetadata(x, y, z) & 3], false));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart("big_dripleaf_stem"));
		}
	}

	// ---------------------------------------------------------------- the small dripleaf

	/** 26.3's small dripleaf: two tall, on clay or moss, or on dirt under a water source; shears only; bonemeal makes a big one. */
	public static final class SmallDripleaf extends Plant implements IGrowable {
		public SmallDripleaf() {
			super("small_dripleaf", 0.0F, McFlySoundType.SMALL_DRIPLEAF);
			setBlockBounds(2.0F / 16.0F, 0.0F, 2.0F / 16.0F, 14.0F / 16.0F, 13.0F / 16.0F, 14.0F / 16.0F);
		}

		private static boolean upper(int meta) {
			return (meta & 4) != 0;
		}

		/** 26.3's `mayPlaceOn`: `#supports_small_dripleaf`, or vegetation's ground under a water source. */
		private static boolean mayPlaceOn(World world, int x, int y, int z) {
			Block ground = world.getBlock(x, y - 1, z);
			return ground == Blocks.clay || ground == McFlyBlocks.MOSS_BLOCK.get() || isWaterSource(world, x, y, z) && MangroveBlocks.supportsVegetation(ground);
		}

		@Override
		boolean canStay(World world, int x, int y, int z) {
			if (upper(world.getBlockMetadata(x, y, z))) {
				return world.getBlock(x, y - 1, z) == this && !upper(world.getBlockMetadata(x, y - 1, z));
			}
			Block ground = world.getBlock(x, y - 1, z);
			boolean held = ground == Blocks.clay || ground == McFlyBlocks.MOSS_BLOCK.get() || MangroveBlocks.supportsVegetation(ground);
			return held && world.getBlock(x, y + 1, z) == this && upper(world.getBlockMetadata(x, y + 1, z));
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return world.getBlock(x, y, z).isReplaceable(world, x, y, z) && y + 1 < world.getHeight() && world.getBlock(x, y + 1, z).isReplaceable(world, x, y + 1, z)
				&& mayPlaceOn(world, x, y, z);
		}

		/** Placed with its upper half, both facing the placer. */
		@Override
		public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
			int facing = facingFrom(placer);
			world.setBlock(x, y, z, this, facing, 2);
			world.setBlock(x, y + 1, z, this, facing | 4, 3);
		}

		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!canStay(world, x, y, z)) {
				world.setBlockToAir(x, y, z);
			}
		}

		/** 26.3's loot: itself to shears only. */
		@Override
		public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
			return new ArrayList<>();
		}

		@Override
		public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
			ItemStack held = player.getCurrentEquippedItem();
			if (held != null && held.getItem() instanceof net.minecraft.item.ItemShears) {
				dropBlockAsItem(world, x, y, z, new ItemStack(this));
			}
			super.harvestBlock(world, player, x, y, z, meta);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return true;
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		/** 26.3's `performBonemeal`: from the lower half, a big dripleaf of 2 to 5 facing as it did. */
		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			if (upper(meta)) {
				if (world.getBlock(x, y - 1, z) == this) {
					func_149853_b(world, random, x, y - 1, z);
				}
				return;
			}
			placeWithRandomHeight(world, random, x, y, z, meta & 3);
		}

		@Override
		String[] textures() {
			return new String[]{"small_dripleaf_top", "small_dripleaf_side", "small_dripleaf_stem_top", "small_dripleaf_stem_bottom"};
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			return Collections.singletonList(new ModelPart(upper(meta) ? "small_dripleaf_top" : "small_dripleaf_bottom", 0, TURN[meta & 3], false));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart("small_dripleaf_top"));
		}
	}
}
