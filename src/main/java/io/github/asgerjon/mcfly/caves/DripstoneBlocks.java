package io.github.asgerjon.mcfly.caves;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticles;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Dripstone (SPEC-modern-movement.md §90.7, step F), as 26.3's `SpeleothemBlock` and `PointedDripstoneBlock`, in EFR's
 * ids: `dripstone_block`; `pointed_dripstone`, meta % 5 the thickness in EFR's order (0 tip, 1 frustum, 2 middle, 3 base,
 * 4 tip_merge), 0 to 4 pointing down (a stalactite), 5 to 9 up (a stalagmite). A stalactite that loses its hold falls
 * (`Falling`, McFly's falling block); no waterlogging.
 */
public final class DripstoneBlocks {

	public static final int TIP = 0, FRUSTUM = 1, MIDDLE = 2, BASE = 3, TIP_MERGE = 4;
	private static final String[] THICKNESS = {"tip", "frustum", "middle", "base", "tip_merge"};

	/** 26.3's `stalagmite` and `fallingStalactite` damage (their death messages too). */
	public static final DamageSource STALAGMITE = new DamageSource("stalagmite");
	public static final DamageSource FALLING_STALACTITE = new DamageSource("fallingStalactite");

	private static final float WATER_TRANSFER = 0.17578125F;
	private static final float LAVA_TRANSFER = 0.05859375F;
	private static final float GROWTH = 0.011377778F;
	private static final int NONE = 0, WATER = 1, LAVA = 2;

	private DripstoneBlocks() {
	}

	public static boolean up(int meta) {
		return meta >= 5;
	}

	public static int thickness(int meta) {
		return meta % 5;
	}

	public static int meta(boolean up, int thickness) {
		return (up ? 5 : 0) + thickness;
	}

	public static String thicknessName(int meta) {
		return THICKNESS[thickness(meta)];
	}

	/** 26.3's `Block.getOffset` for `OffsetType.XZ` (`Mth.getSeed` of x, 0, z), clamped to dripstone's 2 pixels. */
	public static double[] offset(int x, int z) {
		long seed = (long) (x * 3129871) ^ (long) z * 116129781L;
		seed = seed * seed * 42317861L + seed * 11L;
		seed >>= 16;
		double max = 0.125D;
		double ox = MathHelper.clamp_double(((seed & 15L) / 15.0F - 0.5D) * 0.5D, -max, max);
		double oz = MathHelper.clamp_double(((seed >> 8 & 15L) / 15.0F - 0.5D) * 0.5D, -max, max);
		return new double[]{ox, oz};
	}

	/** The dripstone block: 1.5 and 1, a pickaxe (needed for drops), its sound, terracotta brown as 1.7.10's brown. */
	public static final class Dripstone extends McFlyBlock {
		public Dripstone() {
			super(Material.rock);
			setNames("dripstone_block");
			strength(1.5F, 1.0F);
			tool("pickaxe", 0);
			sound(McFlySoundType.DRIPSTONE_BLOCK);
			colour(MapColor.brownColor);
		}
	}

	/** Pointed dripstone, 26.3's. */
	public static final class Pointed extends Block implements ModelledBlock, RandomTicking {

		@SideOnly(Side.CLIENT)
		private IIcon[] icons;

		public Pointed() {
			super(Material.rock);
			setBlockName(McFly.name("pointed_dripstone"));
			setHardness(1.5F);
			this.blockResistance = 3.0F * 5.0F;
			setHarvestLevel("pickaxe", 0);
			setStepSound(McFlySoundType.POINTED_DRIPSTONE);
			setCreativeTab(McFlyTab.TAB);
			setTickRandomly(true);
			setLightOpacity(0);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.brownColor;
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
		public int damageDropped(int meta) {
			return 0;
		}

		// ---------------------------------------------------------------- placing and thickness

		private boolean isSpeleothem(IBlockAccess world, int x, int y, int z, boolean up) {
			return world.getBlock(x, y, z) == this && up(world.getBlockMetadata(x, y, z)) == up;
		}

		/** 26.3's `isValidSpeleothemPlacement`: behind it a face sturdy toward the tip, or dripstone pointing the same way. */
		private boolean validPlacement(IBlockAccess world, int x, int y, int z, boolean up) {
			int behind = up ? y - 1 : y + 1;
			if (behind < 0 || behind >= 256) {
				return false;
			}
			return world.isSideSolid(x, behind, z, up ? ForgeDirection.UP : ForgeDirection.DOWN, false) || isSpeleothem(world, x, behind, z, up);
		}

		/** 26.3's `calculateSpeleothemThickness`. */
		private int calculateThickness(IBlockAccess world, int x, int y, int z, boolean up, boolean mergeOpposingTips) {
			int front = up ? y + 1 : y - 1;
			if (isSpeleothem(world, x, front, z, !up)) {
				return !mergeOpposingTips && thickness(world.getBlockMetadata(x, front, z)) != TIP_MERGE ? TIP : TIP_MERGE;
			}
			if (!isSpeleothem(world, x, front, z, up)) {
				return TIP;
			}
			int inFront = thickness(world.getBlockMetadata(x, front, z));
			if (inFront != TIP && inFront != TIP_MERGE) {
				return !isSpeleothem(world, x, up ? y - 1 : y + 1, z, up) ? BASE : MIDDLE;
			}
			return FRUSTUM;
		}

		@Override
		public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
			return world.getBlock(x, y, z).isReplaceable(world, x, y, z) && (validPlacement(world, x, y, z, true) || validPlacement(world, x, y, z, false));
		}

		/** 26.3's `getStateForPlacement`: pointing against the vertical way looked (up: a stalactite), the other way if that can't hold; sneaking, no merge. */
		@Override
		public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
			boolean up = placer == null || placer.rotationPitch >= 0.0F;
			if (!validPlacement(world, x, y, z, up)) {
				up = !up;
			}
			boolean merge = placer == null || !placer.isSneaking();
			world.setBlockMetadataWithNotify(x, y, z, meta(up, calculateThickness(world, x, y, z, up, merge)), 3);
		}

		/** 26.3's `updateShape` from above or below: unheld, it falls or breaks a tick or two on; else its thickness again. */
		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			int meta = world.getBlockMetadata(x, y, z);
			boolean up = up(meta);
			if (!validPlacement(world, x, y, z, up)) {
				world.scheduleBlockUpdate(x, y, z, this, up ? 1 : 2);
				return;
			}
			int thickness = calculateThickness(world, x, y, z, up, thickness(meta) == TIP_MERGE);
			if (thickness != thickness(meta)) {
				world.setBlockMetadataWithNotify(x, y, z, meta(up, thickness), 3);
			}
		}

		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			return validPlacement(world, x, y, z, up(world.getBlockMetadata(x, y, z)));
		}

		/**
		 * The scheduled tick: an unheld stalagmite breaks; an unheld stalactite falls. A held stalactite's tick is a drip's,
		 * as 26.3's cauldron's own tick: the fluid above fills the cauldron under its tip.
		 */
		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (world.isRemote) {
				return;
			}
			int meta = world.getBlockMetadata(x, y, z);
			if (validPlacement(world, x, y, z, up(meta))) {
				if (!up(meta)) {
					fillCauldronBelow(world, x, y, z);
				}
				return;
			}
			if (up(meta)) {
				world.func_147480_a(x, y, z, true);
			} else {
				spawnFallingStalactite(world, x, y, z);
			}
		}

		/** 26.3's `spawnFallingStalactite`: the stalactite falls from here down, its tip hurting (1 a block times its size, at least 6, at most 40). */
		private void spawnFallingStalactite(World world, int x, int y, int z) {
			for (int fy = y; fy >= 0 && isSpeleothem(world, x, fy, z, false); fy--) {
				int meta = world.getBlockMetadata(x, fy, z);
				Falling falling = new Falling(world, x + 0.5D, fy + 0.5D, z + 0.5D, meta);
				world.setBlock(x, fy, z, Blocks.air, 0, 3);
				int thickness = thickness(meta);
				if (thickness == TIP || thickness == TIP_MERGE) {
					int size = Math.max(1 + y - fy, 6);
					falling.hurts(size, 40);
					world.spawnEntityInWorld(falling);
					return;
				}
				world.spawnEntityInWorld(falling);
			}
		}

		/** Landing on a stalagmite's tip: 26.3's fall of 2.5 more at twice the damage, as `stalagmite` (1.7.10's own fall zeroed). */
		@Override
		public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
			int meta = world.getBlockMetadata(x, y, z);
			if (!up(meta) || thickness(meta) != TIP || !(entity instanceof EntityLivingBase)) {
				return;
			}
			int damage = MathHelper.floor_double((fallDistance + 2.5D + 1.0E-6D - 3.0D) * 2.0D);
			entity.fallDistance = 0.0F;
			if (damage > 0 && !world.isRemote) {
				entity.attackEntityFrom(STALAGMITE, damage);
			}
		}

		// ---------------------------------------------------------------- shapes

		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			int thickness = thickness(meta);
			float width = thickness == FRUSTUM ? 8 : thickness == MIDDLE ? 10 : thickness == BASE ? 12 : 6;
			float minY = thickness == TIP && !up(meta) ? 5 : 0, maxY = thickness == TIP && up(meta) ? 11 : 16;
			double[] offset = offset(x, z);
			float half = width / 32.0F;
			setBlockBounds((float) (0.5F - half + offset[0]), minY / 16.0F, (float) (0.5F - half + offset[1]), (float) (0.5F + half + offset[0]), maxY / 16.0F,
				(float) (0.5F + half + offset[1]));
		}

		@Override
		public void setBlockBoundsForItemRender() {
			setBlockBounds(0.3125F, 0.0F, 0.3125F, 0.6875F, 1.0F, 0.6875F);
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			setBlockBoundsBasedOnState(world, x, y, z);
			return super.getCollisionBoundingBoxFromPool(world, x, y, z);
		}

		// ---------------------------------------------------------------- growth and drips

		/** The tip of the dripstone here (up to max - 1 on, through dripstone pointing the same way), or null. */
		private int findTip(World world, int x, int y, int z, int max, boolean includeMerged) {
			int meta = world.getBlockMetadata(x, y, z);
			boolean up = up(meta);
			for (int i = 0, fy = y; i < max; i++, fy += up ? 1 : -1) {
				if (fy < 0 || fy >= 256 || !isSpeleothem(world, x, fy, z, up)) {
					return Integer.MIN_VALUE;
				}
				int thickness = thickness(world.getBlockMetadata(x, fy, z));
				if (thickness == TIP || includeMerged && thickness == TIP_MERGE) {
					return fy;
				}
			}
			return Integer.MIN_VALUE;
		}

		private boolean isStalactiteStart(World world, int x, int y, int z) {
			return isSpeleothem(world, x, y, z, false) && world.getBlock(x, y + 1, z) != this;
		}

		/** 26.3's `getFluidAboveStalactite`: two over the top of the stalactite (its root's above), water (mud too, but in the Nether), lava, or none; its y. */
		private int[] fluidAbove(World world, int x, int y, int z) {
			if (!isSpeleothem(world, x, y, z, false)) {
				return null;
			}
			int root = y + 1;
			for (int i = 1; i < 11 && root < 256 && isSpeleothem(world, x, root, z, false); i++) {
				root++;
			}
			if (root >= 255 || world.getBlock(x, root, z) == this) {
				return null;
			}
			int above = root + 1;
			Block block = world.getBlock(x, above, z);
			int fluid = block == McFlyBlocks.MUD.get() && !world.provider.isHellWorld ? WATER
				: block.getMaterial() == Material.water ? WATER : block.getMaterial() == Material.lava ? LAVA : NONE;
			return new int[]{fluid, above};
		}

		/** 26.3's `canDripThrough`: air, or no full cube, no fluid, and nothing in the middle 4 by 4 column. */
		private static boolean canDripThrough(World world, int x, int y, int z) {
			Block block = world.getBlock(x, y, z);
			if (block.isAir(world, x, y, z)) {
				return true;
			}
			if (block.isOpaqueCube() || block.getMaterial().isLiquid()) {
				return false;
			}
			List<AxisAlignedBB> boxes = new ArrayList<>();
			block.addCollisionBoxesToList(world, x, y, z, AxisAlignedBB.getBoundingBox(x + 0.375D, y, z + 0.375D, x + 0.625D, y + 1.0D, z + 0.625D), boxes, null);
			return boxes.isEmpty();
		}

		/** A cauldron under the tip (up to 10 down, through what drips go through) that takes this fluid; its y or MIN_VALUE. */
		private static int cauldronBelow(World world, int x, int tipY, int z, int fluid) {
			for (int i = 1, y = tipY - 1; i < 11 && y >= 0; i++, y--) {
				Block block = world.getBlock(x, y, z);
				if (block == Blocks.cauldron) {
					int level = world.getBlockMetadata(x, y, z);
					return fluid == WATER && level < 3 || fluid == LAVA && level == 0 && lavaCauldron() != null ? y : Integer.MIN_VALUE;
				}
				if (!canDripThrough(world, x, y, z)) {
					return Integer.MIN_VALUE;
				}
			}
			return Integer.MIN_VALUE;
		}

		/** EFR's lava cauldron (by name: McFly names no EFR class), or null. */
		private static Block lavaCauldron() {
			Block block = Block.getBlockFromName(McFly.DOMAIN + ":lava_cauldron");
			return block == Blocks.air ? null : block;
		}

		@Override
		public void randomTick(World world, int x, int y, int z, Random random) {
			maybeTransferFluid(world, x, y, z, random.nextFloat());
			if (random.nextFloat() < GROWTH && isStalactiteStart(world, x, y, z)) {
				growStalactiteOrStalagmite(world, x, y, z, random);
			}
		}

		/** 26.3's `maybeTransferFluid`: at its chance, from the stalactite's top, mud above turns to clay or the cauldron under the tip fills (50 + its fall ticks on). */
		private void maybeTransferFluid(World world, int x, int y, int z, float value) {
			if (value > WATER_TRANSFER && value > LAVA_TRANSFER || !isStalactiteStart(world, x, y, z)) {
				return;
			}
			int[] above = fluidAbove(world, x, y, z);
			if (above == null || above[0] == NONE || value >= (above[0] == WATER ? WATER_TRANSFER : LAVA_TRANSFER)) {
				return;
			}
			int tip = findTip(world, x, y, z, 11, false);
			if (tip == Integer.MIN_VALUE) {
				return;
			}
			if (above[0] == WATER && world.getBlock(x, above[1], z) == McFlyBlocks.MUD.get()) {
				world.setBlock(x, above[1], z, Blocks.clay);
				drip(world, x, tip, z, WATER);
				return;
			}
			int cauldron = cauldronBelow(world, x, tip, z, above[0]);
			if (cauldron != Integer.MIN_VALUE) {
				drip(world, x, tip, z, above[0]);
				world.scheduleBlockUpdate(x, tip, z, this, 50 + tip - cauldron);
			}
		}

		/** A held tip's drip lands: 26.3's `receiveStalactiteDrip`, water a level up, lava an empty cauldron full (EFR's lava cauldron), with their sounds. */
		private void fillCauldronBelow(World world, int x, int tip, int z) {
			if (thickness(world.getBlockMetadata(x, tip, z)) != TIP) {
				return;
			}
			int[] above = fluidAbove(world, x, tip, z);
			if (above == null || above[0] == NONE) {
				return;
			}
			int cauldron = cauldronBelow(world, x, tip, z, above[0]);
			if (cauldron == Integer.MIN_VALUE) {
				return;
			}
			if (above[0] == WATER) {
				world.setBlockMetadataWithNotify(x, cauldron, z, world.getBlockMetadata(x, cauldron, z) + 1, 3);
				world.playSoundEffect(x + 0.5D, cauldron + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.pointed_dripstone.drip_water_into_cauldron", 2.0F,
					world.rand.nextFloat() * 0.1F + 0.9F);
			} else {
				world.setBlock(x, cauldron, z, lavaCauldron());
				world.playSoundEffect(x + 0.5D, cauldron + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.pointed_dripstone.drip_lava_into_cauldron", 2.0F,
					world.rand.nextFloat() * 0.1F + 0.9F);
			}
		}

		/** 26.3's level event 1504: a drip at the tip, to the players near. */
		private static void drip(World world, int x, int tip, int z, int fluid) {
			if (world instanceof WorldServer server) {
				double[] offset = offset(x, z);
				McFlyParticles.send(server, fluid == LAVA ? McFlyParticles.DRIPPING_DRIPSTONE_LAVA : McFlyParticles.DRIPPING_DRIPSTONE_WATER, x + 0.5D + offset[0],
					tip + 5.0D / 16.0D - 1.0D / 16.0D, z + 0.5D + offset[1], 0, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}

		/** 26.3's growth: a water source two over the dripstone block it hangs from; down, or a stalagmite under it. */
		private void growStalactiteOrStalagmite(World world, int x, int y, int z, Random random) {
			Block below = world.getBlock(x, y + 2, z);
			if (world.getBlock(x, y + 1, z) != McFlyBlocks.DRIPSTONE_BLOCK.get() || below.getMaterial() != Material.water || world.getBlockMetadata(x, y + 2, z) != 0) {
				return;
			}
			int tip = findTip(world, x, y, z, 7, false);
			if (tip == Integer.MIN_VALUE || thickness(world.getBlockMetadata(x, tip, z)) != TIP || !canTipGrow(world, x, tip, z, false)) {
				return;
			}
			if (random.nextBoolean()) {
				grow(world, x, tip, z, false);
			} else {
				growStalagmiteBelow(world, x, tip, z);
			}
		}

		private boolean unmergedTip(World world, int x, int y, int z, boolean up) {
			return isSpeleothem(world, x, y, z, up) && thickness(world.getBlockMetadata(x, y, z)) == TIP;
		}

		/** 26.3's `canTipGrow`: into air, or onto an opposing unmerged tip; never into fluid. */
		private boolean canTipGrow(World world, int x, int tip, int z, boolean up) {
			int to = up ? tip + 1 : tip - 1;
			if (to < 0 || to >= 256) {
				return false;
			}
			Block block = world.getBlock(x, to, z);
			if (block.getMaterial().isLiquid()) {
				return false;
			}
			return block.isAir(world, x, to, z) || unmergedTip(world, x, to, z, !up);
		}

		/** 26.3's `grow`: a new tip that way, or the two tips merged. */
		private void grow(World world, int x, int from, int z, boolean up) {
			int to = up ? from + 1 : from - 1;
			if (unmergedTip(world, x, to, z, !up)) {
				int stalactite = up ? to + 1 : to, stalagmite = up ? to : to - 1;
				world.setBlock(x, stalactite, z, this, meta(false, TIP_MERGE), 3);
				world.setBlock(x, stalagmite, z, this, meta(true, TIP_MERGE), 3);
			} else if (world.isAirBlock(x, to, z) || world.getBlock(x, to, z).getMaterial() == Material.water) {
				world.setBlock(x, to, z, this, meta(up, TIP), 3);
			}
		}

		/** 26.3's `growStalagmiteBelow`: down from the tip, up to 10, the first floor's stalagmite grows (or a new one starts). */
		private void growStalagmiteBelow(World world, int x, int tip, int z) {
			for (int i = 0, y = tip - 1; i < 10 && y > 0; i++, y--) {
				Block block = world.getBlock(x, y, z);
				if (block.getMaterial().isLiquid()) {
					return;
				}
				if (unmergedTip(world, x, y, z, true) && canTipGrow(world, x, y, z, true)) {
					grow(world, x, y, z, true);
					return;
				}
				if (validPlacement(world, x, y, z, true) && world.getBlock(x, y - 1, z).getMaterial() != Material.water) {
					grow(world, x, y - 1, z, true);
					return;
				}
				if (!canDripThrough(world, x, y, z)) {
					return;
				}
			}
		}

		/** 26.3's `animateTick`: a free-hanging tip drips at 0.02, or at 0.12 under water or lava. */
		@Override
		@SideOnly(Side.CLIENT)
		public void randomDisplayTick(World world, int x, int y, int z, Random random) {
			int meta = world.getBlockMetadata(x, y, z);
			if (up(meta) || thickness(meta) != TIP) {
				return;
			}
			float value = random.nextFloat();
			if (value > 0.12F) {
				return;
			}
			int[] above = fluidAbove(world, x, y, z);
			if (above == null || !(value < 0.02F || above[0] != NONE)) {
				return;
			}
			double[] offset = offset(x, z);
			io.github.asgerjon.mcfly.particle.McFlyParticlesClient.add(above[0] == LAVA ? McFlyParticles.DRIPPING_DRIPSTONE_LAVA
					: McFlyParticles.DRIPPING_DRIPSTONE_WATER, x + 0.5D + offset[0], y + 5.0D / 16.0D - 1.0D / 16.0D, z + 0.5D + offset[1], 0.0D, 0.0D,
				0.0D);
		}

		// ---------------------------------------------------------------- look

		@Override
		public int getRenderType() {
			return McFlyRenderIds.MODEL;
		}

		private static String model(int meta) {
			return "pointed_dripstone_" + (up(meta) ? "up_" : "down_") + thicknessName(meta);
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			return Collections.singletonList(new ModelPart(model(world.getBlockMetadata(x, y, z))));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart(model(meta)));
		}

		@Override
		public double[] modelOffset(IBlockAccess world, int x, int y, int z) {
			return offset(x, z);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public String getItemIconName() {
			return "pointed_dripstone";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			icons = new IIcon[10];
			for (int meta = 0; meta < 10; meta++) {
				icons[meta] = register.registerIcon(model(meta));
			}
			blockIcon = icons[BASE];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return icons[Math.min(meta, 9)];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			String name = texture.substring(texture.lastIndexOf('/') + 1);
			for (int meta = 0; meta < 10; meta++) {
				if (model(meta).equals(name)) {
					return icons[meta];
				}
			}
			return blockIcon;
		}
	}

	/**
	 * A falling stalactite (26.3's `FallingBlockEntity` of pointed dripstone): 1.7.10's falling block's physics (0.04 a
	 * tick down, 0.98 drag), hurting what it lands in as `fallingStalactite` when it's the tip (26.3's
	 * `causeFallDamage`: floor(ceil(fall - 1) x per block), at most its max); on landing it breaks, dropping itself, with
	 * 26.3's landing sound (level event 1045); gone after 600 ticks.
	 */
	public static class Falling extends Entity implements IEntityAdditionalSpawnData {

		public int meta;
		private int time;
		private boolean hurtEntities;
		private float damagePerDistance;
		private int damageMax;

		public Falling(World world) {
			super(world);
			preventEntitySpawning = true;
			setSize(0.98F, 0.98F);
			yOffset = height / 2.0F;
		}

		public Falling(World world, double x, double y, double z, int meta) {
			this(world);
			this.meta = meta;
			setPosition(x, y, z);
			motionX = motionY = motionZ = 0.0D;
			prevPosX = x;
			prevPosY = y;
			prevPosZ = z;
		}

		void hurts(float perDistance, int max) {
			hurtEntities = true;
			damagePerDistance = perDistance;
			damageMax = max;
		}

		@Override
		protected void entityInit() {
		}

		@Override
		protected boolean canTriggerWalking() {
			return false;
		}

		@Override
		public boolean canBeCollidedWith() {
			return !isDead;
		}

		@Override
		public void onUpdate() {
			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;
			time++;
			motionY -= 0.04D;
			moveEntity(motionX, motionY, motionZ);
			motionX *= 0.98D;
			motionY *= 0.98D;
			motionZ *= 0.98D;
			if (worldObj.isRemote) {
				return;
			}
			if (onGround) {
				setDead();
				worldObj.playSoundEffect(posX, posY, posZ, McFly.MC_ASSETS + ":block.pointed_dripstone.land", 2.0F, rand.nextFloat() * 0.1F + 0.9F);
				// 26.3's `doEntityDrops` is 1.8's; 1.7.10's falling block always drops
				entityDropItem(new ItemStack(McFlyBlocks.POINTED_DRIPSTONE.get(), 1, 0), 0.0F);
			} else if (time > 100 && (posY < 1.0D || posY > 256.0D) || time > 600) {
				setDead();
			}
		}

		/** 26.3's `FallingBlockEntity.causeFallDamage`: on landing, the living (not creative) in its box hurt. */
		@Override
		protected void fall(float distance) {
			if (!hurtEntities) {
				return;
			}
			int fallen = MathHelper.ceiling_float_int(distance - 1.0F);
			if (fallen < 0) {
				return;
			}
			float damage = Math.min(MathHelper.floor_float(fallen * damagePerDistance), damageMax);
			for (Object entry : new ArrayList<Object>(worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox))) {
				if (entry instanceof EntityLivingBase living && living.isEntityAlive()
					&& !(living instanceof EntityPlayer player && player.capabilities.isCreativeMode)) {
					living.attackEntityFrom(FALLING_STALACTITE, damage);
				}
			}
		}

		@Override
		protected void writeEntityToNBT(NBTTagCompound tag) {
			tag.setByte("Meta", (byte) meta);
			tag.setInteger("Time", time);
			tag.setBoolean("HurtEntities", hurtEntities);
			tag.setFloat("FallHurtAmount", damagePerDistance);
			tag.setInteger("FallHurtMax", damageMax);
		}

		@Override
		protected void readEntityFromNBT(NBTTagCompound tag) {
			meta = tag.getByte("Meta");
			time = tag.getInteger("Time");
			hurtEntities = tag.getBoolean("HurtEntities");
			damagePerDistance = tag.getFloat("FallHurtAmount");
			damageMax = tag.getInteger("FallHurtMax");
		}

		@Override
		public void writeSpawnData(ByteBuf buffer) {
			buffer.writeByte(meta);
		}

		@Override
		public void readSpawnData(ByteBuf buffer) {
			meta = buffer.readByte();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public float getShadowSize() {
			return 0.0F;
		}
	}
}
