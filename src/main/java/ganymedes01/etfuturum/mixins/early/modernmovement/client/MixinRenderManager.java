package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderManager.class)
public class MixinRenderManager {

	/**
	 * F3+B draws a box from posY up by the entity's height instead of the real hitbox. For the local player posY is
	 * at eye level, so its box is drawn 1.62 too high. Draw the actual bounding box at the render position instead,
	 * so pose hitboxes can be checked.
	 */
	@ModifyArg(method = "renderDebugBoundingBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawOutlinedBoundingBox(Lnet/minecraft/util/AxisAlignedBB;I)V"), index = 0)
	private AxisAlignedBB etfu$drawRealBoundingBox(AxisAlignedBB box, @Local(argsOnly = true) Entity entity,
												   @Local(argsOnly = true, ordinal = 0) double x,
												   @Local(argsOnly = true, ordinal = 1) double y,
												   @Local(argsOnly = true, ordinal = 2) double z) {
		return entity.boundingBox.getOffsetBoundingBox(x - entity.posX, y - entity.posY, z - entity.posZ);
	}
}
