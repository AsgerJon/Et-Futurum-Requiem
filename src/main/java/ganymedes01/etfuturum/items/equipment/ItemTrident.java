package ganymedes01.etfuturum.items.equipment;

import com.google.common.collect.Multimap;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import ganymedes01.etfuturum.items.BaseItem;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.trident.Riptide;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

/**
 * The trident (SPEC-modern-movement.md §58.1): a weapon of 9 damage at 1.1 attacks a second, thrown after a charge of 10
 * ticks or more, or with Riptide (§58.2) launching its user instead. No recipe: until the drowned come it comes from
 * /give and /summon.
 */
public class ItemTrident extends BaseItem {

	/** Ticks of charging before letting go throws it. */
	public static final int THROW_THRESHOLD_TIME = 10;
	/** Modern charges for as long as the button is held. */
	public static final int USE_DURATION = 72000;
	public static final float SHOOT_POWER = 2.5F;
	private static final String THROW_SOUND = Reference.MCAssetVer + ":item.trident.throw";

	public ItemTrident() {
		super("trident");
		setMaxStackSize(1);
		setMaxDamage(250);
		setFull3D();
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return EnumRarity.rare;
	}

	@Override
	public int getItemEnchantability() {
		return 1;
	}

	/** +8 attack damage and -2.9 attack speed in the main hand, under modern combat's weapon ids. */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Multimap getItemAttributeModifiers() {
		Multimap modifiers = super.getItemAttributeModifiers();
		modifiers.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(field_111210_e, "Weapon modifier", 8.0D, 0));
		modifiers.put(AttackCooldown.ATTACK_SPEED.getAttributeUnlocalizedName(), new AttributeModifier(AttackCooldown.WEAPON_SPEED_ID, "Weapon modifier", -2.9D, 0));
		return modifiers;
	}

	/** A hit costs 1 use. */
	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
		stack.damageItem(1, attacker);
		return true;
	}

	/** Breaking a block costs 2 uses (not one that breaks at once). */
	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
		if (block.getBlockHardness(world, x, y, z) != 0.0F) {
			stack.damageItem(2, entity);
		}
		return true;
	}

	/** In creative it breaks no blocks, as a sword (modern's tool can't destroy blocks in creative). */
	@Override
	public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
		return player.capabilities.isCreativeMode;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.none;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return USE_DURATION;
	}

	/** Starts charging, unless the next use would break it, or it has Riptide away from water and rain. */
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (!nextUseWillBreak(stack) && (Riptide.strength(stack) <= 0.0F || Riptide.isInWaterOrRain(player))) {
			player.setItemInUse(stack, getMaxItemUseDuration(stack));
		}
		return stack;
	}

	/**
	 * Let go after 10 ticks or more: thrown, taking 1 use that never breaks it. With Riptide (still in water or rain, not
	 * riding) the player is launched instead (§58.2).
	 */
	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int remainingTicks) {
		int timeHeld = getMaxItemUseDuration(stack) - remainingTicks;
		float riptide = Riptide.strength(stack);
		if (timeHeld < THROW_THRESHOLD_TIME || nextUseWillBreak(stack)
			|| riptide > 0.0F && (!Riptide.isInWaterOrRain(player) || player.isRiding())) {
			return;
		}
		if (riptide > 0.0F) {
			if (!world.isRemote) {
				damageWithoutBreaking(stack, player);
				world.playSoundAtEntity(player, Riptide.sound(stack), 1.0F, 1.0F);
				countUse(player);
			}
			Riptide.launch(player, riptide);
			return;
		}
		if (world.isRemote) {
			return;
		}
		damageWithoutBreaking(stack, player);
		ItemStack thrownItem = stack.copy();
		thrownItem.stackSize = 1;
		EntityThrownTrident trident = new EntityThrownTrident(world, player, thrownItem);
		trident.shootFromRotation(player, player.rotationPitch, player.rotationYaw, 0.0F, SHOOT_POWER, 1.0F);
		if (player.capabilities.isCreativeMode) {
			trident.pickup = EntityThrownTrident.PICKUP_CREATIVE_ONLY;
		} else {
			removeFromHands(player, stack);
		}
		world.spawnEntityInWorld(trident);
		world.playSoundAtEntity(trident, THROW_SOUND, 1.0F, 1.0F);
		countUse(player);
	}

	private void countUse(EntityPlayer player) {
		StatBase used = StatList.objectUseStats[Item.getIdFromItem(this)];
		if (used != null) {
			player.addStat(used, 1);
		}
	}

	/** Modern's next damage will break it: 1 use left. */
	public static boolean nextUseWillBreak(ItemStack stack) {
		return stack.isItemStackDamageable() && stack.getItemDamage() >= stack.getMaxDamage() - 1;
	}

	/** One use taken (Unbreaking counting), never the last: modern's hurtWithoutBreaking. Nothing in creative. */
	private static void damageWithoutBreaking(ItemStack stack, EntityPlayer player) {
		if (player.capabilities.isCreativeMode) {
			return;
		}
		stack.attemptDamageItem(1, player.getRNG());
		stack.setItemDamage(Math.min(stack.getItemDamage(), stack.getMaxDamage() - 1));
	}

	/** The thrown stack leaves the hand it was in (the off-hand or a hotbar slot). */
	private static void removeFromHands(EntityPlayer player, ItemStack stack) {
		if (Offhand.getOffhand(player) == stack) {
			Offhand.setOffhand(player, null);
			return;
		}
		InventoryPlayer inventory = player.inventory;
		for (int slot = 0; slot < inventory.mainInventory.length; slot++) {
			if (inventory.mainInventory[slot] == stack) {
				inventory.mainInventory[slot] = null;
				return;
			}
		}
	}
}
