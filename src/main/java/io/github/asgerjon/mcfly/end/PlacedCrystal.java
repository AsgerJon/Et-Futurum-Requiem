package io.github.asgerjon.mcfly.end;

import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.world.World;

/**
 * EFR's old placed end crystal's id (`end_crystal`, entity 9; SPEC-modern-movement.md §85) read back as 26.3's crystal
 * (1.7.10's, with McFly's End mixins): a crystal an old save holds stands again, its base shown. Nothing new is made as
 * one.
 */
public class PlacedCrystal extends EntityEnderCrystal {

	public PlacedCrystal(World world) {
		super(world);
	}
}
