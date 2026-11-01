package ganymedes01.etfuturum.combat;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.items.equipment.ItemShield;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.network.ItemCooldownMessage;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;

/** Blocking with a shield (spec §46.9): which hits it stops, and what a block does to the shield and the attacker. */
public final class ShieldBlocking {

	/** An axe disables a shield for this long. */
	public static final int DISABLE_TICKS = 100;

	private ShieldBlocking() {
	}

	/** The shield a player is blocking with: raised for at least 5 ticks. */
	public static ItemStack blockingWith(EntityLivingBase entity) {
		if (!(entity instanceof EntityPlayer player) || !player.isUsingItem()) {
			return null;
		}
		ItemStack inUse = player.getItemInUse();
		if (inUse == null || !(inUse.getItem() instanceof ItemShield)) {
			return null;
		}
		int raisedFor = inUse.getMaxItemUseDuration() - player.getItemInUseCount();
		return raisedFor >= ItemShield.BLOCK_DELAY ? inUse : null;
	}

	/** What goes through shields: what bypasses armour, fire and lava, cactus, falling anvils and blocks. */
	public static boolean bypasses(DamageSource source) {
		return source.isUnblockable() || source.isFireDamage() || source == DamageSource.cactus || source == DamageSource.anvil
			|| source == DamageSource.fallingBlock;
	}

	/**
	 * Blocks the hit if the defender's shield is raised and faces it (within 90° of the head's yaw); a blocked hit is
	 * blocked in full, wears the shield and plays the block sound, and an axe in melee disables the shield. Inside
	 * the hurt cooldown the hit still wears the shield and an axe still disables it, but makes no sound: modern's
	 * applyItemBlocking comes before the cooldown check, its onBlocked (the sound) only on the way past it. Server.
	 */
	public static boolean tryBlock(EntityLivingBase defender, DamageSource source, float damage) {
		ItemStack shield = blockingWith(defender);
		if (shield == null || damage <= 0.0F || bypasses(source)) {
			return false;
		}
		Entity direct = source.getSourceOfDamage();
		if (direct == null) {
			return false;
		}
		double dx = direct.posX - defender.posX;
		double dz = direct.posZ - defender.posZ;
		double length = Math.sqrt(dx * dx + dz * dz);
		float yaw = defender.rotationYawHead * (float) Math.PI / 180.0F;
		double facing = length < 1.0E-4D ? 1.0D : (-MathHelper.sin(yaw) * dx + MathHelper.cos(yaw) * dz) / length;
		if (Math.acos(Math.max(-1.0D, Math.min(1.0D, facing))) > Math.PI / 2.0D) {
			return false;
		}
		EntityPlayer player = (EntityPlayer) defender;
		boolean inHurtCooldown = (float) defender.hurtResistantTime > (float) defender.maxHurtResistantTime / 2.0F;
		wear(player, shield, damage);
		if (!inHurtCooldown) {
			float pitch = 0.8F + defender.getRNG().nextFloat() * 0.4F;
			defender.worldObj.playSoundEffect(defender.posX, defender.posY, defender.posZ, Reference.MCAssetVer + ":item.shield.block", 1.0F, pitch);
		}
		if (!source.isProjectile() && direct instanceof EntityLivingBase attacker && disablesShields(attacker)) {
			disable(player);
		}
		return true;
	}

	/** Modern's axes disable shields, when the axe is what the attacker hits with rather than an item in use. */
	private static boolean disablesShields(EntityLivingBase attacker) {
		ItemStack weapon = attacker.getHeldItem();
		if (weapon == null || !(weapon.getItem() instanceof ItemAxe)) {
			return false;
		}
		return !(attacker instanceof EntityPlayer player) || !player.isUsingItem() || player.getItemInUse() == weapon;
	}

	/** Nothing below 3 damage, else 1 + the damage rounded down; a broken shield leaves the hand and stops blocking. */
	private static void wear(EntityPlayer player, ItemStack shield, float damage) {
		if (damage < 3.0F) {
			return;
		}
		shield.damageItem(MathHelper.floor_float(1.0F + damage), player);
		if (shield.stackSize <= 0) {
			if (Offhand.getUsingHand(player) == Hand.OFF) {
				Offhand.setOffhand(player, null);
			} else {
				player.destroyCurrentEquippedItem();
			}
			player.clearItemInUse();
		}
	}

	/** On cooldown for 5 s, lowered, the disable sound; the player's client is told. */
	private static void disable(EntityPlayer player) {
		ItemStack shield = player.getItemInUse();
		if (shield == null) {
			return;
		}
		((ICombatPlayer) player).etfu$getCooldowns().add(shield.getItem(), DISABLE_TICKS);
		player.clearItemInUse();
		float pitch = 0.8F + player.getRNG().nextFloat() * 0.4F;
		player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, Reference.MCAssetVer + ":item.shield.break", 0.8F, pitch);
		if (player instanceof EntityPlayerMP playerMP) {
			EtFuturum.networkWrapper.sendTo(new ItemCooldownMessage(shield.getItem(), DISABLE_TICKS), playerMP);
		}
	}
}
