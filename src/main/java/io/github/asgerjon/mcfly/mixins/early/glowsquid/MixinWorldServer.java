package io.github.asgerjon.mcfly.mixins.early.glowsquid;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.entities.GlowSquidSpawning;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** The spawn list a spot gets, by `GlowSquidSpawning` (SPEC-modern-movement.md §67.1): a copy, never the biome's own. */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

	@ModifyExpressionValue(method = "spawnRandomCreature", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;getPotentialSpawns(Lnet/minecraft/world/WorldServer;Lnet/minecraft/entity/EnumCreatureType;IIILjava/util/List;)Ljava/util/List;", remap = false))
	private List<BiomeGenBase.SpawnListEntry> etfu$glowSquidSpots(List<BiomeGenBase.SpawnListEntry> list, @Local(argsOnly = true) EnumCreatureType type,
		@Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
		return GlowSquidSpawning.forSpot((WorldServer) (Object) this, type, x, y, z, list);
	}
}
