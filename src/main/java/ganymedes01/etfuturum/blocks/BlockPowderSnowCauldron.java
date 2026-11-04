package ganymedes01.etfuturum.blocks;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.BlockCauldron;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The powder snow cauldron (SPEC-modern-movement.md §62.8; 26.3 {@code LayeredCauldronBlock} with snow), levels 1 to 3
 * as its metadata. Filled by snowfall ({@code PowderSnowCauldrons.snowfall}) and the powder snow bucket; an empty bucket
 * takes it when full; water and lava buckets replace it; a burning entity in it turns it to water a level lower.
 * Nothing freezes in it.
 */
public class BlockPowderSnowCauldron extends BlockCauldron {

	public BlockPowderSnowCauldron() {
		super();
		setHardness(2.0F);
		setStepSound(Blocks.cauldron.stepSound);
		setBlockName(Utils.getUnlocalisedName("powder_snow_cauldron"));
	}

	public static int contentHeight16(int level) {
		return 6 + 3 * level;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held == null) {
			return false;
		}
		Item item = held.getItem();
		int level = world.getBlockMetadata(x, y, z);
		if (item == Items.bucket) {
			if (level < 3 || !McFlyItems.POWDER_SNOW_BUCKET.isEnabled()) {
				return false;
			}
			if (!world.isRemote) {
				world.setBlock(x, y, z, Blocks.cauldron, 0, 3);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.fill_powder_snow", 1.0F, 1.0F);
				giveFilled(player, held, new ItemStack(McFlyItems.POWDER_SNOW_BUCKET.get()));
			}
			return true;
		}
		if (item == Items.water_bucket) {
			return replace(world, x, y, z, player, held, Blocks.cauldron, 3, "item.bucket.empty");
		}
		if (item == Items.lava_bucket && ModBlocks.LAVA_CAULDRON.isEnabled()) {
			return replace(world, x, y, z, player, held, ModBlocks.LAVA_CAULDRON.get(), 0, "item.bucket.empty_lava");
		}
		if (McFlyItems.POWDER_SNOW_BUCKET.isEnabled() && item == McFlyItems.POWDER_SNOW_BUCKET.get()) {
			return replace(world, x, y, z, player, held, this, 3, "item.bucket.empty_powder_snow");
		}
		return false;
	}

	private static boolean replace(World world, int x, int y, int z, EntityPlayer player, ItemStack held, net.minecraft.block.Block block, int meta, String sound) {
		if (!world.isRemote) {
			world.setBlock(x, y, z, block, meta, 3);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":" + sound, 1.0F, 1.0F);
			if (!player.capabilities.isCreativeMode) {
				held.func_150996_a(Items.bucket);
			}
		}
		return true;
	}

	/** 26.3 {@code ItemUtils.createFilledResult}: one empty bucket off, the filled one in (or dropped); creative keeps it. */
	private static void giveFilled(EntityPlayer player, ItemStack empty, ItemStack filled) {
		if (player.capabilities.isCreativeMode) {
			if (!player.inventory.hasItem(filled.getItem())) {
				player.inventory.addItemStackToInventory(filled);
			}
			return;
		}
		if (--empty.stackSize <= 0) {
			player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
		} else if (!player.inventory.addItemStackToInventory(filled)) {
			player.dropPlayerItemWithRandomChoice(filled, false);
		}
	}

	/** 26.3: a burning entity in the content turns it to water a level lower (a player under spawn protection doesn't), and is put out. */
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (world.isRemote || !entity.isBurning()) {
			return;
		}
		int level = world.getBlockMetadata(x, y, z);
		if (entity.boundingBox.minY > y + contentHeight16(level) / 16.0D) {
			return;
		}
		if (!(entity instanceof EntityPlayerMP) || MinecraftServer.getServer() == null
				|| !MinecraftServer.getServer().isBlockProtected(world, x, y, z, (EntityPlayer) entity)) {
			world.setBlock(x, y, z, Blocks.cauldron, level - 1, 3);
		}
		entity.extinguish();
	}

	/** Rain fills 1.7.10's cauldrons with water; this one only snowfall fills. */
	@Override
	public void fillWithRain(World world, int x, int y, int z) {
	}

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		return world.getBlockMetadata(x, y, z);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Items.cauldron;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Items.cauldron;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.POWDER_SNOW_CAULDRON;
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return Blocks.cauldron.getIcon(side, meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		// the cauldron's own icons
	}
}
