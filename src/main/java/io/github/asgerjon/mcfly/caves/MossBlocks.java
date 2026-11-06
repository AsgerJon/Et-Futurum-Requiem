package io.github.asgerjon.mcfly.caves;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import io.github.asgerjon.mcfly.tree.BendingTrunkPlacer;
import io.github.asgerjon.mcfly.tree.BlockProvider;
import io.github.asgerjon.mcfly.tree.FeatureSize;
import io.github.asgerjon.mcfly.tree.IntRange;
import io.github.asgerjon.mcfly.tree.RandomSpreadFoliagePlacer;
import io.github.asgerjon.mcfly.tree.Tree;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.UseHoeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Moss and azalea as 26.3's (SPEC-modern-movement.md §90.5, step D), EFR's ids where EFR had them: the moss block (its
 * bonemeal 26.3's `moss_patch_bonemeal`), the moss carpet, azalea and flowering azalea (one id; bonemeal at 0.45 grows
 * 26.3's azalea tree), their leaves, rooted dirt (a hoe makes dirt and drops hanging roots, bonemeal hangs roots below),
 * hanging roots.
 */
public final class MossBlocks {

	public static final MossBlocks EVENTS = new MossBlocks();

	private MossBlocks() {
	}

	/** 26.3's `#dirt` and the like where plants grow (`#supports_vegetation`): 1.7.10's and McFly's. */
	static boolean isDirt(Block block) {
		return block == Blocks.grass || block == Blocks.dirt || block == Blocks.mycelium || block == Blocks.farmland || block == McFlyBlocks.MOSS_BLOCK.get()
			|| block == McFlyBlocks.ROOTED_DIRT.get() || block == McFlyBlocks.MUD.get()
			|| (McFly.DOMAIN + ":coarse_dirt").equals(Block.blockRegistry.getNameForObject(block));
	}

	/** 26.3's `#moss_replaceable`: overworld stone (stone, granite and the like, tuff, deepslate), dirt, mud, moss, grass. */
	static boolean mossReplaceable(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == Blocks.stone || block == McFlyBlocks.STONE.get() || block == McFlyBlocks.DEEPSLATE.get()
			|| block == McFlyBlocks.TUFF.get() && world.getBlockMetadata(x, y, z) == 0 || isDirt(block) && block != Blocks.farmland;
	}

	// ---------------------------------------------------------------- the azalea tree (26.3's feature/azalea_tree)

	private static Tree azaleaTree;

	/** 26.3's `azalea_tree`: an oak log bending trunk (4 + rand 3, a bend of 1 to 2), random-spread leaves (3:1 azalea to flowering), rooted dirt under it. */
	public static Tree azaleaTree() {
		if (azaleaTree == null) {
			Block leaves = McFlyBlocks.AZALEA_LEAVES.get();
			azaleaTree = new Tree(BlockProvider.of(Blocks.log, 0), new BendingTrunkPlacer(4, 2, 0, 3, new IntRange(1, 2)),
				BlockProvider.weighted(new BlockProvider.State[]{new BlockProvider.State(leaves, 0), new BlockProvider.State(leaves, 1)}, new int[]{3, 1}),
				new RandomSpreadFoliagePlacer(IntRange.of(3), IntRange.of(0), IntRange.of(2), 50), new FeatureSize(1, 0, 1),
				BlockProvider.of(McFlyBlocks.ROOTED_DIRT.get(), 0), false);
		}
		return azaleaTree;
	}

	// ---------------------------------------------------------------- the moss block

	/** The moss block: 0.1, green, the hoe's, popped; plants grow on it; bonemeal with air above places 26.3's moss patch. */
	public static final class Moss extends McFlyBlock implements IGrowable {

		public Moss() {
			super(Material.grass);
			setNames("moss_block");
			strength(0.1F, 0.1F);
			tool("hoe", 0);
			sound(McFlySoundType.MOSS);
			colour(MapColor.greenColor);
		}

		@Override
		public int getMobilityFlag() {
			return 1;
		}

		@Override
		public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
			return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plantable);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return world.isAirBlock(x, y + 1, z);
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			mossPatch(world, random, x, y + 1, z);
		}
	}

	/**
	 * 26.3's `moss_patch_bonemeal` (a `vegetation_patch` on the floor) from the air above the moss: each column of a square
	 * of radius 2 or 3 but its corners (the edges' at 0.75) finds the floor within 5, turns the block under it to moss
	 * (depth 1) where `#moss_replaceable` and under air; then a plant on each at 0.6 (26.3's `moss_vegetation`).
	 */
	static boolean mossPatch(World world, Random random, int x, int y, int z) {
		int xRadius = new IntRange(1, 2).sample(random) + 1, zRadius = new IntRange(1, 2).sample(random) + 1;
		List<int[]> surface = new ArrayList<>();
		for (int dx = -xRadius; dx <= xRadius; dx++) {
			boolean xEdge = dx == -xRadius || dx == xRadius;
			for (int dz = -zRadius; dz <= zRadius; dz++) {
				boolean zEdge = dz == -zRadius || dz == zRadius;
				boolean corner = xEdge && zEdge, edge = (xEdge || zEdge) && !corner;
				if (corner || edge && random.nextFloat() > 0.75F) {
					continue;
				}
				int px = x + dx, py = y, pz = z + dz;
				for (int i = 0; i < 5 && py > 0 && world.isAirBlock(px, py, pz); i++) {
					py--;
				}
				for (int i = 0; i < 5 && py < world.getHeight() - 1 && !world.isAirBlock(px, py, pz); i++) {
					py++;
				}
				Block below = world.getBlock(px, py - 1, pz);
				if (world.isAirBlock(px, py, pz) && below.isSideSolid(world, px, py - 1, pz, ForgeDirection.UP)) {
					Block moss = McFlyBlocks.MOSS_BLOCK.get();
					if (below == moss || mossReplaceable(world, px, py - 1, pz)) {
						if (below != moss) {
							world.setBlock(px, py - 1, pz, moss, 0, 2);
						}
						surface.add(new int[]{px, py - 1, pz});
					}
				}
			}
		}
		for (int[] at : surface) {
			if (random.nextFloat() < 0.6F) {
				mossVegetation(world, random, at[0], at[1] + 1, at[2]);
			}
		}
		return !surface.isEmpty();
	}

	/** 26.3's `moss_vegetation`: flowering azalea 4, azalea 7, moss carpet 25, short grass 50, tall grass 10, where it can stay. */
	private static void mossVegetation(World world, Random random, int x, int y, int z) {
		int pick = random.nextInt(96);
		Block block;
		int meta = 0;
		if (pick < 4) {
			block = McFlyBlocks.AZALEA.get();
			meta = 1;
		} else if (pick < 11) {
			block = McFlyBlocks.AZALEA.get();
		} else if (pick < 36) {
			block = McFlyBlocks.MOSS_CARPET.get();
		} else if (pick < 86) {
			block = Blocks.tallgrass;
			meta = 1;
		} else {
			if (world.isAirBlock(x, y + 1, z) && Blocks.double_plant.canPlaceBlockAt(world, x, y, z)) {
				Blocks.double_plant.func_149889_c(world, x, y, z, 2, 2);
			}
			return;
		}
		if (world.isAirBlock(x, y, z) && block.canPlaceBlockAt(world, x, y, z)) {
			world.setBlock(x, y, z, block, meta, 2);
		}
	}

	// ---------------------------------------------------------------- the carpet

	/** The moss carpet: 26.3's `CarpetBlock`, 1 pixel high, on anything but air (gone without it, no drops); 0.1, green. */
	public static final class Carpet extends McFlyBlock {

		public Carpet() {
			super(Material.carpet);
			setNames("moss_carpet");
			setBlockTextureName("moss_block");
			strength(0.1F, 0.1F);
			sound(McFlySoundType.MOSS_CARPET);
			colour(MapColor.greenColor);
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 16.0F, 1.0F);
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
		public void setBlockBoundsForItemRender() {
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 16.0F, 1.0F);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
			return side == 1 || super.shouldSideBeRendered(world, x, y, z, side);
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return super.canPlaceBlockAt(world, x, y, z) && !world.isAirBlock(x, y - 1, z);
		}

		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!world.isRemote && world.isAirBlock(x, y - 1, z)) {
				world.setBlockToAir(x, y, z);
			}
		}
	}

	// ---------------------------------------------------------------- azalea

	/**
	 * Azalea (0) and flowering azalea (1): instabreak, popped, fire 30 and 60, their own sounds (the flowering one's by
	 * EFR's per-variant table), on 26.3's `#supports_azalea` (what plants grow on, and clay); 26.3's shape (the top half
	 * and a 4-wide post) and model; bonemeal at 0.45 grows the azalea tree where it has room and no fluid above.
	 */
	public static final class Azalea extends BlockBush implements IGrowable, ModelledBlock, NamedMetas, McFlyPottable {

		private static final String[] NAMES = {"azalea", "flowering_azalea"};
		@SideOnly(Side.CLIENT)
		private IIcon[] icons;

		public Azalea() {
			super(Material.plants);
			setBlockName(McFly.name("azalea"));
			setHardness(0.0F);
			setStepSound(McFlySoundType.AZALEA);
			setCreativeTab(McFlyTab.TAB);
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}

		@Override
		public String nameOf(int meta) {
			return NAMES[meta & 1];
		}

		@Override
		public boolean isVariant(int meta) {
			return meta < 2;
		}

		@Override
		protected boolean canPlaceBlockOn(Block ground) {
			return isDirt(ground) || ground == Blocks.clay;
		}

		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			return canPlaceBlockOn(world.getBlock(x, y - 1, z));
		}

		@Override
		public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
			return false;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.foliageColor;
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
			return 60;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 30;
		}

		@Override
		public int damageDropped(int meta) {
			return meta & 1;
		}

		@Override
		public int getDamageValue(World world, int x, int y, int z) {
			return world.getBlockMetadata(x, y, z) & 1;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			list.add(new ItemStack(item, 1, 0));
			list.add(new ItemStack(item, 1, 1));
		}

		/** 26.3's shape for collision: the top half and a 4-wide post under it. */
		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
			for (AxisAlignedBB box : new AxisAlignedBB[]{AxisAlignedBB.getBoundingBox(x, y + 0.5, z, x + 1, y + 1, z + 1),
				AxisAlignedBB.getBoundingBox(x + 0.375, y, z + 0.375, x + 0.625, y + 0.5, z + 0.625)}) {
				if (mask.intersectsWith(box)) {
					list.add(box);
				}
			}
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return y + 2 < world.getHeight() && !world.getBlock(x, y + 1, z).getMaterial().isLiquid();
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return world.rand.nextFloat() < 0.45F;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			azaleaTree().grow(world, random, x, y, z);
		}

		@Override
		public int getRenderType() {
			return McFlyRenderIds.MODEL;
		}

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			return Collections.singletonList(new ModelPart(NAMES[world.getBlockMetadata(x, y, z) & 1]));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart(NAMES[meta & 1]));
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			icons = new IIcon[]{register.registerIcon("azalea_side"), register.registerIcon("azalea_top"), register.registerIcon("flowering_azalea_side"),
				register.registerIcon("flowering_azalea_top"), register.registerIcon("azalea_plant")};
			blockIcon = icons[1];
			McFlyPots.registerIcons(this, register, 0, 1);
		}

		/** 26.3's `potted_azalea_bush` and `potted_flowering_azalea_bush` (spec §90, step D2). */
		@Override
		public String pottedModel(int meta) {
			return meta == 0 ? "potted_azalea_bush" : meta == 1 ? "potted_flowering_azalea_bush" : null;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return icons[(meta & 1) * 2 + (side < 2 ? 1 : 0)];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			String name = texture.substring(texture.lastIndexOf('/') + 1);
			switch (name) {
				case "azalea_side":
					return icons[0];
				case "azalea_top":
					return icons[1];
				case "flowering_azalea_side":
					return icons[2];
				case "flowering_azalea_top":
					return icons[3];
				default:
					return icons[4];
			}
		}
	}

	/** Azalea leaves (0) and flowering azalea leaves (1), McFly's leaves; their sapling the azalea of their kind. */
	public static final class AzaleaLeaves extends McFlyLeaves {

		public AzaleaLeaves() {
			super("azalea_leaves", "flowering_azalea_leaves");
			setStepSound(McFlySoundType.AZALEA_LEAVES);
		}

		@Override
		protected Item saplingOf(int kind) {
			return Item.getItemFromBlock(McFlyBlocks.AZALEA.get());
		}

		@Override
		protected int saplingMetaOf(int kind) {
			return kind;
		}

		/** 26.3's falling leaves (`UntintedParticleLeavesBlock`: 0.01 a display tick, `tinted_leaves` coloured 0x70922D), both kinds. */
		@Override
		@SideOnly(Side.CLIENT)
		public void randomDisplayTick(World world, int x, int y, int z, Random random) {
			super.randomDisplayTick(world, x, y, z, random);
			fallingLeaves(world, x, y, z, random, 0.01F, io.github.asgerjon.mcfly.particle.McFlyParticles.TINTED_LEAVES, 0x70922D);
		}
	}

	// ---------------------------------------------------------------- rooted dirt and hanging roots

	/** Rooted dirt: 0.5, dirt's colour, a shovel; plants grow on it; bonemeal hangs roots below into air; a hoe (`UseHoeEvent`) makes dirt and drops hanging roots. */
	public static final class RootedDirt extends McFlyBlock implements IGrowable {

		public RootedDirt() {
			super(Material.ground);
			setNames("rooted_dirt");
			strength(0.5F, 0.5F);
			tool("shovel", 0);
			sound(McFlySoundType.ROOTED_DIRT);
			colour(MapColor.dirtColor);
		}

		@Override
		public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
			return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plantable);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return y > 0 && world.isAirBlock(x, y - 1, z);
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			world.setBlock(x, y - 1, z, McFlyBlocks.HANGING_ROOTS.get());
		}
	}

	/** Hanging roots: instabreak, replaceable, under a full face (gone without it, no drops), shears only, fire 30 and 60. */
	public static final class HangingRoots extends Block implements IShearable {

		public HangingRoots() {
			super(Material.vine);
			setBlockName(McFly.name("hanging_roots"));
			setBlockTextureName("hanging_roots");
			setHardness(0.0F);
			setStepSound(McFlySoundType.HANGING_ROOTS);
			setCreativeTab(McFlyTab.TAB);
			// 26.3's `column(12, 10, 16)`
			setBlockBounds(2.0F / 16.0F, 10.0F / 16.0F, 2.0F / 16.0F, 14.0F / 16.0F, 1.0F, 14.0F / 16.0F);
		}

		private static boolean held(World world, int x, int y, int z) {
			return world.getBlock(x, y + 1, z).isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN);
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return super.canPlaceBlockAt(world, x, y, z) && held(world, x, y, z);
		}

		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (!world.isRemote && !held(world, x, y, z)) {
				world.setBlockToAir(x, y, z);
			}
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return null;
		}

		@Override
		public boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z) {
			return true;
		}

		@Override
		public ArrayList<ItemStack> onSheared(ItemStack item, IBlockAccess world, int x, int y, int z, int fortune) {
			ArrayList<ItemStack> drops = new ArrayList<>();
			drops.add(new ItemStack(this));
			return drops;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.dirtColor;
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return true;
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 60;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 30;
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			return null;
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
			return 1;
		}
	}

	// ---------------------------------------------------------------- the hoe on rooted dirt

	public static void register() {
		MinecraftForge.EVENT_BUS.register(EVENTS);
	}

	/** 26.3's `till/rooted_dirt`: dirt, one hanging roots popped from the clicked face, the hoe's sound and wear. */
	@SubscribeEvent
	public void hoe(UseHoeEvent event) {
		if (event.world.getBlock(event.x, event.y, event.z) != McFlyBlocks.ROOTED_DIRT.get()) {
			return;
		}
		if (!event.world.isRemote) {
			event.world.setBlock(event.x, event.y, event.z, Blocks.dirt);
			event.world.playSoundEffect(event.x + 0.5, event.y + 0.5, event.z + 0.5, McFly.MC_ASSETS + ":item.hoe.till", 1.0F, 1.0F);
			EntityItem roots = new EntityItem(event.world, event.x + 0.5, event.y + 1.1, event.z + 0.5, McFlyBlocks.HANGING_ROOTS.newItemStack());
			roots.delayBeforeCanPickup = 10;
			event.world.spawnEntityInWorld(roots);
		}
		event.setResult(Event.Result.ALLOW);
	}
}
