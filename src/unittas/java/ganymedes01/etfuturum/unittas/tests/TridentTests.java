package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.combat.AttackCooldown;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.entities.EntityArmourStand;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import ganymedes01.etfuturum.offhand.Offhand;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The trident, stage 1 (SPEC-modern-movement.md §58.1): the item, its melee, the throw and the thrown trident, and how
 * it looks. Written before the trident exists, so it is found by its modern id, `minecraft:trident`: the item through
 * EFR's item ids, the thrown one through its entity id. Until the trident is in, every test fails at "EFR has no
 * trident".
 */
@TasSuite("trident")
public class TridentTests extends TasTestCase {

	private static final String TRIDENT = "minecraft:trident";
	private static final String THROW = McFly.MC_ASSETS + ":item.trident.throw";
	private static final String HIT_GROUND = McFly.MC_ASSETS + ":item.trident.hit_ground";
	private static final String[] SOUNDS = {"throw", "hit", "hit_ground", "return", "riptide_1", "riptide_2", "riptide_3", "thunder"};
	/** Villagers to throw at stand past creative's reach for mobs (6 in 1.7.10): right-clicking one in reach trades. */
	private static final double TARGET_Z = 7.5D;
	/** 26.3's `pickup` values: nobody, anyone allowed, creative players only. */
	private static final int PICKUP_ALLOWED = 1, PICKUP_CREATIVE_ONLY = 2;

	// ---------------------------------------------------------------- the item

	/** 250 uses, stacks of 1, rare (an aqua name), enchantability 1. */
	public void testItem() {
		ItemStack trident = trident();
		assertEqual(250, trident.getMaxDamage());
		assertEqual(1, trident.getMaxStackSize());
		assertEqual(EnumRarity.rare, trident.getRarity());
		assertEqual(1, trident.getItem().getItemEnchantability());
	}

	public void testGive() {
		assertNoError(command("give @p " + TRIDENT));
		waitUntil("a trident in the inventory", () -> tridentIn(player()) != null, 20);
	}

	public void testSummon() {
		assertNoError(command("summon " + TRIDENT + " ~ ~1 ~3"));
		assertEqual(1, onServer(() -> thrown().size()));
	}

	/**
	 * No snap to its motion on the first tick (1.7.10's arrows have one, 26.3 doesn't): made with no facing and falling
	 * straight down, it turns a fifth of the way a tick, pitch -18 and then -32.4. High in the air, two of its ticks.
	 */
	public void testTurnsAFifthATickFromNoFacing() {
		float[] turned = onServer(() -> {
			EntityThrownTrident thrown = new EntityThrownTrident(world());
			thrown.setItem(trident());
			thrown.setPosition(0.5D, 100.0D, 0.5D);
			thrown.motionY = -1.0D;
			thrown.onUpdate();
			float first = thrown.rotationPitch;
			thrown.onUpdate();
			return new float[]{first, thrown.rotationPitch, thrown.rotationYaw};
		});
		assertAlmostEqual(-18.0, turned[0], 0.01);
		assertAlmostEqual(-32.4, turned[1], 0.01);
		assertAlmostEqual(0.0, turned[2], 0.01);
	}

	/**
	 * Stuck, it is pulled back 0.05 on each axis it moved along (26.3), not 0.05 along its flight: thrown at a wall
	 * at a slant, it hits the face x = 2 at z 0.75 and rests at x 1.95, z 0.70 (along its flight it would be 1.955, 0.728).
	 */
	public void testStuckPulledBackOnEachAxis() {
		double[] at = onServer(() -> {
			world().setBlock(2, 100, 0, Blocks.stone);
			try {
				EntityThrownTrident thrown = new EntityThrownTrident(world());
				thrown.setItem(trident());
				thrown.setPosition(1.5D, 100.5D, 0.5D);
				thrown.motionX = 1.0D;
				thrown.motionZ = 0.5D;
				thrown.onUpdate();
				return new double[]{thrown.posX, thrown.posY, thrown.posZ, nbt(thrown).getBoolean("inGround") ? 1.0 : 0.0};
			} finally {
				world().setBlockToAir(2, 100, 0);
			}
		});
		assertEqual(1.0, at[3]);
		assertAlmostEqual(1.95, at[0], 1.0E-4);
		assertAlmostEqual(100.5, at[1], 1.0E-4);
		assertAlmostEqual(0.70, at[2], 1.0E-4);
	}

	/** 26.3 has no recipe for it: drowned and trial chamber loot only. */
	public void testNoRecipe() {
		Item trident = tridentItem();
		assertFalse(onServer(() -> {
			for (Object recipe : CraftingManager.getInstance().getRecipeList()) {
				ItemStack output = ((IRecipe) recipe).getRecipeOutput();
				if (output != null && output.getItem() == trident) {
					return true;
				}
			}
			return false;
		}), "a crafting recipe makes a trident");
	}

	// ---------------------------------------------------------------- melee

	/** +8 attack damage in the main hand: 9 with the hand's own 1. */
	public void testAttackDamageModifier() {
		double damage = onServer(() -> {
			double sum = 0.0D;
			for (Object modifier : trident().getAttributeModifiers().get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
				sum += ((AttributeModifier) modifier).getAmount();
			}
			return sum;
		});
		assertAlmostEqual(8.0, damage, 1.0E-6);
	}

	/** Attack speed 1.1: modern combat's charge takes 20 / 1.1 ticks. */
	public void testAttackSpeed() {
		holdTrident(trident());
		ticks(2);
		assertAlmostEqual(20.0 / 1.1, onServer(() -> (double) AttackCooldown.delay(player())), 1.0E-3);
	}

	/** A full-strength hit on a villager (no armour) takes 9; in survival it costs 1 use. */
	public void testMeleeHit() {
		survival();
		holdTrident(trident());
		EntityVillager villager = stillVillagerAt(0.5, 2.0);
		aimAt(villager, 0.0F, 0.0F);
		waitUntil("a full charge", () -> AttackCooldown.charge(player(), 0.0F) >= 1.0F, 60);
		tap(mc().gameSettings.keyBindAttack);
		ticks(3);
		assertAlmostEqual(11.0, onServer(villager::getHealth), 1.0E-4);
		assertEqual(1, held().getItemDamage());
	}

	public void testBreakingABlockCostsTwo() {
		survival();
		holdTrident(trident());
		place(Blocks.dirt, 0, 4, 2);
		try {
			onServer(() -> player().theItemInWorldManager.tryHarvestBlock(0, 4, 2));
			assertEqual(Blocks.air, onServer(() -> world().getBlock(0, 4, 2)));
			assertEqual(2, held().getItemDamage());
		} finally {
			clear(0, 0, 4, 4, 2, 2);
		}
	}

	/** In creative it breaks no blocks, as a sword. */
	public void testCreativeBreaksNothing() {
		holdTrident(trident());
		place(Blocks.dirt, 0, 4, 2);
		try {
			look(0.0F, 30.0F);
			waitUntilOnClient("the crosshair on the block", () -> {
				MovingObjectPosition hit = mc().objectMouseOver;
				return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && hit.blockX == 0 && hit.blockY == 4 && hit.blockZ == 2;
			}, 20);
			tap(mc().gameSettings.keyBindAttack);
			ticks(5);
			assertEqual(Blocks.dirt, onServer(() -> world().getBlock(0, 4, 2)));
		} finally {
			clear(0, 0, 4, 4, 2, 2);
		}
	}

	// ---------------------------------------------------------------- throwing

	/** Let go under 10 ticks of charging: nothing thrown, nothing used. */
	public void testShortChargeThrowsNothing() {
		survival();
		holdTrident(trident());
		look(0.0F, -10.0F);
		charge(4);
		assertEqual(0, onServer(() -> thrown().size()));
		assertTrue(isTrident(held()), "the trident left the hand");
		assertEqual(0, held().getItemDamage());
	}

	/** 10 ticks or more: thrown, the hand empty, 1 use taken from the thrown item, `item.trident.throw`. */
	public void testThrow() {
		survival();
		holdTrident(trident());
		look(0.0F, -10.0F);
		clearSounds();
		charge(15);
		List<NBTTagCompound> thrown = onServer(() -> nbtOfThrown());
		assertEqual(1, thrown.size());
		assertIsNone(held());
		assertEqual(1, itemOf(thrown.get(0)).getItemDamage());
		assertEqual(PICKUP_ALLOWED, (int) thrown.get(0).getByte("pickup"));
		waitForSound(THROW, 20);
	}

	/** In creative the trident stays in hand and the thrown one is for creative players only. */
	public void testCreativeThrowKeepsTheTrident() {
		holdTrident(trident());
		look(0.0F, -10.0F);
		charge(15);
		List<NBTTagCompound> thrown = onServer(() -> nbtOfThrown());
		assertEqual(1, thrown.size());
		assertTrue(isTrident(held()), "creative lost its trident");
		assertEqual(PICKUP_CREATIVE_ONLY, (int) thrown.get(0).getByte("pickup"));
	}

	/** With 1 use left the use fails: no throw. */
	public void testNoThrowWhenItWouldBreak() {
		survival();
		holdTrident(trident(1));
		look(0.0F, -10.0F);
		charge(15);
		assertEqual(0, onServer(() -> thrown().size()));
		assertTrue(isTrident(held()), "the trident left the hand");
	}

	/** The throw's use never breaks it: with 2 left the thrown one has 1 left. */
	public void testThrowNeverBreaksIt() {
		survival();
		holdTrident(trident(2));
		look(0.0F, -10.0F);
		charge(15);
		List<NBTTagCompound> thrown = onServer(() -> nbtOfThrown());
		assertEqual(1, thrown.size());
		ItemStack item = itemOf(thrown.get(0));
		assertEqual(1, item.getMaxDamage() - item.getItemDamage());
	}

	// ---------------------------------------------------------------- the thrown trident

	/** 8 damage to the mob it hits (a villager has no armour). */
	public void testHitsForEight() {
		EntityVillager villager = stillVillagerAt(0.5, TARGET_Z);
		holdTrident(trident());
		look(0.0F, 0.0F);
		charge(15);
		waitUntil("the villager hit", () -> villager.getHealth() < 20.0F, 40);
		assertAlmostEqual(12.0, onServer(villager::getHealth), 1.0E-4);
	}

	/** Its damage type is `trident`: "%1$s was impaled by %2$s". */
	public void testImpales() {
		EntityVillager villager = stillVillagerAt(0.5, TARGET_Z);
		onServer(() -> villager.setHealth(5.0F));
		holdTrident(trident());
		look(0.0F, 0.0F);
		// read as it dies: 1.7.10's onDeath then clears the villager's combat record
		DeathMessages deaths = new DeathMessages();
		MinecraftForge.EVENT_BUS.register(deaths);
		try {
			charge(15);
			waitUntil("the villager killed", () -> villager.getHealth() <= 0.0F, 40);
			assertEqual("death.attack.trident", deaths.keys.get(villager.getEntityId()));
		} finally {
			MinecraftForge.EVENT_BUS.unregister(deaths);
		}
	}

	/**
	 * The death message of each mob that dies on the server while registered, by entity id. Public, for Forge's event
	 * bus. The client's copy dies too, with 1.7.10's `generic` (on the death status), under the same id in single
	 * player and on the same bus: it is left out, or it overwrites the real one when it comes later (a flaky
	 * 'death.attack.generic').
	 */
	public static class DeathMessages {
		final Map<Integer, String> keys = new ConcurrentHashMap<>();

		@SubscribeEvent
		public void onDeath(LivingDeathEvent event) {
			if (event.entityLiving.worldObj.isRemote) {
				return;
			}
			IChatComponent message = event.source.func_151519_b(event.entityLiving);
			keys.put(event.entityLiving.getEntityId(), message instanceof ChatComponentTranslation translation ? translation.getKey() : message.getUnformattedText());
		}
	}

	/**
	 * A trident breaks an armour stand at once, as an arrow does (26.3's `always_kills_armor_stands`): thrown by a survival
	 * player, the stand drops as an item. EFR's stand used to ignore it (spec §58.3, found by the user).
	 */
	public void testBreaksAnArmourStand() {
		if (!ModItems.WOODEN_ARMORSTAND.isEnabled()) {
			skipTest("armour stands are turned off (enableArmourStand)");
		}
		survival();
		EntityArmourStand stand = spawn(EntityArmourStand::new, 0.5D, 4.0D, TARGET_Z);
		holdTrident(trident());
		look(0.0F, 0.0F);
		charge(15);
		waitUntil("the armour stand broken", () -> stand.isDead, 20);
		waitUntil("the armour stand dropped", () -> !itemsNear(ModItems.WOODEN_ARMORSTAND.get(), TARGET_Z).isEmpty(), 20);
	}

	/** Thrown by a creative player, it breaks the stand with no drop. */
	public void testCreativeBreaksAnArmourStandWithoutADrop() {
		if (!ModItems.WOODEN_ARMORSTAND.isEnabled()) {
			skipTest("armour stands are turned off (enableArmourStand)");
		}
		EntityArmourStand stand = spawn(EntityArmourStand::new, 0.5D, 4.0D, TARGET_Z);
		holdTrident(trident());
		look(0.0F, 0.0F);
		charge(15);
		waitUntil("the armour stand broken", () -> stand.isDead, 20);
		serverTicks(5);
		assertEqual(0, onServer(() -> itemsNear(ModItems.WOODEN_ARMORSTAND.get(), TARGET_Z).size()), "armour stands dropped");
	}

	/** Dropped items of this kind within 3 blocks of x 0.5 at z; on the server. */
	private static List<EntityItem> itemsNear(Item kind, double z) {
		List<EntityItem> found = new ArrayList<>();
		for (Object loaded : world().loadedEntityList) {
			if (loaded instanceof EntityItem item && !item.isDead && item.getEntityItem().getItem() == kind
				&& (item.posX - 0.5D) * (item.posX - 0.5D) + (item.posZ - z) * (item.posZ - z) <= 9.0D) {
				found.add(item);
			}
		}
		return found;
	}

	/** Only the first mob is hit; the trident stops there and stays in the world. */
	public void testOnlyTheFirstMobIsHit() {
		EntityVillager first = stillVillagerAt(0.5, TARGET_Z);
		EntityVillager second = stillVillagerAt(0.5, TARGET_Z + 3.0D);
		holdTrident(trident());
		look(0.0F, 0.0F);
		charge(15);
		waitUntil("the first villager hit", () -> first.getHealth() < 20.0F, 40);
		serverTicks(20);
		assertAlmostEqual(20.0, onServer(second::getHealth), 1.0E-4);
		assertEqual(1, onServer(() -> thrown().size()), "the trident went away after its hit");
	}

	/** Thrown at a wall it sticks, with `item.trident.hit_ground`. */
	public void testSticksInAWall() {
		wall(4, true);
		try {
			holdTrident(trident());
			look(0.0F, 0.0F);
			clearSounds();
			charge(15);
			waitUntil("the trident in the wall", () -> thrown().size() == 1 && nbt(thrown().get(0)).getBoolean("inGround"), 40);
			waitForSound(HIT_GROUND, 20);
		} finally {
			wall(4, false);
		}
	}

	/** In water it keeps 0.99 of its speed a tick (an arrow keeps 0.6). */
	public void testWaterHardlySlowsIt() {
		pool(true);
		try {
			assertNoError(command("summon " + TRIDENT + " 0.5 5.0 3.0"));
			onServer(() -> {
				Entity trident = thrown().get(0);
				trident.motionX = 0.0D;
				trident.motionY = 0.0D;
				trident.motionZ = 0.4D;
			});
			serverTicks(2);
			// the speed with the world's time it was read at: a wait of one tick can span two
			double[] before = onServer(() -> new double[] {thrown().get(0).motionZ, world().getTotalWorldTime()});
			serverTicks(1);
			double[] after = onServer(() -> new double[] {thrown().get(0).motionZ, world().getTotalWorldTime()});
			double ticks = after[1] - before[1];
			assertGreater(ticks, 0.0);
			assertAlmostEqual(0.99, Math.pow(after[0] / before[0], 1.0D / ticks), 1.0E-3);
		} finally {
			pool(false);
		}
	}

	/** Thrown down at the feet, the thrower picks it back up, as it was (the use taken, the name kept). */
	public void testThrowerPicksItUp() {
		survival();
		ItemStack named = trident();
		named.setStackDisplayName("Neptune");
		holdTrident(named);
		look(0.0F, 90.0F);
		charge(15);
		waitUntil("the trident back in the inventory", () -> thrown().isEmpty() && tridentIn(player()) != null, 60);
		ItemStack back = onServer(() -> tridentIn(player()).copy());
		assertEqual(1, back.getItemDamage());
		assertEqual("Neptune", back.getDisplayName());
	}

	/**
	 * Someone else's trident isn't picked up while that someone is around; once its thrower isn't found (logged out,
	 * or never here), anyone picks it up, as modern's playerTouch.
	 */
	public void testOnlyTheThrowerPicksItUp() {
		survival();
		wall(4, true);
		try {
			EntityVillager someoneElse = stillVillagerAt(6.5D, 0.5D);
			holdTrident(trident());
			look(0.0F, 0.0F);
			charge(15);
			waitUntil("the trident in the wall", () -> thrown().size() == 1 && nbt(thrown().get(0)).getBoolean("inGround"), 40);
			onServer(() -> edit(thrown().get(0), tag -> tag.setIntArray("Owner", uuidInts(someoneElse.getUniqueID()))));
			onServer(() -> player().setPositionAndUpdate(0.5D, 4.0D, 2.7D));
			serverTicks(20);
			assertEqual(1, onServer(() -> thrown().size()), "someone else's trident was picked up");
			assertIsNone(onServer(() -> tridentIn(player())));
			onServer(() -> edit(thrown().get(0), tag -> tag.setIntArray("Owner", uuidInts(new UUID(1L, 2L)))));
			waitUntil("a trident with its thrower gone picked up", () -> thrown().isEmpty() && tridentIn(player()) != null, 40);
		} finally {
			wall(4, false);
		}
	}

	/** 1200 ticks at rest in the ground, then it is gone. */
	public void testDespawnsAfter1200Ticks() {
		wall(4, true);
		try {
			holdTrident(trident());
			look(0.0F, 0.0F);
			charge(15);
			waitUntil("the trident in the wall", () -> thrown().size() == 1 && nbt(thrown().get(0)).getBoolean("inGround"), 40);
			serverTicks(10);
			assertEqual(1, onServer(() -> thrown().size()), "gone after 10 ticks");
			onServer(() -> edit(thrown().get(0), tag -> tag.setShort("life", (short) 1195)));
			serverTicks(10);
			assertEqual(0, onServer(() -> thrown().size()), "still there past 1200 ticks");
		} finally {
			wall(4, false);
		}
	}

	/** A dispenser drops a trident as an item; it doesn't fire it. */
	public void testDispenserDropsIt() {
		onServer(() -> {
			world().setBlock(0, 4, 3, Blocks.dispenser, 3, 3);
			((TileEntityDispenser) world().getTileEntity(0, 4, 3)).setInventorySlotContents(0, trident());
			world().setBlock(0, 5, 3, Blocks.redstone_block, 0, 3);
		});
		try {
			waitUntil("the trident out of the dispenser", () -> !thrown().isEmpty() || !droppedTridents().isEmpty(), 20);
			assertEqual(0, onServer(() -> thrown().size()), "the dispenser fired it");
			assertEqual(1, onServer(() -> droppedTridents().size()));
		} finally {
			clear(0, 0, 4, 5, 3, 3);
		}
	}

	// ---------------------------------------------------------------- how it looks

	/** In hand the 3D model; in the inventory, on the ground and in item frames the flat sprite. */
	public void testDrawnIn3DOnlyInHand() {
		ItemStack trident = trident();
		List<String> wrong = onClient(() -> {
			List<String> found = new ArrayList<>();
			for (IItemRenderer.ItemRenderType type : new IItemRenderer.ItemRenderType[] {IItemRenderer.ItemRenderType.EQUIPPED,
				IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON}) {
				if (MinecraftForgeClient.getItemRenderer(trident, type) == null) {
					found.add(type + " is flat");
				}
			}
			for (IItemRenderer.ItemRenderType type : new IItemRenderer.ItemRenderType[] {IItemRenderer.ItemRenderType.INVENTORY,
				IItemRenderer.ItemRenderType.ENTITY}) {
				if (MinecraftForgeClient.getItemRenderer(trident, type) != null) {
					found.add(type + " is 3D");
				}
			}
			return found;
		});
		assertEqual(Collections.emptyList(), wrong);
	}

	/** While charging, the using arm (the main hand's, left or right) is raised overhead (third person); not before. */
	public void testArmRaisedWhileCharging() {
		holdTrident(trident());
		look(0.0F, -10.0F);
		assertTrue(onClient(() -> mainArmPitch()) > -1.5F, "the arm is raised before charging");
		keyDown(mc().gameSettings.keyBindUseItem);
		try {
			waitUntilOnClient("the client charging", () -> mc().thePlayer.isUsingItem(), 20);
			ticks(3);
			float pitch = onClient(() -> mainArmPitch());
			String state = onClient(() -> "using " + mc().thePlayer.isUsingItem() + " " + id(mc().thePlayer.getItemInUse()) + " with "
				+ Offhand.getUsingHand(mc().thePlayer) + ", left-handed " + mainArmIsLeft());
			assertTrue(pitch < -2.5F, "the arm while charging is at " + pitch + " (raised is near -pi); " + state);
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
			ticks(3);
		}
	}

	/** Its sounds exist, each with a subtitle. */
	public void testSoundsAndSubtitles() {
		List<String> missing = onClient(() -> {
			List<String> found = new ArrayList<>();
			for (String sound : SOUNDS) {
				String path = "item.trident." + sound;
				if (mc().getSoundHandler().getSound(new ResourceLocation(McFly.MC_ASSETS, path)) == null) {
					found.add("the sound " + path);
				}
				if (!StatCollector.canTranslate("subtitle." + path)) {
					found.add("the subtitle of " + path);
				}
			}
			return found;
		});
		assertEqual(Collections.emptyList(), missing);
	}

	// ---------------------------------------------------------------- helpers

	private static Item tridentItem() {
		ItemIds.Entry entry = ItemIds.byId(TRIDENT);
		assertTrue(entry != null && entry.item() != null, "EFR has no trident (" + TRIDENT + ")");
		return entry.item();
	}

	private static ItemStack trident() {
		return new ItemStack(tridentItem());
	}

	/** A trident with this many uses left. */
	private static ItemStack trident(int usesLeft) {
		ItemStack trident = trident();
		trident.setItemDamage(trident.getMaxDamage() - usesLeft);
		return trident;
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

	private static List<NBTTagCompound> nbtOfThrown() {
		List<NBTTagCompound> tags = new ArrayList<>();
		for (Entity trident : thrown()) {
			tags.add(nbt(trident));
		}
		return tags;
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

	/** The thrown trident's item, saved under 26.3's key `item` in 1.7.10's item format. */
	private static ItemStack itemOf(NBTTagCompound thrown) {
		ItemStack item = ItemStack.loadItemStackFromNBT(thrown.getCompoundTag("item"));
		assertIsNotNone(item, "the thrown trident holds no item under `item`");
		return item;
	}

	/** A UUID as 26.3 saves one: four ints, most significant first. */
	private static int[] uuidInts(UUID id) {
		long most = id.getMostSignificantBits();
		long least = id.getLeastSignificantBits();
		return new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
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

	/** Holds use for this many ticks, lets go, and gives the server time to act. */
	private void charge(int ticks) {
		hold(mc().gameSettings.keyBindUseItem, ticks);
		ticks(3);
	}

	/** A villager that doesn't walk, feet on the ground at x z. */
	private EntityVillager stillVillagerAt(double x, double z) {
		EntityVillager villager = spawn(EntityVillager::new, x, 4.0D, z);
		onServer(() -> villager.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D));
		return villager;
	}

	private void aimAt(Entity entity, float yaw, float pitch) {
		int id = onServer(entity::getEntityId);
		look(yaw, pitch);
		waitUntilOnClient("the crosshair on entity " + id, () -> mc().objectMouseOver != null && mc().objectMouseOver.entityHit != null
			&& mc().objectMouseOver.entityHit.getEntityId() == id, 20);
	}

	/** The main hand's arm's pitch as the player model sets it up for the client's player now, holding an item there. */
	private static float mainArmPitch() {
		ModelBiped model = new ModelBiped();
		EntityPlayer player = mc().thePlayer;
		boolean left = mainArmIsLeft();
		if (left) {
			model.heldItemLeft = 1;
		} else {
			model.heldItemRight = 1;
		}
		model.setRotationAngles(0.0F, 0.0F, player.ticksExisted, 0.0F, player.rotationPitch, 0.0625F, player);
		return (left ? model.bipedLeftArm : model.bipedRightArm).rotateAngleX;
	}

	/** Whether the client player's main hand is the left arm (modern's Main Hand option). */
	private static boolean mainArmIsLeft() {
		return Offhand.isEnabled() && Offhand.isLeftHanded(mc().thePlayer);
	}

	private void place(Block block, int x, int y, int z) {
		onServer(() -> world().setBlock(x, y, z, block, 0, 3));
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

	/** Still water ahead (x -1..1, y 4..6, z 2..8) in a glass basin, or taken away. */
	private void pool(boolean fill) {
		onServer(() -> {
			for (int x = -2; x <= 2; x++) {
				for (int y = 4; y <= 6; y++) {
					for (int z = 1; z <= 9; z++) {
						boolean inside = x > -2 && x < 2 && z > 1 && z < 9;
						Block block = !fill ? Blocks.air : inside ? Blocks.water : Blocks.glass;
						world().setBlock(x, y, z, block, 0, 2);
					}
				}
			}
		});
	}

	private void clear(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		onServer(() -> {
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						TileEntity tile = world().getTileEntity(x, y, z);
						if (tile instanceof IInventory inventory) {
							for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
								inventory.setInventorySlotContents(slot, null);
							}
						}
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
		});
	}
}
