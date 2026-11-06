package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import ganymedes01.etfuturum.world.end.modern.dragon.DragonPhase;
import ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/**
 * The dragon as 26.3's (SPEC-modern-movement.md §76.7, stage 2): its node graph and paths, its parts, the damage rules
 * (head and neck in full, the rest a quarter and one; only players and explosions), sitting damage sending it off, the
 * killing blow while flying, its death (XP, the fight told), landing and scanning, and crystals healing it.
 */
@TasSuite("end")
public class EndDragonTests extends TasTestCase {

	/** 26.3's node places (x, z), from its code's table sine. */
	private static final String NODES = "[60 0, 51 30, 29 51, 0 60, -31 51, -52 29, -60 0, -52 -31, -30 -52, 0 -60, 29 -52, 51 -30, "
		+ "40 0, 28 28, 0 40, -29 28, -40 0, -29 -29, 0 -40, 28 -29, 20 0, 0 20, -20 0, 0 -20]";

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off");
		}
	}

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	private static WorldServer end() {
		return MinecraftServer.getServer().worldServerForDimension(1);
	}

	private void toTheEnd() {
		onServer(() -> {
			if (player().dimension != 1) {
				player().travelToDimension(1);
			}
		});
		waitUntilOnClient("the End", () -> mc().thePlayer.dimension == 1 && !(mc().currentScreen instanceof GuiDownloadTerrain), 200);
	}

	/**
	 * A dragon of the test's own at (0, 90, 30), the fight's dragon gone, hovering where it is; the player flying near
	 * first (a chunk out of the player's view unloads, and comes back with a new copy of what was in it).
	 */
	private EntityDragon dragon() {
		toTheEnd();
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5, 100.0, 45.5, 180.0F, 0.0F);
			player().capabilities.isFlying = true;
			player().sendPlayerAbilities();
		});
		ticks(10);
		return onServer(() -> {
			for (Object entity : new ArrayList<Object>(end().loadedEntityList)) {
				if (entity instanceof EntityDragon old) {
					old.setDead();
				}
			}
			EntityDragon dragon = new EntityDragon(end());
			dragon.setLocationAndAngles(0.0, 90.0, 30.0, 0.0F, 0.0F);
			end().spawnEntityInWorld(dragon);
			ModernDragon.of(dragon).setPhase(DragonPhase.HOVER);
			// the fight's dragon (it counts only its own death)
			EnderDragonFight.of(end()).useDragon(dragon);
			return dragon;
		});
	}

	/** Its 24 nodes at 26.3's places, at the island's top plus 5 (the middle ring 15), never under 73. */
	public void testNodes() {
		EntityDragon dragon = dragon();
		String found = onServer(() -> {
			List<String> places = new ArrayList<>();
			List<String> heights = new ArrayList<>();
			ModernDragon modern = ModernDragon.of(dragon);
			for (int i = 0; i < 24; i++) {
				int x = modern.nodeX(i);
				int z = modern.nodeZ(i);
				places.add(x + " " + z);
				int want = Math.max(73, ModernDragon.top(end(), x, z) + (i >= 12 && i < 20 ? 15 : 5));
				if (modern.nodeY(i) != want) {
					heights.add("node " + i + " at y " + modern.nodeY(i) + ", wanted " + want);
				}
			}
			return places + " " + heights;
		});
		assertEqual(NODES + " []", found, "the nodes");
		onServer(dragon::setDead);
	}

	/**
	 * A path along the links: from node 0 to node 6, each step a link, ending at 6 (with crystals standing: with none the
	 * outer ring is closed, as 26.3's; the fight counts them every 100 ticks, not saved, so it's waited for).
	 */
	public void testPath() {
		EntityDragon dragon = dragon();
		// a crystal of the test's own on the first pillar (the tests' sequence leaves the pillars without theirs: spec
		// §76.7's open issue), then the fight's count (every 100 ticks) waited for
		EntityEnderCrystal own = onServer(() -> {
			EntityEnderCrystal crystal = new EntityEnderCrystal(end());
			crystal.setLocationAndAngles(42.5, 104.0, 0.5, 0.0F, 0.0F);
			end().spawnEntityInWorld(crystal);
			return crystal;
		});
		waitUntil("the fight's crystal count", () -> EnderDragonFight.of(end()).aliveCrystals() > 0, 220);
		String found = onServer(() -> {
			List<Integer> path = ModernDragon.of(dragon).findPath(0, 6);
			if (path == null || path.isEmpty()) {
				return "no path";
			}
			for (int i = 1; i < path.size(); i++) {
				if (!ModernDragon.linked(path.get(i - 1), path.get(i))) {
					return "not linked: " + path;
				}
			}
			return path.get(0) + " to " + path.get(path.size() - 1);
		});
		assertEqual("0 to 6", found, "the path");
		onServer(() -> {
			own.setDead();
			dragon.setDead();
			player().capabilities.isFlying = false;
			player().sendPlayerAbilities();
		});
	}

	/** The head and neck take damage in full, the rest a quarter and one; a zombie's hit does nothing. */
	public void testDamageByPart() {
		EntityDragon dragon = dragon();
		String found = onServer(() -> {
			List<String> taken = new ArrayList<>();
			for (EntityDragonPart part : partsNamed(dragon, "head", "neck", "body", "tail")) {
				dragon.setHealth(200.0F);
				dragon.hurtResistantTime = 0;
				part.attackEntityFrom(DamageSource.causePlayerDamage(player()), 20.0F);
				taken.add(part.field_146032_b + " " + (int) (200.0F - dragon.getHealth()));
			}
			dragon.setHealth(200.0F);
			dragon.hurtResistantTime = 0;
			EntityZombie zombie = new EntityZombie(end());
			dragon.dragonPartHead.attackEntityFrom(new EntityDamageSource("mob", zombie), 20.0F);
			taken.add("zombie " + (int) (200.0F - dragon.getHealth()));
			return taken.toString();
		});
		assertEqual("[head 20, neck 20, body 6, tail 6, zombie 0]", found, "damage taken by part");
		onServer(dragon::setDead);
	}

	private static List<EntityDragonPart> partsNamed(EntityDragon dragon, String... names) {
		List<EntityDragonPart> found = new ArrayList<>();
		for (String name : names) {
			for (Object part : dragon.getParts()) {
				if (((EntityDragonPart) part).field_146032_b.equals(name)) {
					found.add((EntityDragonPart) part);
					break;
				}
			}
		}
		return found;
	}

	/** Perched (scanning), more than 50 damage taken sends it off (takeoff). */
	public void testSittingDamageTakesOff() {
		EntityDragon dragon = dragon();
		String found = onServer(() -> {
			ModernDragon.of(dragon).setPhase(DragonPhase.SITTING_SCANNING);
			for (int i = 0; i < 3; i++) {
				dragon.hurtResistantTime = 0;
				dragon.dragonPartHead.attackEntityFrom(DamageSource.causePlayerDamage(player()), 20.0F);
			}
			return ModernDragon.of(dragon).phase().name();
		});
		assertEqual("TAKEOFF", found, "after 60 damage while perched");
		onServer(dragon::setDead);
	}

	/** A killing blow while flying leaves it at 1 health, dying (it flies to the portal to die). */
	public void testKillingBlowWhileFlying() {
		EntityDragon dragon = dragon();
		String found = onServer(() -> {
			ModernDragon.of(dragon).setPhase(DragonPhase.HOLDING_PATTERN);
			dragon.setHealth(5.0F);
			dragon.hurtResistantTime = 0;
			dragon.dragonPartHead.attackEntityFrom(DamageSource.causePlayerDamage(player()), 20.0F);
			return ModernDragon.of(dragon).phase().name() + " " + dragon.getHealth() + " " + dragon.isDead;
		});
		assertEqual("DYING 1.0 false", found, "after a killing blow in flight");
		onServer(dragon::setDead);
	}

	/** Dead (health 0): 200 ticks rising, its XP (500, or 12000 the first kill) out in shares, then gone, the fight told. */
	public void testDeath() {
		EntityDragon dragon = dragon();
		boolean firstKill = onServer(() -> !EnderDragonFight.of(end()).hasPreviouslyKilledDragon());
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(end().loadedEntityList)) {
				if (entity instanceof EntityXPOrb orb) {
					orb.setDead();
				}
			}
			ModernDragon.of(dragon).setPhase(DragonPhase.DYING);
			dragon.setHealth(0.0F);
		});
		waitUntil("the dragon gone", () -> dragon.isDead, 260);
		int xp = onServer(() -> {
			int total = 0;
			for (Object entity : end().loadedEntityList) {
				if (entity instanceof EntityXPOrb orb && !orb.isDead) {
					total += orb.getXpValue();
				}
			}
			return total;
		});
		assertEqual(firstKill ? 12000 : 500, xp, "the XP it left");
		assertTrue(onServer(() -> EnderDragonFight.of(end()).dragonKilled()), "the fight knows it's dead");
	}

	/** Landing on the portal, then scanning; no player near for 100 ticks: off, and charging at the player within 150. */
	public void testLandingThenScanning() {
		EntityDragon dragon = dragon();
		onServer(() -> {
			dragon.setLocationAndAngles(0.5, ModernDragon.top(end(), 0, 0) + 6.0, 6.5, 0.0F, 0.0F);
			ModernDragon.of(dragon).setPhase(DragonPhase.LANDING);
		});
		waitUntil("perched", () -> ModernDragon.of(dragon).phase() == DragonPhase.SITTING_SCANNING, 200);
		// the player 40 off and above the island, in its sight (beyond the 20 it attacks, within the 150 it charges);
		// in survival (creative players aren't targets)
		onServer(() -> {
			player().setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(0.5, 75.0, 40.5, 180.0F, 0.0F);
			player().capabilities.isFlying = true;
			player().sendPlayerAbilities();
		});
		waitUntil("off again", () -> ModernDragon.of(dragon).phase() != DragonPhase.SITTING_SCANNING, 140);
		assertEqual("CHARGING_PLAYER", onServer(() -> ModernDragon.of(dragon).phase().name()), "after 100 ticks with no one near");
		onServer(() -> {
			dragon.setDead();
			player().setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
			player().capabilities.isFlying = false;
			player().sendPlayerAbilities();
		});
	}

	/** A crystal within 32 heals it 1 every 10 ticks. */
	public void testCrystalHeals() {
		EntityDragon dragon = dragon();
		EntityEnderCrystal crystal = onServer(() -> {
			dragon.setHealth(100.0F);
			EntityEnderCrystal healer = new EntityEnderCrystal(end());
			healer.setLocationAndAngles(10.5, 90.0, 30.5, 0.0F, 0.0F);
			end().spawnEntityInWorld(healer);
			return healer;
		});
		// it looks for the nearest crystal 1 tick in 10, then heals 1 each 10 ticks
		waitUntil("the dragon healing from it", () -> dragon.healingEnderCrystal == crystal, 120);
		float from = onServer(dragon::getHealth);
		List<String> samples = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			ticks(1);
			samples.add(onServer(() -> dragon.ticksExisted + ":" + dragon.getHealth() + (dragon.healingEnderCrystal == null ? "-" : "")));
		}
		float health = onServer(dragon::getHealth);
		assertTrue(health - from >= 4.0F && health - from <= 6.0F, "healed " + (health - from) + " in 50 ticks (from " + from + "): " + samples);
		onServer(() -> {
			crystal.setDead();
			dragon.setDead();
			player().capabilities.isFlying = false;
			player().sendPlayerAbilities();
		});
	}
}
