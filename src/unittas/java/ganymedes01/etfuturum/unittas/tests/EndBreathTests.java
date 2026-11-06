package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import ganymedes01.etfuturum.entities.EntityDragonFireball;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * The dragon's breath as 26.3's (SPEC-modern-movement.md §76.8, stage 3): the cloud's timing (it waits, then hits every
 * 5 ticks, each entity once in 20), the fireball's flight and impact cloud, the dragon unhurt by its clouds, and
 * bottling dragon's breath. Instant damage heals the undead, so a villager is the target.
 */
@TasSuite("end")
public class EndBreathTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(player().worldObj.loadedEntityList)) {
				if (entity instanceof EntityVillager || entity instanceof EntityAreaEffectCloud || entity instanceof EntityDragonFireball || entity instanceof EntityDragon) {
					((net.minecraft.entity.Entity) entity).setDead();
				}
			}
		});
	}

	private static EntityVillager villager(World world, double x, double z) {
		EntityVillager villager = new EntityVillager(world);
		villager.setLocationAndAngles(x, 4.0, z, 0.0F, 0.0F);
		// kept still: one that wanders out of the cloud misses its hits (a run once saw "20 14 14")
		villager.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).setBaseValue(0.0);
		world.spawnEntityInWorld(villager);
		return villager;
	}

	/** A cloud of instant damage II: nothing in its first 20 ticks, then 6 a hit, each entity once in 20 ticks. */
	public void testCloudTiming() {
		EntityVillager villager = onServer(() -> {
			EntityVillager target = villager(player().worldObj, 20.5, 20.5);
			EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(player().worldObj, 20.5, 4.0, 20.5);
			cloud.setRadius(3.0F);
			cloud.setDuration(200);
			cloud.addEffect(new PotionEffect(Potion.harm.id, 1, 1));
			player().worldObj.spawnEntityInWorld(cloud);
			return target;
		});
		serverTicks(15);
		float waiting = onServer(villager::getHealth);
		serverTicks(15);
		float once = onServer(villager::getHealth);
		serverTicks(20);
		float twice = onServer(villager::getHealth);
		assertEqual("20 14 8", (int) waiting + " " + (int) once + " " + (int) twice, "the villager's health while it waits, after one hit, after two");
	}

	/** The fireball: 0.1 a tick to start, then (v + 0.1 along v) * 0.95 a tick; it can't be hit. */
	public void testFireballFlight() {
		String found = onServer(() -> {
			EntityDragonFireball fireball = new EntityDragonFireball(player().worldObj, null, 40.5, 30.0, 40.5, 1.0, 0.0, 0.0);
			player().worldObj.spawnEntityInWorld(fireball);
			double start = Math.sqrt(fireball.motionX * fireball.motionX + fireball.motionZ * fireball.motionZ);
			boolean hit = fireball.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(player()), 1.0F);
			return String.format("%.2f %s", start, hit);
		});
		assertEqual("0.10 false", found, "its first speed, and whether a hit counts");
		serverTicks(30);
		double terminal = onServer(() -> {
			for (Object entity : player().worldObj.loadedEntityList) {
				if (entity instanceof EntityDragonFireball fireball) {
					return Math.sqrt(fireball.motionX * fireball.motionX + fireball.motionZ * fireball.motionZ);
				}
			}
			return -1.0;
		});
		// 1.9 - 1.8 * 0.95^n: about 1.51 after 30 ticks (the client's ticks and the server's may differ by one or two)
		assertTrue(terminal > 1.40 && terminal < 1.62, "its speed after 30 ticks: " + terminal);
	}

	/** The fireball hitting the ground: 26.3's cloud (radius 3 growing to 7 over 600 ticks, instant damage II), on the villager near. */
	public void testFireballImpact() {
		String found = onServer(() -> {
			villager(player().worldObj, 61.5, 60.5);
			EntityDragonFireball fireball = new EntityDragonFireball(player().worldObj, null, 60.5, 7.0, 60.5, 0.0, -1.0, 0.0);
			player().worldObj.spawnEntityInWorld(fireball);
			return null;
		});
		waitUntil("the cloud", () -> clouds().size() == 1, 60);
		found = onServer(() -> {
			EntityAreaEffectCloud cloud = clouds().get(0);
			return String.format("radius %.1f, duration %d, growing %.4f, at %.1f %.1f", cloud.getRadius(), cloud.getDuration(), cloud.getRadiusPerTick(), cloud.posX, cloud.posZ);
		});
		assertEqual("radius 3.0, duration 600, growing 0.0067, at 61.5 60.5", found, "the impact's cloud");
	}

	/** A glass bottle near a dragon's cloud: dragon's breath, the cloud half a block smaller. */
	public void testBottling() {
		if (!io.github.asgerjon.mcfly.registry.McFlyItems.DRAGON_BREATH.isEnabled()) {
			skipTest("dragon's breath is off");
		}
		String found = onServer(() -> {
			EntityDragon dragon = new EntityDragon(player().worldObj);
			dragon.setLocationAndAngles(player().posX, player().posY + 30.0, player().posZ, 0.0F, 0.0F);
			EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(player().worldObj, player().posX, player().posY, player().posZ);
			cloud.setRadius(3.0F);
			cloud.setDuration(200);
			cloud.setOwner(dragon);
			player().worldObj.spawnEntityInWorld(cloud);
			ItemStack bottles = new ItemStack(Items.glass_bottle, 2);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, bottles);
			ItemStack after = bottles.getItem().onItemRightClick(bottles, player().worldObj, player());
			boolean breath = player().inventory.hasItem(io.github.asgerjon.mcfly.registry.McFlyItems.DRAGON_BREATH.get());
			player().inventory.clearInventory(null, -1);
			return (breath ? "breath" : "no breath") + String.format(", radius %.1f", cloud.getRadius());
		});
		assertEqual("breath, radius 2.5", found, "after bottling");
	}

	/** The dragon in its own cloud takes nothing. */
	public void testDragonUnhurt() {
		EntityDragon dragon = onServer(() -> {
			EntityDragon made = new EntityDragon(player().worldObj);
			made.setLocationAndAngles(80.5, 4.0, 80.5, 0.0F, 0.0F);
			player().worldObj.spawnEntityInWorld(made);
			ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon.of(made).setPhase(ganymedes01.etfuturum.world.end.modern.dragon.DragonPhase.HOVER);
			EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(player().worldObj, 80.5, 4.0, 80.5);
			cloud.setRadius(6.0F);
			cloud.setDuration(200);
			cloud.setOwner(made);
			cloud.addEffect(new PotionEffect(Potion.harm.id, 1, 1));
			player().worldObj.spawnEntityInWorld(cloud);
			return made;
		});
		serverTicks(40);
		assertEqual(200, (int) (float) onServer(dragon::getHealth), "the dragon's health in its cloud");
	}

	private java.util.List<EntityAreaEffectCloud> clouds() {
		java.util.List<EntityAreaEffectCloud> found = new ArrayList<>();
		for (Object entity : player().worldObj.loadedEntityList) {
			if (entity instanceof EntityAreaEffectCloud cloud && !cloud.isDead) {
				found.add(cloud);
			}
		}
		return found;
	}
}
