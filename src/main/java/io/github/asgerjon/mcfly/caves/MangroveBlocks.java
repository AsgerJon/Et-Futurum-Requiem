package io.github.asgerjon.mcfly.caves;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyLeaves;
import io.github.asgerjon.mcfly.base.McFlyPots;
import io.github.asgerjon.mcfly.base.McFlyPottable;
import io.github.asgerjon.mcfly.base.NamedMetas;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticles;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import io.github.asgerjon.mcfly.tree.AttachedToLeavesDecorator;
import io.github.asgerjon.mcfly.tree.BlockProvider;
import io.github.asgerjon.mcfly.tree.CherryFoliagePlacer;
import io.github.asgerjon.mcfly.tree.CherryTrunkPlacer;
import io.github.asgerjon.mcfly.tree.FeatureSize;
import io.github.asgerjon.mcfly.tree.IntRange;
import io.github.asgerjon.mcfly.tree.LeaveVineDecorator;
import io.github.asgerjon.mcfly.tree.MangroveRootPlacer;
import io.github.asgerjon.mcfly.tree.RandomSpreadFoliagePlacer;
import io.github.asgerjon.mcfly.tree.Tree;
import io.github.asgerjon.mcfly.tree.TreeDecorator;
import io.github.asgerjon.mcfly.tree.UpwardsBranchingTrunkPlacer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Mangrove, and cherry's leaves and sapling (SPEC-modern-movement.md §90.6, step E), as 26.3's, in EFR's ids: the
 * roots and muddy roots; `leaves` (0 mangrove, 1 cherry; 4 placed); `sapling` (meta & 1 the kind, 8 the stage; a hanging
 * propagule's age 0 to 4 on metas 2, 4, 6, 10 and 12); 26.3's mangrove, tall mangrove and cherry trees on McFly's tree
 * system. The mangrove log is `base/McFlyLog`; the cherry log too (the wood sets' family, spec §91.3).
 */
public final class MangroveBlocks {

	private MangroveBlocks() {
	}

	/** A hanging propagule's metas by age (even, so EFR reads each as a propagule). */
	private static final int[] HANGING = {2, 4, 6, 10, 12};

	/** Whether a sapling meta is a hanging propagule (14, unused, as the oldest). */
	public static boolean hanging(int meta) {
		return (meta & 1) == 0 && meta != 0 && meta != 8;
	}

	/** A hanging propagule's age (0 to 4). */
	public static int age(int meta) {
		for (int age = 0; age < HANGING.length; age++) {
			if (HANGING[age] == meta) {
				return age;
			}
		}
		return 4;
	}

	public static int hangingMeta(int age) {
		return HANGING[Math.max(0, Math.min(4, age))];
	}

	/** 26.3's `#supports_vegetation` (`#substrate_overworld`: dirt, coarse dirt, rooted dirt, mud, muddy roots, moss, grass, podzol, mycelium; farmland). */
	public static boolean supportsVegetation(Block block) {
		return block == Blocks.grass || block == Blocks.dirt || block == Blocks.mycelium || block == Blocks.farmland || block == McFlyBlocks.MOSS_BLOCK.get()
			|| block == McFlyBlocks.ROOTED_DIRT.get() || block == McFlyBlocks.MUD.get() || block == McFlyBlocks.MUDDY_MANGROVE_ROOTS.get()
			|| (McFly.DOMAIN + ":coarse_dirt").equals(Block.blockRegistry.getNameForObject(block));
	}

	private static boolean isMangroveLeaves(World world, int x, int y, int z) {
		return world.getBlock(x, y, z) == McFlyBlocks.LEAVES.get() && (world.getBlockMetadata(x, y, z) & 3) == 0;
	}

	private static boolean isPropagule(World world, int x, int y, int z) {
		return world.getBlock(x, y, z) == McFlyBlocks.SAPLING.get() && (world.getBlockMetadata(x, y, z) & 1) == 0;
	}

	/** 26.3's `#mangrove_logs_can_grow_through`. */
	private static boolean logsGrowThrough(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == McFlyBlocks.MUD.get() || block == McFlyBlocks.MUDDY_MANGROVE_ROOTS.get() || block == McFlyBlocks.MANGROVE_ROOTS.get()
			|| isMangroveLeaves(world, x, y, z) || block == McFlyBlocks.MANGROVE_LOG.get() && (world.getBlockMetadata(x, y, z) & 3) == 0
			|| isPropagule(world, x, y, z) || block == McFlyBlocks.MOSS_CARPET.get() || block == Blocks.vine;
	}

	/** 26.3's `#mangrove_roots_can_grow_through`. */
	private static boolean rootsGrowThrough(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == McFlyBlocks.MUD.get() || block == McFlyBlocks.MUDDY_MANGROVE_ROOTS.get() || block == McFlyBlocks.MANGROVE_ROOTS.get()
			|| block == McFlyBlocks.MOSS_CARPET.get() || block == Blocks.vine || isPropagule(world, x, y, z) || block == Blocks.snow_layer;
	}

	// ---------------------------------------------------------------- the trees

	private static Tree mangrove;
	private static Tree tallMangrove;
	private static Tree cherry;

	/** 26.3's `mangrove` (short: 2 + rand 2 + rand 5) or `tall_mangrove` (4 + rand 2 + rand 10), with its roots, vines and hanging propagules. */
	private static Tree mangrove(boolean tall) {
		Block leaves = McFlyBlocks.LEAVES.get();
		MangroveRootPlacer.Blocks blocks = new MangroveRootPlacer.Blocks() {
			@Override
			public boolean canGrowThrough(World world, int x, int y, int z) {
				return rootsGrowThrough(world, x, y, z);
			}

			@Override
			public boolean muddyRootsIn(World world, int x, int y, int z) {
				Block block = world.getBlock(x, y, z);
				return block == McFlyBlocks.MUD.get() || block == McFlyBlocks.MUDDY_MANGROVE_ROOTS.get();
			}
		};
		List<TreeDecorator> decorators = Arrays.asList(new LeaveVineDecorator(0.125F), new AttachedToLeavesDecorator(0.14F, 1, 0,
			random -> new BlockProvider.State(McFlyBlocks.SAPLING.get(), hangingMeta(random.nextInt(5))), 2, new int[][]{{0, -1, 0}}));
		return new Tree(BlockProvider.of(McFlyBlocks.MANGROVE_LOG.get(), 0),
			tall ? new UpwardsBranchingTrunkPlacer(4, 1, 9, new IntRange(1, 6), 0.5F, new IntRange(0, 1), MangroveBlocks::logsGrowThrough)
				: new UpwardsBranchingTrunkPlacer(2, 1, 4, new IntRange(1, 4), 0.5F, new IntRange(0, 1), MangroveBlocks::logsGrowThrough),
			BlockProvider.of(leaves, 0), new RandomSpreadFoliagePlacer(IntRange.of(3), IntRange.of(0), IntRange.of(2), 70),
			new MangroveRootPlacer(tall ? new IntRange(3, 7) : new IntRange(1, 3), BlockProvider.of(McFlyBlocks.MANGROVE_ROOTS.get(), 0),
				BlockProvider.of(McFlyBlocks.MOSS_CARPET.get(), 0), 0.5F, blocks, BlockProvider.of(McFlyBlocks.MUDDY_MANGROVE_ROOTS.get(), 0), 8, 15, 0.2F),
			new FeatureSize(tall ? 3 : 2, 0, 2), BlockProvider.soilBeneathTree(), true, decorators);
	}

	/** 26.3's `cherry`: McFly's cherry log (oak's with cherry blocks off, spec §91.3), 26.3's cherry trunk and foliage, the soil beneath. */
	private static Tree cherry() {
		Block log = McFlyBlocks.CHERRY_LOG.isEnabled() ? McFlyBlocks.CHERRY_LOG.get() : null;
		return new Tree(BlockProvider.of(log == null ? Blocks.log : log, 0),
			new CherryTrunkPlacer(7, 1, 0, new IntRange(1, 3), new IntRange(2, 4), -4, -3, new IntRange(-1, 0)), BlockProvider.of(McFlyBlocks.LEAVES.get(), 1),
			new CherryFoliagePlacer(IntRange.of(4), IntRange.of(0), IntRange.of(5), 0.25F, 0.5F, 0.16666667F, 0.33333334F), null, new FeatureSize(1, 0, 2),
			BlockProvider.soilBeneathTree(), true, Collections.<TreeDecorator>emptyList());
	}

	/** 26.3's `TreeGrower.MANGROVE` (15 `mangrove` to 85 `tall_mangrove`) and `CHERRY` (the bees' cherry left out: no nests here). */
	static Tree grower(int kind, Random random) {
		if (kind == 1) {
			if (cherry == null) {
				cherry = cherry();
			}
			return cherry;
		}
		if (mangrove == null) {
			mangrove = mangrove(false);
			tallMangrove = mangrove(true);
		}
		return random.nextInt(100) < 15 ? mangrove : tallMangrove;
	}

	// ---------------------------------------------------------------- the roots

	/**
	 * Mangrove roots (26.3's `MangroveRootsBlock`): 0.7, an axe, podzol, fire 5 and 20, not suffocating, see-through
	 * (light down by 1), on 26.3's model; no face drawn between two stacked roots. No waterlogging.
	 */
	public static final class Roots extends McFlyBlock implements ModelledBlock {

		@SideOnly(Side.CLIENT)
		private IIcon top;

		public Roots() {
			super(Material.wood);
			setNames("mangrove_roots");
			strength(0.7F, 0.7F);
			tool("axe", 0);
			sound(McFlySoundType.MANGROVE_ROOTS);
			colour(MapColor.obsidianColor);
			setLightOpacity(1);
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
		public boolean isNormalCube() {
			return false;
		}

		@Override
		public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
			return true;
		}

		/** 26.3's `skipRendering`: (x, y, z) is the neighbour; up and down faces against roots aren't drawn. */
		@Override
		public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
			if (side < 2 && world.getBlock(x, y, z) == this) {
				return false;
			}
			return super.shouldSideBeRendered(world, x, y, z, side);
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return true;
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 20;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 5;
		}

		@Override
		public int getRenderType() {
			return McFlyRenderIds.MODEL;
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			return Collections.singletonList(new ModelPart("mangrove_roots"));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart("mangrove_roots"));
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			blockIcon = register.registerIcon("mangrove_roots_side");
			top = register.registerIcon("mangrove_roots_top");
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return side < 2 ? top : blockIcon;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			return texture.endsWith("_top") ? top : blockIcon;
		}
	}

	/** Muddy mangrove roots (26.3's): a pillar, 0.7, a shovel, podzol; plants grow on it as on dirt (it's in `#substrate_overworld`). */
	public static final class MuddyRoots extends BlockRotatedPillar {

		public MuddyRoots() {
			super(Material.ground);
			setBlockName(McFly.name("muddy_mangrove_roots"));
			setHardness(0.7F);
			this.blockResistance = 0.7F * 5.0F;
			setStepSound(McFlySoundType.MUDDY_MANGROVE_ROOTS);
			setHarvestLevel("shovel", 0);
			setCreativeTab(McFlyTab.TAB);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.obsidianColor;
		}

		@Override
		public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
			return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plantable);
		}

		@Override
		public int damageDropped(int meta) {
			return 0;
		}

		@Override
		@SideOnly(Side.CLIENT)
		protected IIcon getSideIcon(int meta) {
			return blockIcon;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			blockIcon = register.registerIcon("muddy_mangrove_roots_side");
			field_150164_N = register.registerIcon("muddy_mangrove_roots_top");
		}
	}

	// ---------------------------------------------------------------- the leaves

	/**
	 * Mangrove leaves (0) and cherry leaves (1), McFly's leaves (D's). Mangrove's: tinted by the foliage, no sapling (26.3's
	 * loot: sticks only), bonemeal hangs a propagule under it, falling `tinted_leaves` at 0.01 tinted as it is there;
	 * cherry's: pink, untinted, its sapling, falling `cherry_leaves` at 0.1. A hoe's (26.3's `#mineable/hoe`).
	 */
	public static final class Leaves extends McFlyLeaves implements IGrowable {

		public Leaves() {
			super("mangrove_leaves", "cherry_leaves");
		}

		@Override
		protected Item saplingOf(int kind) {
			return kind == 1 ? Item.getItemFromBlock(McFlyBlocks.SAPLING.get()) : null;
		}

		@Override
		protected int saplingMetaOf(int kind) {
			return 1;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return (meta & 3) == 1 ? MapColor.pinkColor : MapColor.foliageColor;
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return (world.getBlockMetadata(x, y, z) & 3) == 0 && y > 0 && world.isAirBlock(x, y - 1, z);
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		/** 26.3's `MangroveLeavesBlock.performBonemeal`: a new hanging propagule below. */
		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			world.setBlock(x, y - 1, z, McFlyBlocks.SAPLING.get(), hangingMeta(0), 2);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public int getRenderColor(int meta) {
			// 26.3's item tint for mangrove leaves (`FoliageColor.FOLIAGE_MANGROVE`)
			return (meta & 3) == 0 ? 0x92C648 : 0xFFFFFF;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
			// 1.7.10's leaves' foliage tint (the 3 by 3 average) for mangrove's meta 0
			return (world.getBlockMetadata(x, y, z) & 3) == 0 ? Blocks.leaves.colorMultiplier(world, x, y, z) : 0xFFFFFF;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void randomDisplayTick(World world, int x, int y, int z, Random random) {
			super.randomDisplayTick(world, x, y, z, random);
			if ((world.getBlockMetadata(x, y, z) & 3) == 1) {
				fallingLeaves(world, x, y, z, random, 0.1F, McFlyParticles.CHERRY_LEAVES, -1);
			} else {
				fallingLeaves(world, x, y, z, random, 0.01F, McFlyParticles.TINTED_LEAVES, colorMultiplier(world, x, y, z) & 0xFFFFFF);
			}
		}
	}

	// ---------------------------------------------------------------- the sapling

	/**
	 * The mangrove propagule (meta & 1 0) and the cherry sapling (1), 26.3's `MangrovePropaguleBlock` and `SaplingBlock`:
	 * standing, a stage (meta 8) before its tree, 1 in 7 a random tick (the cherry sapling in light 9 or more), 0.45 a
	 * bonemeal; on 26.3's `#supports_vegetation` (the propagule clay too). A hanging propagule (under mangrove leaves only)
	 * ages by random tick and bonemeal to 4, and drops a propagule only then. Instabreak, popped by pistons; 26.3's
	 * models (its item flat); potted.
	 */
	public static final class Sapling extends BlockSapling implements NamedMetas, ModelledBlock, McFlyPottable {

		@SideOnly(Side.CLIENT)
		private IIcon[] icons;

		public Sapling() {
			setBlockName(McFly.name("mangrove_propagule"));
			setHardness(0.0F);
			setStepSound(soundTypeGrass);
			setCreativeTab(McFlyTab.TAB);
		}

		@Override
		public String nameOf(int meta) {
			return (meta & 1) == 1 ? "cherry_sapling" : "mangrove_propagule";
		}

		@Override
		public boolean isVariant(int meta) {
			return meta < 2;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return (meta & 1) == 1 ? MapColor.pinkColor : MapColor.foliageColor;
		}

		@Override
		protected boolean canPlaceBlockOn(Block ground) {
			return supportsVegetation(ground) || ground == Blocks.clay;
		}

		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			if (world.getBlock(x, y, z) == this && hanging(meta)) {
				return isMangroveLeaves(world, x, y + 1, z);
			}
			Block ground = world.getBlock(x, y - 1, z);
			return (meta & 1) == 1 ? supportsVegetation(ground) : canPlaceBlockOn(ground);
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return world.getBlock(x, y, z).isReplaceable(world, x, y, z) && canPlaceBlockOn(world.getBlock(x, y - 1, z));
		}

		/** 26.3's random tick: a hanging one ages; a standing propagule 1 in 7 (no light needed), a cherry sapling 1 in 7 in light 9 or more. */
		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (world.isRemote) {
				return;
			}
			checkAndDropBlock(world, x, y, z);
			if (world.getBlock(x, y, z) != this) {
				return;
			}
			int meta = world.getBlockMetadata(x, y, z);
			if (hanging(meta)) {
				if (age(meta) < 4) {
					world.setBlockMetadataWithNotify(x, y, z, hangingMeta(age(meta) + 1), 2);
				}
			} else if (((meta & 1) == 0 || world.getBlockLightValue(x, y + 1, z) >= 9) && random.nextInt(7) == 0) {
				func_149879_c(world, x, y, z, random);
			}
		}

		/** 26.3's `advanceTree`: stage 0 to 1, then the tree. */
		@Override
		public void func_149879_c(World world, int x, int y, int z, Random random) {
			int meta = world.getBlockMetadata(x, y, z);
			if (hanging(meta)) {
				return;
			}
			if ((meta & 8) == 0) {
				world.setBlockMetadataWithNotify(x, y, z, meta | 8, 4);
			} else {
				func_149878_d(world, x, y, z, random);
			}
		}

		@Override
		public void func_149878_d(World world, int x, int y, int z, Random random) {
			if (!net.minecraftforge.event.terraingen.TerrainGen.saplingGrowTree(world, random, x, y, z)) {
				return;
			}
			grower(world.getBlockMetadata(x, y, z) & 1, random).grow(world, random, x, y, z);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			int meta = world.getBlockMetadata(x, y, z);
			return !hanging(meta) || age(meta) < 4;
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			return hanging(meta) ? age(meta) < 4 : world.rand.nextFloat() < 0.45F;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			if (hanging(meta)) {
				if (age(meta) < 4) {
					world.setBlock(x, y, z, this, hangingMeta(age(meta) + 1), 2);
				}
			} else {
				func_149879_c(world, x, y, z, random);
			}
		}

		/** 26.3's loot: a propagule only at age 4 (a standing one is always 4), a cherry sapling. */
		@Override
		public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
			ArrayList<ItemStack> drops = new ArrayList<>();
			if (!hanging(meta) || age(meta) == 4) {
				drops.add(new ItemStack(this, 1, meta & 1));
			}
			return drops;
		}

		@Override
		public int damageDropped(int meta) {
			return meta & 1;
		}

		@Override
		public int getDamageValue(World world, int x, int y, int z) {
			return world.getBlockMetadata(x, y, z) & 1;
		}

		/** 26.3's shapes: a standing sapling's; a propagule's 2-wide column from 13, 10, 7, 3 or 0 pixels up by age (standing: 0). */
		@Override
		public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			if ((meta & 1) == 1) {
				setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 0.8F, 0.9F);
				return;
			}
			int[] bottom = {13, 10, 7, 3, 0};
			setBlockBounds(7.0F / 16.0F, (hanging(meta) ? bottom[age(meta)] : 0) / 16.0F, 7.0F / 16.0F, 9.0F / 16.0F, 1.0F, 9.0F / 16.0F);
		}

		@Override
		public void setBlockBoundsForItemRender() {
			setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 0.8F, 0.9F);
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			if (McFlyBlocks.MANGROVE_LOG.isEnabled()) {
				list.add(new ItemStack(item, 1, 0));
			}
			if (io.github.asgerjon.mcfly.config.McFlyConfig.enableCherryTrees) {
				list.add(new ItemStack(item, 1, 1));
			}
		}

		@Override
		public String pottedModel(int meta) {
			return hanging(meta) ? null : (meta & 1) == 1 ? "potted_cherry_sapling" : "potted_mangrove_propagule";
		}

		@Override
		public int getRenderType() {
			return McFlyRenderIds.MODEL_FLAT_ITEM;
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			return Collections.singletonList(new ModelPart((meta & 1) == 1 ? "cherry_sapling" : hanging(meta) ? "mangrove_propagule_hanging_" + age(meta)
				: "mangrove_propagule"));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart((meta & 1) == 1 ? "cherry_sapling" : "mangrove_propagule"));
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			icons = new IIcon[]{register.registerIcon("mangrove_propagule"), register.registerIcon("cherry_sapling"),
				register.registerIcon("mangrove_propagule_hanging"), register.registerIcon("mangrove_propagule_item")};
			blockIcon = icons[0];
			McFlyPots.registerIcons(this, register, 0, 1);
		}

		/** The item's flat picture (26.3's item models: the propagule's own, the cherry sapling's block texture), and the particles'. */
		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return (meta & 1) == 1 ? icons[1] : hanging(meta) ? icons[2] : icons[3];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			String name = texture.substring(texture.lastIndexOf('/') + 1);
			return "cherry_sapling".equals(name) ? icons[1] : "mangrove_propagule_hanging".equals(name) ? icons[2] : icons[0];
		}
	}
}
