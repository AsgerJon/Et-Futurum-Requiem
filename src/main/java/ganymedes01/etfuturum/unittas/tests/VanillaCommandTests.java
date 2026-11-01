package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasCommand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.potion.Potion;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldSettings;

import java.util.List;

/** 1.7.10's vanilla commands in modern form (spec §51). */
public class VanillaCommandTests extends TasTestCase {

	private String name() {
		return playerName();
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().getWorldInfo().setRaining(false);
			world().getWorldInfo().setThundering(false);
			world().getGameRules().setOrCreateGameRule("keepInventory", "false");
			world().setWorldTime(6000);
		});
	}

	// ---------------------------------------------------------------- /teleport

	public void testTeleportToLocation() {
		assertSaid(command("/tp 10 4 10"), "Teleported " + name() + " to 10.500000, 4.000000, 10.500000");
		ticks(2);
		assertAlmostEqual(10.5, onServer(() -> player().posX), 0.01);
		assertAlmostEqual(10.5, onServer(() -> player().posZ), 0.01);
		command("/tp ~5 ~ ~");
		ticks(2);
		assertAlmostEqual(15.5, onServer(() -> player().posX), 0.01);
	}

	public void testTeleportEntityToPlayer() {
		EntityCow cow = spawn(EntityCow::new, 20.5, 4.0, 0.5);
		assertSaid(command("/tp @e[type=cow] " + name()), "Teleported Cow to " + name());
		assertAlmostEqual(0.5, onServer(() -> cow.posX), 0.01);
	}

	public void testTeleportRotation() {
		command("/tp @s 0 4 0 90 0");
		ticks(2);
		assertAlmostEqual(90.0, onClient(() -> (double) mc().thePlayer.rotationYaw), 0.1);
		command("/tp @s 0 4 0 facing 0 4 10");
		ticks(2);
		assertAlmostEqual(0.0, onClient(() -> (double) mc().thePlayer.rotationYaw), 0.1);
	}

	public void testTeleportErrors() {
		assertError(command("/tp 40000000 4 0"), "Invalid position for teleport");
	}

	// ---------------------------------------------------------------- /kill, /tag

	public void testKill() {
		spawn(EntityCow::new, 3.5, 4.0, 0.5);
		spawn(EntityCow::new, -3.5, 4.0, 0.5);
		assertSaid(command("/kill @e[type=cow]"), "Killed 2 entities");
		ticks(2);
		assertTrue(onServer(() -> entitiesNear(EntityCow.class, 16.0).stream().noneMatch(cow -> cow.isEntityAlive())), "a cow lives");
	}

	public void testTag() {
		EntityCow cow = spawn(EntityCow::new, 3.5, 4.0, 0.5);
		assertSaid(command("/tag @e[type=cow] add pet"), "Added tag 'pet' to Cow");
		assertError(command("/tag @e[type=cow] add pet"), "Target either already has the tag or has too many tags");
		assertSaid(command("/tag @e[type=cow] list"), "Cow has 1 tag(s): pet");
		command("/effect give @e[tag=pet] speed");
		assertTrue(onServer(() -> cow.isPotionActive(Potion.moveSpeed)), "tag=pet didn't find the cow");
		assertFalse(onServer(() -> player().isPotionActive(Potion.moveSpeed)), "tag=pet found the player");
		assertSaid(command("/tag @e[type=cow] remove pet"), "Removed tag 'pet' from Cow");
		assertError(command("/tag @e[type=cow] remove pet"), "Target does not have this tag");
	}

	// ---------------------------------------------------------------- game mode, difficulty, rules

	public void testGameMode() {
		assertSaid(command("/gamemode survival"), "Set own game mode to Survival Mode");
		assertEqual(WorldSettings.GameType.SURVIVAL, onServer(() -> player().theItemInWorldManager.getGameType()));
		command("/gamemode 1");
		assertEqual(WorldSettings.GameType.CREATIVE, onServer(() -> player().theItemInWorldManager.getGameType()), "1.7.10's form");
		assertError(command("/gamemode nope"), "Unknown game mode: nope");
	}

	public void testDefaultGameMode() {
		assertSaid(command("/defaultgamemode adventure"), "The default game mode is now Adventure Mode");
		command("/defaultgamemode creative");
	}

	public void testDifficulty() {
		assertSaid(command("/difficulty"), "The difficulty is Easy");
		assertSaid(command("/difficulty normal"), "The difficulty has been set to Normal");
		assertError(command("/difficulty normal"), "The difficulty did not change; it is already set to Normal");
	}

	public void testGameRule() {
		assertSaid(command("/gamerule keep_inventory true"), "Game rule keep_inventory is now set to true");
		assertEqual("true", onServer(() -> world().getGameRules().getGameRuleStringValue("keepInventory")));
		assertSaid(command("/gamerule keep_inventory"), "Game rule keep_inventory is currently set to true");
		assertSaid(command("/gamerule fire_spread_radius_around_player"), "Game rule fire_spread_radius_around_player is currently set to 0");
	}

	// ---------------------------------------------------------------- time, weather

	public void testTime() {
		assertSaid(command("/time set noon"), "Set the time to 6000");
		command("/time set 100t");
		assertEqual(100L, onServer(() -> world().getWorldTime() % 24000L));
		command("/time add 1d");
		assertSaid(command("/time query daytime"), "The time is 100");
		assertSaid(command("/time query day"), "The time is 1");
		assertError(command("/time set 5x"), "Invalid unit");
	}

	public void testWeather() {
		assertSaid(command("/weather rain"), "Set the weather to rain");
		assertTrue(onServer(() -> world().getWorldInfo().isRaining()), "not raining");
		// read in the same tick: the weather timer counts down every tick
		int rainTime = onServer(() -> {
			runCommand("/weather clear 10s");
			return world().getWorldInfo().getRainTime();
		});
		assertFalse(onServer(() -> world().getWorldInfo().isRaining()), "still raining");
		assertEqual(200, rainTime);
	}

	// ---------------------------------------------------------------- experience, clear, spawn points

	public void testExperience() {
		onServer(() -> {
			player().experienceLevel = 0;
			player().experience = 0.0F;
		});
		assertSaid(command("/xp add @s 5 levels"), "Gave 5 experience levels to " + name());
		assertSaid(command("/experience query @s levels"), name() + " has 5 experience levels");
		assertError(command("/experience set @s 99999 points"), "Cannot set experience points above the maximum points for the player's current level");
		command("/xp 3L " + name());
		assertEqual(8, onServer(() -> player().experienceLevel), "1.7.10's form");
	}

	public void testClear() {
		command("/give @s stone 10");
		command("/give @s dirt 5");
		assertSaid(command("/clear @s stone"), "Removed 10 item(s) from player " + name());
		assertSaid(command("/clear @s dirt 0"), "Found 5 matching item(s) on player " + name());
		assertSaid(command("/clear @s dirt 2"), "Removed 2 item(s) from player " + name());
		int left = 0;
		for (net.minecraft.item.ItemStack stack : inventory()) {
			left += stack.stackSize;
		}
		assertEqual(3, left, "dirt left");
		command("/clear");
		assertError(command("/clear @s"), "No items were found on player " + name());
	}

	public void testSpawnPoints() {
		assertSaid(command("/spawnpoint @s 5 4 5"), "Set spawn point to 5, 4, 5 [0.0] in minecraft:overworld for " + name());
		ChunkCoordinates bed = onServer(() -> player().getBedLocation(0));
		assertEqual(5, bed.posX);
		assertSaid(command("/setworldspawn 0 4 0"), "Set the world spawn point to 0, 4, 0 [0.0]");
		assertEqual(0, onServer(() -> world().getWorldInfo().getSpawnX()));
	}

	// ---------------------------------------------------------------- chat

	public void testSayMeMsg() {
		assertSaid(command("/say hello"), "[" + name() + "] hello");
		assertSaid(command("/say hi @p"), "[" + name() + "] hi " + name());
		assertSaid(command("/me waves"), "* " + name() + " waves");
		TasCommand whisper = command("/msg @s psst");
		assertSaid(whisper, name() + " whispers to you: psst");
		assertSaid(whisper, "You whisper to " + name() + ": psst");
		assertSaid(command("/tell @s again"), "whispers to you: again");
	}

	public void testTellRaw() {
		// gold, as unitTAS takes a red line for an error
		TasCommand gold = command("/tellraw @s {text:\"gold text\",color:\"gold\"}");
		assertSaid(gold, "\u00a76gold text");
		assertNoError(gold);
		assertSaid(command("/tellraw @s {\"text\":\"json\"}"), "json");
	}

	public void testSeedListHelp() {
		assertSaid(command("/seed"), "Seed: [20261029]");
		assertSaid(command("/list"), "players online: " + name());
		List<String> help = command("/help").lines();
		assertTrue(help.stream().anyMatch(line -> line.startsWith("/teleport")), "no /teleport in the help");
		assertTrue(command("/help give").lines().stream().anyMatch(line -> line.startsWith("/give")), "no /give usage");
		assertError(command("/help nosuchcommand"), "Unknown command or insufficient permissions");
	}
}
