package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyPots;
import io.github.asgerjon.mcfly.base.McFlyPottable;
import io.github.asgerjon.mcfly.base.NamedMetas;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockRotatedPillar;
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
import net.minecraftforge.common.IShearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Nether's plants and stems (SPEC-modern-movement.md §88.7), in EFR's stored form (meta 0 crimson, 1 warped), as
 * 26.3's: nylium, fungi, roots, nether sprouts, crimson and warped stems. The vines are {@link NetherVinesBlock}, the
 * huge fungus {@link HugeFungus}; wart blocks and shroomlight plain blocks of `McFlyBlocks`.
 */
public final class NetherPlants {

	private NetherPlants() {
	}

	static final String[] WOODS = {"crimson", "warped"};

	/** 26.3's `#supports_*_fungus`, `_roots`, `nether_sprouts`: the overworld's ground, farmland, nylium, mycelium (fungi), soul soil. */
	static boolean netherGround(Block ground, boolean mycelium) {
		return ground == Blocks.grass || ground == Blocks.dirt || ground == Blocks.farmland || ground == Blocks.mycelium && mycelium
			|| McFlyBlocks.NYLIUM.isEnabled() && ground == McFlyBlocks.NYLIUM.get() || McFlyBlocks.SOUL_SOIL.isEnabled() && ground == McFlyBlocks.SOUL_SOIL.get();
	}

	// ---------------------------------------------------------------- nylium

	/**
	 * Crimson and warped nylium (26.3's `NyliumBlock`): 0.4, a pickaxe for drops, netherrack unless Silk Touch; netherrack
	 * again on a random tick under a block that stops light; bonemeal grows its forest's plants on it (26.3's
	 * `NYLIUM_BONEMEAL`: 9 tries 2 around of its vegetation; warped, 9 of sprouts too and 1 in 8 a twisting vine).
	 */
	public static class Nylium extends Block implements NamedMetas, IGrowable, RandomTicking {
		@SideOnly(Side.CLIENT)
		private IIcon[] tops, sides;

		public Nylium() {
			super(Material.rock);
			setCreativeTab(McFlyTab.TAB);
			setBlockName(McFly.name("crimson_nylium"));
			setHardness(0.4F);
			this.blockResistance = 0.4F * 5.0F;
			setStepSound(McFlySoundType.NYLIUM);
			setHarvestLevel("pickaxe", 0);
			setTickRandomly(true);
		}

		@Override
		public String nameOf(int meta) {
			return WOODS[meta & 1] + "_nylium";
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return Item.getItemFromBlock(Blocks.netherrack);
		}

		@Override
		public int damageDropped(int meta) {
			return 0;
		}

		@Override
		protected ItemStack createStackedBlock(int meta) {
			return new ItemStack(this, 1, meta & 1);
		}

		@Override
		public int getDamageValue(World world, int x, int y, int z) {
			return world.getBlockMetadata(x, y, z) & 1;
		}

		@Override
		public void randomTick(World world, int x, int y, int z, Random random) {
			if (world.getBlock(x, y + 1, z).getLightOpacity(world, x, y + 1, z) >= 15) {
				world.setBlock(x, y, z, Blocks.netherrack, 0, 3);
			}
		}

		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			randomTick(world, x, y, z, random);
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
			boolean warped = (world.getBlockMetadata(x, y, z) & 1) == 1;
			vegetation(world, random, x, y + 1, z, warped);
			if (warped) {
				for (int i = 0; i < 9; i++) {
					place(world, random, x, y + 1, z, McFlyBlocks.NETHER_SPROUTS.get(), 0);
				}
				if (random.nextInt(8) == 0 && McFlyBlocks.TWISTING_VINES.isEnabled() && world.isAirBlock(x, y + 1, z)) {
					int length = 1 + random.nextInt(2);
					for (int i = 0; i < length && world.isAirBlock(x, y + 1 + i, z); i++) {
						world.setBlock(x, y + 1 + i, z, McFlyBlocks.TWISTING_VINES.get(), i == length - 1 ? random.nextInt(NetherVinesBlock.MAX_AGE) : 0, 3);
					}
				}
			}
		}

		/**
		 * 26.3's forest vegetation, 9 tries 2 around: crimson roots 87, crimson fungus 11, warped fungus 1 (of 99); warped
		 * roots 85, crimson roots 1, warped fungus 13, crimson fungus 1 (of 100).
		 */
		private static void vegetation(World world, Random random, int x, int y, int z, boolean warped) {
			for (int i = 0; i < 9; i++) {
				int roll = random.nextInt(warped ? 100 : 99);
				boolean roots = warped ? roll < 86 : roll < 87;
				int meta = warped ? (roll < 85 ? 1 : roll < 86 ? 0 : roll < 99 ? 1 : 0) : (roll < 98 ? 0 : 1);
				place(world, random, x, y, z, (roots ? McFlyBlocks.NETHER_ROOTS : McFlyBlocks.NETHER_FUNGUS).get(), meta);
			}
		}

		/** One try of a placement 2 around, in air, if it can stand there (26.3's `SimpleBlockFeature`). */
		private static void place(World world, Random random, int x, int y, int z, Block plant, int meta) {
			int px = x + triangle(random), pz = z + triangle(random);
			if (world.isAirBlock(px, y, pz) && plant.canPlaceBlockAt(world, px, y, pz)) {
				world.setBlock(px, y, pz, plant, meta, 3);
			}
		}

		/** 26.3's `TrapezoidInt.triangle(2)`: -2 to 2, the middle likelier. */
		private static int triangle(Random random) {
			return random.nextInt(3) + random.nextInt(3) - 2;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return (meta & 1) == 1 ? MapColor.cyanColor : MapColor.netherrackColor;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			list.add(new ItemStack(item, 1, 0));
			list.add(new ItemStack(item, 1, 1));
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			tops = new IIcon[]{register.registerIcon("crimson_nylium"), register.registerIcon("warped_nylium")};
			sides = new IIcon[]{register.registerIcon("crimson_nylium_side"), register.registerIcon("warped_nylium_side")};
			blockIcon = tops[0];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return side == 1 ? tops[meta & 1] : side == 0 ? Blocks.netherrack.getIcon(0, 0) : sides[meta & 1];
		}
	}

	// ---------------------------------------------------------------- the plants

	/** A crossed plant of two kinds on 26.3's Nether grounds: replaceable, no collision, instabreak. */
	public static class Plant extends BlockBush implements NamedMetas, McFlyPottable {
		private final String[] names;
		private final boolean mycelium;
		@SideOnly(Side.CLIENT)
		private IIcon[] icons;

		public Plant(String[] names, boolean mycelium, SoundType sound) {
			super(Material.plants);
			this.names = names;
			this.mycelium = mycelium;
			setCreativeTab(McFlyTab.TAB);
			setBlockName(McFly.name(names[0]));
			setHardness(0.0F);
			setStepSound(sound);
			setBlockBounds(0.125F, 0.0F, 0.125F, 0.875F, 0.8125F, 0.875F);
		}

		@Override
		public String nameOf(int meta) {
			return names[Math.min(meta & 1, names.length - 1)];
		}

		@Override
		protected boolean canPlaceBlockOn(Block ground) {
			return netherGround(ground, mycelium);
		}

		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			return canPlaceBlockOn(world.getBlock(x, y - 1, z));
		}

		@Override
		public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
			return !mycelium;
		}

		@Override
		public int damageDropped(int meta) {
			return Math.min(meta & 1, names.length - 1);
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			for (int i = 0; i < names.length; i++) {
				list.add(new ItemStack(item, 1, i));
			}
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			icons = new IIcon[names.length];
			for (int i = 0; i < names.length; i++) {
				icons[i] = register.registerIcon(names[i]);
			}
			blockIcon = icons[0];
			int[] metas = new int[names.length];
			for (int i = 0; i < metas.length; i++) {
				metas[i] = i;
			}
			McFlyPots.registerIcons(this, register, metas);
		}

		/**
		 * 26.3's potted fungi and roots (`potted_crimson_fungus`, `potted_warped_roots`: spec §90, step D2; EFR's pot
		 * item went with EFR's blocks in §88), by the kind's name.
		 */
		@Override
		public String pottedModel(int meta) {
			return meta >= 0 && meta < names.length ? "potted_" + names[meta] : null;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon getIcon(int side, int meta) {
			return icons[Math.min(meta & 1, names.length - 1)];
		}
	}

	/** Crimson and warped fungi (26.3's `NetherFungusBlock`): on their nylium, bonemeal 40% grows the planted huge fungus. */
	public static class Fungus extends Plant implements IGrowable {
		public Fungus() {
			super(new String[]{"crimson_fungus", "warped_fungus"}, true, McFlySoundType.FUNGUS);
		}

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return world.getBlock(x, y - 1, z) == McFlyBlocks.NYLIUM.get() && (world.getBlockMetadata(x, y - 1, z) & 1) == (world.getBlockMetadata(x, y, z) & 1);
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return random.nextFloat() < 0.4F;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			int kind = world.getBlockMetadata(x, y, z) & 1;
			new HugeFungus(McFlyBlocks.NYLIUM.get(), kind, kind == 0 ? McFlyBlocks.CRIMSON_STEM.get() : McFlyBlocks.WARPED_STEM.get(), 0,
				McFlyBlocks.NETHER_WART.get(), kind, McFlyBlocks.SHROOMLIGHT.get(), kind == 0, true).place(world, random, x, y, z);
		}
	}

	/** Nether sprouts (26.3's `NetherSproutsBlock`): dropped by shears only. */
	public static class Sprouts extends Plant implements IShearable {
		public Sprouts() {
			super(new String[]{"nether_sprouts"}, false, McFlySoundType.NETHER_SPROUTS);
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.1875F, 1.0F);
		}

		/** 26.3 pots no sprouts. */
		@Override
		public String pottedModel(int meta) {
			return null;
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
	}

	// ---------------------------------------------------------------- stems

	/**
	 * Crimson and warped stems (26.3's), EFR's form: meta & 3 the kind (0 stem, 1 hyphae, 2 stripped stem, 3 stripped
	 * hyphae), meta & 12 the axis; 2, an axe's, not burning; an axe strips it (`wood/Stripping`'s table), keeping the axis.
	 */
	public static class Stem extends BlockRotatedPillar implements NamedMetas {
		private static final String[] KINDS = {"%s_stem", "%s_hyphae", "stripped_%s_stem", "stripped_%s_hyphae"};
		private final String wood;
		private IIcon[] sides, ends;

		public Stem(String wood) {
			super(Material.wood);
			this.wood = wood;
			setCreativeTab(McFlyTab.TAB);
			setBlockName(McFly.name(wood + "_stem"));
			setHardness(2.0F);
			this.blockResistance = 2.0F * 5.0F;
			setStepSound(McFlySoundType.STEM);
			setHarvestLevel("axe", 0);
		}

		@Override
		public String nameOf(int meta) {
			return String.format(KINDS[meta & 3], wood);
		}

		@Override
		public int damageDropped(int meta) {
			return meta & 3;
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection face) {
			return false;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return wood.equals("crimson") ? MapColor.netherrackColor : MapColor.cyanColor;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void getSubBlocks(Item item, CreativeTabs tab, List list) {
			for (int i = 0; i < 4; i++) {
				list.add(new ItemStack(item, 1, i));
			}
		}

		@Override
		protected IIcon getSideIcon(int meta) {
			return sides[meta & 3];
		}

		@Override
		protected IIcon getTopIcon(int meta) {
			return ends[meta & 3];
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			String stem = wood + "_stem", stripped = "stripped_" + wood + "_stem";
			IIcon side = register.registerIcon(stem), top = register.registerIcon(stem + "_top");
			IIcon strippedSide = register.registerIcon(stripped), strippedTop = register.registerIcon(stripped + "_top");
			sides = new IIcon[]{side, side, strippedSide, strippedSide};
			ends = new IIcon[]{top, side, strippedTop, strippedSide};
			blockIcon = side;
		}
	}
}
