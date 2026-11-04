package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

/**
 * Panes (iron bars, glass panes) traced by their bars, as 26.3 traces a block's shape (SPEC-modern-movement.md
 * §76.15): 1.7.10 traced the one box around them (`setBlockBoundsBasedOnState`), so the open quarters of an L or a
 * cross stopped arrows and the crosshair. The gap under an End cage's corner is one: an arrow through it breaks the
 * caged crystal. The parts traced are the pane's own collision boxes, whatever makes them (1.7.10's, or EFR's thin
 * panes: a lone pane a post).
 */
@Mixin(BlockPane.class)
public abstract class MixinBlockPane extends Block {

	protected MixinBlockPane(Material material) {
		super(material);
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
		List<AxisAlignedBB> parts = new ArrayList<>();
		addCollisionBoxesToList(world, x, y, z, AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2), parts, null);
		setBlockBoundsBasedOnState(world, x, y, z);
		MovingObjectPosition nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (AxisAlignedBB part : parts) {
			MovingObjectPosition hit = part.calculateIntercept(start, end);
			if (hit != null) {
				double distance = hit.hitVec.squareDistanceTo(start);
				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearest = new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec);
				}
			}
		}
		return nearest;
	}
}
