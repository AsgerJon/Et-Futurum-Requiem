package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import java.util.List;

/** Selector options (spec §43), tried through /effect give as in command_tests.txt. The player stands at 0.5 4 0.5. */
public class SelectorTests extends TasTestCase {

	private static final double Y = 4.0;

	/** A zombie that keeps a helmet on, so the noon sun doesn't burn it during the test. */
	private EntityZombie zombie(double x, double z) {
		EntityZombie zombie = spawn(EntityZombie::new, x, Y, z);
		onServer(() -> zombie.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet)));
		return zombie;
	}

	private boolean hasSpeed(EntityLivingBase entity) {
		return onServer(() -> entity.isPotionActive(Potion.moveSpeed));
	}

	public void testSuggestsOptionKeys() {
		List<String> shown = type("/effect give @e[").suggestions();
		assertIn("type=", shown);
		assertIn("distance=", shown);
		assertNotIn("]", shown);
	}

	public void testSuggestsCommaOrEndAfterAValue() {
		List<String> shown = type("/effect give @e[type=zombie").suggestions();
		assertIn(",", shown);
		assertIn("]", shown);
	}

	public void testTypeAndDistance() {
		EntityZombie near = zombie(3.5, 0.5);
		EntityZombie far = zombie(20.5, 0.5);
		EntityCow cow = spawn(EntityCow::new, -3.5, Y, 0.5);
		command("/effect give @e[type=zombie,distance=..10] speed");
		assertTrue(hasSpeed(near), "the near zombie has no Speed");
		assertFalse(hasSpeed(far), "the far zombie got Speed");
		assertFalse(hasSpeed(cow), "the cow got Speed");
	}

	public void testNotPlayer() {
		EntityZombie zombie = zombie(3.5, 0.5);
		EntityCow cow = spawn(EntityCow::new, -3.5, Y, 0.5);
		command("/effect give @e[type=!player] speed");
		assertTrue(hasSpeed(zombie), "the zombie has no Speed");
		assertTrue(hasSpeed(cow), "the cow has no Speed");
		assertFalse(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "the player got Speed");
	}

	public void testLimitAndSortNearest() {
		EntityCow first = spawn(EntityCow::new, 2.5, Y, 0.5);
		EntityCow second = spawn(EntityCow::new, 6.5, Y, 0.5);
		EntityCow third = spawn(EntityCow::new, 14.5, Y, 0.5);
		command("/effect give @e[type=cow,limit=2,sort=nearest] speed");
		assertTrue(hasSpeed(first) && hasSpeed(second), "the two nearest cows should have Speed");
		assertFalse(hasSpeed(third), "the third cow got Speed");
	}

	public void testLimitNotForSelf() {
		assertError(command("/effect give @s[limit=1] speed"), "Option 'limit' isn't applicable here");
	}

	public void testGamemode() {
		command("/effect give @a[gamemode=creative] speed");
		assertTrue(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "no Speed in creative");
		assertError(command("/effect give @a[gamemode=spectator] speed"), "Invalid or unknown game mode 'spectator'");
	}

	public void testName() {
		EntityZombie bob = zombie(3.5, 0.5);
		EntityZombie other = zombie(-3.5, 0.5);
		onServer(() -> bob.setCustomNameTag("Bob"));
		command("/effect give @e[name=\"Bob\"] speed");
		assertTrue(hasSpeed(bob), "Bob has no Speed");
		assertFalse(hasSpeed(other), "the unnamed zombie got Speed");
	}

	public void testBox() {
		EntityZombie inside = zombie(2.5, 2.5);
		EntityZombie outside = zombie(10.5, 0.5);
		command("/effect give @e[type=zombie,x=0,y=3,z=0,dx=5,dy=5,dz=5] speed");
		assertTrue(hasSpeed(inside), "the zombie inside the box has no Speed");
		assertFalse(hasSpeed(outside), "the zombie outside got Speed");
	}

	public void testLevel() {
		onServer(() -> player().experienceLevel = 0);
		assertError(command("/effect give @a[level=5..] speed"), "No entity was found");
		onServer(() -> player().addExperienceLevel(5));
		command("/effect give @a[level=5..] speed");
		assertTrue(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "no Speed at level 5");
	}

	public void testDistanceErrors() {
		assertError(command("/effect give @e[distance=-1] speed"), "Distance cannot be negative");
		assertError(command("/effect give @e[distance=5..1] speed"), "Min cannot be bigger than max");
	}

	public void testScores() {
		command("/scoreboard objectives add unittas dummy");
		command("/scoreboard players set " + playerName() + " unittas 5");
		command("/effect give @a[scores={unittas=5}] speed");
		assertTrue(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "no Speed with score 5");
		assertError(command("/effect give @a[scores={unittas=6..}] speed"), "No entity was found");
		command("/scoreboard objectives remove unittas");
	}

	public void testOptionErrors() {
		assertError(command("/effect give @e[foo=1] speed"), "Unknown option 'foo'");
		assertError(command("/effect give @e[type] speed"), "Expected value for option 'type'");
		assertError(command("/effect give @e[type=cow"), "Expected end of options");
	}

	/** Spec §52: the saved data holds the pattern, translated where modern's form differs. */
	public void testNbt() {
		EntityZombie bob = zombie(3.5, 0.5);
		EntityZombie other = zombie(-3.5, 0.5);
		onServer(() -> bob.setCustomNameTag("Bob"));
		command("/effect give @e[nbt={CustomName:\"Bob\"}] speed");
		assertTrue(hasSpeed(bob), "nbt with a name didn't find Bob");
		assertFalse(hasSpeed(other), "nbt with a name found the other zombie");
		command("/effect give @e[type=zombie,nbt=!{CustomName:\"Bob\"}] haste");
		assertTrue(onServer(() -> other.isPotionActive(Potion.digSpeed)), "nbt=! didn't find the other zombie");
		command("/give @s diamond 3");
		command("/effect give @a[nbt={SelectedItem:{id:\"diamond\"}}] jump_boost");
		assertTrue(onServer(() -> player().isPotionActive(Potion.jump)), "SelectedItem didn't match the held diamonds");
		command("/effect give @a[nbt={Inventory:[{id:\"diamond\",count:3b}]}] night_vision");
		assertTrue(onServer(() -> player().isPotionActive(Potion.nightVision)), "Inventory didn't match");
		assertError(command("/effect give @e[nbt={equipment:{head:{id:\"nope\"}}}] speed"), "Unknown item 'minecraft:nope'");
	}

	public void testWitherSkeletonIsItsOwnType() {
		EntitySkeleton plain = spawn(EntitySkeleton::new, 3.5, Y, 0.5);
		EntitySkeleton wither = spawn(EntitySkeleton::new, -3.5, Y, 0.5);
		onServer(() -> {
			wither.setSkeletonType(1);
			plain.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
			wither.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
		});
		command("/effect give @e[type=wither_skeleton] speed");
		assertTrue(hasSpeed(wither), "the wither skeleton has no Speed");
		assertFalse(hasSpeed(plain), "the plain skeleton got Speed");
	}

	public void testLegacyRadius() {
		command("/effect @a[r=5] 1");
		assertTrue(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "1.7.10's @a[r=5] gave no Speed");
	}
}
