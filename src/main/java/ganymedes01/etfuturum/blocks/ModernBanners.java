package ganymedes01.etfuturum.blocks;

import io.github.asgerjon.mcfly.McFly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.itemblocks.ItemBlockBanner;
import ganymedes01.etfuturum.lib.EnumColor;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Banners as 26.3 has them (SPEC-modern-movement.md §66): the loom's patterns, the dyes it takes, a layer added. EFR
 * keeps a banner as it always has: `BlockEntityTag` with `Base` and `Patterns` (each a short `Pattern` id and a
 * `Color`), colours numbered as wool and 26.3's `DyeColor` number them (white 0 to black 15).
 */
public final class ModernBanners {

	/** A banner takes no layer past 6 (26.3's loom). */
	public static final int MAX_LAYERS = 6;

	/** 26.3's `no_item_required` banner patterns, in its order: the loom's list when no pattern item is in. */
	private static final String[] NO_ITEM_REQUIRED_NAMES = {"square_bottom_left", "square_bottom_right", "square_top_left",
		"square_top_right", "stripe_bottom", "stripe_top", "stripe_left", "stripe_right", "stripe_center", "stripe_middle",
		"stripe_downright", "stripe_downleft", "small_stripes", "cross", "straight_cross", "triangle_bottom", "triangle_top",
		"triangles_bottom", "triangles_top", "diagonal_left", "diagonal_up_right", "diagonal_up_left", "diagonal_right",
		"circle", "rhombus", "half_vertical", "half_horizontal", "half_vertical_right", "half_horizontal_bottom", "border",
		"gradient", "gradient_up"};

	public static final List<EnumBannerPattern> NO_ITEM_REQUIRED;

	/**
	 * 26.3's 43 patterns (the base, the 32, the 10 from pattern items): drawn from 26.3's textures by its rule (spec
	 * §66.11). A pattern another mod adds through EFR's API isn't among them.
	 */
	private static final java.util.Set<String> MODERN_NAMES = new java.util.HashSet<>();

	static {
		MODERN_NAMES.add("base");
		Collections.addAll(MODERN_NAMES, NO_ITEM_REQUIRED_NAMES);
		Collections.addAll(MODERN_NAMES, "bricks", "curly_border", "flower", "creeper", "skull", "mojang", "globe", "piglin",
			"flow", "guster");
	}

	static {
		List<EnumBannerPattern> list = new ArrayList<>();
		for (String name : NO_ITEM_REQUIRED_NAMES) {
			EnumBannerPattern pattern = byName(name);
			if (pattern == null) {
				throw new IllegalStateException("EFR has no banner pattern " + name);
			}
			list.add(pattern);
		}
		NO_ITEM_REQUIRED = Collections.unmodifiableList(list);
	}

	/**
	 * 26.3's `DyeColor.getTextureDiffuseColor`, by colour (white 0 to black 15, EFR's banner colours): what a banner's
	 * layers are tinted by (spec §66.8; EFR mixed in 1.7.10's map colours before).
	 */
	private static final int[] TEXTURE_COLOURS = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA,
		0x474F52, 0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

	private ModernBanners() {
	}

	/** A shield pattern's mask (26.3's shield pattern texture as EFR's masks are: white where it covers), by 26.3's name. */
	public static String shieldMask(String pattern) {
		return McFly.DOMAIN + ":textures/entity/shield/pattern/" + pattern + ".png";
	}

	/** One of 26.3's patterns (not one another mod added through EFR's API). */
	public static boolean isModern(EnumBannerPattern pattern) {
		return MODERN_NAMES.contains(pattern.getPatternName());
	}

	/**
	 * A pattern's banner texture: 26.3's (`etfuturum:textures/entity/banner/`, drawn by 26.3's rule); for one another
	 * mod added through EFR's API, its 1.8 mask where such a mod puts it (drawn by 1.8's rule, as before).
	 */
	public static String bannerTexture(EnumBannerPattern pattern) {
		return isModern(pattern) ? McFly.DOMAIN + ":textures/entity/banner/" + pattern.getPatternName() + ".png"
			: "textures/entity/banner/" + pattern.getPatternName() + ".png";
	}

	/** A banner's or shield's layers as tooltip lines ("Red Base"), up to 6 (26.3's BannerPatternLayers tooltip). */
	public static void addLayerLines(ItemStack stack, List<String> tooltip) {
		NBTTagCompound entity = ItemBlockBanner.getSubTag(stack, "BlockEntityTag", false);
		if (entity == null || !entity.hasKey("Patterns", 9)) {
			return;
		}
		NBTTagList layers = entity.getTagList("Patterns", 10);
		for (int i = 0; i < layers.tagCount() && i < MAX_LAYERS; i++) {
			NBTTagCompound layer = layers.getCompoundTagAt(i);
			EnumColor colour = EnumColor.fromDamage(layer.getInteger("Color"));
			EnumBannerPattern pattern = EnumBannerPattern.getPatternByID(layer.getString("Pattern"));
			if (pattern != null) {
				tooltip.add(net.minecraft.util.StatCollector.translateToLocal("item.banner." + pattern.getPatternName() + "."
					+ ganymedes01.etfuturum.recipes.ModRecipes.dye_names[colour.getDamage()]));
			}
		}
	}

	/** A shield's base colour (white 0 to black 15), or -1 for a plain shield. */
	public static int shieldBase(ItemStack shield) {
		NBTTagCompound entity = ItemBlockBanner.getSubTag(shield, "BlockEntityTag", false);
		return entity != null && entity.hasKey("Base", 99) ? entity.getInteger("Base") & 15 : -1;
	}

	/** A banner colour's tint (26.3's). */
	public static int textureColour(EnumColor colour) {
		return TEXTURE_COLOURS[colour.getDamage() & 15];
	}

	/** A pattern by 26.3's name ("stripe_bottom"), or null. */
	public static EnumBannerPattern byName(String name) {
		for (EnumBannerPattern pattern : EnumBannerPattern.VALUES) {
			if (pattern.getPatternName().equals(name)) {
				return pattern;
			}
		}
		return null;
	}

	public static boolean isBanner(ItemStack stack) {
		return stack != null && ModBlocks.BANNER.isEnabled() && stack.getItem() == Item.getItemFromBlock(ModBlocks.BANNER.get());
	}

	/** The colour a dye gives (white 0 to black 15), or -1: any dye, by the ore dictionary's `dye<Colour>` names. */
	public static int dyeColour(ItemStack stack) {
		if (stack == null) {
			return -1;
		}
		for (int oreId : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
			String ore = net.minecraftforge.oredict.OreDictionary.getOreName(oreId);
			for (EnumColor colour : EnumColor.VALUES) {
				if (ore.equals(colour.getOreName())) {
					return colour.getDamage();
				}
			}
		}
		return -1;
	}

	public static boolean isDye(ItemStack stack) {
		return dyeColour(stack) >= 0;
	}

	/** A banner pattern item (spec §66.6). */
	public static boolean isPatternItem(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ganymedes01.etfuturum.items.ItemBannerPattern;
	}

	/** The patterns the loom offers with this in its pattern slot: the 32 when it's empty, a pattern item's own. */
	public static List<EnumBannerPattern> selectable(ItemStack patternItem) {
		if (patternItem == null) {
			return NO_ITEM_REQUIRED;
		}
		if (isPatternItem(patternItem)) {
			EnumBannerPattern pattern = byName(((ganymedes01.etfuturum.items.ItemBannerPattern) patternItem.getItem()).pattern());
			return pattern == null ? Collections.emptyList() : Collections.singletonList(pattern);
		}
		return Collections.emptyList();
	}

	public static int layers(ItemStack banner) {
		return TileEntityBanner.getPatterns(banner);
	}

	/** One of the banner with a layer added on top in that colour. */
	public static ItemStack withLayer(ItemStack banner, EnumBannerPattern pattern, int colour) {
		ItemStack result = banner.copy();
		result.stackSize = 1;
		NBTTagCompound entity = ItemBlockBanner.getSubTag(result, "BlockEntityTag", true);
		NBTTagList layers = entity.hasKey("Patterns", 9) ? entity.getTagList("Patterns", 10) : new NBTTagList();
		NBTTagCompound layer = new NBTTagCompound();
		layer.setString("Pattern", pattern.getPatternID());
		layer.setInteger("Color", colour);
		layers.appendTag(layer);
		entity.setTag("Patterns", layers);
		return result;
	}
}
