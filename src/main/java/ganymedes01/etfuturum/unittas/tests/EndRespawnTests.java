package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EndCrystals;
import ganymedes01.etfuturum.world.end.modern.EndSpikes;
import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Respawning the dragon as 26.3 does (SPEC-modern-movement.md §76.9, stage 4): the crystal item, the four on the exit
 * portal starting it, every stage (the beams, each pillar's top cleared and rebuilt, the summoning), and breaking one
 * of the four stopping it.
 */
public class EndRespawnTests extends TasTestCase {

	private static final int PODIUM_Y = 60;
	/** The four places 3 out from the portal's middle, one up. */
	private static final int[][] RIM = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive() || !ModItems.END_CRYSTAL.isEnabled()) {
			skipTest("modernEnd or the end crystal is off");
		}
	}

	@Override
	protected int timeoutSeconds() {
		return 150;
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

	/** The dragon dead: every dragon flying goes (an earlier test's too), and the fight is told, as a death does. */
	private void dragonDead() {
		toTheEnd();
		if (!onServer(() -> EnderDragonFight.of(end()).dragonKilled())) {
			waitUntil("the dragon", () -> !dragons().isEmpty(), 300);
		}
		onServer(() -> {
			for (EntityDragon dragon : dragons()) {
				dragon.setDead();
				if (!EnderDragonFight.of(end()).dragonKilled()) {
					// the fight counts only its own dragon's death
					EnderDragonFight.of(end()).useDragon(dragon);
					EnderDragonFight.of(end()).setDragonKilled(dragon);
				}
			}
		});
		// a dead dragon stays listed until the next tick: its parts would fill the crystals' room on the portal
		ticks(3);
	}

	private static List<EntityDragon> dragons() {
		List<EntityDragon> found = new ArrayList<>();
		for (Object entity : end().loadedEntityList) {
			if (entity instanceof EntityDragon dragon && !dragon.isDead) {
				found.add(dragon);
			}
		}
		return found;
	}

	/**
	 * The four places cleared: crystals light fire where they stand (26.3's too), a crystal needs air above and no
	 * entity there, and an earlier test's explosion drops things on the rim.
	 */
	private static void clearRim() {
		for (int[] at : RIM) {
			if (end().getBlock(at[0], PODIUM_Y + 1, at[1]) == Blocks.fire) {
				end().setBlockToAir(at[0], PODIUM_Y + 1, at[1]);
			}
		}
		for (Object entity : new ArrayList<Object>(end().loadedEntityList)) {
			if ((entity instanceof EntityItem || entity instanceof net.minecraft.entity.item.EntityXPOrb) && ((net.minecraft.entity.Entity) entity).getDistance(0.5, PODIUM_Y + 1, 0.5) < 8.0) {
				((net.minecraft.entity.Entity) entity).setDead();
			}
		}
	}

	/** Uses a crystal from the player's hand on the block's top. */
	private static boolean use(World world, int x, int y, int z) {
		ItemStack stack = new ItemStack(ModItems.END_CRYSTAL.get(), 4);
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		return stack.getItem().onItemUse(stack, player(), world, x, y, z, 1, 0.5F, 1.0F, 0.5F);
	}

	private static List<EntityEnderCrystal> crystalsAt(World world, double x, double feetY, double z) {
		List<EntityEnderCrystal> found = new ArrayList<>();
		for (Object entity : world.loadedEntityList) {
			if (entity instanceof EntityEnderCrystal crystal && !crystal.isDead && Math.abs(crystal.posX - x) < 0.01 && Math.abs(crystal.posZ - z) < 0.01
				&& Math.abs(crystal.boundingBox.minY - feetY) < 0.01) {
				found.add(crystal);
			}
		}
		return found;
	}

	/** On obsidian or bedrock with room above: a crystal without its base, one item used (none in creative). */
	public void testCrystalItem() {
		String found = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			World world = player().worldObj;
			world.setBlock(300, 30, 300, Blocks.obsidian);
			world.setBlock(302, 30, 300, Blocks.stone);
			world.setBlock(304, 30, 300, Blocks.bedrock);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			if (!use(world, 300, 30, 300) || crystalsAt(world, 300.5, 31.0, 300.5).size() != 1) {
				wrong.add("no crystal on obsidian");
			} else if (EndCrystals.showsBottom(crystalsAt(world, 300.5, 31.0, 300.5).get(0))) {
				wrong.add("its base shows");
			}
			if (player().getHeldItem() == null || player().getHeldItem().stackSize != 3) {
				wrong.add("survival: " + player().getHeldItem() + " left of 4");
			}
			if (use(world, 302, 30, 300) || !crystalsAt(world, 302.5, 31.0, 300.5).isEmpty()) {
				wrong.add("a crystal on stone");
			}
			EntityItem blocker = new EntityItem(world, 304.5, 31.2, 300.5, new ItemStack(Blocks.dirt));
			blocker.delayBeforeCanPickup = 32767;
			world.spawnEntityInWorld(blocker);
			if (use(world, 304, 30, 300) || !crystalsAt(world, 304.5, 31.0, 300.5).isEmpty()) {
				wrong.add("a crystal over an entity");
			}
			blocker.setDead();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			// a fresh block: the dead blocker counts until the next tick
			world.setBlock(306, 30, 300, Blocks.bedrock);
			if (!use(world, 306, 30, 300) || player().getHeldItem().stackSize != 4) {
				wrong.add("creative: " + player().getHeldItem() + " left of 4");
			}
			for (Object entity : new ArrayList<Object>(world.loadedEntityList)) {
				if (entity instanceof EntityEnderCrystal crystal && crystal.posY < 40 && crystal.posX > 290) {
					crystal.setDead();
				}
			}
			for (int x = 300; x <= 306; x++) {
				world.setBlockToAir(x, 30, 300);
			}
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			return wrong.toString();
		});
		assertEqual("[]", found, "the crystal item");
	}

	/** The four on the portal start it; breaking one stops it: the portal active again, the pillars' crystals reset. */
	public void testRespawnStops() {
		dragonDead();
		onServer(EndRespawnTests::clearRim);
		ticks(2);
		onServer(() -> {
			for (int[] at : RIM) {
				use(end(), at[0], PODIUM_Y, at[1]);
			}
		});
		ticks(3);
		assertTrue(onServer(() -> crystalsAt(end(), 3.5, PODIUM_Y + 1, 0.5).size() == 1), "a crystal on the rim");
		String started = onServer(() -> {
			ChunkCoordinates target = EndCrystals.beamTarget(crystalsAt(end(), 3.5, PODIUM_Y + 1, 0.5).get(0));
			return target == null ? "no beam" : target.posX + " " + target.posY + " " + target.posZ;
		});
		assertEqual("0 128 0", started, "the four's beams at the start");
		onServer(() -> crystalsAt(end(), 3.5, PODIUM_Y + 1, 0.5).get(0).attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		ticks(2);
		String after = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			// the far side: the broken crystal's explosion blasts the near side's new portal blocks, as 26.3's does (its
			// blocks are hit after the entities, whatever stands there by then)
			if (end().getBlock(-2, PODIUM_Y, 0) != Blocks.end_portal) {
				wrong.add("the portal isn't active again");
			}
			if (EnderDragonFight.of(end()).respawnStage() != null) {
				wrong.add("the respawn still runs: " + EnderDragonFight.of(end()).respawnStage());
			}
			for (Object entity : end().loadedEntityList) {
				if (entity instanceof EntityEnderCrystal crystal && !crystal.isDead && crystal.posY > 70
					&& (EndCrystals.beamTarget(crystal) != null || ((io.github.asgerjon.mcfly.mixins.early.modernend.AccessorEntityInvulnerable) crystal).etfu$isInvulnerable())) {
					wrong.add("a pillar's crystal still beaming or invulnerable at " + (int) crystal.posX + " " + (int) crystal.posZ);
				}
			}
			// the other three stay (26.3 leaves them); cleared for the next test
			for (int[] at : RIM) {
				for (EntityEnderCrystal crystal : crystalsAt(end(), at[0] + 0.5, PODIUM_Y + 1, at[1] + 0.5)) {
					crystal.setDead();
				}
			}
			return wrong.toString();
		});
		assertEqual("[]", after, "after breaking one of the four");
	}

	/**
	 * The whole respawn: the beams at the sky and the portal remade inactive; each pillar's top cleared and rebuilt with
	 * an invulnerable crystal beaming at (0, 128, 0); then the four explode and go, the pillars' crystals are reset, and
	 * there's a new dragon.
	 */
	public void testRespawnWholeSequence() {
		dragonDead();
		onServer(EndRespawnTests::clearRim);
		ticks(2);
		onServer(() -> {
			for (int[] at : RIM) {
				use(end(), at[0], PODIUM_Y, at[1]);
			}
		});
		ticks(3);
		assertTrue(onServer(() -> EnderDragonFight.of(end()).respawnStage() != null), "the respawn started");
		assertEqual("air", onServer(() -> end().getBlock(-2, PODIUM_Y, 0) == Blocks.air ? "air" : end().getBlock(-2, PODIUM_Y, 0).getUnlocalizedName()),
			"inside the rim once it starts (remade inactive)");
		// the first pillar in the seed's order rebuilt: its crystal invulnerable, beaming at (0, 128, 0)
		EndSpikes.Spike first = onServer(() -> EndSpikes.forSeed(end().getSeed()).get(0));
		waitUntil("the first pillar rebuilt", () -> {
			List<EntityEnderCrystal> on = crystalsAt(end(), first.x + 0.5, first.height + 1, first.z + 0.5);
			if (on.isEmpty()) {
				return false;
			}
			ChunkCoordinates target = EndCrystals.beamTarget(on.get(0));
			return target != null && target.posY == 128 && ((io.github.asgerjon.mcfly.mixins.early.modernend.AccessorEntityInvulnerable) on.get(0)).etfu$isInvulnerable();
		}, 260);
		waitUntil("the new dragon", () -> !dragons().isEmpty(), 800);
		String after = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			for (int[] at : RIM) {
				if (!crystalsAt(end(), at[0] + 0.5, PODIUM_Y + 1, at[1] + 0.5).isEmpty()) {
					wrong.add("a crystal still on the rim at " + at[0] + " " + at[1]);
				}
			}
			for (EndSpikes.Spike spike : EndSpikes.forSeed(end().getSeed())) {
				List<EntityEnderCrystal> on = crystalsAt(end(), spike.x + 0.5, spike.height + 1, spike.z + 0.5);
				if (on.size() != 1) {
					wrong.add(on.size() + " crystals on the pillar at " + spike.x + " " + spike.z);
				} else if (EndCrystals.beamTarget(on.get(0)) != null || ((io.github.asgerjon.mcfly.mixins.early.modernend.AccessorEntityInvulnerable) on.get(0)).etfu$isInvulnerable()) {
					wrong.add("the pillar's crystal at " + spike.x + " " + spike.z + " still beaming or invulnerable");
				}
			}
			if (EnderDragonFight.of(end()).dragonKilled()) {
				wrong.add("the fight thinks the dragon is still dead");
			}
			if (dragons().size() != 1) {
				wrong.add(dragons().size() + " dragons");
			}
			return wrong.toString();
		});
		assertEqual("[]", after, "after the respawn");
	}
}
