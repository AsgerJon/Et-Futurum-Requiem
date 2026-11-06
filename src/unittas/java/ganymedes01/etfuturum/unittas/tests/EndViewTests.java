package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.world.WorldSettings;

/**
 * The End beside 26.3's in the comparison (SPEC-modern-movement.md §76): arriving on the platform, facing west to the
 * main island (`end.arrival`), a caged pillar's top (`end.spike_cage`), the exit portal (`end.podium`), and the
 * dragon's respawn 40 and 120 ticks in (`end.respawn_start`, `end.respawn_pillar`: the beams at the sky, then at the
 * first pillar), the dragon perched (`end.dragon_perched`), a gateway's beam at full height (`end.gateway`), a dragon head (`end.dragon_head`), shulkers closed, open and red (`end.shulker`), the outer islands' chorus (`end.outer_islands`), and the first end city (`end.city`).
 */
@TasSuite("end")
public class EndViewTests extends ComparisonTestCase {

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off");
		}
	}

	/** The chorus chunk 26.3's feature dump names (EndFeatureTests'), or null. */
	private static int[] chorusChunk() throws java.io.IOException {
		try (java.io.InputStream in = EndViewTests.class.getResourceAsStream("/assets/etfuturum/unittas/end_features_20261029.txt")) {
			if (in == null) {
				return null;
			}
			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				String[] parts = line.split(" ");
				if (line.startsWith("chunk ") && parts[3].equals("chorus")) {
					return new int[] {Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
				}
			}
		}
		return null;
	}

	/** The first end city's start chunk in 26.3's city dump (EndCityTests'), or null. */
	private static int[] cityChunk() throws java.io.IOException {
		try (java.io.InputStream in = EndViewTests.class.getResourceAsStream("/assets/etfuturum/unittas/end_city_20261029.txt")) {
			if (in == null) {
				return null;
			}
			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (line.startsWith("start ")) {
					String[] parts = line.split(" ");
					return new int[] {Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
				}
			}
		}
		return null;
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			onServer(() -> player().travelToDimension(1));
			waitUntilOnClient("the End", () -> mc().thePlayer.dimension == 1 && !(mc().currentScreen instanceof GuiDownloadTerrain), 200);
			look(90.0F, 0.0F);
			// the island's chunks drawn
			ticks(80);
			comparisonShot("end.arrival");
			// a caged pillar's top from 8 out and 2 up: the first caged one in the seed's order
			int[] cage = onServer(() -> {
				for (ganymedes01.etfuturum.world.end.modern.EndSpikes.Spike spike : ganymedes01.etfuturum.world.end.modern.EndSpikes.forSeed(player().worldObj.getSeed())) {
					if (spike.guarded) {
						return new int[] {spike.x, spike.height, spike.z};
					}
				}
				return null;
			});
			double eyeX = cage[0] + 8.5;
			double eyeY = cage[1] + 3.0;
			double eyeZ = cage[2] + 8.5;
			double dx = cage[0] + 0.5 - eyeX;
			double dy = cage[1] + 2.0 - eyeY;
			double dz = cage[2] + 0.5 - eyeZ;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
			// put in the air first, then flying (1.7.10 turns flying off for a player on the ground), so it stays there
			onServer(() -> {
				player().playerNetServerHandler.setPlayerLocation(eyeX, eyeY - 1.62, eyeZ, yaw, pitch);
				player().capabilities.isFlying = true;
				player().sendPlayerAbilities();
			});
			ticks(60);
			look(yaw, pitch);
			comparisonShot("end.spike_cage");
			// the exit portal from the south, 10 out and 5 up, looking down at it
			int podium = onServer(() -> {
				int top = -1;
				for (int y = 0; y < 128; y++) {
					if (player().worldObj.getBlock(0, y, 0) == net.minecraft.init.Blocks.bedrock) {
						top = y;
					}
				}
				return top - 3;
			});
			onServer(() -> player().playerNetServerHandler.setPlayerLocation(0.5, podium + 5, 10.5, 180.0F, 25.0F));
			ticks(40);
			look(180.0F, 25.0F);
			comparisonShot("end.podium");
			// the respawn (spec §76.9): the dragon gone, four crystals on the portal's rim, seen from the south looking up
			onServer(() -> {
				net.minecraft.world.World end = player().worldObj;
				ganymedes01.etfuturum.world.end.modern.EnderDragonFight fight = ganymedes01.etfuturum.world.end.modern.EnderDragonFight.of(end);
				for (Object entity : new java.util.ArrayList<Object>(end.loadedEntityList)) {
					if (entity instanceof net.minecraft.entity.boss.EntityDragon dragon && !dragon.isDead) {
						dragon.setDead();
						if (!fight.dragonKilled()) {
							fight.setDragonKilled(dragon);
						}
					}
				}
				player().playerNetServerHandler.setPlayerLocation(0.5, podium + 4 - 1.62, 40.5, 180.0F, -45.0F);
				int[][] rim = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
				net.minecraft.item.ItemStack crystals = new net.minecraft.item.ItemStack(io.github.asgerjon.mcfly.registry.McFlyItems.END_CRYSTAL.get(), 4);
				for (int[] at : rim) {
					if (end.getBlock(at[0], podium + 1, at[1]) == net.minecraft.init.Blocks.fire) {
						end.setBlockToAir(at[0], podium + 1, at[1]);
					}
					crystals.getItem().onItemUse(crystals, player(), end, at[0], podium, at[1], 1, 0.5F, 1.0F, 0.5F);
				}
			});
			ticks(40);
			look(180.0F, -45.0F);
			comparisonShot("end.respawn_start");
			ticks(80);
			look(180.0F, -45.0F);
			comparisonShot("end.respawn_pillar");
			// the dragon perched on the portal, facing south, seen from 16 out and 4 above its top (spec §76.7)
			onServer(() -> {
				net.minecraft.world.World end = player().worldObj;
				net.minecraft.entity.boss.EntityDragon dragon = new net.minecraft.entity.boss.EntityDragon(end);
				dragon.setLocationAndAngles(0.5, podium + 4, 0.5, 180.0F, 0.0F);
				end.spawnEntityInWorld(dragon);
				ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon.of(dragon).setPhase(ganymedes01.etfuturum.world.end.modern.dragon.DragonPhase.SITTING_SCANNING);
				player().playerNetServerHandler.setPlayerLocation(0.5, podium + 8 - 1.62, 16.5, 180.0F, 15.0F);
			});
			ticks(40);
			look(180.0F, 15.0F);
			comparisonShot("end.dragon_perched");
			// a gateway (spec §76.10) 8 east of the arrival platform, from the platform facing it, 100 ticks after it
			// appears: its beam at full height (the top of its sine, so a tick either way doesn't show)
			onServer(() -> {
				for (Object entity : new java.util.ArrayList<Object>(player().worldObj.loadedEntityList)) {
					if (entity instanceof net.minecraft.entity.boss.EntityDragon dragon) {
						dragon.setDead();
					}
				}
				ganymedes01.etfuturum.world.end.modern.EndGateways.placeShell(player().worldObj, 108, 51, 0, null, false);
				player().playerNetServerHandler.setPlayerLocation(100.5, 49.0, 0.5, -90.0F, 0.0F);
			});
			ticks(100);
			look(-90.0F, 0.0F);
			comparisonShot("end.gateway");
			// a dragon head (spec §76.12, 7d) on the platform 2 west, facing east at the player on the platform looking down at it
			onServer(() -> {
				player().worldObj.setBlock(98, 49, 0, net.minecraft.init.Blocks.skull, 1, 3);
				net.minecraft.tileentity.TileEntitySkull head = (net.minecraft.tileentity.TileEntitySkull) player().worldObj.getTileEntity(98, 49, 0);
				head.func_152107_a(5);
				head.func_145903_a(12);
				player().worldObj.markBlockForUpdate(98, 49, 0);
				player().playerNetServerHandler.setPlayerLocation(100.5, 49.0, 0.5, 90.0F, 30.0F);
			});
			ticks(40);
			look(90.0F, 30.0F);
			comparisonShot("end.dragon_head");
			onServer(() -> player().worldObj.setBlockToAir(98, 49, 0));
			// shulkers (spec §76.13) on the platform's west edge, without AI, their heads south: closed, fully open,
			// red; from the platform looking west and down at them
			onServer(() -> {
				net.minecraft.world.World end = player().worldObj;
				for (int i = 0; i < 3; i++) {
					ganymedes01.etfuturum.entities.EntityShulker shulker = new ganymedes01.etfuturum.entities.EntityShulker(end);
					shulker.setLocationAndAngles(98.5, 49.0, -1.5 + i * 2, 0.0F, 0.0F);
					net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
					shulker.writeToNBT(tag);
					tag.setBoolean("NoAI", true);
					shulker.readFromNBT(tag);
					shulker.rotationYawHead = 0.0F;
					if (i == 1) {
						shulker.setRawPeekAmount(100);
					}
					if (i == 2) {
						shulker.setColor((byte) 14);
					}
					end.spawnEntityInWorld(shulker);
				}
				player().playerNetServerHandler.setPlayerLocation(100.5, 49.0, 0.5, 90.0F, 20.0F);
			});
			ticks(40);
			look(90.0F, 20.0F);
			comparisonShot("end.shulker");
			onServer(() -> {
				for (Object entity : new java.util.ArrayList<Object>(player().worldObj.loadedEntityList)) {
					if (entity instanceof ganymedes01.etfuturum.entities.EntityShulker shulker) {
						shulker.setDead();
					}
				}
			});
			// the outer islands (spec §76.11): the chorus chunk of 26.3's dump, from 14 above its middle column's top end
			// stone and 18 south of it, looking north and down
			int[] chorus = chorusChunk();
			if (chorus != null) {
				int top = onServer(() -> {
					net.minecraft.world.World end = player().worldObj;
					for (int ox = -1; ox <= 2; ox++) {
						for (int oz = -1; oz <= 2; oz++) {
							end.getChunkFromChunkCoords(chorus[0] + ox, chorus[1] + oz);
						}
					}
					for (int y = 255; y >= 0; y--) {
						if (end.getBlock(chorus[0] * 16 + 8, y, chorus[1] * 16 + 8) == net.minecraft.init.Blocks.end_stone) {
							return y;
						}
					}
					return 64;
				});
				// in the air, then flying again (standing on the platform for the gateway turned it off)
				onServer(() -> {
					player().playerNetServerHandler.setPlayerLocation(chorus[0] * 16 + 8.5, top + 14 - 1.62, chorus[1] * 16 + 26.5, 180.0F, 35.0F);
					player().capabilities.isFlying = true;
					player().sendPlayerAbilities();
				});
				ticks(100);
				look(180.0F, 35.0F);
				comparisonShot("end.outer_islands");
			}
			// the first end city (spec §76.12) from 30 north of its start, at y 100, looking south and a little down
			int[] city = cityChunk();
			if (city != null) {
				onServer(() -> {
					player().playerNetServerHandler.setPlayerLocation(city[0] * 16 + 7.5, 100 - 1.62, city[1] * 16 + 7.5 - 30, 0.0F, 15.0F);
					player().capabilities.isFlying = true;
					player().sendPlayerAbilities();
				});
				ticks(140);
				look(0.0F, 15.0F);
				comparisonShot("end.city");
			}
			onServer(() -> {
				player().capabilities.isFlying = false;
				player().sendPlayerAbilities();
			});
		});
	}
}
