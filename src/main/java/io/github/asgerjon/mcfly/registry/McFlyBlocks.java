package io.github.asgerjon.mcfly.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.blocks.BlockDriedKelp;
import ganymedes01.etfuturum.blocks.BlockKelp;
import io.github.asgerjon.mcfly.lodestone.LodestoneBlock;
import ganymedes01.etfuturum.blocks.BlockPowderSnow;
import ganymedes01.etfuturum.blocks.BlockPowderSnowCauldron;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Our blocks (SPEC-modern-movement.md §79, B6), registered by McFly in pre-init, each one that's on under its entry's
 * name in lower case: in this fork the mod being loaded is EFR's, so the ids are the ones EFR's list gave them
 * (`etfuturum:lodestone` and so on), which old saves hold. The same API as EFR's list had for them.
 */
public enum McFlyBlocks {
	// copper (spec §87), in EFR's stored form: the copper block (and cut copper) first, which the slab and stairs are drawn from
	COPPER_BLOCK(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperFullBlock(), io.github.asgerjon.mcfly.copper.CopperMetaBlock.CopperItem.class),
	CHISELED_COPPER(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.ChiseledCopperBlock(), io.github.asgerjon.mcfly.copper.CopperMetaBlock.CopperItem.class),
	COPPER_GRATE(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperGrateBlock(), io.github.asgerjon.mcfly.copper.CopperMetaBlock.CopperItem.class),
	COPPER_BULB(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperBulbBlock(false), io.github.asgerjon.mcfly.copper.CopperMetaBlock.CopperItem.class),
	POWERED_COPPER_BULB(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperBulbBlock(true), false),
	CUT_COPPER_SLAB(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperSlabBlock(COPPER_BLOCK.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_CUT_COPPER_SLAB(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperSlabBlock((io.github.asgerjon.mcfly.copper.CopperSlabBlock) CUT_COPPER_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 0, false)),
	EXPOSED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 1, false)),
	WEATHERED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 2, false)),
	OXIDIZED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 3, false)),
	WAXED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 0, true)),
	WAXED_EXPOSED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 1, true)),
	WAXED_WEATHERED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 2, true)),
	WAXED_OXIDIZED_CUT_COPPER_STAIRS(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperStairsBlock(COPPER_BLOCK.get(), 3, true)),
	COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(0, false), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	EXPOSED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(1, false), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	WEATHERED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(2, false), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	OXIDIZED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(3, false), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	WAXED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(0, true), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	WAXED_EXPOSED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(1, true), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	WAXED_WEATHERED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(2, true), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	WAXED_OXIDIZED_COPPER_DOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperDoorBlock(3, true), io.github.asgerjon.mcfly.copper.CopperDoorItem.class),
	COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(0, false)),
	EXPOSED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(1, false)),
	WEATHERED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(2, false)),
	OXIDIZED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(3, false)),
	WAXED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(0, true)),
	WAXED_EXPOSED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(1, true)),
	WAXED_WEATHERED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(2, true)),
	WAXED_OXIDIZED_COPPER_TRAPDOOR(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperTrapdoorBlock(3, true)),
	// the lightning rod and its copper ages (spec §58, §87)
	LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(0, false)),
	EXPOSED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(1, false)),
	WEATHERED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(2, false)),
	OXIDIZED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(3, false)),
	WAXED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(0, true)),
	WAXED_EXPOSED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(1, true)),
	WAXED_WEATHERED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(2, true)),
	WAXED_OXIDIZED_LIGHTNING_ROD(McFlyConfig.enableLightningRod && McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.LightningRodBlock(3, true)),
	// copper's ores and the raw ores' blocks (copper, iron, gold)
	COPPER_ORE(McFlyConfig.enableCopper, new io.github.asgerjon.mcfly.copper.CopperOreBlock(false)),
	DEEPSLATE_COPPER_ORE(McFlyConfig.enableCopper && ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems.enableDeepslate && ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems.enableDeepslateOres, new io.github.asgerjon.mcfly.copper.CopperOreBlock(true)),
	RAW_ORE_BLOCK(McFlyConfig.enableRawOres, new io.github.asgerjon.mcfly.copper.RawOreBlock(), io.github.asgerjon.mcfly.copper.RawOreBlock.RawOreBlockItem.class),
	// the lodestone (spec §57)
	LODESTONE(McFlyConfig.enableLodestone, new io.github.asgerjon.mcfly.lodestone.LodestoneBlock()),
	// powder snow and its cauldron (spec §62): no items of their own (the bucket is the powder snow's)
	POWDER_SNOW(McFlyConfig.enablePowderSnow, new BlockPowderSnow(), false),
	POWDER_SNOW_CAULDRON(McFlyConfig.enablePowderSnow, new BlockPowderSnowCauldron(), false),
	// kelp (the aquatic update)
	KELP(McFlyConfig.enableKelp, new BlockKelp()),
	DRIED_KELP_BLOCK(McFlyConfig.enableKelp, new BlockDriedKelp()),
	// banners (spec §80): the item its own, the tile entity registered with it (as EFR named it: old saves hold it)
	BANNER(McFlyConfig.enableBanners, new io.github.asgerjon.mcfly.banner.BannerBlock(), io.github.asgerjon.mcfly.banner.BannerItem.class),
	// shulker boxes (spec §81): one block for all 17, as EFR named it (old saves hold it)
	SHULKER_BOX(McFlyConfig.enableShulkerBoxes, new io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxBlock(), io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxItem.class),
	// the End's blocks (spec §84), under EFR's ids (old saves hold them): end stone bricks are EFR's `end_bricks`
	PURPUR_BLOCK(McFlyConfig.enableChorusBlocks, io.github.asgerjon.mcfly.end.EndBlocks.stone("purpur_block", 1.5F, 6.0F, net.minecraft.block.material.MapColor.magentaColor)),
	PURPUR_PILLAR(McFlyConfig.enableChorusBlocks, io.github.asgerjon.mcfly.end.EndBlocks.purpurPillar()),
	PURPUR_STAIRS(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlyStairs(PURPUR_BLOCK.get(), "purpur_stairs")),
	PURPUR_SLAB(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlySlab(PURPUR_BLOCK.get(), "purpur_slab").strength(2.0F, 6.0F),
		io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_PURPUR_SLAB(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlySlab((io.github.asgerjon.mcfly.base.McFlySlab) PURPUR_SLAB.get()).strength(2.0F, 6.0F),
		io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	END_BRICKS(McFlyConfig.enableChorusBlocks, io.github.asgerjon.mcfly.end.EndBlocks.stone("end_bricks", 3.0F, 9.0F, net.minecraft.block.material.MapColor.sandColor)),
	END_BRICK_STAIRS(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlyStairs(END_BRICKS.get(), "end_brick_stairs")),
	END_BRICK_SLAB(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlySlab(END_BRICKS.get(), "end_brick_slab").strength(3.0F, 9.0F),
		io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_END_BRICK_SLAB(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlySlab((io.github.asgerjon.mcfly.base.McFlySlab) END_BRICK_SLAB.get()).strength(3.0F, 9.0F),
		io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	END_BRICK_WALL(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.base.McFlyWall(END_BRICKS.get(), "end_brick_wall").strength(3.0F, 9.0F)),
	END_ROD(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.end.EndRodBlock()),
	CHORUS_PLANT(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.end.ChorusPlantBlock()),
	CHORUS_FLOWER(McFlyConfig.enableChorusBlocks, new io.github.asgerjon.mcfly.end.ChorusFlowerBlock()),
	// the Nether's stones (spec §88.3), in EFR's stored form: blackstone first, which its slab, stairs, wall, plate and button are drawn from
	BLACKSTONE(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyMetaBlock(net.minecraft.block.material.Material.rock, Block.soundTypePiston,
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("blackstone", "blackstone", "blackstone_top", 1.5F, 6.0F, MapColor.blackColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("polished_blackstone", 2.0F, 6.0F, MapColor.blackColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("polished_blackstone_bricks", 1.5F, 6.0F, MapColor.blackColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("cracked_polished_blackstone_bricks", 1.5F, 6.0F, MapColor.blackColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("chiseled_polished_blackstone", 1.5F, 6.0F, MapColor.blackColor)).tool("pickaxe", 0), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	GILDED_BLACKSTONE(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.nether.GildedBlackstoneBlock()),
	BLACKSTONE_SLAB(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("blackstone_slab", BLACKSTONE.get(), 0, 2.0F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_blackstone_slab", BLACKSTONE.get(), 1, 2.0F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_blackstone_brick_slab", BLACKSTONE.get(), 2, 2.0F, 6.0F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_BLACKSTONE_SLAB(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) BLACKSTONE_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	BLACKSTONE_STAIRS(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyStairs(BLACKSTONE.get(), 0, "blackstone_stairs").strength(1.5F, 6.0F)),
	POLISHED_BLACKSTONE_STAIRS(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyStairs(BLACKSTONE.get(), 1, "polished_blackstone_stairs").strength(2.0F, 6.0F)),
	POLISHED_BLACKSTONE_BRICK_STAIRS(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyStairs(BLACKSTONE.get(), 2, "polished_blackstone_brick_stairs").strength(1.5F, 6.0F)),
	BLACKSTONE_WALL(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyWall(BLACKSTONE.get(), new String[]{"blackstone_wall", "polished_blackstone_wall", "polished_blackstone_brick_wall"},
		new int[]{0, 1, 2}).strength(1.5F, 6.0F).strength(1, 2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	POLISHED_BLACKSTONE_PRESSURE_PLATE(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyPressurePlate("polished_blackstone_pressure_plate",
		net.minecraft.block.material.Material.rock, net.minecraft.block.BlockPressurePlate.Sensitivity.mobs, BLACKSTONE.get(), 1, true)),
	POLISHED_BLACKSTONE_BUTTON(McFlyConfig.enableBlackstone, new io.github.asgerjon.mcfly.base.McFlyButton("polished_blackstone_button", false, BLACKSTONE.get(), 1)),
	BASALT(McFlyConfig.enableBasalt, new io.github.asgerjon.mcfly.nether.BasaltBlock(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	SMOOTH_BASALT(McFlyConfig.enableBasalt, new io.github.asgerjon.mcfly.base.McFlyBlock(net.minecraft.block.material.Material.rock).setNames("smooth_basalt").strength(1.25F, 4.2F)
		.tool("pickaxe", 0).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.BASALT).colour(MapColor.blackColor)),
	NETHER_GOLD_ORE(McFlyConfig.enableNetherGold, new io.github.asgerjon.mcfly.nether.NetherGoldOreBlock()),
	CRYING_OBSIDIAN(McFlyConfig.enableCryingObsidian, new io.github.asgerjon.mcfly.nether.CryingObsidianBlock()),
	RED_NETHERBRICK(McFlyConfig.enableRedNetherBricks, new io.github.asgerjon.mcfly.base.McFlyMetaBlock(net.minecraft.block.material.Material.rock, io.github.asgerjon.mcfly.sound.McFlySoundType.NETHER_BRICKS,
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("red_nether_bricks", 2.0F, 6.0F, MapColor.netherrackColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("cracked_nether_bricks", 2.0F, 6.0F, MapColor.netherrackColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("chiseled_nether_bricks", 2.0F, 6.0F, MapColor.netherrackColor)).tool("pickaxe", 0), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	RED_NETHERBRICK_SLAB(McFlyConfig.enableRedNetherBricks, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("red_nether_brick_slab", RED_NETHERBRICK.get(), 0, 2.0F, 6.0F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_RED_NETHERBRICK_SLAB(McFlyConfig.enableRedNetherBricks, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) RED_NETHERBRICK_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	RED_NETHERBRICK_STAIRS(McFlyConfig.enableRedNetherBricks, new io.github.asgerjon.mcfly.base.McFlyStairs(RED_NETHERBRICK.get(), 0, "red_nether_brick_stairs").strength(2.0F, 6.0F)),
	RED_NETHER_BRICK_WALL(McFlyConfig.enableRedNetherBricks, new io.github.asgerjon.mcfly.base.McFlyWall(RED_NETHERBRICK.get(), "red_nether_brick_wall").strength(2.0F, 6.0F)),
	QUARTZ_BRICKS(McFlyConfig.enableQuartzBricks, new io.github.asgerjon.mcfly.base.McFlyBlock(net.minecraft.block.material.Material.rock).setNames("quartz_bricks").strength(0.8F, 0.8F)
		.tool("pickaxe", 0).colour(MapColor.quartzColor)),
	// netherite (spec §88.4): the block first, which EFR's stairs are drawn from
	NETHERITE_BLOCK(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteBlocks.NetheriteBlock()),
	ANCIENT_DEBRIS(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteBlocks.AncientDebris()),
	NETHERITE_STAIRS(McFlyConfig.enableNetherite, new io.github.asgerjon.mcfly.nether.NetheriteBlocks.Stairs(NETHERITE_BLOCK.get())),
	// the chain and the lights (spec §88.5), in EFR's stored form
	CHAIN(McFlyConfig.enableChain, new io.github.asgerjon.mcfly.nether.ChainBlock(), io.github.asgerjon.mcfly.nether.NetherLightItem.class),
	LANTERN(McFlyConfig.enableLantern, new io.github.asgerjon.mcfly.nether.LanternBlock("lantern", 15), io.github.asgerjon.mcfly.nether.NetherLightItem.class),
	SOUL_LANTERN(McFlyConfig.enableLantern && McFlyConfig.enableSoulLighting, new io.github.asgerjon.mcfly.nether.LanternBlock("soul_lantern", 10), io.github.asgerjon.mcfly.nether.NetherLightItem.class),
	SOUL_TORCH(McFlyConfig.enableSoulLighting, new io.github.asgerjon.mcfly.nether.SoulTorchBlock()),
	// soul soil and soul fire (spec §88.6); soul fire has no item
	SOUL_SOIL(McFlyConfig.enableSoulSoil, new io.github.asgerjon.mcfly.base.McFlyBlock(net.minecraft.block.material.Material.ground).setNames("soul_soil")
		.strength(0.5F, 0.5F).tool("shovel", 0).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.SOUL_SOIL).colour(MapColor.brownColor)),
	SOUL_FIRE(McFlyConfig.enableSoulSoil || McFlyConfig.enableSoulLighting, new io.github.asgerjon.mcfly.nether.SoulFireBlock(), false),
	// the Nether's plants and stems (spec §88.7), in EFR's stored form; wart blocks and shroomlight first, the huge fungus's hat
	NETHER_WART(McFlyConfig.enableNetherwartBlock || McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.base.McFlyMetaBlock(net.minecraft.block.material.Material.grass,
		io.github.asgerjon.mcfly.sound.McFlySoundType.WART_BLOCK,
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("nether_wart_block", 1.0F, 1.0F, MapColor.redColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("warped_wart_block", 1.0F, 1.0F, MapColor.cyanColor)).tool("hoe", 0), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	SHROOMLIGHT(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.base.McFlyBlock(net.minecraft.block.material.Material.grass).setNames("shroomlight").strength(1.0F, 1.0F)
		.tool("hoe", 0).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.SHROOMLIGHT).colour(MapColor.redColor).light(15)),
	CRIMSON_STEM(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherPlants.Stem("crimson"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WARPED_STEM(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherPlants.Stem("warped"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	NYLIUM(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherPlants.Nylium(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	NETHER_FUNGUS(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherPlants.Fungus(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	NETHER_ROOTS(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherPlants.Plant(new String[]{"crimson_roots", "warped_roots"}, false,
		io.github.asgerjon.mcfly.sound.McFlySoundType.ROOTS), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	NETHER_SPROUTS(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherPlants.Sprouts(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WEEPING_VINES(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherVinesBlock("weeping_vines", net.minecraftforge.common.util.ForgeDirection.DOWN)),
	TWISTING_VINES(McFlyConfig.enableNetherPlants, new io.github.asgerjon.mcfly.nether.NetherVinesBlock("twisting_vines", net.minecraftforge.common.util.ForgeDirection.UP)),
	// the target (spec §88.8), EFR's form: the meta its power
	TARGET(McFlyConfig.enableTarget, new io.github.asgerjon.mcfly.nether.TargetBlock()),
	// deepslate and its ores (spec §89, step B), EFR's ids
	DEEPSLATE(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Deepslate()),
	COBBLED_DEEPSLATE(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyBlock(net.minecraft.block.material.Material.rock).setNames("cobbled_deepslate")
		.strength(3.5F, 6.0F).tool("pickaxe", 0).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.DEEPSLATE).colour(MapColor.grayColor)),
	POLISHED_DEEPSLATE(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyBlock(net.minecraft.block.material.Material.rock).setNames("polished_deepslate")
		.strength(3.5F, 6.0F).tool("pickaxe", 0).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.POLISHED_DEEPSLATE).colour(MapColor.grayColor)),
	DEEPSLATE_COAL_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Ore("deepslate_coal_ore", net.minecraft.init.Blocks.coal_ore)),
	DEEPSLATE_IRON_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Ore("deepslate_iron_ore", net.minecraft.init.Blocks.iron_ore)),
	DEEPSLATE_GOLD_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Ore("deepslate_gold_ore", net.minecraft.init.Blocks.gold_ore)),
	DEEPSLATE_LAPIS_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Ore("deepslate_lapis_ore", net.minecraft.init.Blocks.lapis_ore)),
	DEEPSLATE_DIAMOND_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Ore("deepslate_diamond_ore", net.minecraft.init.Blocks.diamond_ore)),
	DEEPSLATE_EMERALD_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.Ore("deepslate_emerald_ore", net.minecraft.init.Blocks.emerald_ore)),
	// deepslate's set (spec §89, step C), EFR's ids; the tiles' and polished variants' own sounds by EFR's MultiBlockSoundRegistry (a bridge)
	DEEPSLATE_BRICKS(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyMetaBlock(net.minecraft.block.material.Material.rock, io.github.asgerjon.mcfly.sound.McFlySoundType.DEEPSLATE_BRICKS,
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("deepslate_bricks", 3.5F, 6.0F, MapColor.grayColor), new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("cracked_deepslate_bricks", 3.5F, 6.0F, MapColor.grayColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("deepslate_tiles", 3.5F, 6.0F, MapColor.grayColor), new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("cracked_deepslate_tiles", 3.5F, 6.0F, MapColor.grayColor),
		new io.github.asgerjon.mcfly.base.McFlyMetaBlock.Variant("chiseled_deepslate", 3.5F, 6.0F, MapColor.grayColor)).tool("pickaxe", 0), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	DEEPSLATE_SLAB(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("cobbled_deepslate_slab", COBBLED_DEEPSLATE.get(), 0, 3.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_deepslate_slab", POLISHED_DEEPSLATE.get(), 0, 3.5F, 6.0F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_DEEPSLATE_SLAB(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) DEEPSLATE_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DEEPSLATE_BRICK_SLAB(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("deepslate_brick_slab", DEEPSLATE_BRICKS.get(), 0, 3.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("deepslate_tile_slab", DEEPSLATE_BRICKS.get(), 2, 3.5F, 6.0F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_DEEPSLATE_BRICK_SLAB(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) DEEPSLATE_BRICK_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	COBBLED_DEEPSLATE_STAIRS(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyStairs(COBBLED_DEEPSLATE.get(), 0, "cobbled_deepslate_stairs").strength(3.5F, 6.0F)),
	POLISHED_DEEPSLATE_STAIRS(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyStairs(POLISHED_DEEPSLATE.get(), 0, "polished_deepslate_stairs").strength(3.5F, 6.0F)),
	DEEPSLATE_BRICK_STAIRS(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyStairs(DEEPSLATE_BRICKS.get(), 0, "deepslate_brick_stairs").strength(3.5F, 6.0F)),
	DEEPSLATE_TILE_STAIRS(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyStairs(DEEPSLATE_BRICKS.get(), 2, "deepslate_tile_stairs").strength(3.5F, 6.0F).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.DEEPSLATE_TILES)),
	DEEPSLATE_WALL(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyWall(new net.minecraft.block.Block[]{COBBLED_DEEPSLATE.get(), POLISHED_DEEPSLATE.get()},
		new String[]{"cobbled_deepslate_wall", "polished_deepslate_wall"}, new int[]{0, 0}).strength(3.5F, 6.0F), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	DEEPSLATE_BRICK_WALL(McFlyConfig.enableDeepslate, new io.github.asgerjon.mcfly.base.McFlyWall(DEEPSLATE_BRICKS.get(), new String[]{"deepslate_brick_wall", "deepslate_tile_wall"},
		new int[]{0, 2}).strength(3.5F, 6.0F), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	DEEPSLATE_REDSTONE_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.RedstoneOre(false)),
	DEEPSLATE_LIT_REDSTONE_ORE(McFlyConfig.enableDeepslate && McFlyConfig.enableDeepslateOres, new io.github.asgerjon.mcfly.stone.DeepslateBlocks.RedstoneOre(true), false),
	// the old stones (spec §89, step D), EFR's ids: granite, diorite, andesite and polished (`stone` 1 to 6, 0 EFR's look-alike of granite),
	// red sandstone, the smooth blocks, their slabs, stairs and walls; the vanilla stones' slabs, stairs and walls 1.7.10 lacks
	STONE(McFlyConfig.enableStones && !McFlyConfig.disableBaseBountifulStonesOnly, new io.github.asgerjon.mcfly.stone.StoneBlocks.OldStone(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	RED_SANDSTONE(McFlyConfig.enableRedSandstone, io.github.asgerjon.mcfly.stone.StoneBlocks.redSandstone(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	SMOOTH_STONE(McFlyConfig.enableSmoothStone, io.github.asgerjon.mcfly.stone.StoneBlocks.smooth("smooth_stone", "stone_slab_top", MapColor.stoneColor)),
	SMOOTH_SANDSTONE(McFlyConfig.enableSmoothSandstone, io.github.asgerjon.mcfly.stone.StoneBlocks.smooth("smooth_sandstone", "sandstone_top", MapColor.sandColor)),
	SMOOTH_RED_SANDSTONE(McFlyConfig.enableRedSandstone, io.github.asgerjon.mcfly.stone.StoneBlocks.smooth("smooth_red_sandstone", "red_sandstone_top", MapColor.adobeColor)),
	SMOOTH_QUARTZ(McFlyConfig.enableSmoothQuartz, io.github.asgerjon.mcfly.stone.StoneBlocks.smooth("smooth_quartz", "quartz_block_bottom", MapColor.quartzColor)),
	RED_SANDSTONE_SLAB(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("red_sandstone_slab", RED_SANDSTONE.get(), 0, 2.0F, 0.8F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("cut_red_sandstone_slab", RED_SANDSTONE.get(), 2, 2.0F, 0.8F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_RED_SANDSTONE_SLAB(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) RED_SANDSTONE_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	STONE_SLAB(McFlyConfig.enableExtraVanillaSlabs, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("stone_slab", net.minecraft.init.Blocks.stone, 0, 2.0F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("mossy_cobblestone_slab", net.minecraft.init.Blocks.mossy_cobblestone, 0, 2.0F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("mossy_stone_brick_slab", net.minecraft.init.Blocks.stonebrick, 1, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("cut_sandstone_slab", net.minecraft.init.Blocks.sandstone, 2, 2.0F, 6.0F)).tool("pickaxe", 0), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_STONE_SLAB(McFlyConfig.enableExtraVanillaSlabs, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) STONE_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	STONE_SLAB_2(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("granite_slab", STONE.get(), 1, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_granite_slab", STONE.get(), 2, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("diorite_slab", STONE.get(), 3, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_diorite_slab", STONE.get(), 4, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("andesite_slab", STONE.get(), 5, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_andesite_slab", STONE.get(), 6, 1.5F, 6.0F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_STONE_SLAB_2(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) STONE_SLAB_2.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	SMOOTH_SANDSTONE_SLAB(McFlyConfig.enableSmoothSandstone, new io.github.asgerjon.mcfly.base.McFlySlab(SMOOTH_SANDSTONE.get(), "smooth_sandstone_slab").strength(2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_SMOOTH_SANDSTONE_SLAB(McFlyConfig.enableSmoothSandstone, new io.github.asgerjon.mcfly.base.McFlySlab((io.github.asgerjon.mcfly.base.McFlySlab) SMOOTH_SANDSTONE_SLAB.get()).strength(2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	SMOOTH_RED_SANDSTONE_SLAB(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlySlab(SMOOTH_RED_SANDSTONE.get(), "smooth_red_sandstone_slab").strength(2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_SMOOTH_RED_SANDSTONE_SLAB(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlySlab((io.github.asgerjon.mcfly.base.McFlySlab) SMOOTH_RED_SANDSTONE_SLAB.get()).strength(2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	SMOOTH_QUARTZ_SLAB(McFlyConfig.enableSmoothQuartz, new io.github.asgerjon.mcfly.base.McFlySlab(SMOOTH_QUARTZ.get(), "smooth_quartz_slab").strength(2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_SMOOTH_QUARTZ_SLAB(McFlyConfig.enableSmoothQuartz, new io.github.asgerjon.mcfly.base.McFlySlab((io.github.asgerjon.mcfly.base.McFlySlab) SMOOTH_QUARTZ_SLAB.get()).strength(2.0F, 6.0F), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	SMOOTH_SANDSTONE_STAIRS(McFlyConfig.enableSmoothSandstone, new io.github.asgerjon.mcfly.base.McFlyStairs(SMOOTH_SANDSTONE.get(), 0, "smooth_sandstone_stairs").strength(2.0F, 6.0F)),
	SMOOTH_RED_SANDSTONE_STAIRS(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlyStairs(SMOOTH_RED_SANDSTONE.get(), 0, "smooth_red_sandstone_stairs").strength(2.0F, 6.0F)),
	SMOOTH_QUARTZ_STAIRS(McFlyConfig.enableSmoothQuartz, new io.github.asgerjon.mcfly.base.McFlyStairs(SMOOTH_QUARTZ.get(), 0, "smooth_quartz_stairs").strength(2.0F, 6.0F)),
	GRANITE_STAIRS(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyStairs(STONE.get(), 1, "granite_stairs").strength(1.5F, 6.0F)),
	POLISHED_GRANITE_STAIRS(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyStairs(STONE.get(), 2, "polished_granite_stairs").strength(1.5F, 6.0F)),
	DIORITE_STAIRS(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyStairs(STONE.get(), 3, "diorite_stairs").strength(1.5F, 6.0F)),
	POLISHED_DIORITE_STAIRS(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyStairs(STONE.get(), 4, "polished_diorite_stairs").strength(1.5F, 6.0F)),
	ANDESITE_STAIRS(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyStairs(STONE.get(), 5, "andesite_stairs").strength(1.5F, 6.0F)),
	POLISHED_ANDESITE_STAIRS(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyStairs(STONE.get(), 6, "polished_andesite_stairs").strength(1.5F, 6.0F)),
	RED_SANDSTONE_STAIRS(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlyStairs(RED_SANDSTONE.get(), 0, "red_sandstone_stairs").strength(0.8F, 0.8F)),
	MOSSY_STONE_BRICK_STAIRS(McFlyConfig.enableExtraVanillaStairs, new io.github.asgerjon.mcfly.base.McFlyStairs(net.minecraft.init.Blocks.stonebrick, 1, "mossy_stone_brick_stairs").strength(1.5F, 6.0F).tool("pickaxe", 0)),
	MOSSY_COBBLESTONE_STAIRS(McFlyConfig.enableExtraVanillaStairs, new io.github.asgerjon.mcfly.base.McFlyStairs(net.minecraft.init.Blocks.mossy_cobblestone, 0, "mossy_cobblestone_stairs").strength(2.0F, 6.0F).tool("pickaxe", 0)),
	STONE_STAIRS(McFlyConfig.enableExtraVanillaStairs, new io.github.asgerjon.mcfly.base.McFlyStairs(net.minecraft.init.Blocks.stone, 0, "stone_stairs").strength(1.5F, 6.0F).tool("pickaxe", 0)),
	STONE_WALL(McFlyConfig.enableExtraVanillaWalls, new io.github.asgerjon.mcfly.base.McFlyWall(new Block[]{net.minecraft.init.Blocks.stonebrick, net.minecraft.init.Blocks.stonebrick, net.minecraft.init.Blocks.sandstone, net.minecraft.init.Blocks.brick_block},
		new String[]{"stone_brick_wall", "mossy_stone_brick_wall", "sandstone_wall", "brick_wall"}, new int[]{0, 1, 0, 0}).strength(1.5F, 6.0F).strength(2, 0.8F, 0.8F)
		.strength(3, 2.0F, 6.0F).tool("pickaxe", 0), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	NETHER_BRICK_WALL(McFlyConfig.enableExtraVanillaWalls, new io.github.asgerjon.mcfly.base.McFlyWall(net.minecraft.init.Blocks.nether_brick, "nether_brick_wall").strength(2.0F, 6.0F).tool("pickaxe", 0)
		.sound(io.github.asgerjon.mcfly.sound.McFlySoundType.NETHER_BRICKS)),
	STONE_WALL_2(McFlyConfig.enableStones, new io.github.asgerjon.mcfly.base.McFlyWall(STONE.get(), new String[]{"granite_wall", "diorite_wall", "andesite_wall"}, new int[]{1, 3, 5}).strength(1.5F, 6.0F),
		io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	RED_SANDSTONE_WALL(McFlyConfig.enableRedSandstone, new io.github.asgerjon.mcfly.base.McFlyWall(RED_SANDSTONE.get(), "red_sandstone_wall").strength(0.8F, 0.8F)),
	// tuff and calcite (spec §89, step E), EFR's ids; polished tuff's and the bricks' own sounds by EFR's MultiBlockSoundRegistry (a bridge)
	TUFF(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.stone.StoneBlocks.Tuff(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	CALCITE(McFlyConfig.enableCalcite, io.github.asgerjon.mcfly.stone.StoneBlocks.calcite()),
	TUFF_SLAB(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("tuff_slab", TUFF.get(), 0, 1.5F, 6.0F),
		new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("polished_tuff_slab", TUFF.get(), 1, 1.5F, 6.0F), new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("tuff_brick_slab", TUFF.get(), 2, 1.5F, 6.0F)), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_TUFF_SLAB(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) TUFF_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	TUFF_STAIRS(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.base.McFlyStairs(TUFF.get(), 0, "tuff_stairs").strength(1.5F, 6.0F)),
	POLISHED_TUFF_STAIRS(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.base.McFlyStairs(TUFF.get(), 1, "polished_tuff_stairs").strength(1.5F, 6.0F).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.POLISHED_TUFF)),
	TUFF_BRICK_STAIRS(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.base.McFlyStairs(TUFF.get(), 2, "tuff_brick_stairs").strength(1.5F, 6.0F).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.TUFF_BRICKS)),
	TUFF_WALL(McFlyConfig.enableTuff, new io.github.asgerjon.mcfly.base.McFlyWall(TUFF.get(), new String[]{"tuff_wall", "polished_tuff_wall", "tuff_brick_wall"}, new int[]{0, 1, 2})
		.strength(1.5F, 6.0F), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	// glow lichen and cave vines (spec §90.3, step A), EFR's ids and tile entities
	GLOW_LICHEN(McFlyConfig.enableGlowLichen, new io.github.asgerjon.mcfly.caves.GlowLichenBlock(), io.github.asgerjon.mcfly.caves.GlowLichenItem.class),
	CAVE_VINE(McFlyConfig.enableGlowBerries, new io.github.asgerjon.mcfly.caves.CaveVines.Head()),
	CAVE_VINE_PLANT(McFlyConfig.enableGlowBerries, new io.github.asgerjon.mcfly.caves.CaveVines.Body((io.github.asgerjon.mcfly.caves.CaveVines.Head) CAVE_VINE.get())),
	// amethyst (spec §90.4, step B), EFR's ids: the clusters' meta facing + 6 x size
	AMETHYST_BLOCK(McFlyConfig.enableAmethyst, new io.github.asgerjon.mcfly.caves.AmethystBlocks.Amethyst("amethyst_block")),
	BUDDING_AMETHYST(McFlyConfig.enableAmethyst, new io.github.asgerjon.mcfly.caves.AmethystBlocks.Budding()),
	AMETHYST_CLUSTER_1(McFlyConfig.enableAmethyst, new io.github.asgerjon.mcfly.caves.AmethystBlocks.Cluster(0), io.github.asgerjon.mcfly.caves.AmethystBlocks.ClusterItem.class),
	AMETHYST_CLUSTER_2(McFlyConfig.enableAmethyst, new io.github.asgerjon.mcfly.caves.AmethystBlocks.Cluster(1), io.github.asgerjon.mcfly.caves.AmethystBlocks.ClusterItem.class),
	TINTED_GLASS(McFlyConfig.enableAmethyst, new io.github.asgerjon.mcfly.caves.AmethystBlocks.TintedGlass()),
	// mud (spec §90, step C), EFR's ids: packed mud and mud bricks in one id
	MUD(McFlyConfig.enableMud, new io.github.asgerjon.mcfly.caves.MudBlocks.Mud()),
	PACKED_MUD(McFlyConfig.enableMud, new io.github.asgerjon.mcfly.caves.MudBlocks.PackedMud(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	MUD_BRICK_SLAB(McFlyConfig.enableMud, new io.github.asgerjon.mcfly.base.McFlyVariantSlab(new io.github.asgerjon.mcfly.base.McFlyVariantSlab.Variant("mud_brick_slab", PACKED_MUD.get(), 1, 1.5F, 3.0F)).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.MUD_BRICKS), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_MUD_BRICK_SLAB(McFlyConfig.enableMud, new io.github.asgerjon.mcfly.base.McFlyVariantSlab((io.github.asgerjon.mcfly.base.McFlyVariantSlab) MUD_BRICK_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	MUD_BRICK_STAIRS(McFlyConfig.enableMud, new io.github.asgerjon.mcfly.base.McFlyStairs(PACKED_MUD.get(), 1, "mud_brick_stairs").strength(1.5F, 3.0F).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.MUD_BRICKS)),
	MUD_BRICK_WALL(McFlyConfig.enableMud, new io.github.asgerjon.mcfly.base.McFlyWall(PACKED_MUD.get(), new String[]{"mud_brick_wall"}, new int[]{1}).strength(1.5F, 3.0F).sound(io.github.asgerjon.mcfly.sound.McFlySoundType.MUD_BRICKS)),
	// moss and azalea (spec §90.5, step D), EFR's ids (EFR's were an experiment); rooted dirt and hanging roots new
	MOSS_BLOCK(McFlyConfig.enableMossAzalea, new io.github.asgerjon.mcfly.caves.MossBlocks.Moss()),
	MOSS_CARPET(McFlyConfig.enableMossAzalea, new io.github.asgerjon.mcfly.caves.MossBlocks.Carpet()),
	AZALEA(McFlyConfig.enableMossAzalea, new io.github.asgerjon.mcfly.caves.MossBlocks.Azalea(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	AZALEA_LEAVES(McFlyConfig.enableMossAzalea, new io.github.asgerjon.mcfly.caves.MossBlocks.AzaleaLeaves(), io.github.asgerjon.mcfly.base.McFlyLeaves.LeavesItem.class),
	ROOTED_DIRT(McFlyConfig.enableMossAzalea, new io.github.asgerjon.mcfly.caves.MossBlocks.RootedDirt()),
	HANGING_ROOTS(McFlyConfig.enableMossAzalea, new io.github.asgerjon.mcfly.caves.MossBlocks.HangingRoots()),
	// mangrove, and cherry's leaves and sapling (spec §90.6), EFR's ids: `leaves` and `sapling` each hold both kinds
	MANGROVE_LOG(McFlyConfig.enableMangrove, new io.github.asgerjon.mcfly.base.McFlyLog("mangrove", MapColor.redColor, MapColor.obsidianColor),
		io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	MANGROVE_ROOTS(McFlyConfig.enableMangrove, new io.github.asgerjon.mcfly.caves.MangroveBlocks.Roots()),
	MUDDY_MANGROVE_ROOTS(McFlyConfig.enableMangrove, new io.github.asgerjon.mcfly.caves.MangroveBlocks.MuddyRoots()),
	LEAVES(McFlyConfig.enableMangrove || McFlyConfig.enableCherryTrees, new io.github.asgerjon.mcfly.caves.MangroveBlocks.Leaves(),
		io.github.asgerjon.mcfly.base.McFlyLeaves.LeavesItem.class),
	SAPLING(McFlyConfig.enableMangrove || McFlyConfig.enableCherryTrees, new io.github.asgerjon.mcfly.caves.MangroveBlocks.Sapling(),
		io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	// the wood sets' logs (spec §91.3, step A), EFR's ids: the vanilla woods' stripped logs, woods and stripped woods, cherry's, bamboo's
	LOG_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs, io.github.asgerjon.mcfly.wood.WoodLogs.strippedLogs(true), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	LOG2_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs, io.github.asgerjon.mcfly.wood.WoodLogs.strippedLogs(false), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BARK(io.github.asgerjon.mcfly.config.McFlyConfig.enableBarkLogs, io.github.asgerjon.mcfly.wood.WoodLogs.woods(true), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BARK2(io.github.asgerjon.mcfly.config.McFlyConfig.enableBarkLogs, io.github.asgerjon.mcfly.wood.WoodLogs.woods(false), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WOOD_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && io.github.asgerjon.mcfly.config.McFlyConfig.enableBarkLogs, io.github.asgerjon.mcfly.wood.WoodLogs.strippedWoods(true), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WOOD2_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && io.github.asgerjon.mcfly.config.McFlyConfig.enableBarkLogs, io.github.asgerjon.mcfly.wood.WoodLogs.strippedWoods(false), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	CHERRY_LOG(io.github.asgerjon.mcfly.config.McFlyConfig.enableCherryBlocks, io.github.asgerjon.mcfly.wood.WoodLogs.cherryLog(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BAMBOO_BLOCK(io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks, io.github.asgerjon.mcfly.wood.WoodLogs.bambooBlock(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	// Biomes O' Plenty's and Witchery's, as EFR had them (its ids, gated on the mod)
	BOP_LOG_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_log", false, true, "sacredoak", "cherry", "dark", "fir"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_LOG_STRIPPED2(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_log2", false, true, "ethereal", "magic", "mangrove", "palm"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_LOG_STRIPPED3(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_log3", false, true, "redwood", "willow", "dead", "bigflowerstem"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_LOG_STRIPPED4(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_log4", false, true, "pine", "hellbark", "jacaranda", "mahogany"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_wood", true, false, "sacredoak", "cherry", "dark", "fir"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD2(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_wood2", true, false, "ethereal", "magic", "mangrove", "palm"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD3(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_wood3", true, false, "redwood", "willow", "dead", "bigflowerstem"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD4(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_wood4", true, false, "pine", "hellbark", "jacaranda", "mahogany"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_wood", true, true, "sacredoak", "cherry", "dark", "fir"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD_STRIPPED2(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_wood2", true, true, "ethereal", "magic", "mangrove", "palm"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD_STRIPPED3(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_wood3", true, true, "redwood", "willow", "dead", "bigflowerstem"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD_STRIPPED4(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("bop", "bop_stripped_wood4", true, true, "pine", "hellbark", "jacaranda", "mahogany"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WITCHERY_LOG_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("witchery", "witchery_stripped_log", false, true, "rowan", "alder", "hawthorn"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WITCHERY_WOOD(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("witchery", "witchery_wood", true, false, "rowan", "alder", "hawthorn"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WITCHERY_WOOD_STRIPPED(io.github.asgerjon.mcfly.config.McFlyConfig.enableStrippedLogs && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodLogs.compat("witchery", "witchery_stripped_wood", true, true, "rowan", "alder", "hawthorn"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	// the wood sets' planks, slabs, stairs and the mosaic (spec §91.4, step B), EFR's ids: the planks and slabs hold five woods
	WOOD_PLANKS(io.github.asgerjon.mcfly.wood.WoodPlanks.anyEnabled(), io.github.asgerjon.mcfly.wood.WoodPlanks.planks(), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WOOD_SLAB(io.github.asgerjon.mcfly.wood.WoodPlanks.anyEnabled(), io.github.asgerjon.mcfly.wood.WoodPlanks.slab(WOOD_PLANKS.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_WOOD_SLAB(io.github.asgerjon.mcfly.wood.WoodPlanks.anyEnabled(), io.github.asgerjon.mcfly.wood.WoodPlanks.doubled(WOOD_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	CRIMSON_STAIRS(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0), io.github.asgerjon.mcfly.wood.WoodPlanks.stairs(WOOD_PLANKS.get(), 0)),
	WARPED_STAIRS(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1), io.github.asgerjon.mcfly.wood.WoodPlanks.stairs(WOOD_PLANKS.get(), 1)),
	MANGROVE_STAIRS(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2), io.github.asgerjon.mcfly.wood.WoodPlanks.stairs(WOOD_PLANKS.get(), 2)),
	CHERRY_STAIRS(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3), io.github.asgerjon.mcfly.wood.WoodPlanks.stairs(WOOD_PLANKS.get(), 3)),
	BAMBOO_STAIRS(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4), io.github.asgerjon.mcfly.wood.WoodPlanks.stairs(WOOD_PLANKS.get(), 4)),
	BAMBOO_MOSAIC(io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks, io.github.asgerjon.mcfly.wood.WoodPlanks.mosaic()),
	BAMBOO_MOSAIC_SLAB(io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks, io.github.asgerjon.mcfly.wood.WoodPlanks.mosaicSlab(BAMBOO_MOSAIC.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	DOUBLE_BAMBOO_MOSAIC_SLAB(io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks, io.github.asgerjon.mcfly.wood.WoodPlanks.doubled(BAMBOO_MOSAIC_SLAB.get()), io.github.asgerjon.mcfly.base.McFlySlabItem.class),
	BAMBOO_MOSAIC_STAIRS(io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks, io.github.asgerjon.mcfly.wood.WoodPlanks.mosaicStairs(BAMBOO_MOSAIC.get())),
	// the wood sets' fences and gates (spec §91.5, step C), EFR's ids
	FENCE_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences, io.github.asgerjon.mcfly.wood.WoodFences.oldFence(0), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	FENCE_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences, io.github.asgerjon.mcfly.wood.WoodFences.oldFence(1), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	FENCE_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences, io.github.asgerjon.mcfly.wood.WoodFences.oldFence(2), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	FENCE_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences, io.github.asgerjon.mcfly.wood.WoodFences.oldFence(3), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	FENCE_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences, io.github.asgerjon.mcfly.wood.WoodFences.oldFence(4), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WOOD_FENCE(io.github.asgerjon.mcfly.wood.WoodPlanks.anyEnabled() && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewFences, io.github.asgerjon.mcfly.wood.WoodFences.modernFence(WOOD_PLANKS.get()), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	BOP_WOOD_FENCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatFence("bop"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	WITCHERY_WOOD_FENCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodFences.compatFence("witchery"), io.github.asgerjon.mcfly.base.McFlyMetaItem.class),
	FENCE_GATE_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates, io.github.asgerjon.mcfly.wood.WoodFences.oldGate(0)),
	FENCE_GATE_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates, io.github.asgerjon.mcfly.wood.WoodFences.oldGate(1)),
	FENCE_GATE_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates, io.github.asgerjon.mcfly.wood.WoodFences.oldGate(2)),
	FENCE_GATE_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates, io.github.asgerjon.mcfly.wood.WoodFences.oldGate(3)),
	FENCE_GATE_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates, io.github.asgerjon.mcfly.wood.WoodFences.oldGate(4)),
	CRIMSON_FENCE_GATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewGates, io.github.asgerjon.mcfly.wood.WoodFences.modernGate(WOOD_PLANKS.get(), 0)),
	WARPED_FENCE_GATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewGates, io.github.asgerjon.mcfly.wood.WoodFences.modernGate(WOOD_PLANKS.get(), 1)),
	MANGROVE_FENCE_GATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewGates, io.github.asgerjon.mcfly.wood.WoodFences.modernGate(WOOD_PLANKS.get(), 2)),
	CHERRY_FENCE_GATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewGates, io.github.asgerjon.mcfly.wood.WoodFences.modernGate(WOOD_PLANKS.get(), 3)),
	BAMBOO_FENCE_GATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewGates, io.github.asgerjon.mcfly.wood.WoodFences.modernGate(WOOD_PLANKS.get(), 4)),
	BOP_SACREDOAK_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 0)),
	BOP_CHERRY_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 1)),
	BOP_DARK_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 2)),
	BOP_FIR_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 3)),
	BOP_ETHEREAL_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 4)),
	BOP_MAGIC_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 5)),
	BOP_MANGROVE_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 6)),
	BOP_PALM_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 7)),
	BOP_REDWOOD_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 8)),
	BOP_WILLOW_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 9)),
	BOP_BAMBOO_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 10)),
	BOP_PINE_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 11)),
	BOP_HELLBARK_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 12)),
	BOP_JACARANDA_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 13)),
	BOP_MAHOGANY_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("bop", 14)),
	WITCHERY_ROWAN_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("witchery", 0)),
	WITCHERY_ALDER_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("witchery", 1)),
	WITCHERY_HAWTHORN_FENCE_GATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaGates && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodFences.compatGate("witchery", 2)),
	// the wood sets' doors and trapdoors (spec §91.6, step D), EFR's ids; EFR's iron trapdoor
	DOOR_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldDoor(0), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	DOOR_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldDoor(1), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	DOOR_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldDoor(2), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	DOOR_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldDoor(3), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	DOOR_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldDoor(4), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	CRIMSON_DOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewDoors, io.github.asgerjon.mcfly.wood.WoodDoors.newDoor(0), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	WARPED_DOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewDoors, io.github.asgerjon.mcfly.wood.WoodDoors.newDoor(1), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	MANGROVE_DOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewDoors, io.github.asgerjon.mcfly.wood.WoodDoors.newDoor(2), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	CHERRY_DOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewDoors, io.github.asgerjon.mcfly.wood.WoodDoors.newDoor(3), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BAMBOO_DOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewDoors, io.github.asgerjon.mcfly.wood.WoodDoors.newDoor(4), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_SACREDOAK_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "sacredoak"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_CHERRY_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "cherry"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_DARK_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "dark"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_FIR_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "fir"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_ETHEREAL_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "ethereal"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_MAGIC_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "magic"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_MANGROVE_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "mangrove"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_PALM_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "palm"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_REDWOOD_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "redwood"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_WILLOW_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "willow"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_BAMBOO_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "bamboo"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_PINE_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "pine"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_HELLBARK_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "hellbark"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_JACARANDA_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "jacaranda"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	BOP_MAHOGANY_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("bop", "mahogany"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	WITCHERY_ROWAN_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("witchery", "rowan"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	WITCHERY_ALDER_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("witchery", "alder"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	WITCHERY_HAWTHORN_DOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaDoors && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodDoors.compatDoor("witchery", "hawthorn"), io.github.asgerjon.mcfly.base.McFlyDoor.DoorItem.class),
	TRAPDOOR_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldTrapdoor(0)),
	TRAPDOOR_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldTrapdoor(1)),
	TRAPDOOR_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldTrapdoor(2)),
	TRAPDOOR_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldTrapdoor(3)),
	TRAPDOOR_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.oldTrapdoor(4)),
	CRIMSON_TRAPDOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.newTrapdoor(0)),
	WARPED_TRAPDOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.newTrapdoor(1)),
	MANGROVE_TRAPDOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.newTrapdoor(2)),
	CHERRY_TRAPDOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.newTrapdoor(3)),
	BAMBOO_TRAPDOOR(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewTrapdoors, io.github.asgerjon.mcfly.wood.WoodDoors.newTrapdoor(4)),
	BOP_SACREDOAK_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "sacredoak")),
	BOP_CHERRY_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "cherry")),
	BOP_DARK_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "dark")),
	BOP_FIR_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "fir")),
	BOP_ETHEREAL_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "ethereal")),
	BOP_MAGIC_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "magic")),
	BOP_MANGROVE_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "mangrove")),
	BOP_PALM_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "palm")),
	BOP_REDWOOD_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "redwood")),
	BOP_WILLOW_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "willow")),
	BOP_BAMBOO_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "bamboo")),
	BOP_PINE_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "pine")),
	BOP_HELLBARK_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "hellbark")),
	BOP_JACARANDA_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "jacaranda")),
	BOP_MAHOGANY_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("bop", "mahogany")),
	WITCHERY_ROWAN_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("witchery", "rowan")),
	WITCHERY_ALDER_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("witchery", "alder")),
	WITCHERY_HAWTHORN_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaTrapdoors && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodDoors.compatTrapdoor("witchery", "hawthorn")),
	IRON_TRAPDOOR(io.github.asgerjon.mcfly.config.McFlyConfig.enableIronTrapdoor, io.github.asgerjon.mcfly.wood.WoodDoors.ironTrapdoor()),
	// the wood sets' buttons and pressure plates (spec §91.7, step E), EFR's ids (and its gates: the mods' buttons and Witchery's
	// plates by the vanilla woods' option, Biomes O' Plenty's plates by the new woods')
	BUTTON_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldButton(0)),
	BUTTON_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldButton(1)),
	BUTTON_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldButton(2)),
	BUTTON_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldButton(3)),
	BUTTON_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldButton(4)),
	CRIMSON_BUTTON(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newButton(WOOD_PLANKS.get(), 0)),
	WARPED_BUTTON(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newButton(WOOD_PLANKS.get(), 1)),
	MANGROVE_BUTTON(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newButton(WOOD_PLANKS.get(), 2)),
	CHERRY_BUTTON(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newButton(WOOD_PLANKS.get(), 3)),
	BAMBOO_BUTTON(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newButton(WOOD_PLANKS.get(), 4)),
	BOP_SACREDOAK_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "sacredoak", 0)),
	BOP_CHERRY_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "cherry", 1)),
	BOP_DARK_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "dark", 2)),
	BOP_FIR_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "fir", 3)),
	BOP_ETHEREAL_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "ethereal", 4)),
	BOP_MAGIC_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "magic", 5)),
	BOP_MANGROVE_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "mangrove", 6)),
	BOP_PALM_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "palm", 7)),
	BOP_REDWOOD_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "redwood", 8)),
	BOP_WILLOW_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "willow", 9)),
	BOP_BAMBOO_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "bamboo", 10)),
	BOP_PINE_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "pine", 11)),
	BOP_HELLBARK_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "hellbark", 12)),
	BOP_JACARANDA_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "jacaranda", 13)),
	BOP_MAHOGANY_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("bop", "mahogany", 14)),
	WITCHERY_ROWAN_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("witchery", "rowan", 0)),
	WITCHERY_ALDER_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("witchery", "alder", 1)),
	WITCHERY_HAWTHORN_BUTTON(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatButton("witchery", "hawthorn", 2)),
	PRESSURE_PLATE_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldPlate(0)),
	PRESSURE_PLATE_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldPlate(1)),
	PRESSURE_PLATE_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldPlate(2)),
	PRESSURE_PLATE_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldPlate(3)),
	PRESSURE_PLATE_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.oldPlate(4)),
	CRIMSON_PRESSURE_PLATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newPlate(WOOD_PLANKS.get(), 0)),
	WARPED_PRESSURE_PLATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newPlate(WOOD_PLANKS.get(), 1)),
	MANGROVE_PRESSURE_PLATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newPlate(WOOD_PLANKS.get(), 2)),
	CHERRY_PRESSURE_PLATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newPlate(WOOD_PLANKS.get(), 3)),
	BAMBOO_PRESSURE_PLATE(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone, io.github.asgerjon.mcfly.wood.WoodRedstone.newPlate(WOOD_PLANKS.get(), 4)),
	BOP_SACREDOAK_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "sacredoak", 0)),
	BOP_CHERRY_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "cherry", 1)),
	BOP_DARK_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "dark", 2)),
	BOP_FIR_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "fir", 3)),
	BOP_ETHEREAL_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "ethereal", 4)),
	BOP_MAGIC_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "magic", 5)),
	BOP_MANGROVE_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "mangrove", 6)),
	BOP_PALM_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "palm", 7)),
	BOP_REDWOOD_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "redwood", 8)),
	BOP_WILLOW_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "willow", 9)),
	BOP_BAMBOO_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "bamboo", 10)),
	BOP_PINE_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "pine", 11)),
	BOP_HELLBARK_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "hellbark", 12)),
	BOP_JACARANDA_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "jacaranda", 13)),
	BOP_MAHOGANY_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("bop", "mahogany", 14)),
	WITCHERY_ROWAN_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("witchery", "rowan", 0)),
	WITCHERY_ALDER_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("witchery", "alder", 1)),
	WITCHERY_HAWTHORN_PRESSURE_PLATE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaWoodRedstone && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodRedstone.compatPlate("witchery", "hawthorn", 2)),
	// dripstone (spec §90.7), EFR's ids
	DRIPSTONE_BLOCK(McFlyConfig.enableDripstone, new io.github.asgerjon.mcfly.caves.DripstoneBlocks.Dripstone()),
	POINTED_DRIPSTONE(McFlyConfig.enableDripstone, new io.github.asgerjon.mcfly.caves.DripstoneBlocks.Pointed()),
	// the lush caves' plants (spec §90.8), McFly's new ids
	SPORE_BLOSSOM(McFlyConfig.enableLushCavePlants, new io.github.asgerjon.mcfly.caves.LushPlants.SporeBlossom()),
	BIG_DRIPLEAF(McFlyConfig.enableLushCavePlants, new io.github.asgerjon.mcfly.caves.LushPlants.BigDripleaf()),
	BIG_DRIPLEAF_STEM(McFlyConfig.enableLushCavePlants, new io.github.asgerjon.mcfly.caves.LushPlants.Stem(), false),
	SMALL_DRIPLEAF(McFlyConfig.enableLushCavePlants, new io.github.asgerjon.mcfly.caves.LushPlants.SmallDripleaf()),
	// the sculk sensors (spec §90.9, step H2), McFly's new ids
	SCULK_SENSOR(McFlyConfig.enableSculk, new io.github.asgerjon.mcfly.sculk.SculkSensorBlock(false)),
	CALIBRATED_SCULK_SENSOR(McFlyConfig.enableSculk, new io.github.asgerjon.mcfly.sculk.SculkSensorBlock(true)),
	// sculk and the catalyst (spec §90.9, step H3), under EFR's ids
	SCULK(McFlyConfig.enableSculk, new io.github.asgerjon.mcfly.sculk.SculkBlock()),
	SCULK_CATALYST(McFlyConfig.enableSculk, new io.github.asgerjon.mcfly.sculk.SculkCatalystBlock()),
	// the vein (step H3), a new id; placed face by face as glow lichen
	SCULK_VEIN(McFlyConfig.enableSculk, new io.github.asgerjon.mcfly.sculk.SculkVeinBlock(), io.github.asgerjon.mcfly.caves.GlowLichenItem.class),
	// the wood sets' signs (spec 91.8, step F), EFR's ids: the vanilla woods' items are McFlyItems' item_sign_<wood>,
	// the new woods' and the mods' the standing sign's item block
	SIGN_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(0, true), false),
	WALL_SIGN_SPRUCE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(0, false), false),
	SIGN_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(1, true), false),
	WALL_SIGN_BIRCH(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(1, false), false),
	SIGN_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(2, true), false),
	WALL_SIGN_JUNGLE(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(2, false), false),
	SIGN_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(3, true), false),
	WALL_SIGN_ACACIA(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(3, false), false),
	SIGN_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(4, true), false),
	WALL_SIGN_DARK_OAK(io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns, io.github.asgerjon.mcfly.wood.WoodSigns.oldSign(4, false), false),
	CRIMSON_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(0, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	CRIMSON_WALL_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(0, false), false),
	WARPED_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(1, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	WARPED_WALL_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(1, false), false),
	MANGROVE_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(2, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	MANGROVE_WALL_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(2, false), false),
	CHERRY_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(3, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	CHERRY_WALL_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(3, false), false),
	BAMBOO_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(4, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BAMBOO_WALL_SIGN(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4) && io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns, io.github.asgerjon.mcfly.wood.WoodSigns.newSign(4, false), false),
	BOP_SACREDOAK_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "sacredoak", 0, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_SACREDOAK_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "sacredoak", 0, false), false),
	BOP_CHERRY_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "cherry", 1, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_CHERRY_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "cherry", 1, false), false),
	BOP_DARK_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "dark", 2, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_DARK_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "dark", 2, false), false),
	BOP_FIR_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "fir", 3, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_FIR_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "fir", 3, false), false),
	BOP_ETHEREAL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "ethereal", 4, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_ETHEREAL_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "ethereal", 4, false), false),
	BOP_MAGIC_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "magic", 5, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_MAGIC_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "magic", 5, false), false),
	BOP_MANGROVE_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "mangrove", 6, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_MANGROVE_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "mangrove", 6, false), false),
	BOP_PALM_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "palm", 7, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_PALM_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "palm", 7, false), false),
	BOP_REDWOOD_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "redwood", 8, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_REDWOOD_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "redwood", 8, false), false),
	BOP_WILLOW_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "willow", 9, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_WILLOW_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "willow", 9, false), false),
	BOP_BAMBOO_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "bamboo", 10, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_BAMBOO_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "bamboo", 10, false), false),
	BOP_PINE_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "pine", 11, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_PINE_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "pine", 11, false), false),
	BOP_HELLBARK_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "hellbark", 12, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_HELLBARK_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "hellbark", 12, false), false),
	BOP_JACARANDA_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "jacaranda", 13, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_JACARANDA_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "jacaranda", 13, false), false),
	BOP_MAHOGANY_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "mahogany", 14, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BOP_MAHOGANY_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("BiomesOPlenty"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("bop", "mahogany", 14, false), false),
	WITCHERY_ROWAN_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("witchery", "rowan", 0, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	WITCHERY_ROWAN_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("witchery", "rowan", 0, false), false),
	WITCHERY_ALDER_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("witchery", "alder", 1, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	WITCHERY_ALDER_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("witchery", "alder", 1, false), false),
	WITCHERY_HAWTHORN_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("witchery", "hawthorn", 2, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	WITCHERY_HAWTHORN_WALL_SIGN(io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && cpw.mods.fml.common.Loader.isModLoaded("witchery"), io.github.asgerjon.mcfly.wood.WoodSigns.compatSign("witchery", "hawthorn", 2, false), false),
	// the hanging signs (spec 91.9, step G), new ids: the ceiling sign holds the item
	OAK_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(0), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(0, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	OAK_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(0), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(0, false), false),
	SPRUCE_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(1), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(1, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	SPRUCE_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(1), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(1, false), false),
	BIRCH_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(2), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(2, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BIRCH_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(2), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(2, false), false),
	JUNGLE_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(3), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(3, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	JUNGLE_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(3), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(3, false), false),
	ACACIA_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(4), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(4, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	ACACIA_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(4), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(4, false), false),
	DARK_OAK_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(5), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(5, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	DARK_OAK_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(5), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(5, false), false),
	CRIMSON_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(6), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(6, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	CRIMSON_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(6), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(6, false), false),
	WARPED_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(7), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(7, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	WARPED_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(7), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(7, false), false),
	MANGROVE_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(8), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(8, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	MANGROVE_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(8), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(8, false), false),
	CHERRY_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(9), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(9, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	CHERRY_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(9), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(9, false), false),
	BAMBOO_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(10), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(10, true), io.github.asgerjon.mcfly.wood.SignItem.OfBlock.class),
	BAMBOO_WALL_HANGING_SIGN(io.github.asgerjon.mcfly.wood.WoodSigns.hangingEnabled(10), io.github.asgerjon.mcfly.wood.WoodSigns.hangingSign(10, false), false),
	// bamboo, the plant (spec 91.11, step I), EFR's ids: no item blocks (the item is McFlyItems' bamboo)
	BAMBOO_SAPLING(McFlyConfig.enableBambooBlocks, new io.github.asgerjon.mcfly.wood.Bamboo.Shoot(), false),
	BAMBOO(McFlyConfig.enableBambooBlocks, new io.github.asgerjon.mcfly.wood.Bamboo.Stalk(), false),
	// the shelves (spec 91.10, step H), new ids, with each wood
	OAK_SHELF(true, io.github.asgerjon.mcfly.wood.WoodShelves.shelf(0)),
	SPRUCE_SHELF(true, io.github.asgerjon.mcfly.wood.WoodShelves.shelf(1)),
	BIRCH_SHELF(true, io.github.asgerjon.mcfly.wood.WoodShelves.shelf(2)),
	JUNGLE_SHELF(true, io.github.asgerjon.mcfly.wood.WoodShelves.shelf(3)),
	ACACIA_SHELF(true, io.github.asgerjon.mcfly.wood.WoodShelves.shelf(4)),
	DARK_OAK_SHELF(true, io.github.asgerjon.mcfly.wood.WoodShelves.shelf(5)),
	CRIMSON_SHELF(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(0), io.github.asgerjon.mcfly.wood.WoodShelves.shelf(6)),
	WARPED_SHELF(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(1), io.github.asgerjon.mcfly.wood.WoodShelves.shelf(7)),
	MANGROVE_SHELF(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(2), io.github.asgerjon.mcfly.wood.WoodShelves.shelf(8)),
	CHERRY_SHELF(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(3), io.github.asgerjon.mcfly.wood.WoodShelves.shelf(9)),
	BAMBOO_SHELF(io.github.asgerjon.mcfly.wood.WoodPlanks.enabled(4), io.github.asgerjon.mcfly.wood.WoodShelves.shelf(10)),
	// the shrieker (step H4), a new id
	SCULK_SHRIEKER(McFlyConfig.enableSculk, new io.github.asgerjon.mcfly.sculk.SculkShriekerBlock()),
	// the smithing table (spec §88.4, step C2)
	SMITHING_TABLE(McFlyConfig.enableSmithingTable, new io.github.asgerjon.mcfly.smithing.SmithingTableBlock()),
	// the smoker and blast furnace (spec 92.3), EFR's ids; lit is a block of its own, with no item
	SMOKER(McFlyConfig.enableSmoker, new io.github.asgerjon.mcfly.workstation.CookerBlock(io.github.asgerjon.mcfly.workstation.CookerBlock.Kind.SMOKER, false)),
	LIT_SMOKER(McFlyConfig.enableSmoker, new io.github.asgerjon.mcfly.workstation.CookerBlock(io.github.asgerjon.mcfly.workstation.CookerBlock.Kind.SMOKER, true), false),
	BLAST_FURNACE(McFlyConfig.enableBlastFurnace, new io.github.asgerjon.mcfly.workstation.CookerBlock(io.github.asgerjon.mcfly.workstation.CookerBlock.Kind.BLAST_FURNACE, false)),
	LIT_BLAST_FURNACE(McFlyConfig.enableBlastFurnace, new io.github.asgerjon.mcfly.workstation.CookerBlock(io.github.asgerjon.mcfly.workstation.CookerBlock.Kind.BLAST_FURNACE, true), false),
	// the barrel (spec 92.4), EFR's id
	BARREL(McFlyConfig.enableBarrel, new io.github.asgerjon.mcfly.workstation.BarrelBlock()),
	// the observer (spec §86), under EFR's id
	OBSERVER(McFlyConfig.enableObservers, new io.github.asgerjon.mcfly.redstone.ObserverBlock()),
	// the end gateway (spec §76.10, §84): its item kept (old saves' registries list it), in no tab
	END_GATEWAY(McFlyConfig.modernEnd || ganymedes01.etfuturum.configuration.configs.ConfigExperiments.endDimensionProvider,
		new io.github.asgerjon.mcfly.end.EndGatewayBlock());

	public static final McFlyBlocks[] VALUES = values();

	/**
	 * Ours with an item but in no creative tab (spec §84): 26.3 has no double slab blocks and no gateway item; their items
	 * are kept only so that old saves' registries still find them.
	 */
	private static final java.util.Set<McFlyBlocks> NOT_IN_A_TAB = java.util.EnumSet.of(BIG_DRIPLEAF_STEM, DOUBLE_PURPUR_SLAB, DOUBLE_END_BRICK_SLAB, END_GATEWAY, DOUBLE_CUT_COPPER_SLAB,
		DOUBLE_BLACKSTONE_SLAB, DOUBLE_RED_NETHERBRICK_SLAB, NETHERITE_STAIRS,
		DOUBLE_DEEPSLATE_SLAB, DOUBLE_DEEPSLATE_BRICK_SLAB, DOUBLE_RED_SANDSTONE_SLAB, DOUBLE_STONE_SLAB, DOUBLE_STONE_SLAB_2, DOUBLE_SMOOTH_SANDSTONE_SLAB,
		DOUBLE_SMOOTH_RED_SANDSTONE_SLAB, DOUBLE_SMOOTH_QUARTZ_SLAB, DOUBLE_TUFF_SLAB,
		CAVE_VINE, CAVE_VINE_PLANT, DOUBLE_MUD_BRICK_SLAB, DOUBLE_WOOD_SLAB, DOUBLE_BAMBOO_MOSAIC_SLAB);

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
		// McFly's signs' tile entity under EFR's id (spec 91.8): always, whatever the sign options
		GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.wood.Signs.Tile.class, io.github.asgerjon.mcfly.McFly.name("sign"));
		// the shelves' (spec 91.10): always
		GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.wood.Shelves.Tile.class, io.github.asgerjon.mcfly.McFly.name("shelf"));
		// the hanging signs' (spec 91.9): always
		GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.wood.HangingSigns.Tile.class, io.github.asgerjon.mcfly.McFly.name("hanging_sign"));
		// the trapdoors' powered state (spec §91.6), vanilla's oak trapdoor's too: always
		GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.wood.Trapdoors.Tile.class, io.github.asgerjon.mcfly.McFly.name("trapdoor"));
		if (BANNER.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.banner.BannerBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("banner"));
		}
		// the smoker's and blast furnace's (spec 92.3), EFR's ids
		if (SMOKER.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.workstation.CookerTile.Smoker.class, io.github.asgerjon.mcfly.McFly.name("smoker"));
		}
		if (BLAST_FURNACE.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.workstation.CookerTile.BlastFurnace.class, io.github.asgerjon.mcfly.McFly.name("blast_furnace"));
		}
		// the barrel's (spec 92.4), EFR's id
		if (BARREL.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.workstation.BarrelTile.class, io.github.asgerjon.mcfly.McFly.name("barrel"));
		}
		if (SHULKER_BOX.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("shulker_box"));
		}
		if (GLOW_LICHEN.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.caves.GlowLichenBlock.Tile.class, io.github.asgerjon.mcfly.McFly.name("glow_lichen"));
		}
		if (CAVE_VINE.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.caves.CaveVines.Tile.class, io.github.asgerjon.mcfly.McFly.name("cave_vines"));
		}
		if (END_GATEWAY.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.end.EndGatewayBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("end_gateway"));
		}
		if (SCULK_SENSOR.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.sculk.SculkSensorBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("sculk_sensor"));
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.sculk.SculkSensorBlockEntity.Calibrated.class,
				io.github.asgerjon.mcfly.McFly.name("calibrated_sculk_sensor"));
			// EFR's id: its empty ones load into ours
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.sculk.SculkCatalystBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("sculk_catalyst"));
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.sculk.SculkVeinBlock.Tile.class, io.github.asgerjon.mcfly.McFly.name("sculk_vein"));
		}
		if (SCULK_SHRIEKER.enabled) {
			GameRegistry.registerTileEntity(io.github.asgerjon.mcfly.sculk.SculkShriekerBlockEntity.class, io.github.asgerjon.mcfly.McFly.name("sculk_shrieker"));
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** Whether it's shown in McFly's creative tab (all but `NOT_IN_A_TAB`). */
	public boolean inTab() {
		return !NOT_IN_A_TAB.contains(this);
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
