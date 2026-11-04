package ganymedes01.etfuturum.trident;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The skeleton horse trap natural lightning can leave (SPEC-modern-movement.md §58.3; 26.3's SkeletonHorse and
 * SkeletonTrapGoal): a skeleton horse marked as one (saved as SkeletonTrap, SkeletonTrapTime). A living player within
 * 10 blocks springs it; left alone it goes after 18 000 ticks unless it was made persistent.
 */
public final class SkeletonTrap {

	/** How long a trap left alone lasts, in its own ticks. */
	public static final int LIFE = 18000;
	private static final double RANGE = 10.0D;
	/** The hurt cooldown its skeletons and extra horses start with (26.3's setInvulnerableTime). */
	private static final int INVULNERABLE_TICKS = 60;
	/** How far the three extra horses are pushed out, each way at most (a triangular spread). */
	private static final double PUSH = 1.1485D;

	private SkeletonTrap() {
	}

	public static boolean isTrap(EntityHorse horse) {
		return ((ISkeletonTrapHorse) horse).etfu$isTrap();
	}

	public static void set(EntityHorse horse, boolean trap) {
		((ISkeletonTrapHorse) horse).etfu$setTrap(trap);
	}

	public static boolean playerNear(EntityHorse horse) {
		for (Object entity : horse.worldObj.playerEntities) {
			EntityPlayer player = (EntityPlayer) entity;
			if (player.isEntityAlive() && player.getDistanceSq(horse.posX, horse.posY, horse.posZ) < RANGE * RANGE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sprung: the horse stops being a trap and is tame and grown, a harmless bolt strikes it, a skeleton mounts it, and
	 * three more tame, grown, persistent skeleton horses come with a skeleton each, pushed outward a little.
	 */
	public static void spring(EntityHorse horse) {
		World world = horse.worldObj;
		float difficulty = Lightning.effectiveDifficulty(world, MathHelper.floor_double(horse.posX), MathHelper.floor_double(horse.posY),
			MathHelper.floor_double(horse.posZ));
		set(horse, false);
		horse.setHorseTamed(true);
		horse.setGrowingAge(0);
		world.addWeatherEffect(Lightning.visualBolt(world, horse.posX, horse.posY, horse.posZ));
		skeleton(world, horse, difficulty).mountEntity(horse);
		Random random = horse.getRNG();
		for (int i = 0; i < 3; i++) {
			EntityHorse other = extraHorse(world, horse);
			skeleton(world, other, difficulty).mountEntity(other);
			other.addVelocity(PUSH * (random.nextDouble() - random.nextDouble()), 0.0D, PUSH * (random.nextDouble() - random.nextDouble()));
		}
	}

	private static EntityHorse extraHorse(World world, EntityHorse trap) {
		EntityHorse horse = new EntityHorse(world);
		horse.setLocationAndAngles(trap.posX, trap.posY, trap.posZ, world.rand.nextFloat() * 360.0F, 0.0F);
		horse.onSpawnWithEgg(new EntityHorse.GroupData(4, 0));
		horse.setGrowingAge(0);
		horse.hurtResistantTime = INVULNERABLE_TICKS;
		horse.func_110163_bv();
		horse.setHorseTamed(true);
		world.spawnEntityInWorld(horse);
		return horse;
	}

	/** A skeleton spawned as normal, persistent, with an iron helmet if it has none, its weapon and helmet enchanted. */
	private static EntitySkeleton skeleton(World world, EntityHorse horse, float difficulty) {
		EntitySkeleton skeleton = new EntitySkeleton(world);
		skeleton.setLocationAndAngles(horse.posX, horse.posY, horse.posZ, horse.rotationYaw, 0.0F);
		skeleton.onSpawnWithEgg(null);
		skeleton.hurtResistantTime = INVULNERABLE_TICKS;
		skeleton.func_110163_bv();
		if (skeleton.getEquipmentInSlot(4) == null) {
			skeleton.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
		}
		enchant(skeleton, 0, difficulty);
		enchant(skeleton, 4, difficulty);
		world.spawnEntityInWorld(skeleton);
		return skeleton;
	}

	/**
	 * Its enchantments replaced as 26.3's spawn equipment for the local difficulty: an enchanting cost from 5 to 5 + 17
	 * times the difficulty's special multiplier.
	 */
	private static void enchant(EntitySkeleton skeleton, int slot, float difficulty) {
		ItemStack stack = skeleton.getEquipmentInSlot(slot);
		if (stack == null) {
			return;
		}
		if (stack.stackTagCompound != null) {
			stack.stackTagCompound.removeTag("ench");
		}
		float special = difficulty < 2.0F ? 0.0F : difficulty > 4.0F ? 1.0F : (difficulty - 2.0F) / 2.0F;
		Random random = skeleton.getRNG();
		EnchantmentHelper.addRandomEnchantment(random, stack, 5 + random.nextInt((int) (special * 17.0F) + 1));
	}
}
