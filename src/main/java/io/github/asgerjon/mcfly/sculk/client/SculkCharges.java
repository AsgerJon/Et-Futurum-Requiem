package io.github.asgerjon.mcfly.sculk.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 26.3's level event 3006, a sculk charge (`LevelEventHandler`; SPEC-modern-movement.md §90.9, step H3c), on the client:
 * - data's count (data >> 6) above 0: now and then `block.sculk.charge`, and `sculk_charge` particles on each face (all
 *   six, 0.65 and 0.57 out, if data's faces are 0; else the faces, 0.35 out), 0 to count a face, drifting up to 0.005;
 * - 0: the pop, `block.sculk.charge`, and 40 (a full block) or 20 `sculk_charge_pop` bursting from the middle.
 */
@SideOnly(Side.CLIENT)
public final class SculkCharges {

	private static final int[][] STEP = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private SculkCharges() {
	}

	public static void levelEvent(World world, Random random, int x, int y, int z, int data) {
		int count = data >> 6;
		if (count > 0) {
			if (random.nextFloat() < 0.3F + count * 0.1F) {
				float volume = 0.15F + 0.02F * count * count * random.nextFloat();
				float pitch = 0.4F + 0.3F * count * random.nextFloat();
				world.playSound(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.sculk.charge", volume, pitch, false);
			}
			int faces = data & 63;
			if (faces == 0) {
				for (int face = 0; face < 6; face++) {
					float roll = face == 0 ? (float) Math.PI : 0.0F;
					onFace(world, random, x, y, z, face, count, roll, face < 2 ? 0.65D : 0.57D);
				}
			} else {
				for (int face = 0; face < 6; face++) {
					if ((faces & 1 << face) != 0) {
						onFace(world, random, x, y, z, face, count, face == 1 ? (float) Math.PI : 0.0F, 0.35D);
					}
				}
			}
			return;
		}
		world.playSound(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":block.sculk.charge", 1.0F, 1.0F, false);
		boolean solid = fullBlock(world, x, y, z);
		int particles = solid ? 40 : 20;
		float spread = solid ? 0.45F : 0.25F;
		for (int i = 0; i < particles; i++) {
			float vx = 2.0F * random.nextFloat() - 1.0F, vy = 2.0F * random.nextFloat() - 1.0F, vz = 2.0F * random.nextFloat() - 1.0F;
			add(SculkParticle.pop(world, x + 0.5D + vx * spread, y + 0.5D + vy * spread, z + 0.5D + vz * spread, vx * 0.07F, vy * 0.07F, vz * 0.07F));
		}
	}

	/** 26.3's `ParticleUtils.spawnParticlesOnBlockFace` with `UniformInt.of(0, count)`. */
	private static void onFace(World world, Random random, int x, int y, int z, int face, int count, float roll, double step) {
		int n = random.nextInt(count + 1);
		for (int i = 0; i < n; i++) {
			double sx = nextDouble(random, -0.005D, 0.005D), sy = nextDouble(random, -0.005D, 0.005D), sz = nextDouble(random, -0.005D, 0.005D);
			// 26.3's `spawnParticleOnFace`: on the face's plane, anywhere across it
			double px = x + 0.5D + (STEP[face][0] == 0 ? nextDouble(random, -0.5D, 0.5D) : STEP[face][0] * step);
			double py = y + 0.5D + (STEP[face][1] == 0 ? nextDouble(random, -0.5D, 0.5D) : STEP[face][1] * step);
			double pz = z + 0.5D + (STEP[face][2] == 0 ? nextDouble(random, -0.5D, 0.5D) : STEP[face][2] * step);
			// its speed along the face's own axis none
			add(SculkParticle.charge(world, px, py, pz, STEP[face][0] == 0 ? sx : 0.0D, STEP[face][1] == 0 ? sy : 0.0D, STEP[face][2] == 0 ? sz : 0.0D, roll));
		}
	}

	private static double nextDouble(Random random, double min, double max) {
		return min + random.nextDouble() * (max - min);
	}

	/** 26.3's `isCollisionShapeFullBlock`. */
	private static boolean fullBlock(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
		return box != null && box.minX == x && box.minY == y && box.minZ == z && box.maxX == x + 1 && box.maxY == y + 1 && box.maxZ == z + 1;
	}

	private static void add(SculkParticle particle) {
		Minecraft.getMinecraft().effectRenderer.addEffect(particle);
	}
}
