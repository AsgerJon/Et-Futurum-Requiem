package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Leaves of ours (SPEC-modern-movement.md §90.5), no GTNHLib: 1.7.10's leaves and their metas (meta & 3 the kind, 4 placed:
 * 26.3's persistent, 8 to be checked: kept, not read). 26.3 stores each leaf's distance to a log (1 to 7) and a random
 * tick decays it at 7 when not persistent; four meta bits can't hold that with the kind and the placed bit, so a random
 * tick here works the distance out (through leaves, at most 6 steps to a log) and decays it when there is none: 26.3's
 * outcome, found when a tick asks rather than on each neighbour's change. Drops by 26.3's leaves loot: shears or Silk
 * Touch the leaf; else its sapling and sticks at 26.3's Fortune tables. Untinted (azalea's); 0.2, fire 30 and 60.
 */
public abstract class McFlyLeaves extends BlockLeaves implements NamedMetas {

	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	/** 26.3's `decay_distance` less one: the farthest a log may be. */
	public static final int REACH = 6;
	/** 26.3's sapling chances by Fortune (0 to 3+), and the sticks' (0 to 4+). */
	private static final float[] SAPLING = {0.05F, 0.0625F, 0.083333336F, 0.1F};
	private static final float[] STICKS = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};
	private final String[] names;
	@SideOnly(Side.CLIENT)
	private IIcon[][] icons;

	protected McFlyLeaves(String... names) {
		this.names = names;
		setBlockName(McFly.name(names[0]));
		setHardness(0.2F);
		setLightOpacity(1);
		// 26.3's `#mineable/hoe` has `#leaves`
		setHarvestLevel("hoe", 0);
		setCreativeTab(McFlyTab.TAB);
	}

	/** What a kind drops by chance when it isn't sheared (26.3's sapling; null for none), and its meta. */
	protected abstract Item saplingOf(int kind);

	protected abstract int saplingMetaOf(int kind);

	@Override
	public String nameOf(int meta) {
		return names[Math.min(meta & 3, names.length - 1)];
	}

	@Override
	public boolean isVariant(int meta) {
		return (meta & 12) == 0 && (meta & 3) < names.length;
	}

	@Override
	public String[] func_150125_e() {
		return names;
	}

	/** Whether a log is within 6 steps through leaves (26.3's distance under 7). */
	public static boolean nearLog(World world, int x, int y, int z) {
		Set<Long> seen = new HashSet<>();
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[]{x, y, z, 0});
		seen.add(key(x, y, z));
		while (!queue.isEmpty()) {
			int[] at = queue.poll();
			for (int[] o : OFFSET) {
				int nx = at[0] + o[0], ny = at[1] + o[1], nz = at[2] + o[2];
				if (ny < 0 || ny >= world.getHeight() || !seen.add(key(nx, ny, nz))) {
					continue;
				}
				Block block = world.getBlock(nx, ny, nz);
				if (block.isWood(world, nx, ny, nz)) {
					return true;
				}
				if (at[3] + 1 < REACH && block.isLeaves(world, nx, ny, nz)) {
					queue.add(new int[]{nx, ny, nz, at[3] + 1});
				}
			}
		}
		return false;
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | (long) z & 0x3FFFFFF;
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (world.isRemote) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		if ((meta & 4) == 0 && !nearLog(world, x, y, z)) {
			dropBlockAsItem(world, x, y, z, meta, 0);
			world.setBlockToAir(x, y, z);
		}
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		int kind = Math.min(meta & 3, names.length - 1);
		// a kind without a sapling (mangrove's) rolls none
		Item sapling = saplingOf(kind);
		if (sapling != null && world.rand.nextFloat() < SAPLING[Math.min(fortune, SAPLING.length - 1)]) {
			drops.add(new ItemStack(sapling, 1, saplingMetaOf(kind)));
		}
		if (world.rand.nextFloat() < STICKS[Math.min(fortune, STICKS.length - 1)]) {
			drops.add(new ItemStack(Items.stick, 1 + world.rand.nextInt(2)));
		}
		return drops;
	}

	@Override
	protected boolean canSilkHarvest() {
		return true;
	}

	/**
	 * Sheared, no more: Forge's shears already gave the leaves (`IShearable`), and 26.3's loot rolls no sapling or sticks
	 * with shears (Forge's `BlockLeaves.harvestBlock` would roll them all the same).
	 */
	@Override
	public void harvestBlock(World world, net.minecraft.entity.player.EntityPlayer player, int x, int y, int z, int meta) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held != null && held.getItem() instanceof net.minecraft.item.ItemShears) {
			player.addStat(net.minecraft.stats.StatList.mineBlockStatArray[getIdFromBlock(this)], 1);
			player.addExhaustion(0.025F);
			return;
		}
		super.harvestBlock(world, player, x, y, z, meta);
	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(this, 1, meta & 3);
	}

	@Override
	public int damageDropped(int meta) {
		return meta & 3;
	}

	@Override
	public int getDamageValue(World world, int x, int y, int z) {
		return world.getBlockMetadata(x, y, z) & 3;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection face) {
		return true;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection face) {
		return 60;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection face) {
		return 30;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.foliageColor;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int i = 0; i < names.length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	// ---------------------------------------------------------------- look: untinted, fancy or fast by the graphics setting

	@Override
	@SideOnly(Side.CLIENT)
	public int getBlockColor() {
		return 0xFFFFFF;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderColor(int meta) {
		return 0xFFFFFF;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
		return 0xFFFFFF;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		icons = new IIcon[2][names.length];
		for (int i = 0; i < names.length; i++) {
			icons[0][i] = register.registerIcon(names[i]);
			icons[1][i] = register.registerIcon(names[i] + "_opaque");
		}
		blockIcon = icons[0][0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return icons[field_150121_P ? 0 : 1][Math.min(meta & 3, names.length - 1)];
	}

	/**
	 * 26.3's `FallingParticlesLeavesBlock` (spec §90, step D2), from a display tick: at this chance, unless the block
	 * below has a full top face, a falling leaf 0.05 under a random spot of this block (26.3's `spawnParticleBelow`),
	 * given this tint (RGB) when it isn't -1. 1.7.10's dripping water in the rain, 26.3's too, stays its own.
	 */
	@SideOnly(Side.CLIENT)
	protected static void fallingLeaves(World world, int x, int y, int z, Random random, float chance, String particle, int tint) {
		if (random.nextFloat() >= chance || fullTopFace(world, x, y - 1, z)) {
			return;
		}
		net.minecraft.client.particle.EntityFX leaf = io.github.asgerjon.mcfly.particle.McFlyParticlesClient.add(particle, x + random.nextDouble(),
			y - 0.05D, z + random.nextDouble(), 0.0D, 0.0D, 0.0D);
		if (leaf != null && tint != -1) {
			leaf.setRBGColorF((tint >> 16 & 255) / 255.0F, (tint >> 8 & 255) / 255.0F, (tint & 255) / 255.0F);
		}
	}

	/** 26.3's `isFaceFull(collisionShape, UP)`: the block's collision boxes, together, cover its whole top face. */
	public static boolean fullTopFace(World world, int x, int y, int z) {
		List<net.minecraft.util.AxisAlignedBB> boxes = new ArrayList<>();
		world.getBlock(x, y, z).addCollisionBoxesToList(world, x, y, z, net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1), boxes, null);
		List<double[]> top = new ArrayList<>();
		java.util.TreeSet<Double> xs = new java.util.TreeSet<>(), zs = new java.util.TreeSet<>();
		xs.add(0.0D);
		xs.add(1.0D);
		zs.add(0.0D);
		zs.add(1.0D);
		for (net.minecraft.util.AxisAlignedBB box : boxes) {
			if (box.maxY >= y + 1 - 1.0E-7D && box.minY < y + 1) {
				double[] square = {Math.max(0.0D, box.minX - x), Math.min(1.0D, box.maxX - x), Math.max(0.0D, box.minZ - z), Math.min(1.0D, box.maxZ - z)};
				top.add(square);
				xs.add(square[0]);
				xs.add(square[1]);
				zs.add(square[2]);
				zs.add(square[3]);
			}
		}
		// each cell between the squares' edges inside one of them
		Double[] ex = xs.toArray(new Double[0]), ez = zs.toArray(new Double[0]);
		for (int i = 0; i + 1 < ex.length; i++) {
			for (int k = 0; k + 1 < ez.length; k++) {
				double cx = (ex[i] + ex[i + 1]) / 2.0D, cz = (ez[k] + ez[k + 1]) / 2.0D;
				boolean covered = false;
				for (double[] square : top) {
					if (cx >= square[0] && cx <= square[1] && cz >= square[2] && cz <= square[3]) {
						covered = true;
						break;
					}
				}
				if (!covered) {
					return false;
				}
			}
		}
		return true;
	}

	/** The item: its damage the kind, placed with the placed bit (as 1.7.10's leaves item), named by kind. */
	public static class LeavesItem extends ItemBlock {

		public LeavesItem(Block block) {
			super(block);
			setHasSubtypes(true);
			setMaxDamage(0);
		}

		@Override
		public int getMetadata(int damage) {
			return damage & 3 | 4;
		}

		/** The kind's own picture (1.7.10's `ItemBlock` shows meta 0's for every damage). */
		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIconFromDamage(int damage) {
			return field_150939_a.getIcon(2, damage);
		}

		/** The kind's item tint (1.7.10's leaves item's `getRenderColor`): mangrove's foliage green. */
		@Override
		@SideOnly(Side.CLIENT)
		public int getColorFromItemStack(ItemStack stack, int pass) {
			return field_150939_a.getRenderColor(stack.getItemDamage());
		}

		@Override
		public String getUnlocalizedName(ItemStack stack) {
			return "tile." + McFly.name(((NamedMetas) field_150939_a).nameOf(stack.getItemDamage()));
		}
	}
}
