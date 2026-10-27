package ganymedes01.etfuturum.mixins.early.darkspawns;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hostile mobs need block light 0 to spawn, as since 1.18. Modern's check is the sky-light die, then the dimension's
 * block-light limit, then the combined light against a 0..7 die; the limit is 0 everywhere but the Nether, where it
 * is 15 and never applies. 1.7.10's isValidLightLevel already has the first and last steps, so only the middle one
 * is added, which keeps night-time surface spawns and the thundering rule as they are.
 */
@Mixin(EntityMob.class)
public abstract class MixinEntityMob extends EntityCreature {

	public MixinEntityMob(World world) {
		super(world);
	}

	@Inject(method = "isValidLightLevel", at = @At("HEAD"), cancellable = true)
	private void etfu$needBlockLightZero(CallbackInfoReturnable<Boolean> cir) {
		if (this.worldObj.provider.isHellWorld) {
			return;
		}
		int x = MathHelper.floor_double(this.posX);
		int y = MathHelper.floor_double(this.boundingBox.minY);
		int z = MathHelper.floor_double(this.posZ);
		if (this.worldObj.getSavedLightValue(EnumSkyBlock.Block, x, y, z) > 0) {
			cir.setReturnValue(false);
		}
	}
}
