package io.github.asgerjon.mcfly.caves;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyMetaItem;
import io.github.asgerjon.mcfly.base.NamedMetas;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Amethyst as 26.3's (SPEC-modern-movement.md §90.4), EFR's ids: the block and budding amethyst, the buds and cluster in
 * two ids (meta facing + 6 x size: `amethyst_cluster_1` the small and medium bud, `_2` the large bud and the cluster),
 * tinted glass.
 */
public final class AmethystBlocks {

	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private AmethystBlocks() {
	}

	/** The amethyst block (and budding's base): 1.5 and 1.5, purple, a pickaxe for drops; a projectile's hit chimes. */
	public static class Amethyst extends McFlyBlock {

		public Amethyst(String name) {
			super(Material.rock);
			setNames(name);
			strength(1.5F, 1.5F);
			tool("pickaxe", 0);
			sound(McFlySoundType.AMETHYST_BLOCK);
			colour(MapColor.purpleColor);
		}

		/** 26.3's `onProjectileHit` (1.7.10's arrows and the fork's throwables touch the block when they hit). */
		@Override
		public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
			if (!world.isRemote && entity instanceof IProjectile) {
				world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":block.amethyst_block.chime", 1.0F, 0.5F + world.rand.nextFloat() * 1.2F);
			}
		}
	}

	/**
	 * Budding amethyst: 1 in 5 a random tick, toward a random face, into air or a water source (no waterlogging: the water
	 * goes) a small bud facing that way, or a bud facing that way a size up; popped by pistons; drops nothing (EFR's
	 * `buddingAmethystMode`: 1 by Silk Touch, 2 itself).
	 */
	public static class Budding extends Amethyst implements RandomTicking {

		public Budding() {
			super("budding_amethyst");
			setTickRandomly(true);
		}

		@Override
		public void randomTick(World world, int x, int y, int z, Random random) {
			if (random.nextInt(5) != 0) {
				return;
			}
			int face = random.nextInt(6);
			int tx = x + OFFSET[face][0], ty = y + OFFSET[face][1], tz = z + OFFSET[face][2];
			if (ty < 0 || ty >= world.getHeight()) {
				return;
			}
			Block there = world.getBlock(tx, ty, tz);
			int size;
			if (there.isAir(world, tx, ty, tz) || (there == Blocks.water || there == Blocks.flowing_water) && world.getBlockMetadata(tx, ty, tz) == 0) {
				size = 0;
			} else if (there instanceof Cluster cluster && Cluster.facing(world.getBlockMetadata(tx, ty, tz)) == face && cluster.size(world.getBlockMetadata(tx, ty, tz)) < 3) {
				size = cluster.size(world.getBlockMetadata(tx, ty, tz)) + 1;
			} else {
				return;
			}
			world.setBlock(tx, ty, tz, Cluster.of(size), face + 6 * (size % 2), 3);
			// 1.7.10 relights a block change only when the block's own light differs; a meta's size does here
			world.func_147451_t(tx, ty, tz);
		}

		@Override
		public int getMobilityFlag() {
			return McFlyConfig.buddingAmethystMode == 0 ? 1 : 0;
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return McFlyConfig.buddingAmethystMode == 2 ? Item.getItemFromBlock(this) : null;
		}

		@Override
		protected boolean canSilkHarvest() {
			return McFlyConfig.buddingAmethystMode == 1;
		}
	}

	/**
	 * A bud or the cluster, two sizes an id. 1.5 and 1.5, light 1, 2, 4, 5, popped by pistons; held by a full face behind
	 * (gone without drops when it isn't); placed facing the clicked face; 26.3's shapes (3 by 8, 4 by 10, 5 by 10, 7 by 10
	 * pixels); drops: the buds to Silk Touch only, the cluster itself to Silk Touch, else 4 shards to a pickaxe with
	 * Fortune's `ore_drops`, else 2. Drawn by 26.3's cross models turned to the facing. Sounds by size (26.3's swap: the
	 * medium bud the large's sound, the large the medium's); each id's own the second size's, the first's by EFR's table.
	 */
	public static final class Cluster extends Block implements ModelledBlock, NamedMetas {

		private static final String[] NAMES = {"small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "amethyst_cluster"};
		private static final int[] LIGHT = {1, 2, 4, 5};
		private static final float[] HEIGHT = {3, 4, 5, 7}, WIDTH = {8, 10, 10, 10};
		public static final Block.SoundType[] SOUNDS = {McFlySoundType.SMALL_AMETHYST_BUD, McFlySoundType.LARGE_AMETHYST_BUD,
			McFlySoundType.MEDIUM_AMETHYST_BUD, McFlySoundType.AMETHYST_CLUSTER};
		/** 26.3's blockstate turns (x, y) by facing: down, up, north, south, west, east. */
		private static final int[][] TURN = {{180, 0}, {0, 0}, {90, 0}, {90, 180}, {90, 270}, {90, 90}};
		private static Cluster first, second;
		private final int half;
		@SideOnly(Side.CLIENT)
		private IIcon[] icons;

		/** The first id (`half` 0: small and medium buds) or the second (1: the large bud and the cluster). */
		public Cluster(int half) {
			super(Material.glass);
			this.half = half;
			if (half == 0) {
				first = this;
			} else {
				second = this;
			}
			setBlockName(McFly.name(NAMES[half * 2]));
			setBlockTextureName(NAMES[half * 2]);
			setHardness(1.5F);
			setResistance(1.5F * 5.0F / 3.0F);
			setHarvestLevel("pickaxe", 0);
			setStepSound(SOUNDS[half * 2 + 1]);
			setCreativeTab(McFlyTab.TAB);
		}

		/** The id holding a size (0 small, 1 medium, 2 large, 3 cluster). */
		public static Cluster of(int size) {
			return size < 2 ? first : second;
		}

		public static int facing(int meta) {
			return meta % 6;
		}

		public int size(int meta) {
			return half * 2 + Math.min(1, meta / 6);
		}

		@Override
		public String nameOf(int meta) {
			return NAMES[size(meta)];
		}

		@Override
		public boolean isVariant(int meta) {
			return meta == 0 || meta == 6;
		}

		@Override
		public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double ex, double ey, double ez) {
			return 1.5F;
		}

		@Override
		public int getLightValue(IBlockAccess world, int x, int y, int z) {
			return world.getBlock(x, y, z) == this ? LIGHT[size(world.getBlockMetadata(x, y, z))] : LIGHT[half * 2];
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.purpleColor;
		}

		@Override
		public int getMobilityFlag() {
			return 1;
		}

		// ---------------------------------------------------------------- held by the face behind

		private static boolean held(IBlockAccess world, int x, int y, int z, int facing) {
			int bx = x - OFFSET[facing][0], by = y - OFFSET[facing][1], bz = z - OFFSET[facing][2];
			return world.getBlock(bx, by, bz).isSideSolid(world, bx, by, bz, ForgeDirection.getOrientation(facing));
		}

		@Override
		public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
			return super.canPlaceBlockAt(world, x, y, z) && held(world, x, y, z, side);
		}

		/** Facing the clicked face; the item's damage (0 or 6) the size. */
		@Override
		public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
			return side + (meta >= 6 ? 6 : 0);
		}

		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!world.isRemote && !held(world, x, y, z, facing(world.getBlockMetadata(x, y, z)))) {
				world.setBlockToAir(x, y, z);
			}
		}

		// ---------------------------------------------------------------- drops

		@Override
		public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
			ArrayList<ItemStack> drops = new ArrayList<>();
			if (size(meta) < 3) {
				return drops;
			}
			EntityPlayer harvester = harvesters.get();
			ItemStack tool = harvester != null ? harvester.getHeldItem() : null;
			if (tool != null && tool.getItem().getToolClasses(tool).contains("pickaxe")) {
				int bonus = Math.max(0, world.rand.nextInt(fortune + 2) - 1);
				drops.add(McFlyItems.AMETHYST_SHARD.newItemStack(4 * (1 + bonus)));
			} else {
				drops.add(McFlyItems.AMETHYST_SHARD.newItemStack(2));
			}
			return drops;
		}

		@Override
		protected boolean canSilkHarvest() {
			return true;
		}

		@Override
		protected ItemStack createStackedBlock(int meta) {
			return new ItemStack(this, 1, meta >= 6 ? 6 : 0);
		}

		@Override
		public int damageDropped(int meta) {
			return meta >= 6 ? 6 : 0;
		}

		@Override
		public int getDamageValue(World world, int x, int y, int z) {
			return damageDropped(world.getBlockMetadata(x, y, z));
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			list.add(new ItemStack(item, 1, 0));
			list.add(new ItemStack(item, 1, 6));
		}

		// ---------------------------------------------------------------- shape

		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			float h = HEIGHT[size(meta)] / 16.0F, w = WIDTH[size(meta)] / 32.0F;
			float lo = 0.5F - w, hi = 0.5F + w;
			switch (facing(meta)) {
				case 0:
					setBlockBounds(lo, 1 - h, lo, hi, 1, hi);
					break;
				case 1:
					setBlockBounds(lo, 0, lo, hi, h, hi);
					break;
				case 2:
					setBlockBounds(lo, lo, 1 - h, hi, hi, 1);
					break;
				case 3:
					setBlockBounds(lo, lo, 0, hi, hi, h);
					break;
				case 4:
					setBlockBounds(1 - h, lo, lo, 1, hi, hi);
					break;
				default:
					setBlockBounds(0, lo, lo, h, hi, hi);
			}
		}

		@Override
		public void setBlockBoundsForItemRender() {
			setBlockBounds(0, 0, 0, 1, 1, 1);
		}

		@Override
		public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			setBlockBoundsBasedOnState(world, x, y, z);
			return super.getCollisionBoundingBoxFromPool(world, x, y, z);
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
			return McFlyRenderIds.MODEL_FLAT_ITEM;
		}

		// ---------------------------------------------------------------- look

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			int[] turn = TURN[facing(meta)];
			return Collections.singletonList(new ModelPart(NAMES[size(meta)], turn[0], turn[1], false));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart(NAMES[size(meta)]));
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			icons = new IIcon[]{register.registerIcon(NAMES[half * 2]), register.registerIcon(NAMES[half * 2 + 1])};
			blockIcon = icons[0];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return icons == null ? blockIcon : icons[Math.min(1, meta / 6)];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			return icons[texture.endsWith(NAMES[half * 2 + 1]) ? 1 : 0];
		}
	}

	/** The buds' and cluster's item: named by size, drawn flat from the block's texture (26.3's `item/generated`). */
	public static final class ClusterItem extends McFlyMetaItem {

		public ClusterItem(Block block) {
			super(block);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIconFromDamage(int damage) {
			return field_150939_a.getIcon(1, damage);
		}
	}

	/**
	 * Tinted glass: glass's strength and sound, gray, all light stopped (the beacon's beam too: `BeaconBeam` passes what
	 * lets light through), no spawns on it; drops itself (26.3's loot, unlike glass).
	 */
	public static final class TintedGlass extends BlockBreakable {

		public TintedGlass() {
			super("tinted_glass", Material.glass, false);
			setBlockName(McFly.name("tinted_glass"));
			setHardness(0.3F);
			setResistance(0.3F * 5.0F / 3.0F);
			setStepSound(soundTypeGlass);
			setLightOpacity(255);
			setCreativeTab(McFlyTab.TAB);
			// it stops all light, so its own space reads 0: lit by its neighbours, as McFly's slabs are
			this.useNeighborBrightness = true;
		}

		@Override
		public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double ex, double ey, double ez) {
			return 0.3F;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.grayColor;
		}

		@Override
		public int quantityDropped(Random random) {
			return 1;
		}

		@Override
		public int getRenderBlockPass() {
			return 1;
		}

		@Override
		public boolean renderAsNormalBlock() {
			return false;
		}

		@Override
		public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
			return false;
		}
	}

	/** The geode's and others' blocks by size (EFR's generator places them). */
	public static Block clusterBlock(int size) {
		return size < 2 ? McFlyBlocks.AMETHYST_CLUSTER_1.get() : McFlyBlocks.AMETHYST_CLUSTER_2.get();
	}
}
