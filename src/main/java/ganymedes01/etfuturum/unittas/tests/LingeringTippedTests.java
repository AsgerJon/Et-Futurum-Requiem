package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Lingering potions and tipped arrows as our own (SPEC-modern-movement.md §82): McFly's under EFR's ids, and the clouds a
 * lingering potion and a creeper leave as 26.3's area effect clouds.
 */
public class LingeringTippedTests extends TasTestCase {

	private static final int X = 300;
	private static final int Y = 4;
	private static final int Z = 300;

	@Override
	protected void setUp() {
		if (Item.itemRegistry.getObject("etfuturum:lingering_potion") == null || !NamedPotions.isReady()) {
			skipTest("lingering potions or potions by name are off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (Object entity : world().getEntitiesWithinAABB(Entity.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(X - 8, Y - 2, Z - 8, X + 8, Y + 8, Z + 8))) {
				if (entity != player()) {
					((Entity) entity).setDead();
				}
			}
			for (int x = X - 3; x <= X + 3; x++) {
				for (int z = Z - 3; z <= Z + 3; z++) {
					world().setBlock(x, Y - 1, z, Blocks.stone);
					world().setBlockToAir(x, Y, z);
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	/** The classes EFR's lingering potions and tipped arrows were (spec §82): none of them is left. */
	static final String[] EFR_CLASSES = {"ganymedes01.etfuturum.items.ItemLingeringPotion", "ganymedes01.etfuturum.items.ItemArrowTipped",
		"ganymedes01.etfuturum.entities.EntityLingeringPotion", "ganymedes01.etfuturum.entities.EntityLingeringEffect",
		"ganymedes01.etfuturum.entities.EntityTippedArrow", "ganymedes01.etfuturum.recipes.crafting.RecipeTippedArrow",
		"ganymedes01.etfuturum.dispenser.DispenserBehaviourLingeringPotion", "ganymedes01.etfuturum.dispenser.DispenserBehaviourTippedArrow",
		"ganymedes01.etfuturum.client.renderer.entity.LingeringPotionRenderer", "ganymedes01.etfuturum.client.renderer.entity.LingeringEffectRenderer"};

	/** Ours: the two items and two entities McFly's under the old ids, no cloud entity of EFR's, none of EFR's classes. */
	public void testOursUnderTheOldIds() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String id : new String[]{"etfuturum:lingering_potion", "etfuturum:tipped_arrow"}) {
			Object item = Item.itemRegistry.getObject(id);
			if (item == null || !item.getClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(id + " is " + (item == null ? "nothing" : item.getClass().getName()));
			}
		}
		cpw.mods.fml.common.ModContainer efr = cpw.mods.fml.common.Loader.instance().getIndexedModList().get("etfuturum");
		for (Object[] entity : new Object[][]{{2, "tipped_arrow"}, {6, "lingering_potion"}}) {
			cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration registration = cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn(efr, (Integer) entity[0]);
			if (registration == null || !entity[1].equals(registration.getEntityName()) || !registration.getEntityClass().getName().startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(entity[1] + " is " + (registration == null ? "nothing" : registration.getEntityClass().getName()));
			}
		}
		if (cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn(efr, 7) != null) {
			wrong.add("EFR's lingering_effect still registered");
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "lingering potions and tipped arrows");
	}

	private static Object field(Object on, String name) throws Exception {
		Field field = EntityAreaEffectCloud.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(on);
	}

	/** A cloud's numbers as 26.3 sets them: radius, wait, duration, shrinking by tick and by use, the effect scale. */
	@SuppressWarnings("unchecked")
	private static String numbers(EntityAreaEffectCloud cloud) throws Exception {
		List<String> effects = new ArrayList<>();
		for (PotionEffect effect : (List<PotionEffect>) field(cloud, "effects")) {
			effects.add(Potion.potionTypes[effect.getPotionID()].getName() + "/" + effect.getDuration() + "/" + effect.getAmplifier());
		}
		return "radius " + cloud.getRadius() + " wait " + field(cloud, "waitTime") + " duration " + cloud.getDuration()
			+ " per tick " + cloud.getRadiusPerTick() + " on use " + field(cloud, "radiusOnUse") + " scale " + field(cloud, "potionDurationScale")
			+ " effects " + effects;
	}

	private static EntityAreaEffectCloud cloudNear() {
		for (Object entity : world().getEntitiesWithinAABB(EntityAreaEffectCloud.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(X - 6, Y - 2, Z - 6, X + 6, Y + 6, Z + 6))) {
			return (EntityAreaEffectCloud) entity;
		}
		return null;
	}

	/**
	 * A thrown lingering potion leaves 26.3's cloud (`ThrownLingeringPotion.onHitAsPotion`): radius 3, waiting 10,
	 * 600 ticks, shrinking 3/600 a tick and 0.5 a use, the potion's effects given at a quarter, in the potion's colour.
	 */
	public void testThrownPotionsCloud() throws Exception {
		ItemStack potion = BrewingTable.create(BrewingTable.Container.LINGERING, NamedPotions.byId("minecraft:slowness"));
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setPositionAndRotation(X + 0.5, Y, Z + 0.5, 0.0F, 90.0F);
			player().setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
			player().rotationPitch = 90.0F;
			potion.getItem().onItemRightClick(potion.copy(), world(), player());
		});
		waitUntil("a cloud", () -> cloudNear() != null, 60);
		String got = onServer(() -> {
			EntityAreaEffectCloud cloud = cloudNear();
			Object colour;
			try {
				colour = String.format("%06X", (Integer) cloud.getClass().getMethod("colour").invoke(cloud) & 0xFFFFFF);
			} catch (NoSuchMethodException e) {
				colour = "none";
			}
			return numbers(cloud) + " colour " + colour;
		});
		assertEqual("radius 3.0 wait 10 duration 600 per tick -0.005 on use -0.5 scale 0.25 effects [potion.moveSlowdown/1800/0] colour 8BAFE0", got,
			"the thrown lingering potion's cloud");
	}

	/** A creeper with an effect leaves 26.3's cloud when it explodes (`Creeper.spawnLingeringCloud`). */
	public void testCreepersCloud() throws Exception {
		onServer(() -> {
			EntityCreeper creeper = new EntityCreeper(world());
			creeper.setPosition(X + 0.5, Y, Z + 0.5);
			creeper.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 600, 1));
			world().spawnEntityInWorld(creeper);
			world().newExplosion(creeper, creeper.posX, creeper.posY, creeper.posZ, 0.1F, false, false);
		});
		waitUntil("a cloud", () -> cloudNear() != null, 20);
		String got = onServer(() -> numbers(cloudNear()));
		assertEqual("radius 2.5 wait 10 duration 300 per tick -0.008333334 on use -0.5 scale 1.0 effects [potion.moveSpeed/600/1]", got, "the creeper's cloud");
	}
}
