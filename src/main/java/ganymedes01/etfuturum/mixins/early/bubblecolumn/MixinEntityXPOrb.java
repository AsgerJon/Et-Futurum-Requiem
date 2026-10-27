package ganymedes01.etfuturum.mixins.early.bubblecolumn;

import ganymedes01.etfuturum.movement.BubbleColumnQueue;
import ganymedes01.etfuturum.movement.IBubbleColumnEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bubble column pushes land at the end of this entity's tick (§28). 1.7.10 flips the vertical motion of anything
 * lying on the ground every tick, which cancelled a push applied inside the move: the entity never left the floor of
 * a column. After the flip the push stands, and the next move lifts it off.
 */
@Mixin(EntityXPOrb.class)
public abstract class MixinEntityXPOrb extends Entity implements IBubbleColumnEntity {

	@Unique
	private BubbleColumnQueue etfu$bubbleColumns = new BubbleColumnQueue();

	public MixinEntityXPOrb(World world) {
		super(world);
	}

	@Override
	public void etfu$queueBubbleColumn(boolean drag, boolean above) {
		etfu$bubbleColumns.queue(this, drag, above);
	}

	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$pushedByBubbleColumns(CallbackInfo ci) {
		etfu$bubbleColumns.apply(this);
	}
}
