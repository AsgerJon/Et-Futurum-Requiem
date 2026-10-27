package ganymedes01.etfuturum.mixins.early.bubblecolumn;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.BubbleColumnQueue;
import ganymedes01.etfuturum.movement.BubbleColumns;
import ganymedes01.etfuturum.movement.IBubbleColumnEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bubble columns and living entities (§28): the pushes land after the movement step, as in modern, and eyes in a
 * column breathe.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity implements IBubbleColumnEntity {

	@Unique
	private BubbleColumnQueue etfu$bubbleColumns = new BubbleColumnQueue();

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	@Override
	public void etfu$queueBubbleColumn(boolean drag, boolean above) {
		etfu$bubbleColumns.queue(this, drag, above);
	}

	/**
	 * Modern applies block effects after the movement step, so a push is used in full by the next move before the
	 * water drag and gravity get to it.
	 */
	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveEntityWithHeading(FF)V", shift = At.Shift.AFTER))
	private void etfu$pushedByBubbleColumns(CallbackInfo ci) {
		etfu$bubbleColumns.apply(this);
	}

	/**
	 * With the eyes in a column cell there is no drowning, and the air comes back as out of water (§28).
	 */
	@ModifyExpressionValue(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z", ordinal = 0))
	private boolean etfu$breatheInBubbleColumns(boolean eyesInWater) {
		return eyesInWater && !BubbleColumns.isEyeInColumn(this);
	}
}
