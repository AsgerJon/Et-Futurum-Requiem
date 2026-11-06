package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 26.3's sculk (`SculkBlock`; SPEC-modern-movement.md §90.9, step H3), under EFR's id: 0.2, the hoe, map colour black,
 * its sound type; only to Silk Touch it drops itself, else 1 experience. A catalyst's charge on it grows a sensor above
 * (1 in 11 a shrieker) past 4 from the catalyst, or decays (26.3's `attemptUseCharge`).
 */
public class SculkBlock extends McFlyBlock implements SculkBehaviour {

	public SculkBlock() {
		super(Material.ground);
		setNames("sculk");
		strength(0.2F, 0.2F).tool("hoe", 0).sound(McFlySoundType.SCULK).colour(MapColor.blackColor);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return null;
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int meta) {
		return true;
	}

	@Override
	public int getExpDrop(IBlockAccess world, int meta, int fortune) {
		return 1;
	}

	// ---------------------------------------------------------------- 26.3's charge

	/**
	 * 26.3's `attemptUseCharge`: on 1 in the decay rate, past the no-growth radius where growth can go, a growth above
	 * on a roll under the charge, costing the growth's charge; else 1 in the further rate a decay (1 near the catalyst,
	 * more by distance).
	 */
	@Override
	public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, World world, int ox, int oy, int oz, Random random, SculkSpreader spreader, boolean spreadVeins) {
		int charge = cursor.charge();
		if (charge == 0 || random.nextInt(spreader.chargeDecayRate()) != 0) {
			return charge;
		}
		int x = cursor.x(), y = cursor.y(), z = cursor.z();
		double dx = x - ox, dy = y - oy, dz = z - oz;
		boolean nearCatalyst = dx * dx + dy * dy + dz * dz < (double) spreader.noGrowthRadius() * spreader.noGrowthRadius();
		if (!nearCatalyst && canPlaceGrowth(world, x, y, z)) {
			int cost = spreader.growthSpawnCost();
			if (random.nextInt(cost) < charge) {
				Block growth = randomGrowth(random);
				world.setBlock(x, y + 1, z, growth, 0, 3);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, growth.stepSound.func_150496_b(), 1.0F, 1.0F);
			}
			return Math.max(0, charge - cost);
		}
		return random.nextInt(spreader.additionalDecayRate()) != 0 ? charge : charge - (nearCatalyst ? 1 : decayPenalty(spreader, dx, dy, dz, charge));
	}

	/** 26.3's `getDecayPenalty`. */
	private static int decayPenalty(SculkSpreader spreader, double dx, double dy, double dz, int charge) {
		int radius = spreader.noGrowthRadius();
		float outer = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) - radius;
		float outerSquared = outer * outer;
		int maxReachSquared = (24 - radius) * (24 - radius);
		float factor = Math.min(1.0F, outerSquared / maxReachSquared);
		return Math.max(1, (int) (charge * factor * 0.5F));
	}

	/** 26.3's `getRandomGrowthState`: 1 in 11 a shrieker (`can_summon` off: the level's spreader), else a sensor. */
	private static Block randomGrowth(Random random) {
		return random.nextInt(11) == 0 && McFlyBlocks.SCULK_SHRIEKER.isEnabled() ? McFlyBlocks.SCULK_SHRIEKER.get() : McFlyBlocks.SCULK_SENSOR.get();
	}

	/** 26.3's `canPlaceGrowth`: air above, and at most 2 of `#sculk_growth_inhibitors` (the sensor, the shrieker) within 4 across and 2 up. */
	private static boolean canPlaceGrowth(World world, int x, int y, int z) {
		if (!world.isAirBlock(x, y + 1, z)) {
			return false;
		}
		int found = 0;
		for (int ax = x - 4; ax <= x + 4; ax++) {
			for (int ay = y; ay <= y + 2; ay++) {
				for (int az = z - 4; az <= z + 4; az++) {
					Block there = world.getBlock(ax, ay, az);
					if ((there == McFlyBlocks.SCULK_SENSOR.get() || there == McFlyBlocks.SCULK_SHRIEKER.get()) && ++found > 2) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean canChangeBlockStateOnSpread() {
		return false;
	}
}
