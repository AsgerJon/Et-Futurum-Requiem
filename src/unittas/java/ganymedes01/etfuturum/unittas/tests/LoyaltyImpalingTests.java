package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.command.modern.arguments.EnchantmentIds;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Stage 4 of the trident (SPEC-modern-movement.md §58.4, tests §58.6): Loyalty and Impaling, and getting the trident's
 * enchantments. Written before the code.
 */
@TasSuite("trident")
public class LoyaltyImpalingTests extends TasTestCase {

	private static final String TRIDENT = "minecraft:trident";
	private static final String RETURN = McFly.MC_ASSETS + ":item.trident.return";
	private static final int PICKUP_ALLOWED = 1;
	/** Past creative's 6-block mob reach. */
	private static final double TARGET_Z = 7.5D;

	// ---------------------------------------------------------------- the enchantments

	/** Levels I-III, weight 5, enchanting cost 12 + 7 a level above I to 50, tridents only, never with Riptide. */
	public void testLoyaltyEnchantment() {
		Enchantment loyalty = enchantment("minecraft:loyalty");
		Enchantment riptide = enchantment("minecraft:riptide");
		Enchantment channeling = enchantment("minecraft:channeling");
		assertEqual(3, loyalty.getMaxLevel());
		assertEqual(5, loyalty.getWeight());
		assertEqual(12, loyalty.getMinEnchantability(1));
		assertEqual(19, loyalty.getMinEnchantability(2));
		assertEqual(26, loyalty.getMinEnchantability(3));
		assertEqual(50, loyalty.getMaxEnchantability(3));
		assertTrue(loyalty.canApply(trident()), "can't go on a trident");
		assertFalse(loyalty.canApply(new ItemStack(Items.iron_sword)), "goes on a sword");
		assertFalse(loyalty.canApplyTogether(riptide), "goes with Riptide");
		assertFalse(riptide.canApplyTogether(loyalty), "Riptide goes with it");
		assertTrue(loyalty.canApplyTogether(channeling), "doesn't go with Channeling");
		assertTrue(channeling.canApplyTogether(loyalty), "Channeling doesn't go with it");
	}

	/** Levels I-V, weight 2, cost 1 + 8 a level above I, up to 20 more; tridents only; never with the damage enchantments. */
	public void testImpalingEnchantment() {
		Enchantment impaling = enchantment("minecraft:impaling");
		assertEqual(5, impaling.getMaxLevel());
		assertEqual(2, impaling.getWeight());
		assertEqual(1, impaling.getMinEnchantability(1));
		assertEqual(33, impaling.getMinEnchantability(5));
		assertEqual(21, impaling.getMaxEnchantability(1));
		assertEqual(53, impaling.getMaxEnchantability(5));
		assertTrue(impaling.canApply(trident()), "can't go on a trident");
		assertFalse(impaling.canApply(new ItemStack(Items.iron_sword)), "goes on a sword");
		for (Enchantment damage : new Enchantment[] {Enchantment.sharpness, Enchantment.smite, Enchantment.baneOfArthropods}) {
			assertFalse(impaling.canApplyTogether(damage), "goes with " + damage.getName());
			assertFalse(damage.canApplyTogether(impaling), damage.getName() + " goes with it");
		}
	}

	/** The enchanting table offers all four on a trident (at level 30), never two that clash, nothing of a sword's. */
	public void testEnchantingTable() {
		Set<Integer> seen = new HashSet<>();
		int riptide = enchantment("minecraft:riptide").effectId;
		int loyalty = enchantment("minecraft:loyalty").effectId;
		int channeling = enchantment("minecraft:channeling").effectId;
		for (int seed = 0; seed < 3000; seed++) {
			@SuppressWarnings("unchecked")
			List<EnchantmentData> offer = EnchantmentHelper.buildEnchantmentList(new Random(seed), trident(), 30);
			if (offer == null) {
				continue;
			}
			Set<Integer> ids = new HashSet<>();
			for (EnchantmentData data : offer) {
				ids.add(data.enchantmentobj.effectId);
			}
			assertFalse(ids.contains(riptide) && (ids.contains(loyalty) || ids.contains(channeling)), "Riptide offered with Loyalty or Channeling");
			seen.addAll(ids);
		}
		for (String id : new String[] {"minecraft:loyalty", "minecraft:impaling", "minecraft:riptide", "minecraft:channeling"}) {
			assertTrue(seen.contains(enchantment(id).effectId), "the table never offers " + id);
		}
		for (Enchantment sword : new Enchantment[] {Enchantment.sharpness, Enchantment.smite, Enchantment.baneOfArthropods,
			Enchantment.knockback, Enchantment.fireAspect, Enchantment.looting}) {
			assertFalse(seen.contains(sword.effectId), "the table offers " + sword.getName());
		}
	}

	/** A Loyalty III book on a trident in the anvil; a Riptide book doesn't go on a Loyalty trident. */
	public void testAnvilBooks() {
		survival();
		ItemStack result = onServer(() -> anvil(trident(), book("minecraft:loyalty", 3)));
		assertIsNotNone(result, "no result for a Loyalty book on a trident");
		assertEqual(3, EnchantmentHelper.getEnchantmentLevel(enchantment("minecraft:loyalty").effectId, result));
		ItemStack loyal = trident(enchantment("minecraft:loyalty"), 1);
		ItemStack clash = onServer(() -> anvil(loyal, book("minecraft:riptide", 1)));
		assertTrue(clash == null || EnchantmentHelper.getEnchantmentLevel(enchantment("minecraft:riptide").effectId, clash) == 0,
			"Riptide went onto a Loyalty trident");
	}

	/** /enchant puts Loyalty on a held trident, and refuses Riptide on top of it. */
	public void testEnchantCommand() {
		holdTrident(trident());
		assertNoError(command("enchant @s minecraft:loyalty 3"));
		assertEqual(3, onServer(() -> EnchantmentHelper.getEnchantmentLevel(enchantment("minecraft:loyalty").effectId, player().getHeldItem())));
		assertError(command("enchant @s minecraft:riptide 1"), "cannot support that enchantment");
	}

	// ---------------------------------------------------------------- Loyalty

	/** Thrown at a mob, a Loyalty trident hits it and flies back into the thrower's inventory, with its sound. */
	public void testLoyaltyReturnsAfterAHit() {
		survival();
		EntityVillager villager = stillMob(EntityVillager::new, 0.5D, TARGET_Z);
		holdTrident(trident(enchantment("minecraft:loyalty"), 1));
		look(0.0F, 0.0F);
		clearSounds();
		hold(mc().gameSettings.keyBindUseItem, 15);
		waitUntil("the villager hit", () -> villager.getHealth() < villager.getMaxHealth(), 20);
		waitForSound(RETURN, 20);
		waitUntil("the trident back in the inventory", () -> thrown().isEmpty() && tridentIn(player()) != null, 100);
	}

	/** Stuck in a wall, it waits 4 ticks in the ground, then comes back through blocks. */
	public void testLoyaltyReturnsFromTheGround() {
		survival();
		wall(4, true);
		try {
			EntityThrownTrident trident = launch(trident(enchantment("minecraft:loyalty"), 1), 0.5D, 5.0D, 2.5D, 1.0D);
			waitUntil("the trident in the wall", () -> nbt(trident).getBoolean("inGround"), 20);
			serverTicks(2);
			assertTrue(onServer(() -> nbt(trident).getBoolean("inGround")), "it left the wall before 4 ticks in the ground");
			waitUntil("the trident back in the inventory", () -> thrown().isEmpty() && tridentIn(player()) != null, 100);
		} finally {
			wall(4, false);
		}
	}

	/** Loyalty III comes back sooner than Loyalty I from the same spot. */
	public void testHigherLoyaltyReturnsFaster() {
		survival();
		int levelOne = ticksToReturn(1);
		onServer(() -> player().inventory.clearInventory(null, -1));
		int levelThree = ticksToReturn(3);
		assertTrue(levelThree < levelOne, "Loyalty III took " + levelThree + " ticks, Loyalty I " + levelOne);
	}

	/** Its thrower dead, the trident drops as an item and is gone. */
	public void testDropsWhenTheThrowerIsDead() {
		EntityZombie zombie = stillMob(EntityZombie::new, 3.5D, 3.5D);
		onServer(() -> zombie.attackEntityFrom(DamageSource.outOfWorld, 1000.0F));
		EntityThrownTrident trident = onServer(() -> {
			EntityThrownTrident made = new EntityThrownTrident(world(), zombie, trident(enchantment("minecraft:loyalty"), 1));
			made.setPosition(0.5D, 4.5D, 10.5D);
			made.motionX = made.motionY = made.motionZ = 0.0D;
			made.pickup = PICKUP_ALLOWED;
			world().spawnEntityInWorld(made);
			edit(made, tag -> tag.setBoolean("DealtDamage", true));
			return made;
		});
		waitUntil("the trident gone", () -> trident.isDead, 10);
		assertEqual(1, onServer(() -> droppedTridents().size()), "dropped tridents");
	}

	/** A Loyalty trident that could be picked up never despawns; a plain one does, after 1200 ticks at rest. */
	public void testLoyaltyNeverDespawns() {
		EntityThrownTrident loyal = resting(trident(enchantment("minecraft:loyalty"), 1), -1.5D);
		EntityThrownTrident plain = resting(trident(), 2.5D);
		waitUntil("both on the ground", () -> nbt(loyal).getBoolean("inGround") && nbt(plain).getBoolean("inGround"), 40);
		onServer(() -> {
			edit(loyal, tag -> tag.setShort("life", (short) 1195));
			edit(plain, tag -> tag.setShort("life", (short) 1195));
		});
		serverTicks(15);
		assertTrue(onServer(() -> plain.isDead), "the plain trident didn't despawn (the test's own setup is wrong)");
		assertFalse(onServer(() -> loyal.isDead), "the Loyalty trident despawned");
	}

	// ---------------------------------------------------------------- Impaling

	/** +2.5 a level on an aquatic mob (the squid), in melee: Impaling II does 9 + 5 at full charge. */
	public void testImpalingOnASquidInMelee() {
		EntitySquid squid = sturdy(stillMob(EntitySquid::new, 0.5D, 2.0D));
		assertEqual(14.0F, meleeLoss(squid, trident(enchantment("minecraft:impaling"), 2), 35.0F));
	}

	/**
	 * Nothing extra on a mob that isn't aquatic: the same as a plain trident's hit (a zombie's 2 armour takes a little
	 * off both). Fire resistant zombies: in the sun they catch fire at random, and 1.7.10's burning skips armour.
	 */
	public void testImpalingOnAZombieInMelee() {
		HurtLog hurts = new HurtLog();
		MinecraftForge.EVENT_BUS.register(hurts);
		try {
			EntityZombie impaled = fireproofZombie();
			float withImpaling = meleeLoss(impaled, trident(enchantment("minecraft:impaling"), 2), 0.0F);
			onServer(impaled::setDead);
			EntityZombie plain = fireproofZombie();
			float without = meleeLoss(plain, trident(), 0.0F);
			assertEqual(without, withImpaling, "the hurts were " + hurts.entries);
		} finally {
			MinecraftForge.EVENT_BUS.unregister(hurts);
		}
	}

	/** Every hurt while registered: the entity id, the damage type and amount, the world time. Public, for Forge's event bus. */
	public static class HurtLog {
		final List<String> entries = Collections.synchronizedList(new ArrayList<>());

		@SubscribeEvent
		public void onHurt(LivingHurtEvent event) {
			entries.add(event.entityLiving.getEntityId() + " " + event.source.getDamageType() + " " + event.ammount
				+ " @" + event.entityLiving.worldObj.getTotalWorldTime());
		}
	}

	/** Thrown too: Impaling II does 8 + 5 to a squid. */
	public void testImpalingOnASquidThrown() {
		EntitySquid squid = sturdy(stillMob(EntitySquid::new, 0.5D, TARGET_Z));
		launch(trident(enchantment("minecraft:impaling"), 2), 0.5D, 4.5D, 4.5D, 1.0D);
		waitUntil("the squid hit", () -> squid.getHealth() < squid.getMaxHealth(), 20);
		assertEqual(13.0F, onServer(() -> squid.getMaxHealth() - squid.getHealth()));
	}

	// ---------------------------------------------------------------- helpers

	private static Enchantment enchantment(String id) {
		Enchantment enchantment = EnchantmentIds.byId(id);
		assertIsNotNone(enchantment, "EFR has no " + id);
		return enchantment;
	}

	private static Item tridentItem() {
		ItemIds.Entry entry = ItemIds.byId(TRIDENT);
		assertTrue(entry != null && entry.item() != null, "EFR has no trident (" + TRIDENT + ")");
		return entry.item();
	}

	private static ItemStack trident() {
		return new ItemStack(tridentItem());
	}

	private static ItemStack trident(Enchantment enchantment, int level) {
		ItemStack trident = trident();
		trident.addEnchantment(enchantment, level);
		return trident;
	}

	private static ItemStack book(String id, int level) {
		return Items.enchanted_book.getEnchantedItemStack(new EnchantmentData(enchantment(id), level));
	}

	private static boolean isTrident(ItemStack stack) {
		return stack != null && stack.getItem() == tridentItem();
	}

	/** The first trident in the player's inventory, or null; on the server. */
	private static ItemStack tridentIn(EntityPlayer player) {
		for (ItemStack stack : player.inventory.mainInventory) {
			if (isTrident(stack)) {
				return stack;
			}
		}
		return null;
	}

	/** The thrown tridents in the world; on the server. */
	private static List<Entity> thrown() {
		List<Entity> found = new ArrayList<>();
		for (Object loaded : world().loadedEntityList) {
			Entity entity = (Entity) loaded;
			if (!entity.isDead && TRIDENT.equals(EntityTypeIds.idOf(entity))) {
				found.add(entity);
			}
		}
		return found;
	}

	/** The dropped tridents (item entities); on the server. */
	private static List<EntityItem> droppedTridents() {
		List<EntityItem> found = new ArrayList<>();
		for (Object loaded : world().loadedEntityList) {
			if (loaded instanceof EntityItem item && !item.isDead && isTrident(item.getEntityItem())) {
				found.add(item);
			}
		}
		return found;
	}

	private static NBTTagCompound nbt(Entity entity) {
		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBT(tag);
		return tag;
	}

	/** Changes the entity's saved data and loads it back. */
	private static void edit(Entity entity, Consumer<NBTTagCompound> change) {
		NBTTagCompound tag = nbt(entity);
		change.accept(tag);
		entity.readFromNBT(tag);
	}

	/** The anvil's result for these two inputs, or null; on the server. */
	private static ItemStack anvil(ItemStack first, ItemStack second) {
		ContainerRepair anvil = new ContainerRepair(player().inventory, world(), 0, 4, 0, player());
		anvil.getSlot(0).putStack(first);
		anvil.getSlot(1).putStack(second);
		anvil.updateRepairOutput();
		ItemStack result = anvil.getSlot(2).getStack();
		return result == null ? null : result.copy();
	}

	/** A trident of the player's flying from x y z toward +z at this speed, able to be picked up. */
	private EntityThrownTrident launch(ItemStack stack, double x, double y, double z, double speed) {
		return onServer(() -> {
			EntityThrownTrident trident = new EntityThrownTrident(world(), player(), stack);
			trident.setPosition(x, y, z);
			trident.motionX = 0.0D;
			trident.motionY = 0.0D;
			trident.motionZ = speed;
			trident.pickup = PICKUP_ALLOWED;
			world().spawnEntityInWorld(trident);
			return trident;
		});
	}

	/** A trident without a thrower, dropped onto the ground at x, 3.5 ahead. */
	private EntityThrownTrident resting(ItemStack stack, double x) {
		return onServer(() -> {
			EntityThrownTrident trident = new EntityThrownTrident(world());
			trident.setItem(stack);
			trident.setPosition(x, 4.3D, 3.5D);
			trident.pickup = PICKUP_ALLOWED;
			world().spawnEntityInWorld(trident);
			return trident;
		});
	}

	/** Ticks until a Loyalty trident of this level, done with its hit, 12 blocks ahead, is back in the inventory. */
	private int ticksToReturn(int level) {
		EntityThrownTrident trident = launch(trident(enchantment("minecraft:loyalty"), level), 0.5D, 4.5D, 12.5D, 0.0D);
		onServer(() -> edit(trident, tag -> tag.setBoolean("DealtDamage", true)));
		long start = onServer(() -> world().getTotalWorldTime());
		waitUntil("the Loyalty " + level + " trident back", () -> trident.isDead && tridentIn(player()) != null, 200);
		return (int) (onServer(() -> world().getTotalWorldTime()) - start);
	}

	/** A mob that doesn't walk, at x z on the ground. */
	private <E extends EntityLiving> E stillMob(Function<World, E> make, double x, double z) {
		E mob = spawn(make, x, 4.0D, z);
		onServer(() -> mob.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D));
		return mob;
	}

	/** The mob with 100 health, so one hit shows its whole damage. */
	private <E extends EntityLiving> E sturdy(E mob) {
		onServer(() -> {
			mob.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(100.0D);
			mob.setHealth(100.0F);
		});
		return mob;
	}

	/**
	 * A still zombie with 100 health at 0.5 2.0, fire resistant before its first tick: in the sun it may catch fire at
	 * once, and 1.7.10's first fire damage comes the tick it does.
	 */
	private EntityZombie fireproofZombie() {
		return onServer(() -> {
			EntityZombie zombie = new EntityZombie(world());
			zombie.setLocationAndAngles(0.5D, 4.0D, 2.0D, 0.0F, 0.0F);
			zombie.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 1200));
			zombie.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D);
			zombie.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(100.0D);
			zombie.setHealth(100.0F);
			world().spawnEntityInWorld(zombie);
			return zombie;
		});
	}

	/** The health a full-charge melee hit with this trident takes from the mob, looked at with this pitch. */
	private float meleeLoss(EntityLiving mob, ItemStack trident, float pitch) {
		holdTrident(trident);
		int id = onServer(mob::getEntityId);
		look(0.0F, pitch);
		waitUntilOnClient("the crosshair on the mob", () -> mc().objectMouseOver != null && mc().objectMouseOver.entityHit != null
			&& mc().objectMouseOver.entityHit.getEntityId() == id, 20);
		waitUntil("a full charge", () -> AttackCooldown.charge(player(), 0.0F) >= 1.0F, 60);
		tap(mc().gameSettings.keyBindAttack);
		waitUntil("the mob hit", () -> mob.getHealth() < mob.getMaxHealth(), 10);
		return onServer(() -> mob.getMaxHealth() - mob.getHealth());
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		ticks(2);
	}

	/** Puts the trident in hand and waits for the client to have it. */
	private void holdTrident(ItemStack trident) {
		setHeld(trident);
		waitUntilOnClient("the trident in hand", () -> isTrident(mc().thePlayer.getHeldItem()), 40);
	}

	/** A stone wall across the way at this z, 3 wide and 4 high, or taken down. */
	private void wall(int z, boolean build) {
		onServer(() -> {
			for (int x = -1; x <= 1; x++) {
				for (int y = 4; y <= 7; y++) {
					world().setBlock(x, y, z, build ? Blocks.stone : Blocks.air, 0, 3);
				}
			}
		});
	}
}
