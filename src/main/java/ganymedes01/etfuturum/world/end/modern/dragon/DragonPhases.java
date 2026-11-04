package ganymedes01.etfuturum.world.end.modern.dragon;

import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;


/**
 * 26.3's dragon phases (SPEC-modern-movement.md §76.7): what each does a tick, where it flies, how fast it climbs and
 * turns, and when it goes to which phase. One instance of each per dragon, kept between uses as 26.3 keeps them.
 */
final class DragonPhases {

	private DragonPhases() {
	}

	static Instance make(DragonPhase phase, ModernDragon dragon) {
		return switch (phase) {
			case HOLDING_PATTERN -> new HoldingPattern(dragon);
			case STRAFE_PLAYER -> new StrafePlayer(dragon);
			case LANDING_APPROACH -> new LandingApproach(dragon);
			case LANDING -> new Landing(dragon);
			case TAKEOFF -> new Takeoff(dragon);
			case SITTING_FLAMING -> new SittingFlaming(dragon);
			case SITTING_SCANNING -> new SittingScanning(dragon);
			case SITTING_ATTACKING -> new SittingAttacking(dragon);
			case CHARGING_PLAYER -> new ChargingPlayer(dragon);
			case DYING -> new Dying(dragon);
			case HOVER -> new Hover(dragon);
		};
	}

	abstract static class Instance {
		final ModernDragon modern;
		final EntityDragon dragon;
		Vec3 target;

		Instance(ModernDragon modern) {
			this.modern = modern;
			this.dragon = modern.dragon;
		}

		abstract DragonPhase phase();

		void begin() {
		}

		void end() {
		}

		void serverTick() {
		}

		void clientTick() {
		}

		Vec3 flyTarget() {
			return target;
		}

		float flySpeed() {
			return 0.6F;
		}

		/** 26.3's default: 0.7F / min(v, 40F) / v, v the horizontal speed plus 1. */
		float turnSpeed() {
			float v = MathHelper.sqrt_double(dragon.motionX * dragon.motionX + dragon.motionZ * dragon.motionZ) + 1.0F;
			return 0.7F / Math.min(v, 40.0F) / v;
		}

		float onHurt(DamageSource source, float damage) {
			return damage;
		}

		void onCrystalDestroyed(EntityPlayer player) {
		}

		/** Squared distance to the target (0 with none), and whether that asks for a new one (26.3's <100 or >22500). */
		boolean needsNewTarget() {
			double d = target == null ? 0.0 : dragon.getDistanceSq(target.xCoord, target.yCoord, target.zCoord);
			return d < 100.0 || d > 22500.0;
		}
	}

	/** The phases that need a path: 26.3's navigateToNextPathNode. */
	abstract static class Pathing extends Instance {
		DragonPath path;

		Pathing(ModernDragon modern) {
			super(modern);
		}

		void navigateToNextPathNode() {
			if (path != null && !path.isDone()) {
				int[] node = path.next();
				path.advance();
				target = Vec3.createVectorHelper(node[0], node[1] + dragon.getRNG().nextFloat() * 20.0F, node[2]);
			}
		}
	}

	// ---------------------------------------------------------------- 0 HoldingPattern

	static final class HoldingPattern extends Pathing {
		boolean clockwise;

		HoldingPattern(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.HOLDING_PATTERN;
		}

		@Override
		void begin() {
			path = null;
			target = null;
		}

		@Override
		void serverTick() {
			if (needsNewTarget()) {
				findNewTarget();
			}
		}

		private void findNewTarget() {
			if (path != null && path.isDone()) {
				int[] egg = modern.podiumTop(false);
				int crystals = modern.aliveCrystals();
				if (dragon.getRNG().nextInt(crystals + 3) == 0) {
					modern.setPhase(DragonPhase.LANDING_APPROACH);
					return;
				}
				EntityPlayer player = modern.nearestPlayer(egg[0] + 0.5, egg[1] + 0.5, egg[2] + 0.5, Double.MAX_VALUE, false);
				double q = player != null ? player.getDistanceSq(egg[0] + 0.5, egg[1] + 0.5, egg[2] + 0.5) / 512.0 : 64.0;
				if (player != null && (dragon.getRNG().nextInt((int) (q + 2.0)) == 0 || dragon.getRNG().nextInt(crystals + 2) == 0)) {
					modern.setPhase(DragonPhase.STRAFE_PLAYER);
					((StrafePlayer) modern.instance(DragonPhase.STRAFE_PLAYER)).setTarget(player);
					return;
				}
			}
			if (path == null || path.isDone()) {
				int current = modern.findClosestNode();
				int next = current;
				if (dragon.getRNG().nextInt(8) == 0) {
					clockwise = !clockwise;
					next += 6;
				}
				next += clockwise ? 1 : -1;
				// 26.3's wrap for this phase: "at least 0 crystals", so always the outer ring with a fight
				next = modern.hasFight() ? Math.floorMod(next, 12) : ((next - 12) & 7) + 12;
				path = modern.findPath(current, next, null);
				if (path != null) {
					path.advance();
				}
			}
			navigateToNextPathNode();
		}

		@Override
		void onCrystalDestroyed(EntityPlayer player) {
			if (player != null && modern.canAttack(player)) {
				modern.setPhase(DragonPhase.STRAFE_PLAYER);
				((StrafePlayer) modern.instance(DragonPhase.STRAFE_PLAYER)).setTarget(player);
			}
		}
	}

	// ---------------------------------------------------------------- 1 StrafePlayer

	static final class StrafePlayer extends Pathing {
		int charge;
		EntityPlayer attackTarget;
		boolean clockwise;

		StrafePlayer(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.STRAFE_PLAYER;
		}

		@Override
		void begin() {
			charge = 0;
			target = null;
			path = null;
			attackTarget = null;
		}

		void setTarget(EntityPlayer player) {
			attackTarget = player;
			int current = modern.findClosestNode();
			int goal = modern.findClosestNode(player.posX, player.boundingBox.minY, player.posZ);
			int fx = MathHelper.floor_double(player.posX);
			int fz = MathHelper.floor_double(player.posZ);
			double dx = fx - dragon.posX;
			double dz = fz - dragon.posZ;
			double sd = Math.sqrt(dx * dx + dz * dz);
			double ho = Math.min(0.4F + sd / 80.0 - 1.0, 10.0);
			int fy = MathHelper.floor_double(player.boundingBox.minY + ho);
			path = modern.findPath(current, goal, new int[] {fx, fy, fz});
			if (path != null) {
				path.advance();
				navigateToNextPathNode();
			}
		}

		@Override
		void serverTick() {
			if (attackTarget == null || attackTarget.isDead) {
				modern.setPhase(DragonPhase.HOLDING_PATTERN);
				return;
			}
			if (path != null && path.isDone()) {
				double dx = attackTarget.posX - dragon.posX;
				double dz = attackTarget.posZ - dragon.posZ;
				double h = Math.min(0.4F + Math.sqrt(dx * dx + dz * dz) / 80.0 - 1.0, 10.0);
				target = Vec3.createVectorHelper(attackTarget.posX, attackTarget.boundingBox.minY + h, attackTarget.posZ);
			}
			if (needsNewTarget()) {
				findNewTarget();
			}
			if (attackTarget.getDistanceSqToEntity(dragon) < 4096.0) {
				if (dragon.canEntityBeSeen(attackTarget)) {
					charge++;
					double ax = attackTarget.posX - dragon.posX;
					double az = attackTarget.posZ - dragon.posZ;
					double length = Math.sqrt(ax * ax + az * az);
					float yaw = (float) Math.toRadians(dragon.rotationYaw);
					double dot = length == 0 ? 1.0 : (MathHelper.sin(yaw) * ax - MathHelper.cos(yaw) * az) / length;
					float angle = (float) (Math.acos(Math.max(-1.0, Math.min(1.0, dot))) * 180.0 / Math.PI) + 0.5F;
					if (charge >= 5 && angle >= 0.0F && angle < 10.0F) {
						DragonBreath.fireball(modern, attackTarget);
						charge = 0;
						if (path != null) {
							while (!path.isDone()) {
								path.advance();
							}
						}
						modern.setPhase(DragonPhase.HOLDING_PATTERN);
					}
				} else if (charge > 0) {
					charge--;
				}
			} else if (charge > 0) {
				charge--;
			}
		}

		private void findNewTarget() {
			if (path == null || path.isDone()) {
				int current = modern.findClosestNode();
				int next = current;
				if (dragon.getRNG().nextInt(8) == 0) {
					clockwise = !clockwise;
					next += 6;
				}
				next += clockwise ? 1 : -1;
				next = modern.aliveCrystals() > 0 && modern.hasFight() ? Math.floorMod(next, 12) : ((next - 12) & 7) + 12;
				path = modern.findPath(current, next, null);
				if (path != null) {
					path.advance();
				}
			}
			navigateToNextPathNode();
		}
	}

	// ---------------------------------------------------------------- 2 LandingApproach

	static final class LandingApproach extends Pathing {
		LandingApproach(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.LANDING_APPROACH;
		}

		@Override
		void begin() {
			path = null;
			target = null;
		}

		@Override
		void serverTick() {
			if (needsNewTarget()) {
				if (path == null || path.isDone()) {
					int current = modern.findClosestNode();
					int[] egg = modern.podiumTop(false);
					EntityPlayer player = modern.nearestPlayer(egg[0] + 0.5, egg[1] + 0.5, egg[2] + 0.5, Double.MAX_VALUE, false);
					int goal;
					if (player != null) {
						double length = Math.sqrt(player.posX * player.posX + player.posZ * player.posZ);
						double ax = length == 0 ? 0 : player.posX / length;
						double az = length == 0 ? 0 : player.posZ / length;
						goal = modern.findClosestNode(-ax * 40.0, 105.0, -az * 40.0);
					} else {
						goal = modern.findClosestNode(40.0, egg[1], 0.0);
					}
					path = modern.findPath(current, goal, egg);
					if (path != null) {
						path.advance();
					}
				}
				navigateToNextPathNode();
				if (path != null && path.isDone()) {
					modern.setPhase(DragonPhase.LANDING);
				}
			}
		}
	}

	// ---------------------------------------------------------------- 3 Landing

	static final class Landing extends Instance {
		Landing(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.LANDING;
		}

		@Override
		void begin() {
			target = null;
		}

		@Override
		void serverTick() {
			if (target == null) {
				int[] egg = modern.podiumTop(false);
				target = Vec3.createVectorHelper(egg[0] + 0.5, egg[1], egg[2] + 0.5);
			}
			if (dragon.getDistanceSq(target.xCoord, target.yCoord, target.zCoord) < 1.0) {
				((SittingFlaming) modern.instance(DragonPhase.SITTING_FLAMING)).flameCount = 0;
				modern.setPhase(DragonPhase.SITTING_SCANNING);
			}
		}

		@Override
		void clientTick() {
			DragonBreath.landingParticles(modern);
		}

		@Override
		float flySpeed() {
			return 1.5F;
		}

		@Override
		float turnSpeed() {
			float v = MathHelper.sqrt_double(dragon.motionX * dragon.motionX + dragon.motionZ * dragon.motionZ) + 1.0F;
			return Math.min(v, 40.0F) / v;
		}
	}

	// ---------------------------------------------------------------- 4 Takeoff

	static final class Takeoff extends Pathing {
		boolean firstTick;

		Takeoff(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.TAKEOFF;
		}

		@Override
		void begin() {
			firstTick = true;
			path = null;
			target = null;
		}

		@Override
		void serverTick() {
			if (!firstTick && path != null) {
				int[] egg = modern.podiumTop(false);
				if (dragon.getDistanceSq(egg[0] + 0.5, egg[1] + 0.5, egg[2] + 0.5) >= 100.0) {
					modern.setPhase(DragonPhase.HOLDING_PATTERN);
				}
			} else {
				firstTick = false;
				int current = modern.findClosestNode();
				Vec3 look = modern.headLookVector();
				int goal = modern.findClosestNode(-look.xCoord * 40.0, 105.0, -look.zCoord * 40.0);
				goal = modern.aliveCrystals() > 0 && modern.hasFight() ? Math.floorMod(goal, 12) : ((goal - 12) & 7) + 12;
				path = modern.findPath(current, goal, null);
				if (path != null) {
					path.advance();
					navigateToNextPathNode();
				}
			}
		}
	}

	// ---------------------------------------------------------------- 5, 6, 7 sitting

	abstract static class Sitting extends Instance {
		Sitting(ModernDragon modern) {
			super(modern);
		}

		/** Arrows (and wind charges) are set on fire and bounce off a perched dragon (26.3's sitting phases). */
		@Override
		float onHurt(DamageSource source, float damage) {
			if (source.getSourceOfDamage() instanceof EntityArrow arrow) {
				arrow.setFire(1);
				return 0.0F;
			}
			if (source.getSourceOfDamage() != null && source.getSourceOfDamage().getClass().getSimpleName().contains("WindCharge")) {
				source.getSourceOfDamage().setFire(1);
				return 0.0F;
			}
			return damage;
		}

		@Override
		Vec3 flyTarget() {
			return null;
		}
	}

	static final class SittingFlaming extends Sitting {
		int flameTicks;
		int flameCount;
		Object cloud;

		SittingFlaming(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.SITTING_FLAMING;
		}

		@Override
		void begin() {
			flameTicks = 0;
			flameCount++;
		}

		@Override
		void serverTick() {
			flameTicks++;
			if (flameTicks >= 200) {
				modern.setPhase(flameCount >= 4 ? DragonPhase.TAKEOFF : DragonPhase.SITTING_SCANNING);
			} else if (flameTicks == 10) {
				cloud = DragonBreath.flameCloud(modern);
			}
		}

		@Override
		void clientTick() {
			flameTicks++;
			if (flameTicks % 2 == 0 && flameTicks < 10) {
				DragonBreath.flameParticles(modern);
			}
		}

		@Override
		void end() {
			DragonBreath.removeCloud(cloud);
			cloud = null;
		}
	}

	static final class SittingScanning extends Sitting {
		int scanningTime;

		SittingScanning(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.SITTING_SCANNING;
		}

		@Override
		void begin() {
			scanningTime = 0;
		}

		@Override
		void serverTick() {
			scanningTime++;
			EntityPlayer near = modern.nearestPlayerAround(20.0, 10.0);
			if (near != null) {
				if (scanningTime > 25) {
					modern.setPhase(DragonPhase.SITTING_ATTACKING);
				} else {
					double ax = near.posX - dragon.posX;
					double az = near.posZ - dragon.posZ;
					double length = Math.sqrt(ax * ax + az * az);
					float yaw = (float) Math.toRadians(dragon.rotationYaw);
					double dot = length == 0 ? 1.0 : (MathHelper.sin(yaw) * ax - MathHelper.cos(yaw) * az) / length;
					float angle = (float) (Math.acos(Math.max(-1.0, Math.min(1.0, dot))) * 180.0 / Math.PI) + 0.5F;
					if (angle < 0.0F || angle > 10.0F) {
						double xd = near.posX - dragon.dragonPartHead.posX;
						double zd = near.posZ - dragon.dragonPartHead.posZ;
						double delta = MathHelper.clamp_double(MathHelper.wrapAngleTo180_double(180.0 - Math.atan2(xd, zd) * 180.0 / Math.PI - dragon.rotationYaw), -100.0, 100.0);
						modern.yRotA *= 0.8F;
						float dist = (float) Math.sqrt(xd * xd + zd * zd) + 1.0F;
						float rs = dist;
						dist = Math.min(dist, 40.0F);
						modern.yRotA += (float) delta * (0.7F / dist / rs);
						dragon.rotationYaw += modern.yRotA;
					}
				}
			} else if (scanningTime >= 100) {
				EntityPlayer far = modern.nearestPlayer(dragon.posX, dragon.posY, dragon.posZ, 150.0, true);
				modern.setPhase(DragonPhase.TAKEOFF);
				if (far != null) {
					modern.setPhase(DragonPhase.CHARGING_PLAYER);
					((ChargingPlayer) modern.instance(DragonPhase.CHARGING_PLAYER))
						.setTarget(Vec3.createVectorHelper(far.posX, far.boundingBox.minY, far.posZ));
				}
			}
		}
	}

	static final class SittingAttacking extends Sitting {
		int ticks;

		SittingAttacking(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.SITTING_ATTACKING;
		}

		@Override
		void begin() {
			ticks = 0;
		}

		@Override
		void serverTick() {
			if (ticks++ >= 40) {
				modern.setPhase(DragonPhase.SITTING_FLAMING);
			}
		}

		@Override
		void clientTick() {
			dragon.worldObj.playSound(dragon.posX, dragon.posY, dragon.posZ, "mob.enderdragon.growl", 2.5F, 0.8F + dragon.getRNG().nextFloat() * 0.3F, false);
		}
	}

	// ---------------------------------------------------------------- 8 ChargingPlayer

	static final class ChargingPlayer extends Instance {
		int timeSinceCharge;

		ChargingPlayer(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.CHARGING_PLAYER;
		}

		@Override
		void begin() {
			target = null;
			timeSinceCharge = 0;
		}

		void setTarget(Vec3 at) {
			target = at;
		}

		@Override
		void serverTick() {
			if (target == null) {
				modern.setPhase(DragonPhase.HOLDING_PATTERN);
			} else if (timeSinceCharge > 0 && timeSinceCharge++ >= 10) {
				modern.setPhase(DragonPhase.HOLDING_PATTERN);
			} else if (needsNewTarget()) {
				timeSinceCharge++;
			}
		}

		@Override
		float flySpeed() {
			return 3.0F;
		}
	}

	// ---------------------------------------------------------------- 9 Dying

	static final class Dying extends Instance {
		int time;

		Dying(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.DYING;
		}

		@Override
		void begin() {
			target = null;
			time = 0;
		}

		@Override
		void serverTick() {
			time++;
			if (target == null) {
				int[] top = modern.podiumTop(true);
				target = Vec3.createVectorHelper(top[0] + 0.5, top[1], top[2] + 0.5);
			}
			double d = dragon.getDistanceSq(target.xCoord, target.yCoord, target.zCoord);
			dragon.setHealth(d < 100.0 || d > 22500.0 ? 0.0F : 1.0F);
		}

		@Override
		void clientTick() {
			if (time++ % 10 == 0) {
				java.util.Random random = dragon.getRNG();
				dragon.worldObj.spawnParticle("hugeexplosion", dragon.posX + (random.nextFloat() - 0.5F) * 8.0F,
					dragon.posY + 2.0 + (random.nextFloat() - 0.5F) * 4.0F, dragon.posZ + (random.nextFloat() - 0.5F) * 8.0F, 0.0, 0.0, 0.0);
			}
		}

		@Override
		float flySpeed() {
			return 3.0F;
		}
	}

	// ---------------------------------------------------------------- 10 Hover

	static final class Hover extends Instance {
		Hover(ModernDragon modern) {
			super(modern);
		}

		@Override
		DragonPhase phase() {
			return DragonPhase.HOVER;
		}

		@Override
		void begin() {
			target = null;
		}

		@Override
		void serverTick() {
			if (target == null) {
				target = Vec3.createVectorHelper(dragon.posX, dragon.posY, dragon.posZ);
			}
		}

		@Override
		float flySpeed() {
			return 1.0F;
		}
	}

	/** Players in the list that can be attacked (alive, not creative). */
	static boolean attackable(EntityPlayer player) {
		return player.isEntityAlive() && !player.capabilities.disableDamage;
	}
}
