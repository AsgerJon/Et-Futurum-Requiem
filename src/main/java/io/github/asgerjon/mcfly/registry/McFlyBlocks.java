package io.github.asgerjon.mcfly.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.blocks.BlockDriedKelp;
import ganymedes01.etfuturum.blocks.BlockKelp;
import ganymedes01.etfuturum.blocks.BlockLightningRod;
import ganymedes01.etfuturum.blocks.BlockLodestone;
import ganymedes01.etfuturum.blocks.BlockPowderSnow;
import ganymedes01.etfuturum.blocks.BlockPowderSnowCauldron;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Our blocks (SPEC-modern-movement.md §79, B6), registered by McFly in pre-init, each one that's on under its entry's
 * name in lower case: in this fork the mod being loaded is EFR's, so the ids are the ones EFR's list gave them
 * (`etfuturum:lodestone` and so on), which old saves hold. The same API as EFR's list had for them.
 */
public enum McFlyBlocks {
	// the lightning rod and its copper ages (spec §58); EFR's copper must be on
	LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(0)),
	EXPOSED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(1)),
	WEATHERED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(2)),
	OXIDIZED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(3)),
	WAXED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(8)),
	WAXED_EXPOSED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(9)),
	WAXED_WEATHERED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(10)),
	WAXED_OXIDIZED_LIGHTNING_ROD(ConfigBlocksItems.enableLightningRod && ConfigBlocksItems.enableCopper, new BlockLightningRod(11)),
	// the lodestone (spec §57)
	LODESTONE(McFlyConfig.enableLodestone, new BlockLodestone()),
	// powder snow and its cauldron (spec §62): no items of their own (the bucket is the powder snow's)
	POWDER_SNOW(McFlyConfig.enablePowderSnow, new BlockPowderSnow(), false),
	POWDER_SNOW_CAULDRON(McFlyConfig.enablePowderSnow, new BlockPowderSnowCauldron(), false),
	// kelp (the aquatic update)
	KELP(McFlyConfig.enableKelp, new BlockKelp()),
	DRIED_KELP_BLOCK(McFlyConfig.enableKelp, new BlockDriedKelp()),
	// banners (spec §80): the item its own, the tile entity registered with it (as EFR named it: old saves hold it)
	BANNER(McFlyConfig.enableBanners, new io.github.asgerjon.mcfly.banner.BannerBlock(), io.github.asgerjon.mcfly.banner.BannerItem.class);

	public static final McFlyBlocks[] VALUES = values();

	private final boolean enabled;
	private final Block block;
	private final Class<? extends ItemBlock> item;

	McFlyBlocks(boolean enabled, Block block) {
		this(enabled, block, ItemBlock.class);
	}

	McFlyBlocks(boolean enabled, Block block, boolean hasItem) {
		this(enabled, block, hasItem ? ItemBlock.class : null);
	}

	McFlyBlocks(boolean enabled, Block block, Class<? extends ItemBlock> item) {
		this.enabled = enabled;
		this.block = block;
		this.item = item;
	}

	/** In pre-init: each one that's on, with its item block (or none), under its entry's name; the banner's tile entity. */
	public static void register() {
		for (McFlyBlocks entry : VALUES) {
			if (entry.enabled) {
				GameRegistry.registerBlock(entry.block, entry.item, entry.name().toLowerCase());
			}
		}
		if (BANNER.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.banner.BannerBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("banner"));
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Block get() {
		return block;
	}

	public Item getItem() {
		return Item.getItemFromBlock(block);
	}

	public ItemStack newItemStack() {
		return newItemStack(1);
	}

	public ItemStack newItemStack(int count) {
		return newItemStack(count, 0);
	}

	public ItemStack newItemStack(int count, int meta) {
		return new ItemStack(block, count, meta);
	}
}
