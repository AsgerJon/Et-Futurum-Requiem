package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/** The item components of spec §50, given with /give and read back from the stack. */
public class ComponentTests extends TasTestCase {

	private ItemStack give(String item) {
		assertNoError(command("/give @s " + item));
		List<ItemStack> stacks = inventory();
		assertTrue(!stacks.isEmpty(), "/give @s " + item + " gave nothing");
		return stacks.get(0);
	}

	/** "speed 2" for Speed III: each effect the potion gives, by 1.7.10's effect name and amplifier. */
	@SuppressWarnings("unchecked")
	private static List<String> effects(ItemStack stack) {
		List<PotionEffect> effects = stack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem arrow ? arrow.getEffects(stack)
			: ((ItemPotion) stack.getItem()).getEffects(stack);
		List<String> named = new ArrayList<>();
		if (effects != null) {
			for (PotionEffect effect : effects) {
				named.add(Potion.potionTypes[effect.getPotionID()].getName() + " " + effect.getAmplifier());
			}
		}
		return named;
	}

	public void testPotions() {
		ItemStack swiftness = give("potion[potion_contents=\"swiftness\"]");
		assertEqual("Potion of Swiftness", swiftness.getDisplayName());
		assertIn("potion.moveSpeed 0", effects(swiftness));
		onServer(() -> player().inventory.clearInventory(null, -1));
		assertIn("potion.heal 1", effects(give("potion[potion_contents={potion:\"strong_healing\"}]")));
	}

	public void testSplashPotion() {
		ItemStack splash = give("splash_potion[potion_contents=\"poison\"]");
		assertEqual("Splash Potion of Poison", splash.getDisplayName());
	}

	public void testPotionThat1710Lacks() {
		List<String> effects = effects(give("potion[potion_contents=\"turtle_master\"]"));
		assertIn("potion.moveSlowdown 3", effects);
		assertIn("potion.resistance 2", effects);
	}

	public void testCustomEffects() {
		assertIn("potion.moveSpeed 2", effects(give("potion[potion_contents={custom_effects:[{id:\"speed\",amplifier:2b,duration:200}]}]")));
	}

	public void testLingeringAndTipped() {
		assertIn("potion.regeneration 0", effects(give("lingering_potion[potion_contents=\"regeneration\"]")));
		onServer(() -> player().inventory.clearInventory(null, -1));
		assertIn("potion.poison 0", effects(give("tipped_arrow[potion_contents=\"poison\"]")));
	}

	public void testFireworks() {
		NBTTagCompound fireworks = give("firework_rocket[fireworks={flight_duration:2b,explosions:[{shape:\"star\",colors:[I;16711680],has_trail:true}]}]")
			.getTagCompound().getCompoundTag("Fireworks");
		assertEqual((byte) 2, fireworks.getByte("Flight"));
		NBTTagCompound explosion = fireworks.getTagList("Explosions", 10).getCompoundTagAt(0);
		assertEqual((byte) 2, explosion.getByte("Type"));
		assertEqual(16711680, explosion.getIntArray("Colors")[0]);
		assertTrue(explosion.getBoolean("Trail"), "no trail");
		onServer(() -> player().inventory.clearInventory(null, -1));
		NBTTagCompound star = give("firework_star[firework_explosion={shape:\"creeper\",colors:[65280]}]").getTagCompound().getCompoundTag("Explosion");
		assertEqual((byte) 3, star.getByte("Type"));
	}

	public void testBooks() {
		ItemStack written = give("written_book[written_book_content={title:\"Hi\",author:\"Asger\",pages:[\"page one\",{text:\"red\",color:\"red\"}]}]");
		assertTrue(ItemEditableBook.validBookTagContents(written.getTagCompound()), "1.7.10 calls the book invalid");
		NBTTagList pages = written.getTagCompound().getTagList("pages", 8);
		assertEqual("page one", pages.getStringTagAt(0));
		assertEqual("§cred", pages.getStringTagAt(1));
		assertEqual("Hi", written.getDisplayName());
		onServer(() -> player().inventory.clearInventory(null, -1));
		ItemStack writable = give("writable_book[writable_book_content={pages:[\"a\",{raw:\"b\"}]}]");
		assertEqual("b", writable.getTagCompound().getTagList("pages", 8).getStringTagAt(1));
	}

	public void testAttributeModifiers() {
		ItemStack sword = give("diamond_sword[attribute_modifiers=[{type:\"attack_damage\",id:\"efr:big\",amount:10,operation:\"add_value\",slot:\"mainhand\"}]]");
		assertIn("generic.attackDamage", sword.getAttributeModifiers().keySet());
		assertEqual(1, sword.getAttributeModifiers().size(), "the modifiers given replace the sword's own");
	}

	public void testPlayerHead() {
		ItemStack head = give("player_head[profile=\"Notch\"]");
		assertIn("Notch", head.getDisplayName());
	}

	public void testBannerPatterns() {
		ItemStack banner = give("white_banner[banner_patterns=[{pattern:\"stripe_bottom\",color:\"red\"}]]");
		NBTTagCompound pattern = banner.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).getCompoundTagAt(0);
		assertEqual("bs", pattern.getString("Pattern"));
		assertEqual(14, pattern.getInteger("Color"));
		// one of the four 1.8 hadn't (spec §66.11), by 26.3's name (the inventory emptied: give reads its first stack)
		onServer(() -> player().inventory.clearInventory(null, -1));
		ItemStack globe = give("white_banner[banner_patterns=[{pattern:\"globe\",color:\"blue\"}]]");
		NBTTagCompound layer = globe.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).getCompoundTagAt(0);
		assertEqual("glb", layer.getString("Pattern"));
		assertEqual(11, layer.getInteger("Color"));
	}

	public void testErrors() {
		// every 26.3 potion's effect exists now (spec §69.11): a test potion that needs one that doesn't
		if (ganymedes01.etfuturum.potion.NamedPotions.byId("unittas:needs_nothing_there") == null) {
			ganymedes01.etfuturum.potion.NamedPotions.register("unittas:needs_nothing_there", "needs_nothing_there", -1,
				ganymedes01.etfuturum.potion.NamedPotions.effect("unittas:nothing_there", 100, 0));
		}
		assertError(command("/give @s potion[potion_contents=\"unittas:needs_nothing_there\"]"), "needs the effect 'unittas:nothing_there', which 1.7.10 doesn't have");
		assertError(command("/give @s potion[potion_contents=\"nope\"]"), "Unknown potion 'minecraft:nope'");
		assertError(command("/give @s written_book[written_book_content={title:\"This title is too long\",author:\"x\"}]"),
			"1.7.10's books take titles of 16 characters at most");
		assertError(command("/give @s white_banner[banner_patterns=[{pattern:\"nope\",color:\"red\"}]]"), "Unknown banner pattern 'minecraft:nope'");
		assertError(command("/give @s diamond_sword[attribute_modifiers=[{type:\"armor\",id:\"x\",amount:1,operation:\"add_value\"}]]"),
			"Unknown attribute 'minecraft:armor'");
	}
}
