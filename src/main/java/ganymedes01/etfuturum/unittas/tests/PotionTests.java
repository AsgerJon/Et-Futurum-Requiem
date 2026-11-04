package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Potions by name, stage A (SPEC-modern-movement.md §69.3, §69.6): 26.3's potions in a registry by name, on the item
 * as 1.7.10 keeps it (a potion 1.7.10 has by its damage value; one it lacks by an NBT `Potion` name, its effects also
 * in `CustomPotionEffects`), read back by one reader; 26.3's effects, names, colours and creative list; the commands.
 */
public class PotionTests extends TasTestCase {

	/** 26.3's `Potions`, in its order: the name, then each effect as effect/duration/amplifier. */
	static final String[] POTIONS_26_3 = {"water", "mundane", "thick", "awkward",
		"night_vision night_vision/3600/0", "long_night_vision night_vision/9600/0",
		"invisibility invisibility/3600/0", "long_invisibility invisibility/9600/0",
		"leaping jump_boost/3600/0", "long_leaping jump_boost/9600/0", "strong_leaping jump_boost/1800/1",
		"fire_resistance fire_resistance/3600/0", "long_fire_resistance fire_resistance/9600/0",
		"swiftness speed/3600/0", "long_swiftness speed/9600/0", "strong_swiftness speed/1800/1",
		"slowness slowness/1800/0", "long_slowness slowness/4800/0", "strong_slowness slowness/400/3",
		"turtle_master slowness/400/3 resistance/400/2", "long_turtle_master slowness/800/3 resistance/800/2",
		"strong_turtle_master slowness/400/5 resistance/400/3",
		"water_breathing water_breathing/3600/0", "long_water_breathing water_breathing/9600/0",
		"healing instant_health/1/0", "strong_healing instant_health/1/1",
		"harming instant_damage/1/0", "strong_harming instant_damage/1/1",
		"poison poison/900/0", "long_poison poison/1800/0", "strong_poison poison/432/1",
		"regeneration regeneration/900/0", "long_regeneration regeneration/1800/0", "strong_regeneration regeneration/450/1",
		"strength strength/3600/0", "long_strength strength/9600/0", "strong_strength strength/1800/1",
		"weakness weakness/1800/0", "long_weakness weakness/4800/0",
		"luck luck/6000/0", "slow_falling slow_falling/1800/0", "long_slow_falling slow_falling/4800/0",
		"wind_charged wind_charged/3600/0", "weaving weaving/3600/0", "oozing oozing/3600/0", "infested infested/3600/0"};

	/** Those whose effects EFR doesn't have (registered, not offered): none since §69.11 (luck and the 1.21 four). */
	static final List<String> NOT_OFFERED = Arrays.asList();

	/** The 1.7.10 potions among them, by the drinkable one's damage value (as the modern commands had them). */
	static final String[] OLD_DAMAGES = {"water 0", "awkward 16", "thick 32", "mundane 64", "regeneration 8193",
		"long_regeneration 8257", "strong_regeneration 8225", "swiftness 8194", "long_swiftness 8258", "strong_swiftness 8226",
		"fire_resistance 8195", "long_fire_resistance 8259", "poison 8196", "long_poison 8260", "strong_poison 8228",
		"healing 8197", "strong_healing 8229", "night_vision 8198", "long_night_vision 8262", "weakness 8200",
		"long_weakness 8264", "strength 8201", "long_strength 8265", "strong_strength 8233", "slowness 8202",
		"long_slowness 8266", "harming 8204", "strong_harming 8236", "water_breathing 8205", "long_water_breathing 8269",
		"invisibility 8206", "long_invisibility 8270"};

	private static final int DRINKABLE = 8192;
	private static final int SPLASH = 16384;

	// ---------------------------------------------------------------- the registry

	/** Every one of 26.3's potions by its name, with 26.3's effects, in 26.3's order. */
	public void testEvery26_3PotionByName() {
		List<String> expected = new ArrayList<>();
		for (String entry : POTIONS_26_3) {
			expected.add("minecraft:" + entry.split(" ")[0]);
			NamedPotion potion = NamedPotions.byId("minecraft:" + entry.split(" ")[0]);
			assertTrue(potion != null, "no potion " + entry.split(" ")[0]);
			assertEqual(entry, describe(potion), "the potion's effects");
		}
		List<String> registered = new ArrayList<>();
		for (NamedPotion potion : NamedPotions.all()) {
			registered.add(potion.id());
		}
		assertEqual(expected, registered.subList(0, Math.min(registered.size(), expected.size())), "26.3's order");
	}

	/** Offered (creative tab, commands, brewing) only when all its effects exist. */
	public void testOfferedWhenTheirEffectsExist() {
		for (String entry : POTIONS_26_3) {
			String name = entry.split(" ")[0];
			NamedPotion potion = NamedPotions.byId("minecraft:" + name);
			assertTrue(potion != null, "no potion " + name);
			assertEqual(!NOT_OFFERED.contains(name), potion.isAvailable(), name + " offered");
		}
	}

	// ---------------------------------------------------------------- on the item

	/** 1.7.10's own potions read as their names, drinkable and splash, with no NBT added. */
	public void testOldPotionsHaveTheirNames() {
		for (String entry : OLD_DAMAGES) {
			String name = "minecraft:" + entry.split(" ")[0];
			int damage = Integer.parseInt(entry.split(" ")[1]);
			ItemStack drinkable = new ItemStack(Items.potionitem, 1, damage);
			assertEqual(name, idOf(drinkable), "damage " + damage);
			assertTrue(!drinkable.hasTagCompound(), "reading added NBT to damage " + damage);
			if ((damage & DRINKABLE) != 0) {
				ItemStack splash = new ItemStack(Items.potionitem, 1, damage & ~DRINKABLE | SPLASH);
				assertEqual(name, idOf(splash), "splash damage " + splash.getItemDamage());
			}
		}
		assertEqual("minecraft:mundane", idOf(new ItemStack(Items.potionitem, 1, DRINKABLE)), "damage 8192");
		// EFR brews leaping by 1.7.10's bits with its rabbit's foot (jump boost as effect number 11)
		if (ModItems.RABBIT_FOOT.isEnabled()) {
			assertEqual("minecraft:leaping", idOf(new ItemStack(Items.potionitem, 1, 8203)), "damage 8203");
			assertEqual("minecraft:long_leaping", idOf(new ItemStack(Items.potionitem, 1, 8267)), "damage 8267");
			assertEqual("minecraft:strong_leaping", idOf(new ItemStack(Items.potionitem, 1, 8235)), "damage 8235");
		}
	}

	/** A 1.7.10 potion gives 26.3's effects (durations, amplifiers); a splash one the same (26.3's full length, §69.4). */
	public void testOldPotionsGive26_3Effects() {
		for (String entry : OLD_DAMAGES) {
			String name = entry.split(" ")[0];
			int damage = Integer.parseInt(entry.split(" ")[1]);
			String expected = expectedEffects(name);
			assertEqual(expected, effectsOf(new ItemStack(Items.potionitem, 1, damage)), name + "'s effects");
			if ((damage & DRINKABLE) != 0) {
				assertEqual(expected, effectsOf(new ItemStack(Items.potionitem, 1, damage & ~DRINKABLE | SPLASH)), "splash " + name + "'s effects");
			}
		}
	}

	/** A potion 1.7.10 lacks: the drinkable or splash damage with no effect bits, its name, its effects in CustomPotionEffects. */
	public void testNewPotionOnTheItem() {
		for (boolean splash : new boolean[]{false, true}) {
			ItemStack stack = create("minecraft:slow_falling", splash);
			assertEqual(splash ? SPLASH : DRINKABLE, stack.getItemDamage(), "the damage");
			assertTrue(stack.hasTagCompound(), "no NBT");
			assertEqual("minecraft:slow_falling", stack.getTagCompound().getString("Potion"), "the Potion tag");
			NBTTagList custom = stack.getTagCompound().getTagList("CustomPotionEffects", 10);
			assertEqual(1, custom.tagCount(), "CustomPotionEffects");
			NBTTagCompound effect = custom.getCompoundTagAt(0);
			// saved by name (spec §70)
			assertEqual("minecraft:slow_falling", effect.getString("id"), "its effect");
			assertEqual(1800, effect.getInteger("Duration"), "its duration");
			assertEqual("minecraft:slow_falling", idOf(stack), "read back");
			assertEqual("slow_falling slow_falling/1800/0", "slow_falling " + effectsOf(stack), "its effects");
		}
		ItemStack leaping = create("minecraft:strong_leaping", false);
		assertEqual("strong_leaping jump_boost/1800/1", "strong_leaping " + effectsOf(leaping), "leaping's effects");
		ItemStack old = create("minecraft:long_swiftness", false);
		assertEqual(8258, old.getItemDamage(), "a 1.7.10 potion made by name keeps 1.7.10's damage");
		assertTrue(!old.hasTagCompound(), "and no NBT");
	}

	/** What isn't 26.3's stays 1.7.10's: an odd bit potion, and CustomPotionEffects that replace a potion's (1.7.10's rule). */
	@SuppressWarnings("unchecked")
	public void testUnnamedPotionsUnchanged() {
		ItemStack odd = new ItemStack(Items.potionitem, 1, 8289);
		assertEqual(null, PotionContents.of(odd).potion(), "8289's name");
		assertEqual(describe(PotionHelper.getPotionEffects(8289, false)), effectsOf(odd), "8289's effects");
		ItemStack replaced = new ItemStack(Items.potionitem, 1, 8194);
		NBTTagList list = new NBTTagList();
		list.appendTag(new PotionEffect(Potion.poison.id, 100, 0).writeCustomPotionEffectToNBT(new NBTTagCompound()));
		replaced.setTagCompound(new NBTTagCompound());
		replaced.getTagCompound().setTag("CustomPotionEffects", list);
		assertEqual(null, PotionContents.of(replaced).potion(), "a swiftness with other CustomPotionEffects");
		assertEqual("poison/100/0", effectsOf(replaced), "its effects (1.7.10's: the custom ones)");
	}

	// ---------------------------------------------------------------- using it

	/** Drinking: the named potion's effects (a new one, and a 1.7.10 one at 26.3's length). */
	public void testDrinking() {
		survival();
		String[] drunk = onServer(() -> {
			ItemStack slowFalling = create("minecraft:slow_falling", false);
			Items.potionitem.onEaten(slowFalling, world(), player());
			ItemStack longSwiftness = new ItemStack(Items.potionitem, 1, 8258);
			Items.potionitem.onEaten(longSwiftness, world(), player());
			PotionEffect falling = player().getActivePotionEffect(EffectIds.byId("minecraft:slow_falling"));
			PotionEffect speed = player().getActivePotionEffect(Potion.moveSpeed);
			return new String[]{falling == null ? "none" : String.valueOf(falling.getDuration()), speed == null ? "none" : String.valueOf(speed.getDuration())};
		});
		assertEqual("1800", drunk[0], "slow falling after drinking");
		assertEqual("9600", drunk[1], "speed after drinking long swiftness");
		onServer(() -> player().clearActivePotions());
	}

	/** A splash potion that hits a mob straight on gives the whole length (26.3's; 1.7.10's was 3/4, so at most 7200 here). */
	public void testSplashFullLength() {
		EntityZombie zombie = onServer(() -> {
			EntityZombie mob = new EntityZombie(world());
			mob.tasks.taskEntries.clear();
			mob.targetTasks.taskEntries.clear();
			mob.setLocationAndAngles(player().posX + 3.0D, player().posY, player().posZ, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			return mob;
		});
		onServer(() -> {
			EntityPotion potion = new EntityPotion(world(), zombie.posX, zombie.posY + 2.6D, zombie.posZ, new ItemStack(Items.potionitem, 1, 8258 & ~DRINKABLE | SPLASH));
			potion.motionX = potion.motionZ = 0.0D;
			potion.motionY = -0.4D;
			world().spawnEntityInWorld(potion);
		});
		serverTicks(20);
		int duration = onServer(() -> {
			PotionEffect speed = zombie.getActivePotionEffect(Potion.moveSpeed);
			zombie.setDead();
			return speed == null ? 0 : speed.getDuration();
		});
		assertTrue(duration > 7300, "the splash's speed lasted " + duration + " (26.3's: about 9600)");
	}

	// ---------------------------------------------------------------- how it looks

	/** 26.3's names for the potion and the splash potion. */
	public void testNames() {
		assertEqual("Potion of Slow Falling", create("minecraft:slow_falling", false).getDisplayName());
		assertEqual("Splash Potion of Slow Falling", create("minecraft:slow_falling", true).getDisplayName());
		assertEqual("Potion of Leaping", create("minecraft:long_leaping", false).getDisplayName());
		assertEqual("Potion of the Turtle Master", create("minecraft:turtle_master", false).getDisplayName());
		assertEqual("Potion of Slowness", create("minecraft:strong_slowness", false).getDisplayName());
		assertEqual("Water Bottle", new ItemStack(Items.potionitem, 1, 0).getDisplayName());
		assertEqual("Splash Water Bottle", create("minecraft:water", true).getDisplayName());
		assertEqual("Awkward Potion", new ItemStack(Items.potionitem, 1, 16).getDisplayName());
		assertEqual("Mundane Potion", new ItemStack(Items.potionitem, 1, DRINKABLE).getDisplayName());
		assertEqual("Potion of Swiftness", new ItemStack(Items.potionitem, 1, 8258).getDisplayName());
		assertEqual("Splash Potion of Healing", new ItemStack(Items.potionitem, 1, 8229 & ~DRINKABLE | SPLASH).getDisplayName());
	}

	/**
	 * 26.3's colours: its effects' colours mixed by amplifier + 1; water's blue with none (spec §69.4: for every potion;
	 * the effects' own colours are 26.3's through ModernPotionColors, `modernPotionColors`).
	 */
	public void testColours() {
		assertEqual(0xF3CFB9, colourOf(create("minecraft:slow_falling", false)), "slow falling");
		assertEqual(0x33EBFF, colourOf(new ItemStack(Items.potionitem, 1, 8194)), "swiftness");
		assertEqual(0x8D82E6, colourOf(create("minecraft:turtle_master", false)), "turtle master (slowness IV, resistance III)");
		assertEqual(0xF82423, colourOf(new ItemStack(Items.potionitem, 1, 8229)), "strong healing");
		assertEqual(0x385DC6, colourOf(new ItemStack(Items.potionitem, 1, 16)), "awkward");
		assertEqual(0xFDFF84, colourOf(create("minecraft:leaping", true)), "splash leaping");
	}

	/** The creative list: every offered potion in 26.3's order, the drinkable ones then the splash ones. */
	@SuppressWarnings("unchecked")
	public void testCreativeList() {
		List<String> expected = new ArrayList<>();
		for (boolean splash : new boolean[]{false, true}) {
			for (String entry : POTIONS_26_3) {
				String name = entry.split(" ")[0];
				if (!NOT_OFFERED.contains(name)) {
					expected.add((splash ? "splash " : "") + "minecraft:" + name);
				}
			}
		}
		List<String> listed = onClient(() -> {
			List<ItemStack> stacks = new ArrayList<>();
			Items.potionitem.getSubItems(Items.potionitem, CreativeTabs.tabBrewing, stacks);
			List<String> names = new ArrayList<>();
			for (ItemStack stack : stacks) {
				PotionContents contents = PotionContents.of(stack);
				names.add((PotionContents.isSplash(stack) ? "splash " : "") + (contents.potion() == null ? "unnamed " + stack.getItemDamage() : contents.potion().id()));
			}
			return names;
		});
		assertEqual(expected, listed.subList(0, Math.min(listed.size(), expected.size())), "the creative list's start");
	}

	// ---------------------------------------------------------------- commands

	/** /give by name: a new potion by its name, a 1.7.10 one by its damage, a custom colour and name kept. */
	public void testGiveByName() {
		assertNoError(command("/give @s potion[potion_contents={potion:\"minecraft:slow_falling\"}]"));
		assertNoError(command("/give @s splash_potion[potion_contents=\"minecraft:long_swiftness\"]"));
		assertNoError(command("/give @s potion[potion_contents={potion:\"minecraft:water\",custom_color:16711680,custom_name:\"slow_falling\"}]"));
		List<ItemStack> stacks = inventory();
		assertEqual(3, stacks.size(), "potions given");
		assertEqual("minecraft:slow_falling", idOf(stacks.get(0)), "the first");
		assertEqual(8258 & ~DRINKABLE | SPLASH, stacks.get(1).getItemDamage(), "the splash long swiftness's damage");
		assertEqual(0xFF0000, colourOf(stacks.get(2)), "the custom colour");
		assertEqual("Potion of Slow Falling", stacks.get(2).getDisplayName(), "the custom name");
		assertEqual("minecraft:water", idOf(stacks.get(2)), "its potion still water");
	}

	// ---------------------------------------------------------------- helpers

	private void survival() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setHealth(player().getMaxHealth());
		});
	}

	private static String idOf(ItemStack stack) {
		NamedPotion potion = PotionContents.of(stack).potion();
		return potion == null ? null : potion.id();
	}

	private static ItemStack create(String id, boolean splash) {
		NamedPotion potion = NamedPotions.byId(id);
		assertTrue(potion != null, "no potion " + id);
		return PotionContents.create(Items.potionitem, splash, potion);
	}

	private static String describe(NamedPotion potion) {
		StringBuilder text = new StringBuilder(potion.id().substring("minecraft:".length()));
		for (NamedPotion.Effect effect : potion.effects()) {
			text.append(' ').append(effect.effect().substring("minecraft:".length())).append('/').append(effect.duration()).append('/').append(effect.amplifier());
		}
		return text.toString();
	}

	/** The effects as effect/duration/amplifier by modern's effect names, space between. */
	private static String describe(List<PotionEffect> effects) {
		List<String> parts = new ArrayList<>();
		if (effects != null) {
			for (PotionEffect effect : effects) {
				String name = EffectIds.idOf(Potion.potionTypes[effect.getPotionID()]);
				parts.add(name.substring(name.indexOf(':') + 1) + "/" + effect.getDuration() + "/" + effect.getAmplifier());
			}
		}
		return String.join(" ", parts);
	}

	@SuppressWarnings("unchecked")
	private static String effectsOf(ItemStack stack) {
		return describe(Items.potionitem.getEffects(stack));
	}

	private static String expectedEffects(String name) {
		for (String entry : POTIONS_26_3) {
			if (entry.split(" ")[0].equals(name)) {
				int space = entry.indexOf(' ');
				return space < 0 ? "" : entry.substring(space + 1);
			}
		}
		throw new AssertionError("no 26.3 potion " + name);
	}

	private int colourOf(ItemStack stack) {
		return onClient(() -> Items.potionitem.getColorFromItemStack(stack, 0) & 0xFFFFFF);
	}
}
