package ganymedes01.etfuturum.combat;

import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.util.UUID;

/** The attack cooldown (spec §46.1, §46.2): the attack speed attribute and the charge it gives. */
public final class AttackCooldown {

	/** Attacks a second; base 4, synced to clients for the indicator. */
	public static final IAttribute ATTACK_SPEED = new RangedAttribute("generic.attackSpeed", 4.0D, 0.0D, 1024.0D)
		.setDescription("Attack Speed").setShouldWatch(true);
	/** The weapon's own speed modifier, as 1.9 numbered it. */
	public static final UUID WEAPON_SPEED_ID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
	/** Full charge, for critical hits, sprint knockback and sweeps. */
	public static final float FULL = 0.9F;

	private AttackCooldown() {
	}

	/** Ticks from one full-strength attack to the next: 20 / attack speed. */
	public static float delay(EntityPlayer player) {
		IAttributeInstance speed = player.getEntityAttribute(ATTACK_SPEED);
		double value = speed == null ? ATTACK_SPEED.getDefaultValue() : speed.getAttributeValue();
		return (float) (1.0D / value * 20.0D);
	}

	/** How far the weapon has charged, 0 to 1, {@code partialTick} ahead. */
	public static float charge(EntityPlayer player, float partialTick) {
		return MathHelper.clamp_float((((ICombatPlayer) player).etfu$getAttackTicks() + partialTick) / delay(player), 0.0F, 1.0F);
	}

	/** The same for the swap timer, which drives the held item's dip. */
	public static float swapCharge(EntityPlayer player, float partialTick) {
		return MathHelper.clamp_float((((ICombatPlayer) player).etfu$getSwapTicks() + partialTick) / delay(player), 0.0F, 1.0F);
	}

	/** The share of the weapon's damage an attack at this charge deals: 0.2 + 0.8 × charge². */
	public static float damageScale(float charge) {
		return 0.2F + charge * charge * 0.8F;
	}
}
