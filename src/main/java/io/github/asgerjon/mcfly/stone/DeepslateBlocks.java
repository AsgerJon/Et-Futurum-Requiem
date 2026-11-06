package io.github.asgerjon.mcfly.stone;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Deepslate and its ores (SPEC-modern-movement.md §89, step B), 26.3's, in EFR's stored form: deepslate a pillar (0, 4,
 * 8 the axis), 3 and 6, dropping cobbled deepslate; the deepslate ores 26.3's `ofLegacyCopy` of the stone ore at 4.5
 * and 3 with deepslate's sound, dropping and giving experience as the stone ore does; the redstone one lit by a touch,
 * as 1.7.10's redstone ore (two blocks).
 */
public final class DeepslateBlocks {

	private DeepslateBlocks() {
	}

	public static class Deepslate extends BlockRotatedPillar {
		private IIcon top;

		public Deepslate() {
			super(Material.rock);
			setCreativeTab(McFlyTab.TAB);
			setBlockName(McFly.name("deepslate"));
			setBlockTextureName("deepslate");
			setHardness(3.0F);
			this.blockResistance = 6.0F * 5.0F;
			setStepSound(McFlySoundType.DEEPSLATE);
			setHarvestLevel("pickaxe", 0);
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return McFlyBlocks.COBBLED_DEEPSLATE.isEnabled() ? McFlyBlocks.COBBLED_DEEPSLATE.getItem() : Item.getItemFromBlock(this);
		}

		@Override
		public int damageDropped(int meta) {
			return 0;
		}

		@Override
		protected ItemStack createStackedBlock(int meta) {
			return new ItemStack(this);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.grayColor;
		}

		/**
		 * As stone for ore generation, its spot recorded for EFR's late generator's pass, which turns those ores into deepslate
		 * ones (world generation's glue, EFR's still till world generation's item).
		 */
		@Override
		public boolean isReplaceableOreGen(World world, int x, int y, int z, Block target) {
			boolean ore = target == Blocks.stone || target == this;
			if (ore) {
				doDeepslateRedoCheck(world, x, y, z);
			}
			return ore;
		}

		public static void doDeepslateRedoCheck(World world, int x, int y, int z) {
			if (!ganymedes01.etfuturum.world.EtFuturumLateWorldGenerator.stopRecording && !world.getChunkFromBlockCoords(x, z).sendUpdates) {
				java.util.Map<Long, java.util.List<Integer>> map = ganymedes01.etfuturum.world.EtFuturumLateWorldGenerator.deepslateRedoCache
					.computeIfAbsent(world.provider.dimensionId, k -> com.google.common.collect.Maps.newConcurrentMap());
				java.util.List<Integer> spots = map.computeIfAbsent(net.minecraft.world.ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4),
					k -> com.google.common.collect.Lists.newLinkedList());
				spots.add((x & 0xF) << 12 | (y & 0xFF) << 4 | (z & 0xF));
			}
		}

		@Override
		protected IIcon getSideIcon(int meta) {
			return blockIcon;
		}

		@Override
		protected IIcon getTopIcon(int meta) {
			return top;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			blockIcon = register.registerIcon("deepslate");
			top = register.registerIcon("deepslate_top");
		}
	}

	/** A deepslate ore: the stone ore's drops, its experience and its tool level, at 4.5 and 3, deepslate's sound. */
	public static class Ore extends Block {
		private final Block stoneOre;

		public Ore(String name, Block stoneOre) {
			super(Material.rock);
			this.stoneOre = stoneOre;
			setCreativeTab(McFlyTab.TAB);
			setBlockName(McFly.name(name));
			setBlockTextureName(name);
			setHardness(4.5F);
			this.blockResistance = 3.0F * 5.0F;
			setStepSound(McFlySoundType.DEEPSLATE);
		}

		/** The stone ore's level, asked when wanted (vanilla's are set by Forge after our blocks are made). */
		@Override
		public int getHarvestLevel(int meta) {
			return stoneOre.getHarvestLevel(0);
		}

		@Override
		public String getHarvestTool(int meta) {
			return "pickaxe";
		}

		public Block stoneOre() {
			return stoneOre;
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			Item item = stoneOre.getItemDropped(0, random, fortune);
			return item == Item.getItemFromBlock(stoneOre) ? Item.getItemFromBlock(this) : item;
		}

		@Override
		public int quantityDropped(int meta, int fortune, Random random) {
			return stoneOre.quantityDropped(0, fortune, random);
		}

		@Override
		public int quantityDroppedWithBonus(int fortune, Random random) {
			return stoneOre.quantityDroppedWithBonus(fortune, random);
		}

		@Override
		public int damageDropped(int meta) {
			return Item.getItemFromBlock(stoneOre) == stoneOre.getItemDropped(0, new Random(), 0) ? 0 : stoneOre.damageDropped(0);
		}

		@Override
		public int getExpDrop(IBlockAccess world, int meta, int fortune) {
			return stoneOre.getExpDrop(world, 0, fortune);
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.grayColor;
		}
	}

	/** The deepslate redstone ore, lit or not (`deepslate_redstone_ore`, `deepslate_lit_redstone_ore`): a touch lights it, a random tick puts it out. */
	public static class RedstoneOre extends Ore {
		private final boolean lit;

		public RedstoneOre(boolean lit) {
			super("deepslate_redstone_ore", lit ? Blocks.lit_redstone_ore : Blocks.redstone_ore);
			this.lit = lit;
			if (lit) {
				setLightLevel(0.625F);
				setTickRandomly(true);
				setCreativeTab(null);
			}
		}

		private void light(World world, int x, int y, int z) {
			spark(world, x, y, z);
			if (!lit && McFlyBlocks.DEEPSLATE_LIT_REDSTONE_ORE.isEnabled()) {
				world.setBlock(x, y, z, McFlyBlocks.DEEPSLATE_LIT_REDSTONE_ORE.get());
			}
		}

		@Override
		public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
			light(world, x, y, z);
			super.onBlockClicked(world, x, y, z, player);
		}

		@Override
		public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
			light(world, x, y, z);
			super.onEntityWalking(world, x, y, z, entity);
		}

		@Override
		public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
			light(world, x, y, z);
			return super.onBlockActivated(world, x, y, z, player, side, hx, hy, hz);
		}

		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (lit && McFlyBlocks.DEEPSLATE_REDSTONE_ORE.isEnabled()) {
				world.setBlock(x, y, z, McFlyBlocks.DEEPSLATE_REDSTONE_ORE.get());
			}
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return Items.redstone;
		}

		@Override
		protected ItemStack createStackedBlock(int meta) {
			return McFlyBlocks.DEEPSLATE_REDSTONE_ORE.newItemStack();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void randomDisplayTick(World world, int x, int y, int z, Random random) {
			if (lit) {
				spark(world, x, y, z);
			}
		}

		/** 1.7.10's redstone ore's sparks: a speck off each open face. */
		private static void spark(World world, int x, int y, int z) {
			Random random = world.rand;
			double off = 0.0625D;
			for (int face = 0; face < 6; face++) {
				double px = x + random.nextFloat(), py = y + random.nextFloat(), pz = z + random.nextFloat();
				if (face == 0 && !world.getBlock(x, y + 1, z).isOpaqueCube()) py = y + 1 + off;
				if (face == 1 && !world.getBlock(x, y - 1, z).isOpaqueCube()) py = y - off;
				if (face == 2 && !world.getBlock(x, y, z + 1).isOpaqueCube()) pz = z + 1 + off;
				if (face == 3 && !world.getBlock(x, y, z - 1).isOpaqueCube()) pz = z - off;
				if (face == 4 && !world.getBlock(x + 1, y, z).isOpaqueCube()) px = x + 1 + off;
				if (face == 5 && !world.getBlock(x - 1, y, z).isOpaqueCube()) px = x - off;
				if (px < x || px > x + 1 || py < 0.0D || py > y + 1 || pz < z || pz > z + 1) {
					world.spawnParticle("reddust", px, py, pz, 0.0D, 0.0D, 0.0D);
				}
			}
		}
	}

	private static final class Items {
		static final Item redstone = net.minecraft.init.Items.redstone;
	}
}
