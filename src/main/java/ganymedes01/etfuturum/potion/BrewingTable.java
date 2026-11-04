package ganymedes01.etfuturum.potion;

import com.github.bsideup.jabel.Desugar;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Brewing by name, as 26.3's `minecraft:brewing` recipes (SPEC-modern-movement.md §69.3, §69.5): mixes (input potion
 * + reagent -> output potion, made for every container) and container changes (potion + gunpowder -> splash; splash +
 * dragon's breath -> lingering, for every potion some mix uses), expanded as 26.3's `BrewingProvider` does from
 * `VanillaBrewingProvider`'s list, the reagents found by their 26.3 item names. The user's rule (§69.4, decision 1):
 * where the table knows the potion and the reagent it decides alone (no recipe, no brew); 1.7.10's bit rules stay for
 * a potion with no name, and for another mod's ingredient on a 1.7.10 potion. Both brewing stands brew a slot through
 * {@link #brew}.
 */
public final class BrewingTable {

	/** A potion item's container. */
	public enum Container {
		POTION, SPLASH, LINGERING
	}

	@Desugar
	private record Mix(NamedPotion input, ItemStack reagent, NamedPotion output) {
	}

	@Desugar
	private record Change(Container input, ItemStack reagent, Container output) {
	}

	/** 26.3's `VanillaBrewingProvider.buildMixes`, in its order: input reagent output; "start" is also water -> mundane. */
	private static final String[] VANILLA_MIXES = {"water glowstone_dust thick", "water redstone mundane",
		"water nether_wart awkward", "start breeze_rod wind_charged", "start slime_block oozing", "start stone infested",
		"start cobweb weaving", "awkward golden_carrot night_vision", "night_vision redstone long_night_vision",
		"night_vision fermented_spider_eye invisibility", "long_night_vision fermented_spider_eye long_invisibility",
		"invisibility redstone long_invisibility", "start magma_cream fire_resistance",
		"fire_resistance redstone long_fire_resistance", "start rabbit_foot leaping", "leaping redstone long_leaping",
		"leaping glowstone_dust strong_leaping", "leaping fermented_spider_eye slowness",
		"long_leaping fermented_spider_eye long_slowness", "slowness redstone long_slowness",
		"slowness glowstone_dust strong_slowness", "awkward turtle_helmet turtle_master",
		"turtle_master redstone long_turtle_master", "turtle_master glowstone_dust strong_turtle_master",
		"swiftness fermented_spider_eye slowness", "long_swiftness fermented_spider_eye long_slowness",
		"start sugar swiftness", "swiftness redstone long_swiftness", "swiftness glowstone_dust strong_swiftness",
		"awkward pufferfish water_breathing", "water_breathing redstone long_water_breathing",
		"start glistering_melon_slice healing", "healing glowstone_dust strong_healing",
		"healing fermented_spider_eye harming", "strong_healing fermented_spider_eye strong_harming",
		"harming glowstone_dust strong_harming", "poison fermented_spider_eye harming",
		"long_poison fermented_spider_eye harming", "strong_poison fermented_spider_eye strong_harming",
		"start spider_eye poison", "poison redstone long_poison", "poison glowstone_dust strong_poison",
		"start ghast_tear regeneration", "regeneration redstone long_regeneration",
		"regeneration glowstone_dust strong_regeneration", "start blaze_powder strength", "strength redstone long_strength",
		"strength glowstone_dust strong_strength", "water fermented_spider_eye weakness", "weakness redstone long_weakness",
		"awkward phantom_membrane slow_falling", "slow_falling redstone long_slow_falling"};

	private static final List<Mix> MIXES = new ArrayList<>();
	private static final List<Change> CHANGES = new ArrayList<>();
	/** The potions some mix uses: the container changes are made for these (26.3's `BrewingProvider.potions`). */
	private static final Set<NamedPotion> MIXED = new LinkedHashSet<>();
	private static boolean built;

	private BrewingTable() {
	}

	// ---------------------------------------------------------------- the table

	/**
	 * Another mod's mix, by potion ids: made for every container, as 26.3's. Nothing is added when a potion isn't
	 * registered or offered (its effects don't exist).
	 */
	public static synchronized void addMix(String input, ItemStack reagent, String output) {
		build();
		mix(NamedPotions.byId(input), reagent, NamedPotions.byId(output));
	}

	/** 26.3's `buildStartMix`: water + reagent -> mundane, and awkward + reagent -> the potion. */
	public static synchronized void addStartMix(ItemStack reagent, String output) {
		build();
		startMix(reagent, NamedPotions.byId(output));
	}

	private static synchronized void build() {
		if (built) {
			return;
		}
		built = true;
		for (String line : VANILLA_MIXES) {
			String[] parts = line.split(" ");
			ItemStack reagent = item(parts[1]);
			if (parts[0].equals("start")) {
				startMix(reagent, NamedPotions.byId(parts[2]));
			} else {
				mix(NamedPotions.byId(parts[0]), reagent, NamedPotions.byId(parts[2]));
			}
		}
		ItemStack gunpowder = new ItemStack(Items.gunpowder);
		CHANGES.add(new Change(Container.POTION, gunpowder, Container.SPLASH));
		if (lingeringItem() != null && ModItems.DRAGON_BREATH.isEnabled()) {
			CHANGES.add(new Change(Container.SPLASH, ModItems.DRAGON_BREATH.newItemStack(), Container.LINGERING));
		}
	}

	/** A start mix comes whole or not at all: water -> mundane is only there for the potion it starts. */
	private static void startMix(ItemStack reagent, NamedPotion output) {
		if (reagent != null && output != null && output.isAvailable()) {
			mix(NamedPotions.byId("minecraft:water"), reagent, NamedPotions.byId("minecraft:mundane"));
			mix(NamedPotions.byId("minecraft:awkward"), reagent, output);
		}
	}

	/** Left out until it can be made: a potion not registered or not offered, or a reagent EFR doesn't have. */
	private static void mix(NamedPotion input, ItemStack reagent, NamedPotion output) {
		if (input == null || output == null || reagent == null || !input.isAvailable() || !output.isAvailable()) {
			return;
		}
		MIXES.add(new Mix(input, reagent, output));
		MIXED.add(input);
		MIXED.add(output);
	}

	/** A 26.3 item id as 1.7.10's item (EFR's included), or null when there is none. */
	private static ItemStack item(String id) {
		ItemIds.Entry entry = ItemIds.byId("minecraft:" + id);
		return entry == null ? null : entry.createStack(1);
	}

	private static Item lingeringItem() {
		return ModItems.LINGERING_POTION.isEnabled() ? ModItems.LINGERING_POTION.get() : null;
	}

	// ---------------------------------------------------------------- brewing

	/** The container of a potion item, or null for anything else. */
	public static Container containerOf(ItemStack stack) {
		if (stack == null) {
			return null;
		}
		if (stack.getItem() == Items.potionitem) {
			return PotionContents.isSplash(stack) ? Container.SPLASH : Container.POTION;
		}
		Item lingering = lingeringItem();
		return lingering != null && stack.getItem() == lingering ? Container.LINGERING : null;
	}

	/** A new item of this container holding the potion. */
	public static ItemStack create(Container container, NamedPotion potion) {
		Item item = container == Container.LINGERING ? lingeringItem() : Items.potionitem;
		return item == null ? null : PotionContents.create(item, container != Container.POTION, potion);
	}

	/** Whether the reagent slot takes this: a reagent of the table, or one of 1.7.10's ingredients. */
	public static boolean isIngredient(ItemStack stack) {
		return stack != null && (isTableReagent(stack) || stack.getItem().isPotionIngredient(stack));
	}

	public static boolean isTableReagent(ItemStack stack) {
		build();
		if (stack == null) {
			return false;
		}
		for (Mix mix : MIXES) {
			if (matches(mix.reagent(), stack)) {
				return true;
			}
		}
		for (Change change : CHANGES) {
			if (matches(change.reagent(), stack)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * What brewing this slot with this reagent makes, or null when it doesn't change. By the table for a named potion
	 * and a reagent the table has (and for a potion kept by name, whose damage value has no bits to brew); else by
	 * 1.7.10's bits, as EFR's brewing stand always did (dragon's breath on a splash potion making a lingering one).
	 */
	public static ItemStack brew(ItemStack potion, ItemStack reagent) {
		build();
		if (potion == null || reagent == null || !(potion.getItem() instanceof ItemPotion)) {
			return null;
		}
		Container container = containerOf(potion);
		NamedPotion named = container == null ? null : PotionContents.of(potion).potion();
		boolean byName = potion.hasTagCompound() && potion.getTagCompound().hasKey(PotionContents.POTION, 8);
		ItemStack made = named != null && (isTableReagent(reagent) || byName) ? byTable(container, named, reagent) : byBits(potion, reagent);
		if (made != null) {
			made.stackSize = potion.stackSize;
		}
		return made;
	}

	private static ItemStack byTable(Container container, NamedPotion potion, ItemStack reagent) {
		for (Mix mix : MIXES) {
			if (mix.input() == potion && matches(mix.reagent(), reagent)) {
				return create(container, mix.output());
			}
		}
		if (MIXED.contains(potion)) {
			for (Change change : CHANGES) {
				if (change.input() == container && matches(change.reagent(), reagent)) {
					return create(change.output(), potion);
				}
			}
		}
		return null;
	}

	/** 1.7.10's brewing stand's rule for one slot (EFR's stand's, with its dragon's breath), unchanged. */
	@SuppressWarnings("rawtypes")
	private static ItemStack byBits(ItemStack potion, ItemStack reagent) {
		if (reagent.getItem() == ModItems.DRAGON_BREATH.get()) {
			Item lingering = lingeringItem();
			if (lingering != null && potion.getItem() == Items.potionitem && ItemPotion.isSplash(potion.getItemDamage())) {
				ItemStack made = new ItemStack(lingering, potion.stackSize, potion.getItemDamage());
				if (potion.hasTagCompound()) {
					made.setTagCompound((net.minecraft.nbt.NBTTagCompound) potion.getTagCompound().copy());
				}
				return made;
			}
			return null;
		}
		if (!reagent.getItem().isPotionIngredient(reagent)) {
			return null;
		}
		int before = potion.getItemDamage();
		int after = PotionHelper.applyIngredient(before, reagent.getItem().getPotionEffect(reagent));
		List effectsBefore = Items.potionitem.getEffects(before);
		List effectsAfter = Items.potionitem.getEffects(after);
		boolean changes = (before <= 0 || effectsBefore != effectsAfter) && (effectsBefore == null || !effectsBefore.equals(effectsAfter) && effectsAfter != null)
			? before != after
			: !ItemPotion.isSplash(before) && ItemPotion.isSplash(after);
		if (!changes) {
			return null;
		}
		ItemStack made = potion.copy();
		made.setItemDamage(after);
		return made;
	}

	private static boolean matches(ItemStack wanted, ItemStack stack) {
		return stack.getItem() == wanted.getItem() && (stack.getItemDamage() == wanted.getItemDamage() || stack.getItem().isDamageable());
	}
}
