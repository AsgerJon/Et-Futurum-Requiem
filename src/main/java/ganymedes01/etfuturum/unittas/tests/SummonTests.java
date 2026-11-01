package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.world.EnumDifficulty;

import java.util.List;

/** Modern /summon (spec §47). The player stands at 0.5 4 0.5 looking south. */
public class SummonTests extends TasTestCase {

	private static final String NETHERITE_SKELETON = "/summon skeleton ~ ~ ~ {equipment:{mainhand:{id:\"bow\"},"
		+ "head:{id:\"netherite_helmet\",components:{\"minecraft:enchantments\":{protection:4}}},"
		+ "chest:{id:\"netherite_chestplate\",components:{\"minecraft:enchantments\":{protection:4}}},"
		+ "legs:{id:\"netherite_leggings\",components:{\"minecraft:enchantments\":{protection:4}}},"
		+ "feet:{id:\"netherite_boots\",components:{\"minecraft:enchantments\":{protection:4}}}}}";

	/** The one entity of the type near the player; fails when there isn't exactly one. */
	private <E extends Entity> E only(Class<E> type) {
		List<E> found = onServer(() -> entitiesNear(type, 16.0));
		assertEqual(1, found.size(), "how many " + type.getSimpleName());
		return found.get(0);
	}

	private String typeOf(Entity entity) {
		return onServer(() -> EntityTypeIds.idOf(entity));
	}

	public void testSuggestsTypes() {
		List<String> shown = type("/summon skel").suggestions();
		for (String id : new String[]{"skeleton", "skeleton_horse", "wither_skeleton"}) {
			assertTrue(shown.stream().anyMatch(text -> text.endsWith(id)), "no " + id + " in " + shown);
		}
	}

	public void testSkeletonGetsItsBow() {
		assertSaid(command("/summon skeleton"), "Summoned new Skeleton");
		EntitySkeleton skeleton = only(EntitySkeleton.class);
		assertEqual("minecraft:bow:0", id(onServer(() -> ItemStack.copyItemStack(skeleton.getHeldItem()))));
	}

	public void testDataSkipsTheSpawnGear() {
		command("/summon skeleton ~ ~ ~ {}");
		EntitySkeleton skeleton = only(EntitySkeleton.class);
		assertIsNone(onServer(() -> skeleton.getHeldItem()));
	}

	public void testNetheriteSkeleton() {
		assertTrue(NETHERITE_SKELETON.length() > 256, "longer than the chat takes; command() runs it as a command block would");
		assertNoError(command(NETHERITE_SKELETON));
		EntitySkeleton skeleton = only(EntitySkeleton.class);
		screenshot("SummonTests.netherite_skeleton");
		assertEqual("minecraft:bow:0", id(onServer(() -> ItemStack.copyItemStack(skeleton.getHeldItem()))));
		String[] pieces = {"Netherite Boots", "Netherite Leggings", "Netherite Chestplate", "Netherite Helmet"};
		for (int slot = 1; slot <= 4; slot++) {
			int armourSlot = slot;
			ItemStack piece = onServer(() -> ItemStack.copyItemStack(skeleton.getEquipmentInSlot(armourSlot)));
			assertIsNotNone(piece, pieces[slot - 1] + " missing");
			assertEqual(pieces[slot - 1], piece.getDisplayName());
			assertEqual(4, EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, piece));
		}
	}

	public void testDropChances() {
		command("/summon skeleton ~ ~ ~ {equipment:{head:{id:\"netherite_helmet\"}},drop_chances:{head:1.0f}}");
		EntitySkeleton skeleton = only(EntitySkeleton.class);
		float head = onServer(() -> {
			NBTTagCompound saved = new NBTTagCompound();
			skeleton.writeToNBT(saved);
			return saved.getTagList("DropChances", 5).func_150308_e(4);
		});
		assertAlmostEqual(1.0, head, 0.001);
	}

	public void testLocalCoordinates() {
		look(0.0F, 0.0F);
		command("/summon zombie ^ ^ ^5");
		Entity zombie = only(EntityZombie.class);
		assertAlmostEqual(5.5, onServer(() -> zombie.posZ), 0.6);
		assertAlmostEqual(0.5, onServer(() -> zombie.posX), 0.6);
	}

	public void testWholeNumbersAreBlockCentres() {
		// read in the same tick, before the creeper takes a step
		double[] at = onServer(() -> {
			runCommand("/summon creeper 10 4 10");
			EntityCreeper creeper = entitiesNear(EntityCreeper.class, 16.0).get(0);
			return new double[]{creeper.posX, creeper.posY, creeper.posZ};
		});
		assertAlmostEqual(10.5, at[0], 0.001);
		assertAlmostEqual(4.0, at[1], 0.001);
		assertAlmostEqual(10.5, at[2], 0.001);
	}

	public void testCoordinateErrors() {
		assertError(command("/summon pig ~ ~ ^1"), "Cannot mix world and local coordinates (everything must either use ^ or not)");
		assertError(command("/summon pig 1 2"), "Incomplete (expected 3 coordinates)");
	}

	public void testVariants() {
		command("/summon wither_skeleton");
		EntitySkeleton wither = only(EntitySkeleton.class);
		assertEqual(1, onServer(() -> wither.getSkeletonType()));
		assertEqual("minecraft:stone_sword:0", id(onServer(() -> ItemStack.copyItemStack(wither.getHeldItem()))));
		String[][] cases = {{"donkey", "minecraft:donkey"}, {"zombie_villager", "minecraft:zombie_villager"}, {"cat", "minecraft:cat"}};
		for (String[] variant : cases) {
			onServer(() -> {
				for (Entity entity : entitiesNear(EntityLiving.class, 16.0)) {
					entity.setDead();
				}
			});
			serverTicks(1);
			assertNoError(command("/summon " + variant[0]));
			List<EntityLiving> found = onServer(() -> entitiesNear(EntityLiving.class, 16.0));
			assertEqual(1, found.size(), variant[0]);
			assertEqual(variant[1], typeOf(found.get(0)));
		}
	}

	public void testLightning() {
		assertNoError(command("/summon lightning_bolt"));
		assertTrue(onServer(() -> !world().weatherEffects.isEmpty()), "no lightning");
	}

	public void testNamedFastZombie() {
		command("/summon zombie ~ ~ ~ {CustomName:{text:\"Bob\",color:\"gold\"},active_effects:[{id:\"speed\",amplifier:1,duration:600}]}");
		EntityZombie zombie = only(EntityZombie.class);
		assertIn("Bob", onServer(() -> zombie.getCustomNameTag()));
		assertEqual(1, onServer(() -> zombie.getActivePotionEffect(Potion.moveSpeed) == null ? -1 : zombie.getActivePotionEffect(Potion.moveSpeed).getAmplifier()));
	}

	public void testPassenger() {
		command("/summon pig ~ ~ ~ {Passengers:[{id:\"skeleton\"}]}");
		EntityPig pig = only(EntityPig.class);
		assertTrue(onServer(() -> pig.riddenByEntity instanceof EntitySkeleton), "no skeleton riding the pig");
	}

	public void testErrors() {
		assertError(command("/summon zombie ~ ~ ~ {equipment:{head:{id:\"nope\"}}}"), "Can't use minecraft:zombie: equipment: Unknown item 'minecraft:nope'");
		assertError(command("/summon player"), "Can't summon entity of type minecraft:player");
	}

	public void testPeaceful() {
		difficulty(EnumDifficulty.PEACEFUL);
		assertError(command("/summon zombie"), "Monsters cannot be summoned in Peaceful difficulty");
	}

	public void testLegacyForm() {
		command("/summon Skeleton ~ ~ ~ {Equipment:[{id:261}]}");
		EntitySkeleton skeleton = only(EntitySkeleton.class);
		assertEqual("minecraft:bow:0", id(onServer(() -> ItemStack.copyItemStack(skeleton.getHeldItem()))));
	}
}
