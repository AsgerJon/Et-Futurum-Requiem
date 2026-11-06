package io.github.asgerjon.mcfly.sound;

import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.Block;

/**
 * 26.3's sound types for our blocks (SPEC-modern-movement.md §79.4, B9; 26.3's `SoundType`): `block.<name>.break`,
 * `.step` and `.place` from MCLib's assets, volume and pitch 1. Their events are in {@link McFlySounds}.
 */
public class McFlySoundType extends Block.SoundType {

	/** The lightning rod's (26.3's `COPPER`). */
	public static final McFlySoundType COPPER = new McFlySoundType("copper");
	/** Copper's grate and bulb (spec §87). */
	public static final McFlySoundType COPPER_GRATE = new McFlySoundType("copper_grate");
	public static final McFlySoundType COPPER_BULB = new McFlySoundType("copper_bulb");
	public static final McFlySoundType LODESTONE = new McFlySoundType("lodestone");
	public static final McFlySoundType POWDER_SNOW = new McFlySoundType("powder_snow");
	/** Kelp's (26.3's `WET_GRASS`). */
	public static final McFlySoundType WET_GRASS = new McFlySoundType("wet_grass");

	/** The Nether's stones (spec §88.3). */
	public static final McFlySoundType GILDED_BLACKSTONE = new McFlySoundType("gilded_blackstone");
	public static final McFlySoundType BASALT = new McFlySoundType("basalt");
	public static final McFlySoundType NETHER_GOLD_ORE = new McFlySoundType("nether_gold_ore");
	public static final McFlySoundType NETHER_BRICKS = new McFlySoundType("nether_bricks");

	/** Netherite's (spec §88.4). */
	public static final McFlySoundType NETHERITE_BLOCK = new McFlySoundType("netherite_block");
	public static final McFlySoundType ANCIENT_DEBRIS = new McFlySoundType("ancient_debris");

	/** The chain's and the lanterns' (spec §88.5). */
	public static final McFlySoundType CHAIN = new McFlySoundType("chain");
	public static final McFlySoundType LANTERN = new McFlySoundType("lantern");

	/** Soul soil's (spec §88.6). */
	public static final McFlySoundType SOUL_SOIL = new McFlySoundType("soul_soil");

	/** The Nether's plants and stems (spec §88.7). */
	public static final McFlySoundType NYLIUM = new McFlySoundType("nylium");
	public static final McFlySoundType FUNGUS = new McFlySoundType("fungus");
	public static final McFlySoundType ROOTS = new McFlySoundType("roots");
	public static final McFlySoundType NETHER_SPROUTS = new McFlySoundType("nether_sprouts");
	public static final McFlySoundType WEEPING_VINES = new McFlySoundType("weeping_vines");
	public static final McFlySoundType STEM = new McFlySoundType("stem");
	public static final McFlySoundType WART_BLOCK = new McFlySoundType("wart_block");
	public static final McFlySoundType SHROOMLIGHT = new McFlySoundType("shroomlight");

	/** Deepslate's (spec §89). */
	public static final McFlySoundType DEEPSLATE = new McFlySoundType("deepslate");
	public static final McFlySoundType POLISHED_DEEPSLATE = new McFlySoundType("polished_deepslate");

	public static final McFlySoundType DEEPSLATE_BRICKS = new McFlySoundType("deepslate_bricks");
	public static final McFlySoundType DEEPSLATE_TILES = new McFlySoundType("deepslate_tiles");

	/** Tuff's three and calcite's (spec §89, step E). */
	public static final McFlySoundType TUFF = new McFlySoundType("tuff");
	public static final McFlySoundType POLISHED_TUFF = new McFlySoundType("polished_tuff");
	public static final McFlySoundType TUFF_BRICKS = new McFlySoundType("tuff_bricks");
	public static final McFlySoundType CALCITE = new McFlySoundType("calcite");

	/** Amethyst's (spec §90.4): the block's and the cluster's; the buds their own break and place, the cluster's step. */
	public static final McFlySoundType AMETHYST_BLOCK = new McFlySoundType("amethyst_block");
	public static final McFlySoundType AMETHYST_CLUSTER = new McFlySoundType("amethyst_cluster");
	public static final McFlySoundType SMALL_AMETHYST_BUD = bud("small_amethyst_bud");
	public static final McFlySoundType MEDIUM_AMETHYST_BUD = bud("medium_amethyst_bud");
	public static final McFlySoundType LARGE_AMETHYST_BUD = bud("large_amethyst_bud");

	private static McFlySoundType bud(String name) {
		return new McFlySoundType(name) {
			@Override
			public String getStepResourcePath() {
				return McFly.MC_ASSETS + ":block.amethyst_cluster.step";
			}
		};
	}

	/** Mud's, packed mud's, mud bricks' (spec §90, step C). */
	public static final McFlySoundType MUD = new McFlySoundType("mud");
	public static final McFlySoundType PACKED_MUD = new McFlySoundType("packed_mud");
	public static final McFlySoundType MUD_BRICKS = new McFlySoundType("mud_bricks");

	/** Moss's, azalea's, their leaves', rooted dirt's, hanging roots' (spec §90.5). */
	public static final McFlySoundType MOSS = new McFlySoundType("moss");
	public static final McFlySoundType MOSS_CARPET = new McFlySoundType("moss_carpet");
	public static final McFlySoundType AZALEA = new McFlySoundType("azalea");
	public static final McFlySoundType FLOWERING_AZALEA = new McFlySoundType("flowering_azalea");
	public static final McFlySoundType AZALEA_LEAVES = new McFlySoundType("azalea_leaves");
	public static final McFlySoundType ROOTED_DIRT = new McFlySoundType("rooted_dirt");
	/** Mangrove's and cherry's (spec §90.6). */
	public static final McFlySoundType MANGROVE_ROOTS = new McFlySoundType("mangrove_roots");
	public static final McFlySoundType MUDDY_MANGROVE_ROOTS = new McFlySoundType("muddy_mangrove_roots");
	public static final McFlySoundType CHERRY_LEAVES = new McFlySoundType("cherry_leaves");
	public static final McFlySoundType CHERRY_SAPLING = new McFlySoundType("cherry_sapling");
	/** Dripstone's (spec §90.7). */
	public static final McFlySoundType DRIPSTONE_BLOCK = new McFlySoundType("dripstone_block");
	public static final McFlySoundType POINTED_DRIPSTONE = new McFlySoundType("pointed_dripstone");
	/** The lush caves' plants (spec §90.8). */
	public static final McFlySoundType SPORE_BLOSSOM = new McFlySoundType("spore_blossom");
	public static final McFlySoundType BIG_DRIPLEAF = new McFlySoundType("big_dripleaf");
	public static final McFlySoundType SMALL_DRIPLEAF = new McFlySoundType("small_dripleaf");
	public static final McFlySoundType HANGING_ROOTS = new McFlySoundType("hanging_roots");
	/** The sculk sensors' (spec §90.9, step H2). */
	public static final McFlySoundType SCULK_SENSOR = new McFlySoundType("sculk_sensor");
	/** Sculk's and the catalyst's (spec §90.9, step H3). */
	public static final McFlySoundType SCULK = new McFlySoundType("sculk");
	public static final McFlySoundType SCULK_CATALYST = new McFlySoundType("sculk_catalyst");
	public static final McFlySoundType SCULK_VEIN = new McFlySoundType("sculk_vein");
	/** The shrieker's (spec §90.9, step H4). */
	public static final McFlySoundType SCULK_SHRIEKER = new McFlySoundType("sculk_shrieker");

	/** Cherry's and bamboo's wood (spec §91.3, step A). */
	public static final McFlySoundType CHERRY_WOOD = new McFlySoundType("cherry_wood");
	public static final McFlySoundType BAMBOO_WOOD = new McFlySoundType("bamboo_wood");
	/** Crimson's and warped's planks and what's made of them (spec §91.4, step B). */
	public static final McFlySoundType NETHER_WOOD = new McFlySoundType("nether_wood");
	/** Iron's doors and trapdoors (26.3's `SoundType.IRON`, spec §91.6). */
	public static final McFlySoundType IRON = new McFlySoundType("iron");
	/** The hanging signs' (26.3's `WoodType.hangingSignSoundType`, spec 91.9). */
	public static final McFlySoundType HANGING_SIGN = new McFlySoundType("hanging_sign");
	public static final McFlySoundType CHERRY_WOOD_HANGING_SIGN = new McFlySoundType("cherry_wood_hanging_sign");
	public static final McFlySoundType BAMBOO_WOOD_HANGING_SIGN = new McFlySoundType("bamboo_wood_hanging_sign");
	public static final McFlySoundType NETHER_WOOD_HANGING_SIGN = new McFlySoundType("nether_wood_hanging_sign");
	/** The shelves' (26.3's `SoundType.SHELF`, spec 91.10). */
	public static final McFlySoundType SHELF = new McFlySoundType("shelf");
	/** Bamboo's and its shoot's (spec 91.11). */
	public static final McFlySoundType BAMBOO = new McFlySoundType("bamboo");
	/** The shoot steps as the stalk (26.3's `SoundType.BAMBOO_SAPLING`: `BAMBOO_STEP`). */
	public static final McFlySoundType BAMBOO_SAPLING = new McFlySoundType("bamboo_sapling") {
		@Override
		public String getStepResourcePath() {
			return McFly.MC_ASSETS + ":block.bamboo.step";
		}
	};

	/** Cave vines' (spec §90.3). */
	public static final McFlySoundType CAVE_VINES = new McFlySoundType("cave_vines");

	/** 26.3's GLOW_LICHEN (spec §90.3): grass's break, place, hit and fall (1.7.10's grass), the vine's step. */
	public static final net.minecraft.block.Block.SoundType GLOW_LICHEN = new net.minecraft.block.Block.SoundType("grass", 1.0F, 1.0F) {
		@Override
		public String getStepResourcePath() {
			return McFly.MC_ASSETS + ":block.vine.step";
		}
	};

	public static final McFlySoundType[] VALUES = {COPPER, COPPER_GRATE, COPPER_BULB, LODESTONE, POWDER_SNOW, WET_GRASS, GILDED_BLACKSTONE, BASALT,
		NETHER_GOLD_ORE, NETHER_BRICKS, NETHERITE_BLOCK, ANCIENT_DEBRIS, CHAIN, LANTERN, SOUL_SOIL, NYLIUM, FUNGUS, ROOTS, NETHER_SPROUTS, WEEPING_VINES, STEM, WART_BLOCK, SHROOMLIGHT, DEEPSLATE, POLISHED_DEEPSLATE, DEEPSLATE_BRICKS, DEEPSLATE_TILES,
		TUFF, POLISHED_TUFF, TUFF_BRICKS, CALCITE, CAVE_VINES,
		AMETHYST_BLOCK, AMETHYST_CLUSTER, MUD, PACKED_MUD, MUD_BRICKS,
		MOSS, MOSS_CARPET, AZALEA, FLOWERING_AZALEA, AZALEA_LEAVES, ROOTED_DIRT, HANGING_ROOTS,
		MANGROVE_ROOTS, MUDDY_MANGROVE_ROOTS, CHERRY_LEAVES, CHERRY_SAPLING, DRIPSTONE_BLOCK, POINTED_DRIPSTONE,
		SPORE_BLOSSOM, BIG_DRIPLEAF, SMALL_DRIPLEAF, SCULK_SENSOR, SCULK, SCULK_CATALYST, SCULK_VEIN, SCULK_SHRIEKER, CHERRY_WOOD, BAMBOO_WOOD, NETHER_WOOD, IRON,
		HANGING_SIGN, CHERRY_WOOD_HANGING_SIGN, BAMBOO_WOOD_HANGING_SIGN, NETHER_WOOD_HANGING_SIGN, SHELF, BAMBOO, BAMBOO_SAPLING};

	public McFlySoundType(String name) {
		super(name, 1.0F, 1.0F);
	}

	@Override
	public String getBreakSound() {
		return McFly.MC_ASSETS + ":block." + soundName + ".break";
	}

	@Override
	public String getStepResourcePath() {
		return McFly.MC_ASSETS + ":block." + soundName + ".step";
	}

	/** The place sound (MCP's `getPlaceSound`). */
	@Override
	public String func_150496_b() {
		return McFly.MC_ASSETS + ":block." + soundName + ".place";
	}
}
