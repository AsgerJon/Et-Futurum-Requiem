package ganymedes01.etfuturum.combat;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ISpecialArmor;

import java.util.UUID;

/**
 * The combat update's damage rules (spec §46.10): modern's armour formula with toughness, protection points without
 * 1.7.10's random halving, burning time and explosion knockback by the levels worn added up, Strength, Weakness, Haste
 * and Mining Fatigue as modern's, and modern's knockback.
 */
public final class DamageRules {

	private DamageRules() {
	}

	/**
	 * Strength as an added 3 a level (MixinPotionAttackDamage gives the amounts; 1.7.10 multiplied), and Haste and
	 * Mining Fatigue on attack speed, ± 10 % of the total a level. Once, at start-up, with the combat update on.
	 */
	public static void install() {
		Potion.damageBoost.func_111186_k().put(SharedMonsterAttributes.attackDamage,
			new AttributeModifier(UUID.fromString("648D7064-6A60-4F59-8ABE-C2C23A6DD7A9"), Potion.damageBoost.getName(), 3.0D, 0));
		Potion.digSpeed.func_111184_a(AttackCooldown.ATTACK_SPEED, "AF8B6E3F-3328-4C0A-AA36-5BA2BB9DBEF3", 0.1D, 2);
		Potion.digSlowdown.func_111184_a(AttackCooldown.ATTACK_SPEED, "55FCED67-E92A-486E-9800-B47F202C4386", -0.1D, 2);
	}

	// ---------------------------------------------------------------- armour

	/** A piece's armour toughness: diamond 2, netherite 3, anything else 0 (other mods' have no modern value). */
	public static float toughness(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemArmor armor)) {
			return 0.0F;
		}
		if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND) {
			return 2.0F;
		}
		return armor.getArmorMaterial() == io.github.asgerjon.mcfly.nether.NetheriteMaterials.ARMOR ? 3.0F : 0.0F;
	}

	/** A piece's knockback resistance: netherite 0.1. */
	public static double knockbackResistance(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ItemArmor armor && armor.getArmorMaterial() == io.github.asgerjon.mcfly.nether.NetheriteMaterials.ARMOR ? 0.1D : 0.0D;
	}

	/** The toughness of the armour worn (equipment slots 1 to 4). */
	public static float wornToughness(EntityLivingBase entity) {
		float toughness = 0.0F;
		for (int slot = 1; slot <= 4; slot++) {
			toughness += toughness(entity.getEquipmentInSlot(slot));
		}
		return toughness;
	}

	/**
	 * The damage left after armour, modern's: armour counts less against big hits, toughness takes some of that back,
	 * and it counts for at least a fifth of itself and at most 20 points.
	 */
	public static float afterArmour(float damage, float armour, float toughness) {
		float counted = MathHelper.clamp_float(armour - damage / (2.0F + toughness / 4.0F), armour * 0.2F, 20.0F);
		return damage * (1.0F - counted / 25.0F);
	}

	/** Armour the modern formula covers: vanilla's and McFly's (netherite, spec §88.4), not other mods' ISpecialArmor. */
	private static boolean isModernArmour(Item item) {
		return item instanceof ItemArmor && !(item instanceof ISpecialArmor);
	}

	/**
	 * A player's armour step, in place of Forge's ArmorProperties.ApplyArmor: vanilla armour and EFR's netherite as
	 * modern's, worn as one set (their points and toughness together), each piece losing max(1, damage ÷ 4) (netherite
	 * nothing to fire); other mods' ISpecialArmor pieces then keep Forge's step, on what is left.
	 */
	public static float playerArmour(EntityPlayer player, ItemStack[] armour, DamageSource source, float damage) {
		ItemStack[] special = new ItemStack[armour.length];
		boolean anySpecial = false;
		float points = 0.0F;
		float toughness = 0.0F;
		boolean anyModern = false;
		for (int slot = 0; slot < armour.length; slot++) {
			ItemStack stack = armour[slot];
			if (stack == null) {
				continue;
			}
			if (isModernArmour(stack.getItem())) {
				points += ((ItemArmor) stack.getItem()).damageReduceAmount;
				toughness += toughness(stack);
				anyModern = true;
			} else {
				special[slot] = stack;
				anySpecial = true;
			}
		}
		float left = damage;
		if (anyModern && !source.isUnblockable()) {
			int wear = (int) Math.max(1.0F, damage / 4.0F);
			for (int slot = 0; slot < armour.length; slot++) {
				ItemStack stack = armour[slot];
				if (stack == null || !isModernArmour(stack.getItem()) || source.isFireDamage() && io.github.asgerjon.mcfly.base.FireResistant.is(stack)) {
					continue;
				}
				stack.damageItem(wear, player);
				if (stack.stackSize <= 0) {
					armour[slot] = null;
				}
			}
			left = afterArmour(damage, points, toughness);
		}
		if (anySpecial) {
			left = ISpecialArmor.ArmorProperties.ApplyArmor(player, special, source, left);
			// Forge empties the slot of a piece it broke, in the array it was given
			for (int slot = 0; slot < armour.length; slot++) {
				if (armour[slot] != null && !isModernArmour(armour[slot].getItem()) && special[slot] == null) {
					armour[slot] = null;
				}
			}
		}
		return left;
	}

	// ---------------------------------------------------------------- protection

	/**
	 * Protection points of one of 1.7.10's protection enchantments (its type: 0 protection, 1 fire, 2 falls, 3
	 * explosions, 4 projectiles): Protection 1 a level for all but the void and /kill, Fire, Blast and Projectile
	 * Protection 2 a level for theirs, Feather Falling 3 a level for falls.
	 */
	public static int protectionPoints(int type, int level, DamageSource source) {
		if (source.canHarmInCreative()) {
			return 0;
		}
		return switch (type) {
			case 0 -> level;
			case 1 -> source.isFireDamage() ? 2 * level : 0;
			case 2 -> source == DamageSource.fall ? 3 * level : 0;
			case 3 -> source.isExplosion() ? 2 * level : 0;
			case 4 -> source.isProjectile() ? 2 * level : 0;
			default -> 0;
		};
	}

	/** The protection points of the stacks' enchantments, added up as they are (the caller caps them at 20). */
	public static int protectionPoints(ItemStack[] stacks, DamageSource source) {
		int points = 0;
		if (stacks == null) {
			return 0;
		}
		for (ItemStack stack : stacks) {
			NBTTagList enchantments = stack == null ? null : stack.getEnchantmentTagList();
			if (enchantments == null) {
				continue;
			}
			for (int i = 0; i < enchantments.tagCount(); i++) {
				int id = enchantments.getCompoundTagAt(i).getShort("id");
				int level = enchantments.getCompoundTagAt(i).getShort("lvl");
				if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
					points += Enchantment.enchantmentsList[id].calcModifierDamage(level, source);
				}
			}
		}
		return points;
	}

	/** The levels of an enchantment on the armour worn (slots 1 to 4), added up, which modern does where 1.7.10 took the highest. */
	public static int wornLevels(EntityLivingBase entity, int enchantmentId) {
		int levels = 0;
		for (int slot = 1; slot <= 4; slot++) {
			ItemStack stack = entity.getEquipmentInSlot(slot);
			NBTTagList enchantments = stack == null ? null : stack.getEnchantmentTagList();
			if (enchantments == null) {
				continue;
			}
			for (int i = 0; i < enchantments.tagCount(); i++) {
				if (enchantments.getCompoundTagAt(i).getShort("id") == enchantmentId) {
					levels += enchantments.getCompoundTagAt(i).getShort("lvl");
				}
			}
		}
		return levels;
	}

	/** Burning for so many ticks, less 15 % a level of Fire Protection worn (none left at 7 levels), rounded up. */
	public static int burningTicks(EntityLivingBase entity, int ticks) {
		double share = Math.max(0.0D, 1.0D - 0.15D * wornLevels(entity, Enchantment.fireProtection.effectId));
		return (int) Math.ceil(ticks * share);
	}

	/** An explosion's push, less 15 % a level of Blast Protection worn (none left at 7 levels). */
	public static double explosionKnockback(EntityLivingBase entity, double knockback) {
		return knockback * (1.0D - Math.min(1.0D, 0.15D * wornLevels(entity, Enchantment.blastProtection.effectId)));
	}

	// ---------------------------------------------------------------- knockback

	/** Knockback resistance: the entity's own (attribute) and its netherite pieces', at most 1. */
	public static double knockbackResistance(EntityLivingBase entity) {
		double resistance = entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue();
		for (int slot = 1; slot <= 4; slot++) {
			resistance += knockbackResistance(entity.getEquipmentInSlot(slot));
		}
		return Math.min(1.0D, resistance);
	}

	/**
	 * Modern's knockback, away from (x, z) (where the push comes from, relative to the entity) with the power less the
	 * resistance: sideways half the motion it had minus the push, up only on the ground (half its motion plus the
	 * push, at most 0.4); nothing at all at full resistance. 1.7.10's resistance was a chance to take none, and its
	 * push always lifted.
	 */
	public static void knockback(EntityLivingBase entity, double power, double x, double z) {
		power *= 1.0D - knockbackResistance(entity);
		if (power <= 0.0D) {
			return;
		}
		entity.isAirBorne = true;
		while (x * x + z * z < 1.0E-5D) {
			x = (entity.getRNG().nextDouble() - entity.getRNG().nextDouble()) * 0.01D;
			z = (entity.getRNG().nextDouble() - entity.getRNG().nextDouble()) * 0.01D;
		}
		double length = Math.sqrt(x * x + z * z);
		entity.motionX = entity.motionX / 2.0D - x / length * power;
		entity.motionZ = entity.motionZ / 2.0D - z / length * power;
		if (entity.onGround) {
			entity.motionY = Math.min(0.4D, entity.motionY / 2.0D + power);
		}
	}
}
