package ganymedes01.etfuturum.trident;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

/**
 * Riptide's launch and spin attack (SPEC-modern-movement.md §58.2), for players. A Riptide trident works only in water
 * or rain; let go after a charge, it launches the player along the look and starts a spin of 20 ticks, in which the
 * first mob in the way takes a melee hit of base damage 8, ending the spin and bouncing the player back. The spin is
 * counted down on each side; the server's is synced in bit 6 of the pose byte when the pose system or the off-hand has
 * registered it.
 */
public final class Riptide {

	public static final int SPIN_TICKS = 20;
	public static final float SPIN_DAMAGE = 8.0F;
	/** The pose byte's free bit: pose 0-2, swimming 3, the off-hand's main arm and using hand 4-5. */
	private static final int SPIN_BIT = 64;

	private Riptide() {
	}

	/** The trident's Riptide level, 0 without it (or without the enchantment). */
	public static int level(ItemStack trident) {
		return ModEnchantments.riptide == null || trident == null ? 0 : EnchantmentHelper.getEnchantmentLevel(ModEnchantments.riptide.effectId, trident);
	}

	/** Modern's spin attack strength: 1.5 at I, 0.75 more a level. */
	public static float strength(ItemStack trident) {
		int level = level(trident);
		return level > 0 ? 1.5F + 0.75F * (level - 1) : 0.0F;
	}

	/** The riptide sound of its level (the third for III and above). */
	public static String sound(ItemStack trident) {
		return Reference.MCAssetVer + ":item.trident.riptide_" + Math.min(Math.max(level(trident), 1), 3);
	}

	/** In water, or in rain falling on the feet's or the head's block (modern's isInWaterOrRain). */
	public static boolean isInWaterOrRain(Entity entity) {
		if (entity.isInWater()) {
			return true;
		}
		int x = MathHelper.floor_double(entity.posX);
		int z = MathHelper.floor_double(entity.posZ);
		World world = entity.worldObj;
		return world.canLightningStrikeAt(x, MathHelper.floor_double(entity.boundingBox.minY), z)
			|| world.canLightningStrikeAt(x, MathHelper.floor_double(entity.boundingBox.maxY), z);
	}

	/** Launches the player along the look with this strength (moved up 1.2 first when on the ground). Both sides. */
	public static void launch(EntityPlayer player, float strength) {
		float toRadians = (float) Math.PI / 180.0F;
		float x = -MathHelper.sin(player.rotationYaw * toRadians) * MathHelper.cos(player.rotationPitch * toRadians);
		float y = -MathHelper.sin(player.rotationPitch * toRadians);
		float z = MathHelper.cos(player.rotationYaw * toRadians) * MathHelper.cos(player.rotationPitch * toRadians);
		float length = MathHelper.sqrt_float(x * x + y * y + z * z);
		player.addVelocity(x * strength / length, y * strength / length, z * strength / length);
		start(player);
		if (player.onGround) {
			player.moveEntity(0.0D, 1.1999999F, 0.0D);
		}
	}

	/** Starts the spin on this side; the server syncs it. */
	public static void start(EntityPlayer player) {
		((IRiptidePlayer) player).etfu$setSpinTicks(SPIN_TICKS);
		if (!player.worldObj.isRemote) {
			setSyncedSpin(player, true);
		}
	}

	/** Ends the spin on this side; the server syncs it. */
	public static void stop(EntityPlayer player) {
		((IRiptidePlayer) player).etfu$setSpinTicks(0);
		if (!player.worldObj.isRemote) {
			setSyncedSpin(player, false);
		}
	}

	/** Whether the entity spins: the synced bit (other players on a client), or this side's count. */
	public static boolean isSpinning(EntityLivingBase entity) {
		if (!(entity instanceof EntityPlayer player)) {
			return false;
		}
		if (((IRiptidePlayer) player).etfu$getSpinTicks() > 0) {
			return true;
		}
		return hasPoseByte() && (player.getDataWatcher().getWatchableObjectByte(McFlyConfig.poseDataWatcherId) & SPIN_BIT) != 0;
	}

	/**
	 * One tick of the spin, after the player moved from {@code before} (modern's checkAutoSpinAttack): the first living
	 * mob in the space swept takes the hit and the player bounces back; a wall ends it too. The local player's client
	 * sends the hit as an attack (the server's own player doesn't move by itself); the server ends the spin when that
	 * attack arrives, or when its count runs out.
	 */
	public static void tick(EntityPlayer player, AxisAlignedBB before) {
		IRiptidePlayer spinner = (IRiptidePlayer) player;
		int ticks = spinner.etfu$getSpinTicks();
		if (ticks <= 0) {
			return;
		}
		spinner.etfu$setSpinTicks(--ticks);
		if (player.worldObj.isRemote && RiptideClient.spinOfTheLocalPlayer(player, before)) {
			return;
		}
		if (ticks <= 0) {
			stop(player);
		}
	}

	/** The local player's spin hits the first living mob it swept through: an ordinary attack, sent; then the bounce. */
	static boolean hitFirstMob(EntityPlayer player, AxisAlignedBB before, Consumer<EntityLivingBase> attack) {
		EntityLivingBase target = firstMobIn(player, before);
		if (target != null) {
			attack.accept(target);
			stop(player);
			player.motionX *= -0.2D;
			player.motionY *= -0.2D;
			player.motionZ *= -0.2D;
			return true;
		}
		if (player.isCollidedHorizontally) {
			stop(player);
			return true;
		}
		return false;
	}

	private static EntityLivingBase firstMobIn(EntityPlayer player, AxisAlignedBB before) {
		AxisAlignedBB now = player.boundingBox;
		AxisAlignedBB swept = AxisAlignedBB.getBoundingBox(Math.min(before.minX, now.minX), Math.min(before.minY, now.minY), Math.min(before.minZ, now.minZ),
			Math.max(before.maxX, now.maxX), Math.max(before.maxY, now.maxY), Math.max(before.maxZ, now.maxZ));
		List<?> entities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, swept);
		for (Object entity : entities) {
			if (entity instanceof EntityLivingBase living && living.isEntityAlive()) {
				return living;
			}
		}
		return null;
	}

	/** The base damage of the player's attack: 8 while spinning, which the hit ends (on the server). */
	public static float attackBase(EntityPlayer player, float base) {
		if (!isSpinning(player)) {
			return base;
		}
		if (!player.worldObj.isRemote) {
			stop(player);
		}
		return SPIN_DAMAGE;
	}

	private static void setSyncedSpin(EntityPlayer player, boolean spinning) {
		if (!hasPoseByte()) {
			return;
		}
		byte current = player.getDataWatcher().getWatchableObjectByte(McFlyConfig.poseDataWatcherId);
		byte value = (byte) (spinning ? current | SPIN_BIT : current & ~SPIN_BIT);
		if (value != current) {
			player.getDataWatcher().updateObject(McFlyConfig.poseDataWatcherId, value);
		}
	}

	/** The pose byte exists when the pose system or the off-hand registered it. */
	private static boolean hasPoseByte() {
		return ModernMovement.isPoseSystemEnabled() || Offhand.isEnabled();
	}
}
