package io.github.asgerjon.mcfly.banner;

import io.github.asgerjon.mcfly.dye.DyeColor;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Banners as 26.3 has them, kept in 1.8's stored form (SPEC-modern-movement.md §66, §80): a banner item's damage is its
 * base colour, its layers in `BlockEntityTag`'s `Patterns` (each a short `Pattern` id and a `Color`, white 0 to black
 * 15); a shield with a banner on it keeps `Base` and `Patterns` there too. The loom's patterns and dyes, a layer added
 * or washed off, the layers' names.
 */
public final class Banners {

	/** A banner takes no layer past 6 (26.3's loom). */
	public static final int MAX_LAYERS = 6;

	public static final String ENTITY_TAG = "BlockEntityTag";

	/** A layer: a pattern in a colour. */
	public static final class Layer {

		private final BannerPattern pattern;
		private final DyeColor colour;

		public Layer(BannerPattern pattern, DyeColor colour) {
			this.pattern = pattern;
			this.colour = colour;
		}

		public BannerPattern pattern() {
			return pattern;
		}

		public DyeColor colour() {
			return colour;
		}
	}

	private Banners() {
	}

	/** A shield pattern's texture by 26.3's name (26.3's shield pattern textures, under our domain). */
	public static String shieldMask(String pattern) {
		return io.github.asgerjon.mcfly.McFly.DOMAIN + ":textures/entity/shield/pattern/" + pattern + ".png";
	}

	/** A stack's `BlockEntityTag` (made if asked for), or null. */
	public static NBTTagCompound entityTag(ItemStack stack, boolean create) {
		if (stack.stackTagCompound != null && stack.stackTagCompound.hasKey(ENTITY_TAG, 10)) {
			return stack.stackTagCompound.getCompoundTag(ENTITY_TAG);
		}
		if (!create) {
			return null;
		}
		NBTTagCompound tag = new NBTTagCompound();
		stack.setTagInfo(ENTITY_TAG, tag);
		return tag;
	}

	public static boolean isBanner(ItemStack stack) {
		return stack != null && McFlyBlocks.BANNER.isEnabled() && stack.getItem() == Item.getItemFromBlock(McFlyBlocks.BANNER.get());
	}

	/** A banner's base colour: its tag's `Base` where one is (as EFR's items kept it), else its damage. */
	public static int base(ItemStack stack) {
		NBTTagCompound tag = entityTag(stack, false);
		return tag != null && tag.hasKey("Base", 99) ? tag.getInteger("Base") & 15 : stack.getItemDamage() & 15;
	}

	/** A shield's base colour (white 0 to black 15), or -1 for a plain shield. */
	public static int shieldBase(ItemStack shield) {
		NBTTagCompound tag = entityTag(shield, false);
		return tag != null && tag.hasKey("Base", 99) ? tag.getInteger("Base") & 15 : -1;
	}

	/** How many layers a banner or shield has. */
	public static int count(ItemStack stack) {
		NBTTagCompound tag = entityTag(stack, false);
		return tag != null && tag.hasKey("Patterns", 9) ? tag.getTagList("Patterns", 10).tagCount() : 0;
	}

	/** The layers stored in a `Patterns` list, in order; one by a short id no pattern has is skipped. */
	public static List<Layer> layers(NBTTagList patterns) {
		if (patterns == null) {
			return Collections.emptyList();
		}
		List<Layer> layers = new ArrayList<>();
		for (int i = 0; i < patterns.tagCount(); i++) {
			NBTTagCompound layer = patterns.getCompoundTagAt(i);
			BannerPattern pattern = BannerPatterns.byShortId(layer.getString("Pattern"));
			if (pattern != null) {
				layers.add(new Layer(pattern, DyeColor.byId(layer.getInteger("Color") & 15)));
			}
		}
		return layers;
	}

	/** A banner's or shield's layers. */
	public static List<Layer> layers(ItemStack stack) {
		NBTTagCompound tag = entityTag(stack, false);
		return tag != null && tag.hasKey("Patterns", 9) ? layers(tag.getTagList("Patterns", 10)) : Collections.<Layer>emptyList();
	}

	/** One of the banner with a layer added on top in that colour. */
	public static ItemStack withLayer(ItemStack banner, BannerPattern pattern, int colour) {
		ItemStack result = banner.copy();
		result.stackSize = 1;
		NBTTagCompound tag = entityTag(result, true);
		NBTTagList layers = tag.hasKey("Patterns", 9) ? tag.getTagList("Patterns", 10) : new NBTTagList();
		NBTTagCompound layer = new NBTTagCompound();
		layer.setString("Pattern", pattern.shortId());
		layer.setInteger("Color", colour);
		layers.appendTag(layer);
		tag.setTag("Patterns", layers);
		return result;
	}

	/** Its top layer off (26.3's washing); with none left, no tag either (a plain banner, as crafted). */
	public static void removeTopLayer(ItemStack stack) {
		NBTTagCompound tag = entityTag(stack, false);
		if (tag == null || !tag.hasKey("Patterns", 9)) {
			return;
		}
		NBTTagList layers = tag.getTagList("Patterns", 10);
		if (layers.tagCount() > 0) {
			layers.removeTag(layers.tagCount() - 1);
		}
		if (layers.tagCount() == 0) {
			stack.getTagCompound().removeTag(ENTITY_TAG);
			if (stack.getTagCompound().hasNoTags()) {
				stack.setTagCompound(null);
			}
		}
	}

	/** A banner's or shield's layers as tooltip lines ("Red Base"), up to 6 (26.3's `BannerPatternLayers` tooltip). */
	public static void addLayerLines(ItemStack stack, List<String> tooltip) {
		List<Layer> layers = layers(stack);
		for (int i = 0; i < layers.size() && i < MAX_LAYERS; i++) {
			Layer layer = layers.get(i);
			tooltip.add(StatCollector.translateToLocal("item.banner." + layer.pattern().name() + "." + layer.colour().getName()));
		}
	}

	/** The colour a dye gives (white 0 to black 15), or -1: any dye, by the ore dictionary's `dye<Colour>` names. */
	public static int dyeColour(ItemStack stack) {
		if (stack == null) {
			return -1;
		}
		for (int oreId : OreDictionary.getOreIDs(stack)) {
			String ore = OreDictionary.getOreName(oreId);
			for (DyeColor colour : DyeColor.values()) {
				if (ore.equals(colour.oreName())) {
					return colour.id();
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
	public static List<BannerPattern> selectable(ItemStack patternItem) {
		if (patternItem == null) {
			return BannerPatterns.noItemRequired();
		}
		if (isPatternItem(patternItem)) {
			BannerPattern pattern = BannerPatterns.byName(((ganymedes01.etfuturum.items.ItemBannerPattern) patternItem.getItem()).pattern());
			return pattern == null ? Collections.<BannerPattern>emptyList() : Collections.singletonList(pattern);
		}
		return Collections.emptyList();
	}
}
