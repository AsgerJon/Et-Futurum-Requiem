package io.github.asgerjon.mcfly.gameevent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.movement.IPosePlayer;
import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 * The game events 1.7.10 has a Forge event for (SPEC-modern-movement.md §90.9, step H1), posted where 26.3 posts them,
 * last of the event's handlers and only if none cancelled it: a block placed (`block_place`) or broken by a player
 * (`block_destroy`), an entity hurt (`entity_damage`) or dying (`entity_die`). The rest come from McFly's mixins
 * (`mixins/early/gameevent`), through the helpers below. A world unloaded forgets its listeners.
 */
public final class GameEventHooks {

	public static final GameEventHooks INSTANCE = new GameEventHooks();

	private GameEventHooks() {
	}

	/** In init. */
	public static void register() {
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/**
	 * Forge fires its place event for any block an item's use changes, not only for one placed. 26.3 posts by what did
	 * it: a tool's change (a hoe's tilling, an axe's stripping, a shovel's path) is `block_change` about the new block
	 * (its `BlockTransformer`); bone meal posts its own `item_interact_finish` and nothing here; the rest `block_place`.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void placed(BlockEvent.PlaceEvent event) {
		ItemStack held = event.itemInHand;
		Item item = held == null ? null : held.getItem();
		if (item instanceof ItemDye && held.getItemDamage() == 15) {
			return;
		}
		String what = item instanceof ItemHoe || item instanceof ItemTool ? "block_change" : "block_place";
		GameEvents.postAtBlock(event.world, what, event.x, event.y, event.z, event.player, event.block, event.blockMetadata);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void broken(BlockEvent.BreakEvent event) {
		GameEvents.postAtBlock(event.world, "block_destroy", event.x, event.y, event.z, event.getPlayer(), event.block, event.blockMetadata);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void hurt(LivingHurtEvent event) {
		GameEvents.postAt(event.entityLiving, "entity_damage");
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void died(LivingDeathEvent event) {
		GameEvents.postAt(event.entityLiving, "entity_die");
	}

	@SubscribeEvent
	public void unloaded(WorldEvent.Unload event) {
		GameEvents.forget(event.world);
	}

	// ---------------------------------------------------------------- the entity's own (from the mixins)

	/**
	 * The block an entity stands on, as 26.3's step and landing name it: the one right under its feet (26.3's
	 * supporting block), else 1.7.10's step block 0.2 below them (a fence, wall or gate under that one standing in).
	 * {block, meta}; air if none.
	 */
	private static Object[] under(Entity entity) {
		World world = entity.worldObj;
		int x = MathHelper.floor_double(entity.posX), z = MathHelper.floor_double(entity.posZ);
		int y = MathHelper.floor_double(entity.boundingBox.minY - 1.0E-5D);
		Block block = world.getBlock(x, y, z);
		if (block.getMaterial() == Material.air) {
			y = MathHelper.floor_double(entity.posY - 0.20000000298023224D - entity.yOffset);
			block = world.getBlock(x, y, z);
			int renderType = world.getBlock(x, y - 1, z).getRenderType();
			if (block.getMaterial() == Material.air && (renderType == 11 || renderType == 32 || renderType == 21)) {
				y--;
				block = world.getBlock(x, y, z);
			}
		}
		return new Object[]{block, world.getBlockMetadata(x, y, z)};
	}

	private static void postAtFeet(Entity entity, String event, boolean withBlock) {
		Object[] block = withBlock ? under(entity) : new Object[]{null, 0};
		GameEvents.post(entity.worldObj, GameEvent.get(event), entity.posX, entity.boundingBox.minY, entity.posZ, entity, (Block) block[0], (Integer) block[1]);
	}

	/**
	 * 1.7.10's `moveEntity` took a step (moved `nextStepDistance` on): 26.3's `step` on the block under (on the ground,
	 * climbing, or sneaking without moving up or down; not swimming), else `swim` in water
	 * (`Entity.applyMovementEmissionAndPlaySound`).
	 */
	public static void stepped(Entity entity, double dy) {
		if (entity.worldObj.isRemote) {
			return;
		}
		boolean climbing = entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isOnLadder();
		boolean swimming = entity instanceof IPosePlayer && ((IPosePlayer) entity).etfu$isSwimming();
		if ((entity.onGround || climbing || entity.isSneaking() && dy == 0.0D) && !swimming) {
			postAtFeet(entity, "step", true);
		} else if (entity.isInWater()) {
			postAtFeet(entity, "swim", false);
		}
	}

	/**
	 * A flapping entity's move without a step: 26.3's `flap` when the block right under it is air and it flaps now
	 * (`Entity.applyMovementEmissionAndPlaySound`, `processFlappingMovement`); not a passenger's.
	 */
	public static void flew(Entity entity, float moved) {
		if (entity.worldObj.isRemote || entity.ridingEntity != null) {
			return;
		}
		int x = MathHelper.floor_double(entity.posX), y = MathHelper.floor_double(entity.boundingBox.minY - 1.0E-5D), z = MathHelper.floor_double(entity.posZ);
		if (entity.worldObj.getBlock(x, y, z).getMaterial() == Material.air && ((Flapping) entity).mcfly$flapped(moved)) {
			postAtFeet(entity, "flap", false);
		}
	}

	/** Landing with any fall distance: 26.3's `hit_ground` on the block under (`Entity.checkFallDamage`). */
	public static void landed(Entity entity) {
		if (!entity.worldObj.isRemote) {
			postAtFeet(entity, "hit_ground", true);
		}
	}

	/** Into water, not on the first tick: 26.3's `splash` (`Entity.doWaterSplashEffect`). */
	public static void splashed(Entity entity) {
		if (!entity.worldObj.isRemote) {
			postAtFeet(entity, "splash", false);
		}
	}

	/** 26.3's `entity_mount` and `entity_dismount` (`Entity.startRiding`, `removeVehicle`): at the vehicle, by the rider. */
	public static void rode(Entity rider, Entity before, Entity now) {
		if (rider.worldObj.isRemote || before == now) {
			return;
		}
		if (before != null) {
			GameEvents.post(rider.worldObj, GameEvent.get("entity_dismount"), before.posX, before.boundingBox.minY, before.posZ, rider, null, 0);
		}
		if (now != null) {
			GameEvents.post(rider.worldObj, GameEvent.get("entity_mount"), now.posX, now.boundingBox.minY, now.posZ, rider, null, 0);
		}
	}

	/** 26.3's `item_interact_start` and `_finish` (`LivingEntity.startUsingItem`, `stopUsingItem`; every item's use effects make them). */
	public static void usedItem(EntityPlayer player, boolean started) {
		if (!player.worldObj.isRemote) {
			postAtFeet(player, started ? "item_interact_start" : "item_interact_finish", false);
		}
	}

	/** Eaten or drunk: 26.3's `eat` or `drink`, by the use's animation (`Consumable.onConsume`). */
	public static void consumed(ItemStack stack, World world, EntityPlayer player) {
		if (world.isRemote || stack.getItem() == null) {
			return;
		}
		EnumAction action = stack.getItemUseAction();
		if (action == EnumAction.drink || action == EnumAction.eat) {
			postAtFeet(player, action == EnumAction.drink ? "drink" : "eat", false);
		}
	}

	/**
	 * 1.7.10's per-tick look at an entity's equipment found a slot changed (0 the hand, 1 to 4 the armour): 26.3's
	 * `equip` if the new item is equippable, else `unequip` (`LivingEntity.onEquipItem`: another item, or other
	 * components; not the count, nor wear, which 26.3 changes in place). Not on its first look (26.3's first tick),
	 * not for a spectator; a player's armour slots only (`Player.doesEmitEquipEvent`).
	 */
	public static void equipmentChanged(EntityLivingBase entity, int slot, ItemStack before, ItemStack now) {
		if (entity.worldObj.isRemote || sameForEquipping(before, now)) {
			return;
		}
		if (entity instanceof EntityPlayer && (slot == 0 || Spectator.isSpectator((EntityPlayer) entity))) {
			return;
		}
		postAtFeet(entity, equippable(now, entity) ? "equip" : "unequip", false);
	}

	private static boolean sameForEquipping(ItemStack a, ItemStack b) {
		if (a == null || b == null) {
			return a == b;
		}
		if (a.getItem() != b.getItem() || !a.isItemStackDamageable() && a.getItemDamage() != b.getItemDamage()) {
			return false;
		}
		return ItemStack.areItemStackTagsEqual(a, b);
	}

	/** 26.3's equippable component: armour, a pumpkin or a head, the elytra (what goes in an armour slot). */
	private static boolean equippable(ItemStack stack, Entity entity) {
		if (stack == null || stack.getItem() == null) {
			return false;
		}
		for (int type = 0; type < 4; type++) {
			if (stack.getItem().isValidArmor(stack, type, entity)) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- blocks and containers (from the mixins)

	/** Who is doing what 1.7.10 calls without them (a container's open and close, a bucket emptied); the server's thread. */
	private static EntityPlayer actor;

	/** Runs this with the player known as the one doing it (26.3 passes it along; 1.7.10's `openInventory` doesn't). */
	public static void byPlayer(EntityPlayer player, Runnable action) {
		EntityPlayer was = actor;
		actor = player;
		try {
			action.run();
		} finally {
			actor = was;
		}
	}

	/** The player doing it now (`byPlayer`), or null. */
	public static EntityPlayer actor() {
		return actor;
	}

	/** At a block's middle, about nothing (26.3's `level.gameEvent(entity, event, pos)`). */
	public static void atBlock(World world, String event, int x, int y, int z, Entity source) {
		GameEvents.postAtBlock(world, event, x, y, z, source, null, 0);
	}

	/** A container's first opener or last closer (26.3's `ContainerOpenersCounter`): `container_open` or `_close` at it, by the player doing it. */
	public static void container(World world, int x, int y, int z, boolean open) {
		atBlock(world, open ? "container_open" : "container_close", x, y, z, actor);
	}

	// ---------------------------------------------------------------- projectiles, placing, mobs (from the mixins)

	/** A projectile's first tick: 26.3's `projectile_shoot` at it, by its owner (`Projectile.tick`). */
	public static void projectileShot(Entity projectile, Entity owner) {
		if (!projectile.worldObj.isRemote) {
			GameEvents.post(projectile.worldObj, GameEvent.get("projectile_shoot"), projectile.posX, projectile.posY, projectile.posZ, owner, null, 0);
		}
	}

	/** A projectile's hit: 26.3's `projectile_land` (`Projectile.onHit`): at the block's middle, about it; at the point an entity was hit. */
	public static void projectileLanded(Entity projectile, MovingObjectPosition hit) {
		World world = projectile.worldObj;
		if (world.isRemote || hit == null) {
			return;
		}
		if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			GameEvents.postAtBlock(world, "projectile_land", hit.blockX, hit.blockY, hit.blockZ, projectile, world.getBlock(hit.blockX, hit.blockY, hit.blockZ),
				world.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ));
		} else if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.hitVec != null) {
			GameEvents.post(world, GameEvent.get("projectile_land"), hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, projectile, null, 0);
		}
	}

	/** At an entity's feet, by another (26.3's `entity.gameEvent(event, source)`; an entity placed, interacted with). */
	public static void atEntity(Entity entity, String event, Entity source) {
		if (entity != null && !entity.worldObj.isRemote) {
			GameEvents.post(entity.worldObj, GameEvent.get(event), entity.posX, entity.boundingBox.minY, entity.posZ, source, null, 0);
		}
	}
}
