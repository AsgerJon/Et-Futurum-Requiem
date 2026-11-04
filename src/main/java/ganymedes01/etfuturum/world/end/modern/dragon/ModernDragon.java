package ganymedes01.etfuturum.world.end.modern.dragon;

import io.github.asgerjon.mcfly.mixins.early.modernend.AccessorEntityDragon;
import io.github.asgerjon.mcfly.mixins.early.modernend.AccessorLivingLerp;
import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The dragon as 26.3's (SPEC-modern-movement.md §76.7), on 1.7.10's `EntityDragon` (its id, parts and renderer): its
 * phases, its flight (26.3's steering and friction), its parts (26.3's sizes, the neck added), its crystals, its damage
 * rules and death, and the node graph it paths over. 1.7.10's fields hold what 26.3 keeps: `animTime` the flap, `slowed`
 * the in-wall flag, `healingEnderCrystal` the nearest crystal, `deathTicks` the death time, `ringBuffer` the history.
 */
public final class ModernDragon {

	/** The data watcher id of the phase (1.7.10's dragon uses none past its living ones). */
	public static final int PHASE_WATCHER = 20;
	private static final int[][] NODES = {{60, 0}, {51, 30}, {29, 51}, {0, 60}, {-31, 51}, {-52, 29}, {-60, 0}, {-52, -31}, {-30, -52},
		{0, -60}, {29, -52}, {51, -30}, {40, 0}, {28, 28}, {0, 40}, {-29, 28}, {-40, 0}, {-29, -29}, {0, -40}, {28, -29}, {20, 0},
		{0, 20}, {-20, 0}, {0, -20}};
	private static final int[] ADJACENCY = {6146, 8197, 8202, 16404, 32808, 32848, 65696, 131392, 131712, 263424, 526848, 525313,
		1581057, 3166214, 2138120, 6373424, 4358208, 12910976, 9044480, 9706496, 15216640, 13688832, 11763712, 8257536};

	final EntityDragon dragon;
	public final EntityDragonPart neck;
	private final DragonPhases.Instance[] phases = new DragonPhases.Instance[DragonPhase.values().length];
	private DragonPhases.Instance current;
	float yRotA;
	private float sittingDamageReceived;
	private int growlTime = 100;
	private int[][] nodes;

	public ModernDragon(EntityDragon dragon) {
		this.dragon = dragon;
		// 26.3's parts: head 1x1, neck 3x3 (added), body 5x3, tails 2x2, wings 4x2
		this.neck = new EntityDragonPart(dragon, "neck", 3.0F, 3.0F);
		resize(dragon.dragonPartHead, 1.0F, 1.0F);
		resize(dragon.dragonPartBody, 5.0F, 3.0F);
		resize(dragon.dragonPartTail1, 2.0F, 2.0F);
		resize(dragon.dragonPartTail2, 2.0F, 2.0F);
		resize(dragon.dragonPartTail3, 2.0F, 2.0F);
		resize(dragon.dragonPartWing1, 4.0F, 2.0F);
		resize(dragon.dragonPartWing2, 4.0F, 2.0F);
		dragon.dragonPartArray = new EntityDragonPart[] {dragon.dragonPartHead, neck, dragon.dragonPartBody, dragon.dragonPartTail1,
			dragon.dragonPartTail2, dragon.dragonPartTail3, dragon.dragonPartWing1, dragon.dragonPartWing2};
		setPhase(DragonPhase.HOVER);
	}

	private static void resize(EntityDragonPart part, float width, float height) {
		((io.github.asgerjon.mcfly.mixins.early.modernend.AccessorEntitySize) part).etfu$setSize(width, height);
	}

	public static ModernDragon of(EntityDragon dragon) {
		return ((IModernDragon) dragon).etfu$modern();
	}

	/** Implemented by the dragon (modernend.MixinEntityDragon). */
	public interface IModernDragon {
		ModernDragon etfu$modern();
	}

	// ---------------------------------------------------------------- phases

	DragonPhases.Instance instance(DragonPhase phase) {
		if (phases[phase.ordinal()] == null) {
			phases[phase.ordinal()] = DragonPhases.make(phase, this);
		}
		return phases[phase.ordinal()];
	}

	public DragonPhase phase() {
		return current.phase();
	}

	/** 26.3's setPhase: the same phase again does nothing; else the old one ends, the new one begins (synced). */
	public void setPhase(DragonPhase phase) {
		if (current != null && current.phase() == phase) {
			return;
		}
		if (current != null) {
			current.end();
		}
		current = instance(phase);
		if (!dragon.worldObj.isRemote && dragon.getDataWatcher() != null && watcherReady()) {
			dragon.getDataWatcher().updateObject(PHASE_WATCHER, phase.ordinal());
		}
		current.begin();
	}

	private boolean watcherReady() {
		try {
			dragon.getDataWatcher().getWatchableObjectInt(PHASE_WATCHER);
			return true;
		} catch (RuntimeException notYet) {
			return false;
		}
	}

	boolean isSitting() {
		return current.phase().isSitting();
	}

	// ---------------------------------------------------------------- the fight

	EnderDragonFight fight() {
		return dragon.worldObj.isRemote ? null : EnderDragonFight.ifThere(dragon.worldObj);
	}

	boolean hasFight() {
		return fight() != null;
	}

	int aliveCrystals() {
		EnderDragonFight fight = fight();
		return fight == null ? 0 : fight.aliveCrystals();
	}

	/** The top of the podium's column (26.3's heightmap, without leaves or with), as a block place. */
	int[] podiumTop(boolean withLeaves) {
		return new int[] {0, top(dragon.worldObj, 0, 0, withLeaves), 0};
	}

	/** 26.3's MOTION_BLOCKING_NO_LEAVES height: the first place above the top block that stops movement (not leaves). */
	public static int top(World world, int x, int z) {
		return top(world, x, z, false);
	}

	static int top(World world, int x, int z, boolean withLeaves) {
		for (int y = world.getActualHeight() - 1; y >= 0; y--) {
			Block block = world.getBlock(x, y, z);
			Material material = block.getMaterial();
			if (material.blocksMovement() && (withLeaves || material != Material.leaves)) {
				return y + 1;
			}
		}
		return 0;
	}

	// ---------------------------------------------------------------- the tick (in place of 1.7.10's onLivingUpdate)

	public void aiStep() {
		World world = dragon.worldObj;
		if (world.isRemote) {
			float flap = MathHelper.cos(dragon.animTime * (float) (Math.PI * 2));
			float oldFlap = MathHelper.cos(dragon.prevAnimTime * (float) (Math.PI * 2));
			if (oldFlap <= -0.3F && flap >= -0.3F ) {
				world.playSound(dragon.posX, dragon.posY, dragon.posZ, "mob.enderdragon.wings", 5.0F, 0.8F + dragon.getRNG().nextFloat() * 0.3F, false);
			}
			syncPhaseFromWatcher();
			if (!isSitting() && --growlTime < 0) {
				world.playSound(dragon.posX, dragon.posY, dragon.posZ, "mob.enderdragon.growl", 2.5F, 0.8F + dragon.getRNG().nextFloat() * 0.3F, false);
				growlTime = 200 + dragon.getRNG().nextInt(200);
			}
		}
		dragon.prevAnimTime = dragon.animTime;
		if (dragon.getHealth() <= 0.0F) {
			world.spawnParticle("largeexplode", dragon.posX + (dragon.getRNG().nextFloat() - 0.5F) * 8.0F, dragon.posY + 2.0 + (dragon.getRNG().nextFloat() - 0.5F) * 4.0F,
				dragon.posZ + (dragon.getRNG().nextFloat() - 0.5F) * 8.0F, 0.0, 0.0, 0.0);
			return;
		}
		checkCrystals();
		float horizontal = MathHelper.sqrt_double(dragon.motionX * dragon.motionX + dragon.motionZ * dragon.motionZ);
		float flapSpeed = 0.2F / (horizontal * 10.0F + 1.0F);
		flapSpeed *= (float) Math.pow(2.0, dragon.motionY);
		if (isSitting()) {
			dragon.animTime += 0.1F;
		} else if (dragon.slowed) {
			dragon.animTime += flapSpeed * 0.5F;
		} else {
			dragon.animTime += flapSpeed;
		}
		dragon.rotationYaw = MathHelper.wrapAngleTo180_float(dragon.rotationYaw);
		record();
		if (!world.isRemote) {
			DragonPhases.Instance phase = current;
			phase.serverTick();
			if (current != phase) {
				current.serverTick();
			}
			Vec3 target = current.flyTarget();
			if (target != null) {
				steer(target);
			}
		} else {
			lerpFromServer();
			current.clientTick();
		}
		dragon.renderYawOffset = dragon.rotationYaw;
		placeParts();
		if (!world.isRemote) {
			boolean head = checkWalls(dragon.dragonPartHead.boundingBox);
			boolean neckWall = checkWalls(neck.boundingBox);
			boolean body = checkWalls(dragon.dragonPartBody.boundingBox);
			dragon.slowed = head | neckWall | body;
			EnderDragonFight fight = fight();
			if (fight != null) {
				fight.updateDragon(dragon);
			}
		}
	}

	/** The client takes the phase the server synced (26.3 runs the phase's begin and end on both sides). */
	private void syncPhaseFromWatcher() {
		if (watcherReady()) {
			DragonPhase synced = DragonPhase.byId(dragon.getDataWatcher().getWatchableObjectInt(PHASE_WATCHER));
			if (synced != current.phase()) {
				setPhase(synced);
			}
		}
	}

	/** 1.7.10's client following the server's places (its own interpolation, kept). */
	private void lerpFromServer() {
		AccessorLivingLerp lerp = (AccessorLivingLerp) dragon;
		int steps = lerp.etfu$getNewPosRotationIncrements();
		if (steps > 0) {
			double x = dragon.posX + (lerp.etfu$getNewPosX() - dragon.posX) / steps;
			double y = dragon.posY + (lerp.etfu$getNewPosY() - dragon.posY) / steps;
			double z = dragon.posZ + (lerp.etfu$getNewPosZ() - dragon.posZ) / steps;
			double yaw = MathHelper.wrapAngleTo180_double(lerp.etfu$getNewRotationYaw() - dragon.rotationYaw);
			dragon.rotationYaw = (float) (dragon.rotationYaw + yaw / steps);
			dragon.rotationPitch = (float) (dragon.rotationPitch + (lerp.etfu$getNewRotationPitch() - dragon.rotationPitch) / steps);
			lerp.etfu$setNewPosRotationIncrements(steps - 1);
			dragon.setPosition(x, y, z);
		}
	}

	/** The history: (yaw, y) before the move, 64 kept, the first filling all (1.7.10's ring buffer). */
	private void record() {
		if (dragon.ringBufferIndex < 0) {
			for (int i = 0; i < dragon.ringBuffer.length; i++) {
				dragon.ringBuffer[i][0] = dragon.rotationYaw;
				dragon.ringBuffer[i][1] = dragon.posY;
			}
		}
		if (++dragon.ringBufferIndex == dragon.ringBuffer.length) {
			dragon.ringBufferIndex = 0;
		}
		dragon.ringBuffer[dragon.ringBufferIndex][0] = dragon.rotationYaw;
		dragon.ringBuffer[dragon.ringBufferIndex][1] = dragon.posY;
	}

	/** The history n ticks back: {yaw, y}. */
	private double[] history(int n) {
		return dragon.ringBuffer[(dragon.ringBufferIndex - n) & 63];
	}

	/** 26.3's steering towards the phase's target, then the move and friction. */
	private void steer(Vec3 target) {
		double dx = target.xCoord - dragon.posX;
		double dy = target.yCoord - dragon.posY;
		double dz = target.zCoord - dragon.posZ;
		double distance = dx * dx + dy * dy + dz * dz;
		float max = current.flySpeed();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal > 0.0) {
			dy = MathHelper.clamp_double(dy / horizontal, -max, max);
		}
		dragon.motionY += dy * 0.01;
		dragon.rotationYaw = MathHelper.wrapAngleTo180_float(dragon.rotationYaw);
		Vec3 aim = Vec3.createVectorHelper(dx, target.yCoord - dragon.posY, dz).normalize();
		float yawRad = dragon.rotationYaw * (float) (Math.PI / 180.0);
		Vec3 dir = Vec3.createVectorHelper(MathHelper.sin(yawRad), dragon.motionY, -MathHelper.cos(yawRad)).normalize();
		float dot = Math.max(((float) dir.dotProduct(aim) + 0.5F) / 1.5F, 0.0F);
		if (Math.abs(dx) > 1.0E-5F || Math.abs(dz) > 1.0E-5F) {
			float yRotD = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(180.0F - (float) Math.atan2(dx, dz) * (180.0F / (float) Math.PI) - dragon.rotationYaw), -50.0F, 50.0F);
			yRotA *= 0.8F;
			yRotA += yRotD * current.turnSpeed();
			dragon.rotationYaw += yRotA * 0.1F;
		}
		float span = (float) (2.0 / (distance + 1.0));
		float speed = 0.06F * (dot * span + (1.0F - span));
		float newYaw = dragon.rotationYaw * (float) (Math.PI / 180.0);
		dragon.motionX += speed * MathHelper.sin(newYaw);
		dragon.motionZ += -speed * MathHelper.cos(newYaw);
		if (dragon.slowed) {
			dragon.moveEntity(dragon.motionX * 0.8F, dragon.motionY * 0.8F, dragon.motionZ * 0.8F);
		} else {
			dragon.moveEntity(dragon.motionX, dragon.motionY, dragon.motionZ);
		}
		Vec3 moving = Vec3.createVectorHelper(dragon.motionX, dragon.motionY, dragon.motionZ).normalize();
		double slide = 0.8 + 0.15 * (moving.dotProduct(dir) + 1.0) / 2.0;
		dragon.motionX *= slide;
		dragon.motionY *= 0.91F;
		dragon.motionZ *= slide;
	}

	/** 26.3's part places, from the moved dragon and its history; the wings push and the head and neck bite on the way. */
	private void placeParts() {
		double tilt = (float) (history(5)[1] - history(10)[1]) * 10.0F * ((float) Math.PI / 180.0F);
		float ct = MathHelper.cos((float) tilt);
		float st = MathHelper.sin((float) tilt);
		float r1 = dragon.rotationYaw * (float) (Math.PI / 180.0);
		float s1 = MathHelper.sin(r1);
		float c1 = MathHelper.cos(r1);
		move(dragon.dragonPartBody, s1 * 0.5F, 0.0, -c1 * 0.5F);
		move(dragon.dragonPartWing1, c1 * 4.5F, 2.0, s1 * 4.5F);
		move(dragon.dragonPartWing2, c1 * -4.5F, 2.0, s1 * -4.5F);
		if (!dragon.worldObj.isRemote && dragon.hurtTime == 0) {
			knockBack(around(dragon.dragonPartWing1.boundingBox.expand(4.0, 2.0, 4.0).offset(0.0, -2.0, 0.0)));
			knockBack(around(dragon.dragonPartWing2.boundingBox.expand(4.0, 2.0, 4.0).offset(0.0, -2.0, 0.0)));
			hurtEntities(around(dragon.dragonPartHead.boundingBox.expand(1.0, 1.0, 1.0)));
			hurtEntities(around(neck.boundingBox.expand(1.0, 1.0, 1.0)));
		}
		float a = dragon.rotationYaw * (float) (Math.PI / 180.0) - yRotA * 0.01F;
		float s2 = MathHelper.sin(a);
		float c2 = MathHelper.cos(a);
		float yOff = isSitting() ? -1.0F : (float) (history(5)[1] - history(0)[1]);
		move(dragon.dragonPartHead, s2 * 6.5F * ct, yOff + st * 6.5F, -c2 * 6.5F * ct);
		move(neck, s2 * 5.5F * ct, yOff + st * 5.5F, -c2 * 5.5F * ct);
		double[] p1 = history(5);
		EntityDragonPart[] tails = {dragon.dragonPartTail1, dragon.dragonPartTail2, dragon.dragonPartTail3};
		for (int i = 0; i < 3; i++) {
			double[] p0 = history(12 + i * 2);
			float rot = dragon.rotationYaw * (float) (Math.PI / 180.0) + (float) MathHelper.wrapAngleTo180_double(p0[0] - p1[0]) * (float) (Math.PI / 180.0);
			float s = MathHelper.sin(rot);
			float c = MathHelper.cos(rot);
			float dd = (i + 1) * 2.0F;
			move(tails[i], -(s1 * 1.5F + s * dd) * ct, p0[1] - p1[1] - (dd + 1.5F) * st + 1.5, (c1 * 1.5F + c * dd) * ct);
		}
	}

	/** A part's last place kept as its previous one (1.7.10 updates parts by hand), then put at the dragon plus the offset. */
	private void move(EntityDragonPart part, double x, double y, double z) {
		part.onUpdate();
		part.setLocationAndAngles(dragon.posX + x, dragon.posY + y, dragon.posZ + z, 0.0F, 0.0F);
	}

	/** Living things in the box, not the dragon, its parts, or creative players. */
	@SuppressWarnings("unchecked")
	private List<EntityLivingBase> around(AxisAlignedBB box) {
		List<EntityLivingBase> found = new ArrayList<>();
		for (Entity entity : (List<Entity>) dragon.worldObj.getEntitiesWithinAABBExcludingEntity(dragon, box)) {
			if (entity instanceof EntityLivingBase living && !(entity instanceof EntityPlayer player && player.capabilities.disableDamage)) {
				found.add(living);
			}
		}
		return found;
	}

	private void knockBack(List<EntityLivingBase> entities) {
		double cx = (dragon.dragonPartBody.boundingBox.minX + dragon.dragonPartBody.boundingBox.maxX) / 2.0;
		double cz = (dragon.dragonPartBody.boundingBox.minZ + dragon.dragonPartBody.boundingBox.maxZ) / 2.0;
		for (EntityLivingBase entity : entities) {
			double dx = entity.posX - cx;
			double dz = entity.posZ - cz;
			double dd = Math.max(dx * dx + dz * dz, 0.1);
			entity.addVelocity(dx / dd * 4.0, 0.2F, dz / dd * 4.0);
			entity.velocityChanged = true;
			if (!isSitting() && entity.func_142015_aE() < entity.ticksExisted - 2) {
				entity.attackEntityFrom(DamageSource.causeMobDamage(dragon), 5.0F);
				EnchantmentHelper.func_151384_a(entity, dragon);
				EnchantmentHelper.func_151385_b(dragon, entity);
			}
		}
	}

	private void hurtEntities(List<EntityLivingBase> entities) {
		for (EntityLivingBase entity : entities) {
			entity.attackEntityFrom(DamageSource.causeMobDamage(dragon), 10.0F);
			EnchantmentHelper.func_151384_a(entity, dragon);
			EnchantmentHelper.func_151385_b(dragon, entity);
		}
	}

	/** 26.3's checkWalls: breakable blocks in the box removed (mob griefing), unbreakable ones slow it. */
	private boolean checkWalls(AxisAlignedBB box) {
		int x0 = MathHelper.floor_double(box.minX);
		int y0 = MathHelper.floor_double(box.minY);
		int z0 = MathHelper.floor_double(box.minZ);
		int x1 = MathHelper.floor_double(box.maxX);
		int y1 = MathHelper.floor_double(box.maxY);
		int z1 = MathHelper.floor_double(box.maxZ);
		boolean hitWall = false;
		boolean destroyed = false;
		World world = dragon.worldObj;
		boolean griefing = world.getGameRules().getGameRuleBooleanValue("mobGriefing");
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					Block block = world.getBlock(x, y, z);
					if (block.getMaterial() == Material.air || block == Blocks.fire) {
						continue;
					}
					if (griefing && !immune(block)) {
						destroyed |= world.setBlockToAir(x, y, z);
					} else {
						hitWall = true;
					}
				}
			}
		}
		if (destroyed && world instanceof WorldServer server) {
			int x = x0 + dragon.getRNG().nextInt(x1 - x0 + 1);
			int y = y0 + dragon.getRNG().nextInt(y1 - y0 + 1);
			int z = z0 + dragon.getRNG().nextInt(z1 - z0 + 1);
			server.func_147487_a("largeexplode", x + 0.5, y + 0.5, z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
		}
		return hitWall;
	}

	private static Set<Block> immune;

	/** 26.3's dragon_immune: what it can't break (EFR's own such blocks by name). */
	private static boolean immune(Block block) {
		if (immune == null) {
			Set<Block> set = new HashSet<>();
			Collections.addAll(set, Blocks.bedrock, Blocks.end_portal, Blocks.end_portal_frame, Blocks.command_block, Blocks.obsidian,
				Blocks.end_stone, Blocks.iron_bars);
			for (String name : new String[] {"etfuturum:end_gateway", "etfuturum:crying_obsidian", "etfuturum:respawn_anchor"}) {
				Object found = Block.blockRegistry.getObject(name);
				if (found instanceof Block efr && efr != Blocks.air) {
					set.add(efr);
				}
			}
			immune = set;
		}
		return immune.contains(block);
	}

	/** 26.3's checkCrystals: the nearest crystal heals it 1 every 10 ticks; one tick in 10 it looks for the nearest. */
	@SuppressWarnings("unchecked")
	private void checkCrystals() {
		EntityEnderCrystal crystal = dragon.healingEnderCrystal;
		if (crystal != null) {
			if (crystal.isDead) {
				dragon.healingEnderCrystal = null;
			} else if (dragon.ticksExisted % 10 == 0 && dragon.getHealth() < dragon.getMaxHealth()) {
				dragon.setHealth(dragon.getHealth() + 1.0F);
			}
		}
		if (dragon.getRNG().nextInt(10) == 0) {
			EntityEnderCrystal nearest = null;
			double best = Double.MAX_VALUE;
			for (EntityEnderCrystal found : (List<EntityEnderCrystal>) dragon.worldObj.getEntitiesWithinAABB(EntityEnderCrystal.class, dragon.boundingBox.expand(32.0, 32.0, 32.0))) {
				double d = found.getDistanceSqToEntity(dragon);
				if (d < best) {
					best = d;
					nearest = found;
				}
			}
			dragon.healingEnderCrystal = nearest;
		}
	}

	// ---------------------------------------------------------------- damage and death

	/** 26.3's hurt via a part (in place of 1.7.10's attackEntityFromPart). */
	public boolean hurt(EntityDragonPart part, DamageSource source, float damage) {
		if (current.phase() == DragonPhase.DYING) {
			return false;
		}
		damage = current.onHurt(source, damage);
		if (part != dragon.dragonPartHead && part != neck) {
			damage = damage / 4.0F + Math.min(damage, 1.0F);
		}
		if (damage < 0.01F) {
			return false;
		}
		if (source.getEntity() instanceof EntityPlayer || source.isExplosion()) {
			float before = dragon.getHealth();
			((AccessorEntityDragon) dragon).etfu$reallyHurt(source, damage);
			if (isSitting()) {
				sittingDamageReceived += before - dragon.getHealth();
				if (sittingDamageReceived > 0.25F * dragon.getMaxHealth()) {
					sittingDamageReceived = 0.0F;
					setPhase(DragonPhase.TAKEOFF);
				}
			}
		}
		return true;
	}

	/** The killing blow (26.3's handleKillingBlow): in flight it lives on at 1, dying; perched it dies there. */
	public boolean holdOffDeath() {
		if (!isSitting()) {
			dragon.setHealth(1.0F);
			setPhase(DragonPhase.DYING);
			return true;
		}
		return false;
	}

	/** 26.3's tickDeath (in place of 1.7.10's onDeathUpdate). */
	public void tickDeath() {
		EnderDragonFight fight = fight();
		if (fight != null) {
			fight.updateDragon(dragon);
		}
		int time = ++dragon.deathTicks;
		java.util.Random random = dragon.getRNG();
		if (time >= 180 && time <= 200) {
			dragon.worldObj.spawnParticle("hugeexplosion", dragon.posX + (random.nextFloat() - 0.5F) * 8.0F, dragon.posY + 2.0 + (random.nextFloat() - 0.5F) * 4.0F,
				dragon.posZ + (random.nextFloat() - 0.5F) * 8.0F, 0.0, 0.0, 0.0);
		}
		int xp = fight != null && !fight.hasPreviouslyKilledDragon() ? 12000 : 500;
		boolean loot = dragon.worldObj.getGameRules().getGameRuleBooleanValue("doMobLoot");
		if (!dragon.worldObj.isRemote) {
			if (time > 150 && time % 5 == 0 && loot) {
				award(MathHelper.floor_float(xp * 0.08F));
			}
			if (time == 1 ) {
				dragon.worldObj.playBroadcastSound(1018, (int) dragon.posX, (int) dragon.posY, (int) dragon.posZ, 0);
			}
		}
		dragon.moveEntity(0.0, 0.1F, 0.0);
		for (Entity part : dragon.getParts()) {
			part.prevPosX = part.posX;
			part.prevPosY = part.posY;
			part.prevPosZ = part.posZ;
			part.setPosition(part.posX, part.posY + 0.1F, part.posZ);
		}
		if (time >= 200 && !dragon.worldObj.isRemote) {
			if (loot) {
				award(MathHelper.floor_float(xp * 0.2F));
			}
			if (fight != null) {
				fight.setDragonKilled(dragon);
			}
			dragon.setDead();
		}
	}

	private void award(int amount) {
		while (amount > 0) {
			int split = EntityXPOrb.getXPSplit(amount);
			amount -= split;
			dragon.worldObj.spawnEntityInWorld(new EntityXPOrb(dragon.worldObj, dragon.posX, dragon.posY, dragon.posZ, split));
		}
	}

	/** A crystal destroyed (the fight tells it): the one healing it hurts its head 10; then the phase hears of it. */
	public void onCrystalDestroyed(EntityEnderCrystal crystal, DamageSource source) {
		EntityPlayer player = source.getEntity() instanceof EntityPlayer by ? by : nearestPlayer(crystal.posX, crystal.posY, crystal.posZ, 64.0, false);
		if (crystal == dragon.healingEnderCrystal) {
			hurt(dragon.dragonPartHead, DamageSource.setExplosionSource(null), 10.0F);
		}
		current.onCrystalDestroyed(player);
	}

	boolean canAttack(EntityPlayer player) {
		return DragonPhases.attackable(player);
	}

	/** The nearest attackable player to a place within a range (squared distance), seen or not. */
	EntityPlayer nearestPlayer(double x, double y, double z, double range, boolean mustSee) {
		EntityPlayer nearest = null;
		double best = range == Double.MAX_VALUE ? Double.MAX_VALUE : range * range;
		for (Object entity : dragon.worldObj.playerEntities) {
			EntityPlayer player = (EntityPlayer) entity;
			if (!DragonPhases.attackable(player) || mustSee && !dragon.canEntityBeSeen(player)) {
				continue;
			}
			double d = player.getDistanceSq(x, y, z);
			if (d < best) {
				best = d;
				nearest = player;
			}
		}
		return nearest;
	}

	/** SittingScanning's: the nearest attackable player within the range, the height difference at most up, seen. */
	EntityPlayer nearestPlayerAround(double range, double height) {
		EntityPlayer nearest = null;
		double best = range * range;
		for (Object entity : dragon.worldObj.playerEntities) {
			EntityPlayer player = (EntityPlayer) entity;
			if (!DragonPhases.attackable(player) || Math.abs(player.posY - dragon.posY) > height || !dragon.canEntityBeSeen(player)) {
				continue;
			}
			double d = player.getDistanceSqToEntity(dragon);
			if (d < best) {
				best = d;
				nearest = player;
			}
		}
		return nearest;
	}

	/** 26.3's head look: its pitch -45 / (the distance to the podium / 4, at least 1), or -45 perched; the yaw its own. */
	Vec3 headLookVector() {
		int[] egg = podiumTop(false);
		double distance = Math.max(Math.sqrt(dragon.getDistanceSq(egg[0] + 0.5, egg[1] + 0.5, egg[2] + 0.5)) / 4.0F, 1.0F);
		float pitch = isSitting() ? -45.0F : (float) (-(6.0F / distance) * 1.5F * 5.0F);
		float yaw = dragon.rotationYaw * (float) (Math.PI / 180.0);
		float p = pitch * (float) (Math.PI / 180.0);
		return Vec3.createVectorHelper(-MathHelper.sin(yaw) * MathHelper.cos(p), -MathHelper.sin(p), MathHelper.cos(yaw) * MathHelper.cos(p));
	}

	// ---------------------------------------------------------------- the node graph

	private int[][] nodes() {
		if (nodes == null) {
			int[][] made = new int[24][3];
			for (int i = 0; i < 24; i++) {
				int x = NODES[i][0];
				int z = NODES[i][1];
				int add = i >= 12 && i < 20 ? 15 : 5;
				made[i][0] = x;
				made[i][1] = Math.max(73, top(dragon.worldObj, x, z) + add);
				made[i][2] = z;
			}
			nodes = made;
		}
		return nodes;
	}

	public int nodeX(int i) {
		return nodes()[i][0];
	}

	public int nodeY(int i) {
		return nodes()[i][1];
	}

	public int nodeZ(int i) {
		return nodes()[i][2];
	}

	public static boolean linked(int from, int to) {
		return (ADJACENCY[from] & 1 << to) != 0;
	}

	int findClosestNode() {
		return findClosestNode(dragon.posX, dragon.posY, dragon.posZ);
	}

	/** The nearest node (from 12 when no crystals are left), strictly, under 100 blocks; else 0. */
	int findClosestNode(double x, double y, double z) {
		int px = MathHelper.floor_double(x);
		int py = MathHelper.floor_double(y);
		int pz = MathHelper.floor_double(z);
		int start = !hasFight() || aliveCrystals() == 0 ? 12 : 0;
		float best = 10000.0F;
		int found = 0;
		for (int i = start; i < 24; i++) {
			int[] node = nodes()[i];
			float dx = node[0] - px;
			float dy = node[1] - py;
			float dz = node[2] - pz;
			float d = dx * dx + dy * dy + dz * dz;
			if (d < best) {
				best = d;
				found = i;
			}
		}
		return found;
	}

	/** For the tests: the node indices of the path between two nodes, or null. */
	public List<Integer> findPath(int from, int to) {
		List<Integer> indices = new ArrayList<>();
		DragonPath path = findPath(from, to, null);
		if (path == null) {
			return null;
		}
		for (int i = 0; i < path.size(); i++) {
			int[] node = path.get(i);
			for (int n = 0; n < 24; n++) {
				if (nodes()[n] == node) {
					indices.add(n);
				}
			}
		}
		return indices;
	}

	/** 26.3's A* over the links (from 12 only with no crystals left), a final node appended; a failure to the nearest found. */
	DragonPath findPath(int startIndex, int endIndex, int[] finalNode) {
		int[][] all = nodes();
		float[] g = new float[24];
		float[] f = new float[24];
		int[] cameFrom = new int[24];
		boolean[] closed = new boolean[24];
		boolean[] open = new boolean[24];
		java.util.Arrays.fill(cameFrom, -1);
		int minIndex = !hasFight() || aliveCrystals() == 0 ? 12 : 0;
		int[] to = all[endIndex];
		f[startIndex] = distance(all[startIndex], to);
		open[startIndex] = true;
		int closest = startIndex;
		while (true) {
			int node = -1;
			for (int i = 0; i < 24; i++) {
				if (open[i] && (node < 0 || f[i] < f[node])) {
					node = i;
				}
			}
			if (node < 0) {
				break;
			}
			open[node] = false;
			if (node == endIndex) {
				return reconstruct(startIndex, endIndex, cameFrom, finalNode);
			}
			if (distance(all[node], to) < distance(all[closest], to)) {
				closest = node;
			}
			closed[node] = true;
			for (int i = minIndex; i < 24; i++) {
				if (!linked(node, i) || closed[i]) {
					continue;
				}
				float tentative = g[node] + distance(all[node], all[i]);
				if (!open[i] || tentative < g[i]) {
					cameFrom[i] = node;
					g[i] = tentative;
					f[i] = g[i] + distance(all[i], to);
					open[i] = true;
				}
			}
		}
		if (closest == startIndex) {
			return null;
		}
		return reconstruct(startIndex, closest, cameFrom, finalNode);
	}

	private DragonPath reconstruct(int from, int end, int[] cameFrom, int[] finalNode) {
		List<int[]> nodes = new ArrayList<>();
		for (int at = end; at >= 0; at = at == from ? -1 : cameFrom[at]) {
			nodes.add(0, nodes()[at]);
		}
		if (finalNode != null) {
			nodes.add(finalNode);
		}
		return new DragonPath(nodes);
	}

	private static float distance(int[] a, int[] b) {
		float dx = b[0] - a[0];
		float dy = b[1] - a[1];
		float dz = b[2] - a[2];
		return MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);
	}

	// ---------------------------------------------------------------- saved

	public void save(NBTTagCompound tag) {
		tag.setInteger("DragonPhase", current.phase().ordinal());
		tag.setInteger("DragonDeathTime", dragon.deathTicks);
		tag.setFloat("sitting_damage_received", sittingDamageReceived);
	}

	public void load(NBTTagCompound tag) {
		// a 1.7.10 dragon saved without a phase was flying about: it holds its pattern
		setPhase(tag.hasKey("DragonPhase") ? DragonPhase.byId(tag.getInteger("DragonPhase")) : DragonPhase.HOLDING_PATTERN);
		dragon.deathTicks = tag.getInteger("DragonDeathTime");
		sittingDamageReceived = tag.getFloat("sitting_damage_received");
	}
}
