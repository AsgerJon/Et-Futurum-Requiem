package ganymedes01.etfuturum;

import baubles.common.lib.PlayerHandler;
import ganymedes01.etfuturum.compat.CompatBackhand;
import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.enchantment.DepthStrider;
import ganymedes01.etfuturum.enchantment.FrostWalker;
import ganymedes01.etfuturum.enchantment.Mending;
import ganymedes01.etfuturum.enchantment.SwiftSneak;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ModEnchantments {

	public static Enchantment frostWalker;
	public static Enchantment mending;
	public static Enchantment swiftSneak;
	public static Enchantment depthStrider;

	private static final Map<EntityLivingBase, double[]> prevMoveCache = new WeakHashMap<>();

	public static void init() {
		if (ConfigEnchantsPotions.enableFrostWalker)
			frostWalker = new FrostWalker();
		if (ConfigEnchantsPotions.enableMending)
			mending = new Mending();
		if (ConfigEnchantsPotions.enableSwiftSneak)
			swiftSneak = new SwiftSneak();
		if (ConfigEnchantsPotions.enableDepthStrider && ConfigMixins.enableModernSwimming)
			depthStrider = new DepthStrider();
	}

	// Frost Walker logic
	public static void onLivingUpdate(EntityLivingBase entity) {
		if (entity.worldObj.isRemote)
			return;
		if (!ConfigEnchantsPotions.enableFrostWalker)
			return;

		ItemStack boots = entity.getEquipmentInSlot(1);
		int level = 0;
		if ((level = EnchantmentHelper.getEnchantmentLevel(frostWalker.effectId, boots)) > 0 && entity.onGround) {
			double[] prevCoords = prevMoveCache.get(entity);
			if (prevCoords == null || (Math.abs(prevCoords[0] - entity.posX) > 0.003D && Math.abs(prevCoords[1] - entity.posZ) > 0.003D)) {
				int x = (int) entity.posX;
				int y = (int) entity.posY;
				int z = (int) entity.posZ;

				int radius = Math.min(16, 2 + level);

				for (int i = -radius; i <= radius; i++) {
					for (int j = -radius; j <= radius; j++) {
						if (i * i + j * j <= radius * radius) {
							Block block = entity.worldObj.getBlock(x + i, y - 1, z + j);
							Block blockUp = entity.worldObj.getBlock(x + i, y, z + j);
							if (!blockUp.isNormalCube() && blockUp.getMaterial() != Material.water && (block == Blocks.water || block == Blocks.flowing_water)) {
								if (entity.worldObj.getEntitiesWithinAABBExcludingEntity(entity, AxisAlignedBB.getBoundingBox(x + i, y - 1, z + j, x + i + 1, y, z + j + 1)).isEmpty()) {
									entity.worldObj.setBlock(x + i, y - 1, z + j, ModBlocks.FROSTED_ICE.get());
								}
							}
						}
					}
				}
				prevMoveCache.put(entity, new double[]{entity.posX, entity.posZ});
			}
		} else {
			prevMoveCache.remove(entity);
		}
	}

	// Mending logic, as modern (SPEC-modern-movement.md §38). The pickup isn't cancelled: the orb keeps what the
	// repairs leave, and vanilla's pickup that follows (delay, animation, sound) gives that to the player.
	public static void onPlayerPickupXP(PlayerPickupXpEvent event) {
		EntityPlayer player = event.entityPlayer;
		EntityXPOrb orb = event.orb;
		if (player.worldObj.isRemote)
			return;
		if (!ConfigEnchantsPotions.enableMending)
			return;

		orb.xpValue = repairWithExperience(player, orb.xpValue);
	}

	// Repairs a random damaged Mending item at 2 durability per point, picking again while experience is left
	private static int repairWithExperience(EntityPlayer player, int xp) {
		while (xp > 0) {
			List<ItemStack> damaged = getDamagedMendingItems(player);
			if (damaged.isEmpty())
				return xp;
			ItemStack stack = damaged.get(player.getRNG().nextInt(damaged.size()));
			int repair = (int) Math.min(stack.getItemDamage(), 2L * xp);
			stack.setItemDamage(stack.getItemDamage() - repair);
			xp -= repair / 2;
		}
		return 0;
	}

	private static List<ItemStack> getDamagedMendingItems(EntityPlayer player) {
		List<ItemStack> stacks = new ArrayList<>();
		stacks.add(player.getCurrentEquippedItem()); // held
		stacks.add(ModsList.BACKHAND.isLoaded() ? CompatBackhand.getOffhandItem(player) : Offhand.getOffhand(player));
		stacks.add(player.getEquipmentInSlot(1)); // boots
		stacks.add(player.getEquipmentInSlot(2)); // leggings
		stacks.add(player.getEquipmentInSlot(3)); // chestplate
		stacks.add(player.getEquipmentInSlot(4)); // helmet
		if (ModsList.BAUBLES.isLoaded() && ConfigModCompat.baublesMending) {
			ItemStack[] baubles = PlayerHandler.getPlayerBaubles(player).stackList;
			if (baubles != null) {
				Collections.addAll(stacks, baubles);
			}
		}
		stacks.removeIf(stack -> !isDamagedMendingItem(stack));
		return stacks;
	}

	private static boolean isDamagedMendingItem(ItemStack stack) {
		Item item = stack == null ? null : stack.getItem();
		// isRepairable means durability and no subtypes, so mod items using their damage value as a subtype stay out
		return item != null && item.isRepairable() && stack.getItemDamage() > 0
			&& EnchantmentHelper.getEnchantmentLevel(mending.effectId, stack) > 0;
	}
}
