package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EndCrystals;
import ganymedes01.etfuturum.world.end.modern.EndSpikes;
import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The main island as 26.3's (SPEC-modern-movement.md §76.6, stage 1): the pillars in the seed's order and their
 * blocks, the end crystal's rules, the exit portal made by the fight's first scan at 26.3's height, the fight making
 * the dragon and showing its bar, and the dragon's death activating the portal with the egg.
 */
@TasSuite("end")
public class EndFightTests extends TasTestCase {

	/** 26.3's pillars at the test world's seed (the reference's dump): x, z, radius, height, caged. */
	private static final String PILLARS_26_3 = "[42 0 5 103, 33 24 2 76, 12 39 2 79 caged, -13 39 4 97, -34 24 3 88, "
		+ "-42 -1 4 100, -34 -25 3 91, -13 -40 3 85, 12 -40 2 82 caged, 33 -25 4 94]";
	/** 26.3's exit portal for the seed: its row at y 60 (the reference's `end-podium.txt`). */
	private static final int PODIUM_Y = 60;

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off");
		}
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

	/** The ten pillars for the seed: 26.3's places, sizes and cages. */
	public void testPillarsAs26_3() {
		String pillars = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (EndSpikes.Spike spike : EndSpikes.forSeed(end().getSeed())) {
				found.add(spike.x + " " + spike.z + " " + spike.radius + " " + spike.height + (spike.guarded ? " caged" : ""));
			}
			return found.toString();
		});
		assertEqual(PILLARS_26_3, pillars, "the pillars");
	}

	/** A caged pillar as generated (12, 39: radius 2, 79 high): obsidian, its cage, bedrock, fire and the crystal on it. */
	public void testPillarBlocks() {
		String problems = onServer(() -> {
			WorldServer end = end();
			for (int cx = -1; cx <= 2; cx++) {
				for (int cz = 1; cz <= 3; cz++) {
					end.theChunkProviderServer.loadChunk(cx, cz);
				}
			}
			List<String> wrong = new ArrayList<>();
			expect(wrong, end, 12, 78, 39, Blocks.obsidian, "the top obsidian");
			expect(wrong, end, 14, 70, 39, Blocks.obsidian, "obsidian 2 out (within radius^2 + 1)");
			expect(wrong, end, 12, 79, 39, Blocks.bedrock, "the crystal's bedrock");
			expect(wrong, end, 12, 80, 39, Blocks.fire, "the crystal's fire");
			expect(wrong, end, 14, 79, 39, Blocks.iron_bars, "the cage's side");
			expect(wrong, end, 12, 82, 39, Blocks.iron_bars, "the cage's top");
			expect(wrong, end, 13, 80, 39, Blocks.air, "inside the cage");
			int crystals = 0;
			for (Object entity : end.loadedEntityList) {
				if (entity instanceof EntityEnderCrystal crystal && Math.abs(crystal.posX - 12.5) < 0.01 && Math.abs(crystal.posZ - 39.5) < 0.01
					&& Math.abs(crystal.boundingBox.minY - 80.0) < 0.01) {
					crystals++;
				}
			}
			if (crystals != 1) {
				List<String> near = new ArrayList<>();
				for (Object entity : end.loadedEntityList) {
					if (entity instanceof EntityEnderCrystal crystal && crystal.getDistance(12.5, 80.0, 39.5) < 12.0) {
						near.add(String.format("crystal %.1f %.2f %.1f dead %s", crystal.posX, crystal.boundingBox.minY, crystal.posZ, crystal.isDead));
					}
					if (entity instanceof EntityDragon dragon) {
						near.add(String.format("dragon %.0f %.0f %.0f dead %s health %.0f", dragon.posX, dragon.posY, dragon.posZ, dragon.isDead, dragon.getHealth()));
					}
				}
				int inChunk = 0;
				for (java.util.List<?> list : end.getChunkFromChunkCoords(0, 2).entityLists) {
					inChunk += list.size();
				}
				wrong.add("chunk (0, 2) holds " + inChunk + " entities, (0, 1) populated " + end.getChunkFromChunkCoords(0, 1).isTerrainPopulated
					+ ", saving off " + end.levelSaving + ", world " + System.identityHashCode(end));
				wrong.add(crystals + " crystals standing at (12.5, 80, 39.5); near: " + near + "; loaded entities " + end.loadedEntityList.size()
					+ "; blocks " + Block.blockRegistry.getNameForObject(end.getBlock(12, 79, 39)) + " " + Block.blockRegistry.getNameForObject(end.getBlock(12, 80, 39)));
			}
			return wrong.toString();
		});
		assertEqual("[]", problems, "the caged pillar");
	}

	private static void expect(List<String> wrong, WorldServer world, int x, int y, int z, Block block, String what) {
		Block found = world.getBlock(x, y, z);
		if (found != block) {
			wrong.add(what + " at " + x + " " + y + " " + z + " is " + Block.blockRegistry.getNameForObject(found));
		}
	}

	/** One hit breaks a crystal, exploding; not the dragon's; an explosion breaks it without another (no chain). */
	public void testCrystalHits() {
		ExplosionCounter counter = new ExplosionCounter();
		int[] explosions = counter.count;
		MinecraftForge.EVENT_BUS.register(counter);
		try {
			String found = onServer(() -> {
				List<String> wrong = new ArrayList<>();
				EntityEnderCrystal hit = crystal(300.5, 40.0, 300.5);
				hit.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
				if (!hit.isDead || explosions[0] != 1) {
					wrong.add("a player's hit: dead " + hit.isDead + ", explosions " + explosions[0]);
				}
				EntityEnderCrystal dragons = crystal(320.5, 40.0, 300.5);
				EntityDragon dragon = new EntityDragon(player().worldObj);
				dragons.attackEntityFrom(new EntityDamageSource("mob", dragon), 10.0F);
				if (dragons.isDead) {
					wrong.add("the dragon broke a crystal");
				}
				dragons.setDead();
				EntityEnderCrystal blown = crystal(340.5, 40.0, 300.5);
				explosions[0] = 0;
				blown.attackEntityFrom(DamageSource.setExplosionSource(null), 10.0F);
				if (!blown.isDead || explosions[0] != 0) {
					wrong.add("an explosion's hit: dead " + blown.isDead + ", explosions " + explosions[0]);
				}
				return wrong.toString();
			});
			assertEqual("[]", found, "the crystal's hits");
		} finally {
			MinecraftForge.EVENT_BUS.unregister(counter);
		}
	}

	/** An invulnerable crystal (the respawn's) takes no survival hit; a creative player's still breaks it. */
	public void testInvulnerableCrystal() {
		String found = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			EntityEnderCrystal crystal = crystal(300.5, 40.0, 300.5);
			EndCrystals.setInvulnerable(crystal, true);
			player().setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
			crystal.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			if (crystal.isDead) {
				wrong.add("a survival hit broke it");
			}
			player().setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
			crystal.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			if (!crystal.isDead) {
				wrong.add("a creative hit didn't break it");
			}
			crystal.setDead();
			return wrong.toString();
		});
		assertEqual("[]", found, "the invulnerable crystal");
	}

	/** Its beam target and base saved as 26.3's (`beam_target`, `ShowBottom`) and read back. */
	public void testCrystalSaved() {
		String found = onServer(() -> {
			EntityEnderCrystal crystal = crystal(300.5, 40.0, 300.5);
			EndCrystals.setBeamTarget(crystal, new ChunkCoordinates(1, 128, -3));
			EndCrystals.setShowBottom(crystal, false);
			NBTTagCompound saved = new NBTTagCompound();
			crystal.writeToNBT(saved);
			crystal.setDead();
			EntityEnderCrystal read = new EntityEnderCrystal(player().worldObj);
			read.readFromNBT(saved);
			ChunkCoordinates target = EndCrystals.beamTarget(read);
			return (saved.hasKey("beam_target") ? "beam_target " : "") + (saved.hasKey("ShowBottom") ? "ShowBottom " : "")
				+ (target == null ? "no target" : target.posX + " " + target.posY + " " + target.posZ) + ", base " + EndCrystals.showsBottom(read);
		});
		assertEqual("beam_target ShowBottom 1 128 -3, base false", found, "the crystal saved and read");
	}

	/** In the End a crystal lights fire where it stands, only on air (1.7.10 replaced any block). */
	public void testCrystalFire() {
		toTheEnd();
		onServer(() -> {
			WorldServer end = end();
			end.setBlock(96, 50, 4, Blocks.glass);
			EntityEnderCrystal onGlass = new EntityEnderCrystal(end);
			onGlass.setLocationAndAngles(96.5, 50.0, 4.5, 0.0F, 0.0F);
			end.spawnEntityInWorld(onGlass);
			// on stone (fire needs something under it, in 26.3 too)
			end.setBlock(104, 49, 4, Blocks.stone);
			EntityEnderCrystal onAir = new EntityEnderCrystal(end);
			onAir.setLocationAndAngles(104.5, 50.0, 4.5, 0.0F, 0.0F);
			end.spawnEntityInWorld(onAir);
			return null;
		});
		serverTicks(5);
		String blocks = onServer(() -> Block.blockRegistry.getNameForObject(end().getBlock(96, 50, 4)) + " " + Block.blockRegistry.getNameForObject(end().getBlock(104, 50, 4)));
		assertEqual("minecraft:glass minecraft:fire", blocks, "under the crystal on glass, and on air");
		onServer(() -> {
			for (Object entity : new ArrayList<Object>(end().loadedEntityList)) {
				if (entity instanceof EntityEnderCrystal crystal && crystal.posY < 60 && crystal.posX > 90) {
					crystal.setDead();
				}
			}
			end().setBlockToAir(104, 50, 4);
			end().setBlockToAir(104, 49, 4);
			end().setBlockToAir(96, 50, 4);
		});
	}

	/**
	 * In order, in the one world the tests share: the first visit's scan makes the exit portal inactive at 26.3's
	 * height (row y 60 for the seed), makes one dragon and shows its bar to the player within 192 of (0, 128, 0); then
	 * the dragon's death activates the portal and puts the egg on its pillar (the first kill).
	 */
	public void testFirstVisitThenTheDeath() {
		toTheEnd();
		waitUntil("the exit portal", () -> end().getBlock(0, PODIUM_Y + 3, 0) == Blocks.bedrock, 200);
		String portal = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			WorldServer end = end();
			for (int y = PODIUM_Y; y <= PODIUM_Y + 3; y++) {
				expect(wrong, end, 0, y, 0, Blocks.bedrock, "the pillar");
			}
			expect(wrong, end, 3, PODIUM_Y, 0, Blocks.bedrock, "the rim");
			expect(wrong, end, 2, PODIUM_Y, 0, Blocks.air, "inside the rim (inactive)");
			expect(wrong, end, 3, PODIUM_Y - 1, 0, Blocks.end_stone, "under the rim");
			expect(wrong, end, 2, PODIUM_Y - 1, 0, Blocks.bedrock, "the floor");
			expect(wrong, end, 1, PODIUM_Y + 2, 0, Blocks.torch, "a torch");
			return wrong.toString();
		});
		assertEqual("[]", portal, "the inactive exit portal");
		waitUntil("one dragon", () -> dragons().size() == 1, 100);
		waitUntilOnClient("the dragon's bar", () -> BossStatus.bossName != null && BossStatus.statusBarTime > 0, 60);
		assertEqual(1, onServer(() -> dragons().size()), "dragons");
		onServer(() -> EnderDragonFight.of(end()).setDragonKilled(dragons().get(0)));
		String found = onServer(() -> {
			List<String> wrong = new ArrayList<>();
			WorldServer end = end();
			expect(wrong, end, 2, PODIUM_Y, 0, Blocks.end_portal, "inside the rim (active)");
			expect(wrong, end, 0, PODIUM_Y + 4, 0, Blocks.dragon_egg, "the egg on the pillar");
			return wrong.toString();
		});
		assertEqual("[]", found, "after the dragon's death");
	}

	/** 26.3's answers on the cage's gap and the exit portal's drops (the reference's `end-cage-podium.txt`). */
	private static final String CAGE_PODIUM = "/assets/etfuturum/unittas/end_cage_podium_20261029.txt";

	/** The dump's lines starting with the word, as one string (lines joined by " / "). */
	private static String fromDump(String... words) throws java.io.IOException {
		List<String> found = new ArrayList<>();
		try (java.io.InputStream in = EndFightTests.class.getResourceAsStream(CAGE_PODIUM)) {
			assertTrue(in != null, "no " + CAGE_PODIUM);
			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				for (String word : words) {
					if (line.startsWith(word + " ")) {
						found.add(line);
					}
				}
			}
		}
		return String.join(" / ", found);
	}

	/**
	 * A caged pillar's corner gap (the user, 2026-11-04: in 26.3 an arrow shot at the gap under a cage reaches its
	 * crystal). A copy of the seed's caged pillar (radius 2, 79 high) in the void at (300, 300); under the cage's
	 * corners there's no obsidian (a radius-2 disc leaves them out), and the corner bar is an L, open on its inside
	 * quarter. The line from the corner column up through that quarter to inside the cage, traced as an arrow traces
	 * blocks; then an arrow fired along it, and whether the crystal broke. As 26.3's.
	 */
	public void testArrowThroughTheCageGap() throws Exception {
		String found = onServer(() -> {
			WorldServer end = end();
			int cx = 300;
			int cz = 300;
			int height = 79;
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					end.theChunkProviderServer.loadChunk((cx >> 4) + x, (cz >> 4) + z);
				}
			}
			EndSpikes.place(end, new EndSpikes.Spike(cx, cz, 2, height, true), new java.util.Random(1L), false, null);
			double[] from = {cx + 2.38, height - 1.5, cz + 2.33};
			double[] to = {cx + 1.78, height + 1.5, cz + 1.75};
			net.minecraft.util.MovingObjectPosition clip = end.func_147447_a(net.minecraft.util.Vec3.createVectorHelper(from[0], from[1], from[2]),
				net.minecraft.util.Vec3.createVectorHelper(to[0], to[1], to[2]), false, true, false);
			StringBuilder out = new StringBuilder("clip ").append(clip == null ? "miss" : "block " + (clip.blockX - cx) + " " + (clip.blockY - height) + " " + (clip.blockZ - cz));
			@SuppressWarnings("unchecked")
			List<EntityEnderCrystal> crystals = end.getEntitiesWithinAABB(EntityEnderCrystal.class,
				net.minecraft.util.AxisAlignedBB.getBoundingBox(cx - 3, height, cz - 3, cx + 4, height + 5, cz + 4));
			net.minecraft.entity.projectile.EntityArrow arrow = new net.minecraft.entity.projectile.EntityArrow(end);
			arrow.setPosition(from[0], from[1], from[2]);
			double dx = to[0] - from[0];
			double dy = to[1] - from[1];
			double dz = to[2] - from[2];
			double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
			arrow.motionX = dx / length * 2.0;
			arrow.motionY = dy / length * 2.0;
			arrow.motionZ = dz / length * 2.0;
			end.spawnEntityInWorld(arrow);
			boolean broken = false;
			if (!crystals.isEmpty()) {
				for (int i = 0; i < 10 && !arrow.isDead && !crystals.get(0).isDead; i++) {
					arrow.onUpdate();
				}
				broken = crystals.get(0).isDead;
			}
			out.append(" / crystals ").append(crystals.size()).append(", after the arrow broken ").append(broken);
			arrow.setDead();
			for (EntityEnderCrystal crystal : crystals) {
				crystal.setDead();
			}
			for (int x = cx - 4; x <= cx + 4; x++) {
				for (int z = cz - 4; z <= cz + 4; z++) {
					for (int y = 0; y <= height + 8; y++) {
						end.setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			return out.toString();
		});
		assertEqual(fromDump("clip", "crystals"), found, "the cage's corner gap (26.3's: the clip, then an arrow along it)");
	}

	/**
	 * A lone iron bar is traced as the post it is (26.3's, and EFR's thin panes: 2 by 2 pixels in the middle), not as
	 * 1.7.10's cross: a line down through its cell 0.35 from the middle misses it, one down the middle hits it.
	 */
	public void testLonePaneTracedAsItsPost() {
		if (!ganymedes01.etfuturum.configuration.configs.ConfigMixins.thinPanes) {
			skipTest("thinPanes is off (1.7.10's lone pane is a cross)");
		}
		String found = onServer(() -> {
			WorldServer world = (WorldServer) player().worldObj;
			int x = (int) Math.floor(player().posX) + 20;
			int y = (int) Math.floor(player().posY) + 5;
			int z = (int) Math.floor(player().posZ) + 20;
			world.setBlock(x, y, z, Blocks.iron_bars);
			net.minecraft.util.MovingObjectPosition aside = world.func_147447_a(net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y + 2.0, z + 0.15),
				net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y - 1.0, z + 0.15), false, true, false);
			net.minecraft.util.MovingObjectPosition middle = world.func_147447_a(net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y + 2.0, z + 0.5),
				net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y - 1.0, z + 0.5), false, true, false);
			world.setBlockToAir(x, y, z);
			return "aside " + (aside == null ? "miss" : "hit") + ", middle " + (middle == null ? "miss" : "hit");
		});
		assertEqual("aside miss, middle hit", found, "lines down through a lone iron bar's cell");
	}

	/**
	 * The exit portal's activation drops what 26.3's does (the user, 2026-11-04: 4 bedrock dropped when they killed the
	 * dragon; 26.3's bedrock has no loot): one placed inactive, then active, at (-300, 70, 300), the items around it.
	 */
	public void testPodiumActivationDrops() throws Exception {
		String found = onServer(() -> {
			WorldServer end = end();
			int ox = -300;
			int oy = 70;
			int oz = 300;
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					end.theChunkProviderServer.loadChunk((ox >> 4) + x, (oz >> 4) + z);
				}
			}
			ganymedes01.etfuturum.world.end.modern.ModernEndPodium.place(end, ox, oy, oz, false);
			ganymedes01.etfuturum.world.end.modern.ModernEndPodium.place(end, ox, oy, oz, true);
			java.util.TreeMap<String, Integer> drops = new java.util.TreeMap<>();
			for (Object entity : end.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
				net.minecraft.util.AxisAlignedBB.getBoundingBox(ox - 8, oy - 8, oz - 8, ox + 9, oy + 9, oz + 9))) {
				net.minecraft.entity.item.EntityItem item = (net.minecraft.entity.item.EntityItem) entity;
				drops.merge(net.minecraft.item.Item.itemRegistry.getNameForObject(item.getEntityItem().getItem()), item.getEntityItem().stackSize, Integer::sum);
				item.setDead();
			}
			for (int x = ox - 4; x <= ox + 4; x++) {
				for (int z = oz - 4; z <= oz + 4; z++) {
					for (int y = oy - 2; y <= oy + 5; y++) {
						end.setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			StringBuilder out = new StringBuilder("podium drops");
			if (drops.isEmpty()) {
				out.append(" none");
			}
			for (java.util.Map.Entry<String, Integer> drop : drops.entrySet()) {
				out.append(' ').append(drop.getKey()).append(' ').append(drop.getValue());
			}
			return out.toString();
		});
		assertEqual(fromDump("podium"), found, "the exit portal's activation drops (26.3's)");
	}

	/** Counts explosions (Forge's bus needs a public class). */
	public static final class ExplosionCounter {
		final int[] count = new int[1];

		@cpw.mods.fml.common.eventhandler.SubscribeEvent
		public void onExplosion(ExplosionEvent.Start event) {
			count[0]++;
		}
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

	private EntityEnderCrystal crystal(double x, double y, double z) {
		EntityEnderCrystal crystal = new EntityEnderCrystal(player().worldObj);
		crystal.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
		player().worldObj.spawnEntityInWorld(crystal);
		return crystal;
	}
}
