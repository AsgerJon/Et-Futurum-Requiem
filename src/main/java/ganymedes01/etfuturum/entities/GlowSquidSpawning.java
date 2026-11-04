package ganymedes01.etfuturum.entities;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Where glow squids may spawn (SPEC-modern-movement.md §67.1). 26.3 spawns them in their own category
 * (`underground_water_creature`); 1.7.10 has only the water creature lists, where squids are too. So at each spot the
 * spawner tries (`WorldServer.spawnRandomCreature`, by `mixins.early.glowsquid.MixinWorldServer`), the list it picks
 * from keeps glow squids only where their rule holds (dark water at y 30 or below) and squids only where it doesn't:
 * squids spawn as before, and a deep dark spot isn't spent on a squid that can't live there. They still share the
 * water creature cap (5).
 * <p>
 * A copy, never the list given: it is the biome's own (Forge's `PotentialSpawns` passes it on as it is), and removing
 * from it would take squids or glow squids out of the biome for good (found by `GlowSquidTests`, 2026-11-03).
 */
public final class GlowSquidSpawning {

	private GlowSquidSpawning() {
	}

	public static List<BiomeGenBase.SpawnListEntry> forSpot(World world, EnumCreatureType type, int x, int y, int z, List<BiomeGenBase.SpawnListEntry> list) {
		if (type != EnumCreatureType.waterCreature || list == null || list.isEmpty()) {
			return list;
		}
		boolean glowSpot = EntityGlowSquid.canSpawnAt(world, x, y, z);
		List<BiomeGenBase.SpawnListEntry> spot = null;
		for (int i = 0; i < list.size(); i++) {
			Class<?> kind = list.get(i).entityClass;
			if (kind == EntityGlowSquid.class ? !glowSpot : glowSpot && kind == EntitySquid.class) {
				if (spot == null) {
					spot = new ArrayList<>(list.subList(0, i));
				}
			} else if (spot != null) {
				spot.add(list.get(i));
			}
		}
		return spot == null ? list : spot;
	}
}
