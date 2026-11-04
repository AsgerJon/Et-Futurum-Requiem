package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.7.10's entity invulnerability (saved as `Invulnerable`), which has no setter: 26.3's respawn crystals need it. */
@Mixin(Entity.class)
public interface AccessorEntityInvulnerable {

	@Accessor("invulnerable")
	boolean etfu$isInvulnerable();

	@Accessor("invulnerable")
	void etfu$setInvulnerable(boolean invulnerable);
}
