package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.items.ItemSuspiciousStew;
import ganymedes01.etfuturum.potion.ModPotions;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityBeacon;

import java.util.List;

/**
 * Named effects, stage B (SPEC-modern-movement.md §70.3, §70.5): effects saved by name, as 26.3 saves them (`id`),
 * and no number; old saves' numbers still read. Effect instances (entities, potions, arrows), the beacon, the
 * suspicious stew, and what the modern commands write.
 */
public class EffectSaveTests extends TasTestCase {

	/** A test effect past a byte's 127, made once a run: 1.7.10 wrote it as a byte that came back negative and dropped. */
	private static Potion high;

	/** An effect instance is written with its name and no number. */
	public void testWrittenByName() {
		NBTTagCompound tag = new PotionEffect(Potion.moveSpeed.id, 100, 1).writeCustomPotionEffectToNBT(new NBTTagCompound());
		assertEqual("minecraft:speed", tag.getString("id"), "the name");
		assertTrue(!tag.hasKey("Id"), "the old number is still written");
		assertEqual(100, tag.getInteger("Duration"), "the duration");
		assertEqual(1, (int) tag.getByte("Amplifier"), "the amplifier");
	}

	/** One is read back by its name; an old save's number is still read. */
	public void testReadByNameAndOldNumber() {
		if (ModPotions.slowFalling != null) {
			NBTTagCompound named = new NBTTagCompound();
			named.setString("id", "minecraft:slow_falling");
			named.setByte("Amplifier", (byte) 1);
			named.setInteger("Duration", 100);
			PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(named);
			assertTrue(effect != null, "nothing read by name");
			assertEqual(ModPotions.slowFalling.id, effect.getPotionID(), "the effect read by name");
			assertEqual(100, effect.getDuration(), "its duration");
		}
		NBTTagCompound old = new NBTTagCompound();
		old.setByte("Id", (byte) Potion.moveSpeed.id);
		old.setInteger("Duration", 60);
		PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(old);
		assertTrue(effect != null, "an old save's effect not read");
		assertEqual(Potion.moveSpeed.id, effect.getPotionID(), "an old save's effect");
	}

	/** An effect past 127 on a mob survives saving and loading (1.7.10 dropped it). */
	public void testHighEffectSavedOnAMob() {
		onServer(() -> {
			if (high == null) {
				high = new TestEffect(150, "high");
			}
		});
		boolean kept = onServer(() -> {
			EntityZombie zombie = new EntityZombie(world());
			zombie.addPotionEffect(new PotionEffect(high.id, 400, 0));
			NBTTagCompound saved = new NBTTagCompound();
			zombie.writeToNBT(saved);
			EntityZombie loaded = new EntityZombie(world());
			loaded.readFromNBT(saved);
			return loaded.isPotionActive(high);
		});
		assertTrue(kept, "the effect at 150 was lost saving and loading the zombie");
	}

	/** A beacon's effects are saved by name (26.3's `primary_effect`, `secondary_effect`); an old beacon's numbers read. */
	public void testBeaconByName() {
		String[] saved = onServer(() -> {
			// a beacon only takes an effect its pyramid allows (1.7.10's setPrimaryEffect): one of level 4
			NBTTagCompound pyramid = new NBTTagCompound();
			pyramid.setInteger("Levels", 4);
			TileEntityBeacon beacon = new TileEntityBeacon();
			beacon.readFromNBT(pyramid);
			beacon.setPrimaryEffect(Potion.moveSpeed.id);
			beacon.setSecondaryEffect(Potion.regeneration.id);
			NBTTagCompound tag = new NBTTagCompound();
			beacon.writeToNBT(tag);
			TileEntityBeacon loaded = new TileEntityBeacon();
			loaded.readFromNBT(tag);
			return new String[]{tag.getString("primary_effect"), tag.getString("secondary_effect"), String.valueOf(tag.hasKey("Primary")),
				loaded.getPrimaryEffect() + " " + loaded.getSecondaryEffect()};
		});
		assertEqual("minecraft:speed", saved[0], "the primary effect's name");
		assertEqual("minecraft:regeneration", saved[1], "the secondary effect's name");
		assertEqual("false", saved[2], "the old Primary number still written");
		assertEqual(Potion.moveSpeed.id + " " + Potion.regeneration.id, saved[3], "read back");
		String old = onServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("Primary", Potion.digSpeed.id);
			tag.setInteger("Secondary", Potion.regeneration.id);
			tag.setInteger("Levels", 4);
			TileEntityBeacon loaded = new TileEntityBeacon();
			loaded.readFromNBT(tag);
			return loaded.getPrimaryEffect() + " " + loaded.getSecondaryEffect();
		});
		assertEqual(Potion.digSpeed.id + " " + Potion.regeneration.id, old, "an old beacon's numbers");
	}

	/** The suspicious stew's effects by name (26.3's `suspicious_stew_effects`), its old numbers still read. */
	public void testStewByName() {
		if (!ModItems.SUSPICIOUS_STEW.isEnabled()) {
			skipTest("suspicious stew is off");
		}
		ItemStack stew = ModItems.SUSPICIOUS_STEW.newItemStack(1, 0);
		ItemSuspiciousStew.addEffect(stew, Potion.nightVision, 100);
		NBTTagCompound entry = stew.getTagCompound().getTagList(ItemSuspiciousStew.effectsList, 10).getCompoundTagAt(0);
		assertEqual("minecraft:night_vision", entry.getString("id"), "the stew's effect name");
		assertTrue(!entry.hasKey(ItemSuspiciousStew.stewEffect), "the old EffectId still written");
		assertEqual(Potion.nightVision, ItemSuspiciousStew.effectOf(entry), "read back");
		NBTTagCompound old = new NBTTagCompound();
		old.setByte(ItemSuspiciousStew.stewEffect, (byte) Potion.blindness.id);
		assertEqual(Potion.blindness, ItemSuspiciousStew.effectOf(old), "an old stew's number");
	}

	/** The modern commands write effects by name: a potion's custom effects. */
	public void testCommandsWriteNames() {
		assertNoError(command("/give @s potion[potion_contents={custom_effects:[{id:\"minecraft:haste\",duration:100}]}]"));
		List<ItemStack> stacks = inventory();
		assertTrue(!stacks.isEmpty(), "nothing given");
		NBTTagList custom = stacks.get(0).getTagCompound().getTagList("CustomPotionEffects", 10);
		assertEqual(1, custom.tagCount(), "custom effects");
		assertEqual("minecraft:haste", custom.getCompoundTagAt(0).getString("id"), "its name");
		assertTrue(!custom.getCompoundTagAt(0).hasKey("Id"), "its old number still written");
	}

	/** A test effect: neither good nor bad, white. */
	private static final class TestEffect extends Potion {
		TestEffect(int id, String name) {
			super(id, false, 0xFFFFFF);
			setPotionName("potion.unittas." + name);
		}
	}
}
