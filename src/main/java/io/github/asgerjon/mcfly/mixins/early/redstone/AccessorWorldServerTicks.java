package io.github.asgerjon.mcfly.mixins.early.redstone;

import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/** The world's scheduled ticks still to come (26.3's `hasScheduledTick` asks them; spec §86). */
@Mixin(WorldServer.class)
public interface AccessorWorldServerTicks {

	@Accessor("pendingTickListEntriesHashSet")
	Set<NextTickListEntry> mcfly$pendingTicks();
}
