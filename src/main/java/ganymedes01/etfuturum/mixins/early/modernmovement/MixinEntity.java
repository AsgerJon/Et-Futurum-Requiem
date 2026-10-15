package ganymedes01.etfuturum.mixins.early.modernmovement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.movement.ModernWater;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

	/**
	 * Players use the modern water scan (§9) instead of 1.7.10's, which squeezes the hitbox by 0.4 at the top and
	 * bottom and misses water around the 0.6 tall swimming hitbox. Vanilla's splash, fall reset and extinguishing
	 * still run on the result.
	 */
	@WrapOperation(method = "handleWaterMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;handleMaterialAcceleration(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z"))
	private boolean etfu$modernWaterScan(World world, AxisAlignedBB box, Material material, Entity entity, Operation<Boolean> original) {
		if (entity instanceof EntityPlayer) {
			return ModernWater.update((EntityPlayer) entity, entity.isPushedByWater());
		}
		return original.call(world, box, material, entity);
	}

	/**
	 * The swimming flag follows right after the water scan (§3, §5). Only the server and the local player decide it;
	 * other players get it synced.
	 */
	@Inject(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;handleWaterMovement()Z", shift = At.Shift.AFTER))
	private void etfu$updateSwimming(CallbackInfo ci) {
		if ((Object) this instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) (Object) this;
			if (player.isClientWorld()) {
				ModernMovement.updateSwimming(player);
			}
		}
	}

	/**
	 * Eyes in water for players come from the water scan: the real eye height against the water surface, with a source
	 * under a solid block counting as full (§9.1).
	 */
	@Inject(method = "isInsideOfMaterial", at = @At("HEAD"), cancellable = true)
	private void etfu$eyesInWater(Material material, CallbackInfoReturnable<Boolean> cir) {
		if (material == Material.water && (Object) this instanceof IPosePlayer) {
			cir.setReturnValue(((IPosePlayer) this).etfu$isEyeInWater());
		}
	}
}
