package io.github.asgerjon.mcfly.mixins.early.strongholds;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

/**
 * Strongholds in 26.3's concentric rings (SPEC-modern-movement.md §64.4; 26.3
 * {@code ChunkGeneratorStructureState.generateRingPositions} with {@code strongholds.json}'s count 128, distance 32,
 * spread 3): rings of 3, 6, 10, 15, 21, 28, 36 and 9, the first 88 to 168 chunks out. 1.7.10's biome list and its
 * search (112 blocks) nudge each, as 26.3's own search does; a preset's own count, distance and spread still apply.
 */
@Mixin(MapGenStronghold.class)
public abstract class MixinMapGenStronghold extends MapGenStructure {

	@Shadow
	private ChunkCoordIntPair[] structureCoords;
	@Shadow
	private boolean ranBiomeCheck;
	@Shadow
	private double field_82671_h;
	@Shadow
	private int field_82672_i;
	@Shadow
	@Final
	public List<BiomeGenBase> field_151546_e;

	@Inject(method = "<init>()V", at = @At("RETURN"))
	private void etfu$modernCount(CallbackInfo ci) {
		if (McFlyConfig.modernStrongholds) {
			structureCoords = new ChunkCoordIntPair[McFlyConfig.strongholdCount];
		}
	}

	@Inject(method = "canSpawnStructureAtCoords", at = @At("HEAD"))
	private void etfu$modernRings(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
		if (!McFlyConfig.modernStrongholds || ranBiomeCheck) {
			return;
		}
		Random random = new Random();
		random.setSeed(worldObj.getSeed());
		double distance = field_82671_h;
		int count = structureCoords.length;
		int spread = field_82672_i;
		double angle = random.nextDouble() * Math.PI * 2.0D;
		int positionInCircle = 0;
		int circle = 0;
		for (int i = 0; i < count; i++) {
			double dist = 4.0D * distance + distance * circle * 6.0D + (random.nextDouble() - 0.5D) * (distance * 2.5D);
			int x = (int) Math.round(Math.cos(angle) * dist);
			int z = (int) Math.round(Math.sin(angle) * dist);
			// 26.3 forks a random for each search (a nextLong off the main one)
			Random search = new Random(random.nextLong());
			ChunkPosition found = worldObj.getWorldChunkManager().findBiomePosition((x << 4) + 8, (z << 4) + 8, 112, field_151546_e, search);
			if (found != null) {
				x = found.chunkPosX >> 4;
				z = found.chunkPosZ >> 4;
			}
			structureCoords[i] = new ChunkCoordIntPair(x, z);
			angle += Math.PI * 2.0D / spread;
			if (++positionInCircle == spread) {
				circle++;
				positionInCircle = 0;
				spread += 2 * spread / (circle + 1);
				spread = Math.min(spread, count - i);
				angle += random.nextDouble() * Math.PI * 2.0D;
			}
		}
		ranBiomeCheck = true;
	}
}
