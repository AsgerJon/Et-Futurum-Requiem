package ganymedes01.etfuturum.potion;

import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A potion item's potion, 26.3's `potion_contents` (SPEC-modern-movement.md §69.3), and the one place that reads and
 * writes it on a 1.7.10 item. A potion 1.7.10 has stays as 1.7.10 makes it: its damage value, no NBT. Otherwise the
 * NBT string `Potion` names it (the format 1.9 to 1.20.4 used), on the drinkable or splash damage with no effect
 * bits, and `CustomPotionEffects` holds its effects then the custom ones, so code that only knows 1.7.10 (drinking,
 * splashing, the tooltip, a witch, other mods) gets them right. `CustomPotionColor` and `CustomPotionName` are 26.3's
 * `custom_color` and `custom_name`. Without a `Potion` tag, `CustomPotionEffects` replace a damage value's effects, as
 * in 1.7.10: such an item has no named potion.
 */
public final class PotionContents {

	public static final int DRINKABLE = 8192;
	public static final int SPLASH = 16384;
	public static final String POTION = "Potion";
	public static final String EFFECTS = "CustomPotionEffects";
	public static final String COLOR = "CustomPotionColor";
	public static final String NAME = "CustomPotionName";
	/** 26.3's `BASE_POTION_COLOR`, a potion with no effects (water's blue, the same in 1.7.10). */
	public static final int BASE_COLOR = 0x385DC6;

	public static final PotionContents EMPTY = new PotionContents(null, Collections.emptyList(), null, null);

	private final NamedPotion potion;
	private final List<PotionEffect> customEffects;
	private final Integer customColor;
	private final String customName;

	private PotionContents(NamedPotion potion, List<PotionEffect> customEffects, Integer customColor, String customName) {
		this.potion = potion;
		this.customEffects = customEffects;
		this.customColor = customColor;
		this.customName = customName;
	}

	/** Whether this item can hold a potion: a potion (drinkable, splash, EFR's lingering) or EFR's tipped arrow. */
	public static boolean holdsPotion(ItemStack stack) {
		return stack != null && (stack.getItem() instanceof ItemPotion || stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem);
	}

	/** What a potion item or tipped arrow holds; {@link #EMPTY} for anything else. */
	public static PotionContents of(ItemStack stack) {
		if (!holdsPotion(stack)) {
			return EMPTY;
		}
		NBTTagCompound tag = stack.getTagCompound();
		boolean arrow = stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem;
		// EFR's own one-effect arrows (a stray's) keep that effect in a `Potion` compound; an arrow of damage 0 holds none
		if (arrow && (tag != null && tag.hasKey(POTION, 10) || stack.getItemDamage() == 0 && (tag == null || !tag.hasKey(POTION, 8)))) {
			return EMPTY;
		}
		NamedPotion potion;
		List<PotionEffect> custom = Collections.emptyList();
		boolean named = tag != null && tag.hasKey(POTION, 8);
		List<PotionEffect> listed = tag != null && tag.hasKey(EFFECTS, 9) ? read(tag.getTagList(EFFECTS, 10)) : null;
		if (named) {
			potion = NamedPotions.byId(tag.getString(POTION));
			if (listed != null) {
				List<PotionEffect> own = potion == null ? Collections.emptyList() : potion.potionEffects();
				custom = startsWith(listed, own) ? listed.subList(own.size(), listed.size()) : listed;
			}
		} else {
			// 1.7.10's rule: custom effects replace the damage value's, so the damage value names nothing then
			potion = listed == null ? NamedPotions.byLegacyDamage(stack.getItemDamage()) : null;
		}
		Integer color = tag != null && tag.hasKey(COLOR, 3) ? tag.getInteger(COLOR) : null;
		String name = tag != null && tag.hasKey(NAME, 8) ? tag.getString(NAME) : null;
		if (potion == null && custom.isEmpty() && color == null && name == null) {
			return EMPTY;
		}
		return new PotionContents(potion, new ArrayList<>(custom), color, name);
	}

	public static boolean isSplash(ItemStack stack) {
		return stack != null && (stack.getItemDamage() & SPLASH) != 0;
	}

	/** A new potion item (drinkable or splash) holding this potion. */
	public static ItemStack create(Item item, boolean splash, NamedPotion potion) {
		ItemStack stack = new ItemStack(item, 1, splash ? SPLASH : DRINKABLE);
		write(stack, splash, potion, Collections.emptyList(), null, null);
		return stack;
	}

	/**
	 * Puts a potion on the item: its damage value (1.7.10's for the potion when it has one in that container, else the
	 * drinkable or splash bit alone), and the NBT the potion needs; any `Potion`, `CustomPotionEffects`,
	 * `CustomPotionColor` and `CustomPotionName` there before go.
	 */
	public static void write(ItemStack stack, boolean splash, NamedPotion potion, List<PotionEffect> custom, Integer color, String name) {
		int legacy = potion == null ? -1 : potion.legacyDamage();
		// 1.7.10 has no splash water, awkward, thick or mundane: those are kept by name too; nor an arrow of water
		boolean representable = legacy >= 0 && (!splash || (legacy & DRINKABLE) != 0)
			&& !(stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem && legacy == 0);
		boolean byDamage = representable && custom.isEmpty();
		stack.setItemDamage(representable ? splash ? legacy & ~DRINKABLE | SPLASH : legacy : splash ? SPLASH : DRINKABLE);
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(POTION);
			tag.removeTag(EFFECTS);
			tag.removeTag(COLOR);
			tag.removeTag(NAME);
		}
		if (!byDamage && potion != null) {
			tag(stack).setString(POTION, potion.id());
		}
		List<PotionEffect> effects = new ArrayList<>();
		if (!byDamage && potion != null) {
			effects.addAll(potion.potionEffects());
		}
		effects.addAll(custom);
		if (!effects.isEmpty()) {
			NBTTagList list = new NBTTagList();
			for (PotionEffect effect : effects) {
				list.appendTag(effect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
			}
			tag(stack).setTag(EFFECTS, list);
		}
		if (color != null) {
			tag(stack).setInteger(COLOR, color);
		}
		if (name != null) {
			tag(stack).setString(NAME, name);
		}
		if (stack.hasTagCompound() && stack.getTagCompound().hasNoTags()) {
			stack.setTagCompound(null);
		}
	}

	/** The named potion, or null (a 1.7.10 bit potion, custom effects only, or not a potion). */
	public NamedPotion potion() {
		return potion;
	}

	public List<PotionEffect> customEffects() {
		return Collections.unmodifiableList(customEffects);
	}

	public Integer customColor() {
		return customColor;
	}

	public String customName() {
		return customName;
	}

	/** Whether this, not 1.7.10's damage value rules, decides the item's effects, name and colour. */
	public boolean isNamed() {
		return potion != null || customName != null || customColor != null;
	}

	/** The potion's effects then the custom ones, new instances. */
	public List<PotionEffect> allEffects() {
		List<PotionEffect> list = potion == null ? new ArrayList<>() : potion.potionEffects();
		for (PotionEffect effect : customEffects) {
			list.add(new PotionEffect(effect));
		}
		return list;
	}

	/**
	 * 26.3's `getColor`: the custom colour, else the effects' colours mixed, each weighted by its amplifier + 1
	 * (the effects' own colours are 26.3's with `modernPotionColors`), else water's blue.
	 */
	public int color() {
		if (customColor != null) {
			return customColor & 0xFFFFFF;
		}
		int red = 0;
		int green = 0;
		int blue = 0;
		int weight = 0;
		for (PotionEffect effect : allEffects()) {
			Potion type = Potion.potionTypes[effect.getPotionID()];
			if (type == null) {
				continue;
			}
			int colour = type.getLiquidColor();
			int amplifier = effect.getAmplifier() + 1;
			red += amplifier * (colour >> 16 & 0xFF);
			green += amplifier * (colour >> 8 & 0xFF);
			blue += amplifier * (colour & 0xFF);
			weight += amplifier;
		}
		return weight == 0 ? BASE_COLOR : (red / weight) << 16 | (green / weight) << 8 | blue / weight;
	}

	/**
	 * 26.3's `potion_duration_scale` for the item: a lingering potion's effects last 1/4 as long, a tipped arrow's
	 * 1/8, a drinkable or splash potion's whole.
	 */
	public static float durationScale(ItemStack stack) {
		if (stack != null && stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem) {
			return 0.125F;
		}
		return stack != null && stack.getItem() instanceof io.github.asgerjon.mcfly.potion.LingeringPotionItem ? 0.25F : 1.0F;
	}

	/** The effects as the item gives them: 26.3's `withScaledDuration` (the floor, at least 1 tick). */
	public List<PotionEffect> scaledEffects(float scale) {
		List<PotionEffect> list = allEffects();
		if (scale == 1.0F) {
			return list;
		}
		List<PotionEffect> scaled = new ArrayList<>();
		for (PotionEffect effect : list) {
			PotionEffect made = new PotionEffect(effect.getPotionID(), Math.max((int) Math.floor(effect.getDuration() * scale), 1),
				effect.getAmplifier(), effect.getIsAmbient());
			made.setCurativeItems(effect.getCurativeItems());
			scaled.add(made);
		}
		return scaled;
	}

	/** 26.3's item name for this kind of item: potion, splash_potion, lingering_potion or tipped_arrow. */
	public static String itemKey(ItemStack stack) {
		if (stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem) {
			return "tipped_arrow";
		}
		if (stack.getItem() instanceof io.github.asgerjon.mcfly.potion.LingeringPotionItem) {
			return "lingering_potion";
		}
		return isSplash(stack) ? "splash_potion" : "potion";
	}

	/** 26.3's lang key for the item's name: `item.minecraft.potion.effect.<name>` (or splash_potion, ...). */
	public String nameKey(String item) {
		String suffix = customName != null ? customName : potion != null ? potion.baseName() : "empty";
		return "item.minecraft." + item + ".effect." + suffix;
	}

	private static NBTTagCompound tag(ItemStack stack) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.getTagCompound();
	}

	private static List<PotionEffect> read(NBTTagList list) {
		List<PotionEffect> effects = new ArrayList<>();
		for (int i = 0; i < list.tagCount(); i++) {
			PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
			if (effect != null) {
				effects.add(effect);
			}
		}
		return effects;
	}

	private static boolean startsWith(List<PotionEffect> list, List<PotionEffect> start) {
		if (list.size() < start.size()) {
			return false;
		}
		for (int i = 0; i < start.size(); i++) {
			PotionEffect a = list.get(i);
			PotionEffect b = start.get(i);
			if (a.getPotionID() != b.getPotionID() || a.getDuration() != b.getDuration() || a.getAmplifier() != b.getAmplifier()) {
				return false;
			}
		}
		return true;
	}
}
