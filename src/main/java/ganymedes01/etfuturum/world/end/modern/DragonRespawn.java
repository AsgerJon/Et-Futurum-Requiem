package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Respawning the dragon as 26.3 does (SPEC-modern-movement.md §76.9, `DragonRespawnStage`): the four crystals on the exit
 * portal beam at the sky, then each pillar in turn has its top cleared in an explosion and is built again with a
 * crystal beaming at the sky, then the four explode and the dragon is made. Breaking one of the four stops it.
 */
final class DragonRespawn {

	static final String START = "start";
	static final String PREPARING = "preparing_to_summon_pillars";
	static final String PILLARS = "summoning_pillars";
	static final String DRAGON = "summoning_dragon";
	static final String END = "end";
	private static final ChunkCoordinates SKY = new ChunkCoordinates(0, 128, 0);

	private DragonRespawn() {
	}

	/** One tick of the stage running (the fight's tick calls it while it ticks). */
	static void tick(WorldServer world, EnderDragonFight fight) {
		List<EntityEnderCrystal> crystals = crystals(world, fight);
		if (crystals.isEmpty()) {
			abort(world, fight);
			return;
		}
		int time = fight.respawnTime++;
		fight.markDirty();
		switch (fight.respawnStage) {
			case START -> {
				for (EntityEnderCrystal crystal : crystals) {
					EndCrystals.setBeamTarget(crystal, SKY);
				}
				setStage(world, fight, PREPARING);
			}
			case PREPARING -> {
				if (time < 100) {
					if (time == 0 || time == 50 || time == 51 || time == 52 || time >= 95) {
						summonSound(world);
					}
				} else {
					setStage(world, fight, PILLARS);
				}
			}
			case PILLARS -> {
				boolean startOfBeam = time % 40 == 0;
				boolean endOfBeam = time % 40 == 39;
				if (startOfBeam || endOfBeam) {
					List<EndSpikes.Spike> spikes = EndSpikes.forSeed(world.getSeed());
					int index = time / 40;
					if (index < spikes.size()) {
						EndSpikes.Spike spike = spikes.get(index);
						if (startOfBeam) {
							for (EntityEnderCrystal crystal : crystals) {
								EndCrystals.setBeamTarget(crystal, new ChunkCoordinates(spike.x, spike.height + 1, spike.z));
							}
						} else {
							for (int x = spike.x - 10; x <= spike.x + 10; x++) {
								for (int y = spike.height - 10; y <= spike.height + 10; y++) {
									for (int z = spike.z - 10; z <= spike.z + 10; z++) {
										world.setBlockToAir(x, y, z);
									}
								}
							}
							world.createExplosion(null, spike.x + 0.5, spike.height, spike.z + 0.5, 5.0F, true);
							EndSpikes.place(world, spike, world.rand, true, SKY);
						}
					} else if (startOfBeam) {
						setStage(world, fight, DRAGON);
					}
				}
			}
			case DRAGON -> {
				if (time >= 100) {
					setStage(world, fight, END);
					resetSpikeCrystals(world);
					for (EntityEnderCrystal crystal : crystals) {
						EndCrystals.setBeamTarget(crystal, null);
						// power 6, breaking no blocks (26.3's ExplosionInteraction.NONE)
						world.newExplosion(crystal, crystal.posX, crystal.posY, crystal.posZ, 6.0F, false, false);
						crystal.setDead();
					}
				} else if (time >= 80) {
					summonSound(world);
				} else if (time == 0) {
					for (EntityEnderCrystal crystal : crystals) {
						EndCrystals.setBeamTarget(crystal, SKY);
					}
				} else if (time < 5) {
					summonSound(world);
				}
			}
			default -> {
			}
		}
	}

	/** 26.3's setRespawnStage: the time from 0; at the end the dragon is made and isn't dead any more. */
	private static void setStage(WorldServer world, EnderDragonFight fight, String stage) {
		fight.respawnTime = 0;
		if (END.equals(stage)) {
			fight.respawnStage = null;
			fight.setDragonKilledFlag(false);
			fight.createNewDragon();
		} else {
			fight.respawnStage = stage;
		}
		fight.markDirty();
	}

	/** 26.3's level event 3001: the dragon's growl at the sky above the portal, heard far off. */
	private static void summonSound(WorldServer world) {
		world.playSoundEffect(0.5, 128.5, 0.5, "mob.enderdragon.growl", 64.0F, 0.8F + world.rand.nextFloat() * 0.3F);
	}

	private static List<EntityEnderCrystal> crystals(WorldServer world, EnderDragonFight fight) {
		List<EntityEnderCrystal> found = new ArrayList<>();
		for (Object entity : world.loadedEntityList) {
			if (entity instanceof EntityEnderCrystal crystal && !crystal.isDead && fight.respawnCrystals.contains(crystal.getUniqueID())) {
				found.add(crystal);
			}
		}
		return found;
	}

	/** 26.3's abortRespawnSequence: no stage, the pillars' crystals reset, the portal active again. */
	static void abort(WorldServer world, EnderDragonFight fight) {
		fight.respawnStage = null;
		fight.respawnTime = 0;
		resetSpikeCrystals(world);
		fight.spawnExitPortal(true);
		fight.markDirty();
	}

	/** Every pillar's crystal: vulnerable, no beam. */
	static void resetSpikeCrystals(WorldServer world) {
		for (EndSpikes.Spike spike : EndSpikes.forSeed(world.getSeed())) {
			for (Object entity : world.getEntitiesWithinAABB(EntityEnderCrystal.class, spike.column())) {
				EntityEnderCrystal crystal = (EntityEnderCrystal) entity;
				EndCrystals.setInvulnerable(crystal, false);
				EndCrystals.setBeamTarget(crystal, null);
			}
		}
	}

	/** Starts it with these four (26.3's respawnDragon, once the dragon is dead and none is running). */
	static void begin(WorldServer world, EnderDragonFight fight, List<EntityEnderCrystal> crystals) {
		fight.respawnStage = START;
		fight.respawnTime = 0;
		fight.respawnCrystals.clear();
		for (EntityEnderCrystal crystal : crystals) {
			UUID id = crystal.getUniqueID();
			if (!fight.respawnCrystals.contains(id)) {
				fight.respawnCrystals.add(id);
			}
		}
		fight.markDirty();
	}
}
