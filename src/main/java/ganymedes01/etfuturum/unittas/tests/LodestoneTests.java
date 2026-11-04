package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.LodestoneNeedle;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.unittas.TasTestCase;
import ganymedes01.etfuturum.world.LodestoneRecord;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.TextureCompass;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lodestones and lodestone compasses (SPEC-modern-movement.md §57.3), as 26.3: the block and its recipe, locking a
 * compass with a real right click, losing the lodestone, the needle. The lodestone the player clicks stands two south of
 * the start at eye height (its north face looks at the player); the others stand in the work area east of the start. A
 * lodestone compass is 1.7.10's compass with the tags below. An item frame's and a dropped item's needle are read
 * through LodestoneNeedle; that the renderers go through it is the user's check in runClient.
 */
public class LodestoneTests extends TasTestCase {

	private static final String TRACKED = "LodestoneTracked", POS = "LodestonePos", DIMENSION = "LodestoneDimension";
	private static final String LOCK = McFly.MC_ASSETS + ":item.lodestone_compass.lock";
	private static final String PLACE = McFly.MC_ASSETS + ":block.lodestone.place";
	/** The lodestone the player clicks. */
	private static final int[] FRONT = {0, 5, 2};
	/** Lodestones in the work area. */
	private static final int[] FIRST = {22, 4, 4}, SECOND = {24, 4, 4};
	/** Straight ahead of the start (south), and east of it, for the needle. */
	private static final int[] AHEAD = {0, 4, 12}, EAST = {12, 4, 0};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear(-1, 1, 4, 6, 1, 3);
			clear(20, 40, 4, 6, -2, 10);
			for (int[] at : new int[][] {AHEAD, EAST}) {
				clear(at[0], at[0], at[1], at[1], at[2], at[2]);
			}
		});
	}

	// ---------------------------------------------------------------- the block

	/** Eight chiseled stone bricks around an iron ingot make one lodestone. */
	public void testRecipe() {
		Block lodestone = lodestone();
		ItemStack made = craft(new ItemStack(Items.iron_ingot));
		assertEqual(id(new ItemStack(lodestone)), id(made));
		assertEqual(1, made.stackSize);
	}

	/** 1.16 to 1.21.4's netherite ingot in the middle makes nothing now. */
	public void testNoNetheriteRecipe() {
		lodestone();
		if (!ModItems.NETHERITE_INGOT.isEnabled()) {
			skipTest("netherite is off");
		}
		assertIsNone(craft(ModItems.NETHERITE_INGOT.newItemStack()));
	}

	/** 26.3's 3.5 for both; 1.7.10's explosions read the resistance as a fifth of three times what was set. */
	public void testHardnessAndBlastResistance() {
		Block lodestone = lodestone();
		assertAlmostEqual(3.5D, onServer(() -> (double) lodestone.getBlockHardness(world(), FIRST[0], FIRST[1], FIRST[2])), 1.0E-6D);
		assertAlmostEqual(3.5D, onServer(() -> (double) lodestone.getExplosionResistance(null)), 1.0E-5D);
	}

	/** Any pickaxe gets the drop, a wooden one too. */
	public void testWoodenPickaxeGetsTheDrop() {
		assertEqual(1, mine(lodestone(), new ItemStack(Items.wooden_pickaxe)));
	}

	public void testHandGetsNothing() {
		assertEqual(0, mine(lodestone(), null));
	}

	/** Pistons can't move it: it stays where it is, while stone in the same setup is pushed. */
	public void testPistonCantPushIt() {
		Block lodestone = lodestone();
		onServer(() -> {
			world().setBlock(30, 4, 0, Blocks.piston, 3, 3);
			world().setBlock(30, 4, 1, lodestone, 0, 3);
			world().setBlock(33, 4, 0, Blocks.piston, 3, 3);
			world().setBlock(33, 4, 1, Blocks.stone, 0, 3);
			world().setBlock(29, 4, 0, Blocks.redstone_block, 0, 3);
			world().setBlock(34, 4, 0, Blocks.redstone_block, 0, 3);
		});
		serverTicks(6);
		assertEqual(Blocks.stone, onServer(() -> world().getBlock(33, 4, 2)), "the stone wasn't pushed (the test's setup)");
		assertEqual(lodestone, onServer(() -> world().getBlock(30, 4, 1)), "the lodestone moved");
	}

	/** It has no instrument of its own in 26.3: a note block on it plays harp (on stone, 1.7.10 plays the bass drum). */
	public void testNoteBlockPlaysHarp() {
		Block lodestone = lodestone();
		onServer(() -> {
			world().setBlock(26, 4, 0, lodestone, 0, 3);
			world().setBlock(26, 5, 0, Blocks.noteblock, 0, 3);
		});
		clearSounds();
		onServer(() -> ((TileEntityNote) world().getTileEntity(26, 5, 0)).triggerNote(world(), 26, 5, 0));
		waitForSound("minecraft:note.harp", 20);
	}

	/** Placed with a real right click, it plays its own place sound. */
	public void testPlacingSoundsLikeALodestone() {
		Block lodestone = lodestone();
		onServer(() -> world().setBlock(FRONT[0], FRONT[1], FRONT[2], Blocks.stone, 0, 3));
		setHeld(new ItemStack(lodestone));
		waitUntilOnClient("the lodestone in hand", () -> mc().thePlayer.getHeldItem() != null, 20);
		look(0.0F, 0.0F);
		aimAt(FRONT, 2);
		clearSounds();
		tap(mc().gameSettings.keyBindUseItem);
		waitForSound(PLACE, 20);
		assertEqual(lodestone, onServer(() -> world().getBlock(FRONT[0], FRONT[1], FRONT[2] - 1)), "not placed");
	}

	public void testSetblockAndGive() {
		Block lodestone = lodestone();
		assertNoError(command("/setblock 36 4 0 minecraft:lodestone"));
		assertEqual(lodestone, onServer(() -> world().getBlock(36, 4, 0)));
		assertNoError(command("/give @s minecraft:lodestone"));
		assertIn(id(new ItemStack(lodestone)), ids(inventory()));
	}

	// ---------------------------------------------------------------- locking a compass

	/** One compass in survival is changed where it is, and the lock sound plays. */
	public void testLockOneCompassInSurvival() {
		place(lodestone(), FRONT);
		survival();
		clearSounds();
		useOnTheLodestone(new ItemStack(Items.compass), false);
		ItemStack held = held();
		assertEqual(1, held.stackSize);
		assertEqual(at(FRONT, 0), target(held));
		assertEqual((byte) 1, held.getTagCompound().getByte(TRACKED));
		waitForSound(LOCK, 20);
	}

	/** From a stack of three: two plain ones stay in hand, a lodestone compass goes into the inventory. */
	public void testLockFromAStack() {
		place(lodestone(), FRONT);
		survival();
		useOnTheLodestone(new ItemStack(Items.compass, 3), false);
		ItemStack held = held();
		assertEqual(2, held.stackSize);
		assertEqual("not a lodestone compass", target(held));
		assertEqual(Collections.singletonList("1x " + at(FRONT, 0)), lodestoneCompasses(inventory()));
	}

	/** With no room for it, the lodestone compass is dropped at the player. */
	public void testLockedCopyDroppedWhenTheInventoryIsFull() {
		place(lodestone(), FRONT);
		survival();
		onServer(() -> {
			for (int slot = 0; slot < player().inventory.mainInventory.length; slot++) {
				if (slot != player().inventory.currentItem) {
					player().inventory.setInventorySlotContents(slot, new ItemStack(Blocks.stone, 64));
				}
			}
			player().inventoryContainer.detectAndSendChanges();
		});
		useOnTheLodestone(new ItemStack(Items.compass, 2), false);
		assertEqual(1, held().stackSize);
		assertEqual(Collections.emptyList(), lodestoneCompasses(inventory()));
		waitUntil("a lodestone compass on the ground", () -> {
			List<ItemStack> dropped = new ArrayList<>();
			for (EntityItem item : entitiesNear(EntityItem.class, 5.0D)) {
				dropped.add(item.getEntityItem());
			}
			return lodestoneCompasses(dropped).contains("1x " + at(FRONT, 0));
		}, 10);
	}

	/** In creative the compass in hand stays as it is and a lodestone compass is added. */
	public void testCreativeKeepsTheCompassAndAddsACopy() {
		place(lodestone(), FRONT);
		useOnTheLodestone(new ItemStack(Items.compass), false);
		ItemStack held = held();
		assertEqual(1, held.stackSize);
		assertEqual("not a lodestone compass", target(held));
		assertEqual(Collections.singletonList("1x " + at(FRONT, 0)), lodestoneCompasses(inventory()));
	}

	/**
	 * The copy from a stack reaches the player's own client, not only the server's inventory (the user, 2026-11-02: the
	 * stack went from 62 to 61 and no lodestone compass came). 1.7.10 resends only the held slot after a block use.
	 */
	public void testCopyFromAStackShowsOnTheClient() {
		place(lodestone(), FRONT);
		survival();
		useOnTheLodestone(new ItemStack(Items.compass, 3), false);
		copyShowsOnTheClient();
		assertEqual(2, onClient(() -> mc().thePlayer.getHeldItem().stackSize), "the stack in the client's hand");
	}

	public void testCreativeCopyShowsOnTheClient() {
		place(lodestone(), FRONT);
		useOnTheLodestone(new ItemStack(Items.compass), false);
		copyShowsOnTheClient();
	}

	/** Using an item on a block while sneaking still uses the item. */
	public void testSneakingStillLocks() {
		place(lodestone(), FRONT);
		survival();
		useOnTheLodestone(new ItemStack(Items.compass), true);
		assertEqual(at(FRONT, 0), target(held()));
	}

	/** A lodestone compass locks onto the next lodestone it's used on. */
	public void testRelock() {
		Block lodestone = lodestone();
		place(lodestone, FRONT);
		place(lodestone, SECOND);
		survival();
		useOnTheLodestone(locked(SECOND, 0), false);
		assertEqual(at(FRONT, 0), target(held()));
	}

	/** The copy keeps everything else on the compass, an anvil's name too. */
	public void testCopyKeepsTheName() {
		place(lodestone(), FRONT);
		survival();
		ItemStack named = new ItemStack(Items.compass, 2);
		named.setStackDisplayName("Home");
		useOnTheLodestone(named, false);
		ItemStack copy = null;
		for (ItemStack stack : inventory()) {
			if (isLodestoneCompass(stack)) {
				copy = stack;
			}
		}
		assertIsNotNone(copy, "no lodestone compass in the inventory");
		assertEqual("Home", copy.getDisplayName());
		assertEqual(at(FRONT, 0), target(copy));
	}

	// ---------------------------------------------------------------- name and glint

	/** With the tracker, a target or not: "Lodestone Compass" with the glint. A plain compass stays as it is. */
	public void testNameAndGlint() {
		assertEqual("Lodestone Compass", onClient(() -> locked(FIRST, 0).getDisplayName()));
		assertEqual("Lodestone Compass", onClient(() -> tracked().getDisplayName()));
		assertTrue(onClient(() -> locked(FIRST, 0).hasEffect(0)), "no glint");
		assertTrue(onClient(() -> tracked().hasEffect(0)), "no glint without a target");
		assertEqual("Compass", onClient(() -> new ItemStack(Items.compass).getDisplayName()));
		assertFalse(onClient(() -> new ItemStack(Items.compass).hasEffect(0)), "a plain compass has the glint");
	}

	// ---------------------------------------------------------------- losing the lodestone

	/** Broken, the lodestone takes the target with it; the compass stays a tracked lodestone compass, and nothing else. */
	public void testBreakingTheLodestoneRemovesTheTarget() {
		place(lodestone(), FIRST);
		setHeld(locked(FIRST, 0));
		serverTicks(5);
		assertEqual(at(FIRST, 0), target(held()), "lost the target with the lodestone still there");
		onServer(() -> world().setBlockToAir(FIRST[0], FIRST[1], FIRST[2]));
		waitUntil("the target gone", () -> "no target".equals(target(player().getHeldItem())), 5);
		assertEqual(Collections.singletonList(TRACKED), keys(held()));
		assertEqual((byte) 1, held().getTagCompound().getByte(TRACKED));
	}

	public void testOffhandToo() {
		if (!Offhand.isEnabled()) {
			skipTest("the off-hand is off");
		}
		place(lodestone(), FIRST);
		onServer(() -> Offhand.setOffhand(player(), locked(FIRST, 0)));
		serverTicks(3);
		onServer(() -> world().setBlockToAir(FIRST[0], FIRST[1], FIRST[2]));
		waitUntil("the off-hand compass's target gone", () -> "no target".equals(target(Offhand.getOffhand(player()))), 5);
	}

	/** A target in another dimension is left alone. */
	public void testTargetInAnotherDimensionStays() {
		lodestone();
		setHeld(locked(FIRST, -1));
		serverTicks(10);
		assertEqual(at(FIRST, -1), target(held()));
	}

	/** An untracked compass (only from commands) is never checked. */
	public void testUntrackedKeepsItsTarget() {
		lodestone();
		ItemStack untracked = locked(FIRST, 0);
		untracked.getTagCompound().setByte(TRACKED, (byte) 0);
		setHeld(untracked);
		serverTicks(10);
		assertEqual(at(FIRST, 0), target(held()));
	}

	/**
	 * Compasses locked to two lodestones, both broken, are the same: one goes back into the inventory and joins the
	 * other, and `compass[lodestone_tracker={}]` joins them too; a plain compass doesn't (spec §57.4).
	 */
	public void testBrokenCompassesStack() {
		Block lodestone = lodestone();
		place(lodestone, FIRST);
		place(lodestone, SECOND);
		onServer(() -> {
			player().inventory.setInventorySlotContents(0, locked(FIRST, 0));
			player().inventory.setInventorySlotContents(1, locked(SECOND, 0));
		});
		serverTicks(3);
		onServer(() -> {
			world().setBlockToAir(FIRST[0], FIRST[1], FIRST[2]);
			world().setBlockToAir(SECOND[0], SECOND[1], SECOND[2]);
		});
		waitUntil("both targets gone", () -> "no target".equals(target(player().inventory.getStackInSlot(0)))
			&& "no target".equals(target(player().inventory.getStackInSlot(1))), 5);
		int together = onServer(() -> {
			ItemStack second = player().inventory.getStackInSlot(1);
			player().inventory.setInventorySlotContents(1, null);
			player().inventory.addItemStackToInventory(second);
			return player().inventory.getStackInSlot(0).stackSize;
		});
		assertEqual(2, together, "the two broken compasses don't stack");
		assertNoError(command("/give @s compass[lodestone_tracker={}]"));
		waitUntil("the given one stacked with them", () -> player().inventory.getStackInSlot(0).stackSize == 3, 10);
		onServer(() -> player().inventory.addItemStackToInventory(new ItemStack(Items.compass)));
		assertEqual(3, onServer(() -> player().inventory.getStackInSlot(0).stackSize), "a plain compass stacked with them");
	}

	/** 26.3 ticks every living entity's equipment: a zombie's compass loses its target too (the copper golem's case). */
	public void testHeldByAMob() {
		place(lodestone(), FIRST);
		EntityZombie zombie = spawn(EntityZombie::new, 32.5D, 4.0D, 8.5D);
		onServer(() -> {
			zombie.setCurrentItemOrArmor(0, locked(FIRST, 0));
			// no burning in the sun while the test runs
			zombie.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
		});
		serverTicks(3);
		assertEqual(at(FIRST, 0), target(onServer(() -> ItemStack.copyItemStack(zombie.getHeldItem()))), "lost the target with the lodestone still there");
		onServer(() -> world().setBlockToAir(FIRST[0], FIRST[1], FIRST[2]));
		waitUntil("the zombie's compass's target gone", () -> "no target".equals(target(zombie.getHeldItem())), 5);
	}

	/** Chests don't tick what's in them: there the compass keeps its target. */
	public void testInAChestItKeepsTheTarget() {
		place(lodestone(), FIRST);
		onServer(() -> {
			world().setBlock(34, 4, 8, Blocks.chest, 0, 3);
			((TileEntityChest) world().getTileEntity(34, 4, 8)).setInventorySlotContents(0, locked(FIRST, 0));
			world().setBlockToAir(FIRST[0], FIRST[1], FIRST[2]);
		});
		serverTicks(10);
		assertEqual(at(FIRST, 0), target(onServer(() -> ItemStack.copyItemStack(((TileEntityChest) world().getTileEntity(34, 4, 8)).getStackInSlot(0)))));
	}

	/** The lodestone's chunk unloaded: the compass keeps its target, and checking it doesn't load the chunk. */
	public void testLodestoneInAnUnloadedChunk() {
		int[] far = {1000, 4, 1000};
		place(lodestone(), far);
		unload(far);
		setHeld(locked(far, 0));
		serverTicks(10);
		assertEqual(at(far, 0), target(held()));
		assertFalse(loaded(far), "checking the compass loaded the lodestone's chunk");
	}

	/** Pointing into an unloaded chunk without a lodestone: the target goes (26.3's store knows), the chunk stays unloaded. */
	/**
	 * A lodestone at any height counts, as a taller world (McFly) can have one below 0 or above 255: EFR has no height
	 * bounds of its own (the user, 2026-11-02: "allow a lodestone with a weird y value"). Recorded as such a world
	 * would record it, in an unloaded chunk.
	 */
	public void testLodestoneAtAnyHeight() {
		lodestone();
		for (int y : new int[] {-30, 300, 5000}) {
			int[] far = {-1000, y, 1000};
			assertFalse(loaded(far), "the chunk was loaded before the test");
			onServer(() -> LodestoneRecord.get(world()).add(far[0], far[1], far[2]));
			try {
				setHeld(locked(far, 0));
				serverTicks(10);
				assertEqual(at(far, 0), target(held()), "a lodestone at y " + y);
			} finally {
				onServer(() -> LodestoneRecord.get(world()).remove(far[0], far[1], far[2]));
			}
		}
	}

	/** Far heights stay apart: a lodestone at y 904 doesn't answer for one at 5000 (904 is 5000 in 12 bits). */
	public void testFarHeightsDontMixUp() {
		lodestone();
		int[] recorded = {-1000, 904, 1000};
		onServer(() -> LodestoneRecord.get(world()).add(recorded[0], recorded[1], recorded[2]));
		try {
			setHeld(locked(new int[] {-1000, 5000, 1000}, 0));
			waitUntil("the target gone", () -> "no target".equals(target(player().getHeldItem())), 10);
		} finally {
			onServer(() -> LodestoneRecord.get(world()).remove(recorded[0], recorded[1], recorded[2]));
		}
	}

	/** In 1.7.10's own world nothing is below 0, so a target there is lost as any target without a lodestone is. */
	public void testNoLodestoneBelowTheFloor() {
		lodestone();
		int[] below = {0, -30, 0};
		assertTrue(loaded(below), "the player's own chunk isn't loaded");
		setHeld(locked(below, 0));
		waitUntil("the target gone", () -> "no target".equals(target(player().getHeldItem())), 10);
	}

	public void testNoLodestoneInAnUnloadedChunk() {
		lodestone();
		int[] far = {-1000, 4, 1000};
		assertFalse(loaded(far), "the chunk was loaded before the test");
		setHeld(locked(far, 0));
		waitUntil("the target gone", () -> "no target".equals(target(player().getHeldItem())), 10);
		assertFalse(loaded(far), "checking the compass loaded the chunk");
	}

	// ---------------------------------------------------------------- /give

	public void testGiveTracker() {
		ItemStack given = give("compass[lodestone_tracker={target:{dimension:\"minecraft:the_nether\",pos:[I;1,2,3]},tracked:false}]");
		assertEqual("1 2 3 in -1", target(given));
		assertEqual((byte) 0, given.getTagCompound().getByte(TRACKED));
	}

	/** An empty tracker: tracked, no target. */
	public void testGiveEmptyTracker() {
		ItemStack given = give("compass[lodestone_tracker={}]");
		assertEqual("no target", target(given));
		assertEqual(Collections.singletonList(TRACKED), keys(given));
		assertEqual((byte) 1, given.getTagCompound().getByte(TRACKED));
	}

	// ---------------------------------------------------------------- the needle

	/** Held, facing the lodestone straight ahead: once the needle settles, frame 16, pointing up. */
	public void testNeedleHeldPointsAhead() {
		needleSettlesAt(AHEAD, 16);
	}

	/**
	 * A shield in an item frame, drawn by the shield's own renderer (which cancels the frame's item drawing), leaves no
	 * owner behind: the needle went on taking that frame, dead, for every compass in a screen or hand afterwards.
	 */
	public void testFramedShieldLeavesNoOwnerBehind() {
		if (!io.github.asgerjon.mcfly.registry.McFlyItems.SHIELD.isEnabled()) {
			skipTest("the shield is turned off");
		}
		onServer(() -> {
			world().setBlock(0, 5, 3, net.minecraft.init.Blocks.stone);
			net.minecraft.entity.item.EntityItemFrame frame = new net.minecraft.entity.item.EntityItemFrame(world(), 0, 5, 3, 2);
			frame.setDisplayedItem(new ItemStack(io.github.asgerjon.mcfly.registry.McFlyItems.SHIELD.get()));
			world().spawnEntityInWorld(frame);
		});
		try {
			look(0.0F, 0.0F);
			ticks(10);
			assertEqual("nobody (the player)", onClient(LodestoneTests::shownBy));
		} finally {
			onServer(() -> {
				for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
					if (entity instanceof net.minecraft.entity.item.EntityItemFrame frame) {
						frame.setDead();
					}
				}
				world().setBlockToAir(0, 5, 3);
			});
		}
	}

	/** Facing south with the lodestone to the east: 26.3's formula gives a quarter turn, frame 8. */
	public void testNeedleHeldPointsEast() {
		needleSettlesAt(EAST, 8);
	}

	/** In an item frame the needle points at once, from the frame's facing and turn, without smoothing. */
	private static final int TURNS = ganymedes01.etfuturum.entities.ModernFrames.active() ? 8 : 4;

	public void testNeedleInAnItemFrame() {
		lodestone();
		List<Integer> frames = onClient(() -> {
			// on a wall, facing south; the lodestone 20 blocks south of it
			EntityItemFrame frame = new EntityItemFrame(mc().theWorld, 0, 5, -5, 0);
			int[] lodestonePos = {MathHelper.floor_double(frame.posX), 4, MathHelper.floor_double(frame.posZ) + 20};
			ItemStack compass = locked(lodestonePos, 0);
			List<Integer> drawnAndExpected = new ArrayList<>();
			// 26.3's frames turn in 8 eighths (spec §67.3), 1.7.10's in 4 quarters (without modernItemFrames)
			for (int rotation = 0; rotation < TURNS; rotation++) {
				frame.setItemRotation(rotation);
				drawnAndExpected.add(LodestoneNeedle.frameOf(LodestoneNeedle.icon(compass, frame, frame.getEntityId())));
				drawnAndExpected.add(pointing(frame.posX, frame.posZ, 180.0D + 90.0D * frame.hangingDirection
					+ ganymedes01.etfuturum.entities.ModernFrames.degreesPerTurn() * frame.getRotation(), lodestonePos[0] + 0.5D, lodestonePos[2] + 0.5D));
			}
			return drawnAndExpected;
		});
		for (int rotation = 0; rotation < TURNS; rotation++) {
			assertEqual(frames.get(2 * rotation + 1), frames.get(2 * rotation), "turned " + rotation + " times");
		}
	}

	/** On the ground the needle turns with the item as it spins: from its age and bob offset, without smoothing. */
	public void testNeedleOnTheGround() {
		lodestone();
		List<Integer> frames = onClient(() -> {
			ItemStack compass = locked(AHEAD, 0);
			EntityItem item = new EntityItem(mc().theWorld, 3.5D, 4.0D, 0.5D, compass);
			List<Integer> drawnAndExpected = new ArrayList<>();
			for (int age : new int[] {0, 30, 63, 100}) {
				item.age = age;
				item.hoverStart = 1.0F;
				drawnAndExpected.add(LodestoneNeedle.frameOf(LodestoneNeedle.icon(compass, item, item.getEntityId())));
				double yaw = 180.0D - Math.toDegrees((age + 0.5D) / 20.0D + item.hoverStart);
				drawnAndExpected.add(pointing(item.posX, item.posZ, yaw, AHEAD[0] + 0.5D, AHEAD[2] + 0.5D));
			}
			return drawnAndExpected;
		});
		for (int i = 0; i < 4; i++) {
			assertEqual(frames.get(2 * i + 1), frames.get(2 * i), "case " + i);
		}
	}

	/** The lodestone in another dimension: the needle spins, a different frame from tick to tick. */
	public void testNeedleSpinsInAnotherDimension() {
		lodestone();
		setHeld(locked(AHEAD, -1));
		Set<Integer> seen = heldFramesOver(20);
		assertNotIn(-1, seen, "not drawn as a needle frame");
		assertGreater(seen.size(), 1);
	}

	public void testNeedleSpinsWithoutATarget() {
		lodestone();
		setHeld(tracked());
		Set<Integer> seen = heldFramesOver(20);
		assertNotIn(-1, seen, "not drawn as a needle frame");
		assertGreater(seen.size(), 1);
	}

	/** A plain compass still draws 1.7.10's animated compass, pointing to the world spawn. */
	public void testPlainCompassUnchanged() {
		assertTrue(onClient(() -> new ItemStack(Items.compass).getIconIndex() instanceof TextureCompass), "a plain compass lost 1.7.10's needle");
	}

	// ---------------------------------------------------------------- helpers

	private static Block lodestone() {
		Block block = GameRegistry.findBlock(McFly.DOMAIN, "lodestone");
		assertIsNotNone(block, "EFR has no lodestone block");
		return block;
	}

	private void place(Block block, int[] at) {
		onServer(() -> world().setBlock(at[0], at[1], at[2], block, 0, 3));
	}

	/** The one lodestone compass the server put in the inventory is in the client's inventory too, within a few ticks. */
	private void copyShowsOnTheClient() {
		List<String> expected = Collections.singletonList("1x " + at(FRONT, 0));
		assertEqual(expected, lodestoneCompasses(inventory()), "on the server");
		waitUntilOnClient("the lodestone compass in the client's inventory", () -> lodestoneCompasses(clientInventory()).equals(expected), 10);
	}

	/** The player's main inventory as the player's own client has it. */
	private static List<ItemStack> clientInventory() {
		List<ItemStack> stacks = new ArrayList<>();
		for (ItemStack stack : mc().thePlayer.inventory.mainInventory) {
			if (stack != null) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	/** Air over the box, with whatever a chest there held taken out first so nothing drops. */
	private static void clear(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
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
	}

	/** A compass locked to the lodestone at the position in the dimension, tracked. */
	private static ItemStack locked(int[] at, int dimension) {
		ItemStack stack = tracked();
		NBTTagCompound pos = new NBTTagCompound();
		pos.setInteger("X", at[0]);
		pos.setInteger("Y", at[1]);
		pos.setInteger("Z", at[2]);
		stack.getTagCompound().setTag(POS, pos);
		stack.getTagCompound().setInteger(DIMENSION, dimension);
		return stack;
	}

	/** A tracked lodestone compass without a target, as a broken one. */
	private static ItemStack tracked() {
		ItemStack stack = new ItemStack(Items.compass);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setByte(TRACKED, (byte) 1);
		stack.setTagCompound(tag);
		return stack;
	}

	private static boolean isLodestoneCompass(ItemStack stack) {
		return stack != null && stack.getItem() == Items.compass && stack.hasTagCompound() && stack.getTagCompound().hasKey(TRACKED);
	}

	/** "x y z in dimension", "no target", or "not a lodestone compass". */
	private static String target(ItemStack stack) {
		if (!isLodestoneCompass(stack)) {
			return "not a lodestone compass";
		}
		NBTTagCompound tag = stack.getTagCompound();
		if (!tag.hasKey(POS)) {
			return "no target";
		}
		NBTTagCompound pos = tag.getCompoundTag(POS);
		return pos.getInteger("X") + " " + pos.getInteger("Y") + " " + pos.getInteger("Z") + " in " + tag.getInteger(DIMENSION);
	}

	private static String at(int[] pos, int dimension) {
		return pos[0] + " " + pos[1] + " " + pos[2] + " in " + dimension;
	}

	/** The stack's NBT keys, sorted. */
	@SuppressWarnings("unchecked")
	private static List<String> keys(ItemStack stack) {
		return stack.hasTagCompound() ? new ArrayList<>(new TreeSet<String>(stack.getTagCompound().func_150296_c())) : Collections.emptyList();
	}

	/** "1x 0 5 2 in 0" for each lodestone compass among the stacks. */
	private static List<String> lodestoneCompasses(List<ItemStack> stacks) {
		List<String> found = new ArrayList<>();
		for (ItemStack stack : stacks) {
			if (isLodestoneCompass(stack)) {
				found.add(stack.stackSize + "x " + target(stack));
			}
		}
		return found;
	}

	private static List<String> ids(List<ItemStack> stacks) {
		List<String> found = new ArrayList<>();
		for (ItemStack stack : stacks) {
			found.add(id(stack));
		}
		return found;
	}

	private ItemStack give(String item) {
		assertNoError(command("/give @s " + item));
		List<ItemStack> stacks = inventory();
		assertTrue(!stacks.isEmpty(), "/give @s " + item + " gave nothing");
		return stacks.get(0);
	}

	/** What eight chiseled stone bricks around the middle item make, or null. */
	private ItemStack craft(ItemStack middle) {
		return onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			for (int slot = 0; slot < 9; slot++) {
				grid.setInventorySlotContents(slot, slot == 4 ? middle.copy() : new ItemStack(Blocks.stonebrick, 1, 3));
			}
			return CraftingManager.getInstance().findMatchingRecipe(grid, world());
		});
	}

	/** In survival, holding the tool, mines a lodestone; how many lodestones drop. */
	private int mine(Block lodestone, ItemStack tool) {
		survival();
		setHeld(tool);
		place(lodestone, FIRST);
		onServer(() -> player().theItemInWorldManager.tryHarvestBlock(FIRST[0], FIRST[1], FIRST[2]));
		serverTicks(2);
		assertEqual(Blocks.air, onServer(() -> world().getBlock(FIRST[0], FIRST[1], FIRST[2])), "the lodestone wasn't mined");
		return onServer(() -> {
			int count = 0;
			AxisAlignedBB around = AxisAlignedBB.getBoundingBox(FIRST[0] - 2, FIRST[1] - 2, FIRST[2] - 2, FIRST[0] + 3, FIRST[1] + 3, FIRST[2] + 3);
			for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, around)) {
				ItemStack stack = ((EntityItem) entity).getEntityItem();
				if (stack.getItem() == Item.getItemFromBlock(lodestone)) {
					count += stack.stackSize;
				}
			}
			return count;
		});
	}

	/** Holds the stack and right-clicks the front lodestone's north face with it, as a player would. */
	private void useOnTheLodestone(ItemStack stack, boolean sneaking) {
		setHeld(stack);
		waitUntilOnClient("the compass in hand", () -> mc().thePlayer.getHeldItem() != null, 20);
		if (sneaking) {
			keyDown(mc().gameSettings.keyBindSneak);
		}
		try {
			if (sneaking) {
				waitUntil("the player sneaking", () -> player().isSneaking(), 10);
			}
			look(0.0F, 0.0F);
			aimAt(FRONT, 2);
			tap(mc().gameSettings.keyBindUseItem);
			serverTicks(4);
		} finally {
			if (sneaking) {
				keyUp(mc().gameSettings.keyBindSneak);
				ticks(2);
			}
		}
	}

	private void aimAt(int[] at, int side) {
		waitUntilOnClient("the crosshair on " + at[0] + " " + at[1] + " " + at[2] + " side " + side, () -> {
			MovingObjectPosition hit = mc().objectMouseOver;
			return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& hit.blockX == at[0] && hit.blockY == at[1] && hit.blockZ == at[2] && hit.sideHit == side;
		}, 20);
	}

	private void unload(int[] at) {
		onServer(() -> world().theChunkProviderServer.unloadChunksIfNotNearSpawn(at[0] >> 4, at[2] >> 4));
		waitUntil("the chunk at " + at[0] + " " + at[2] + " unloaded", () -> !world().theChunkProviderServer.chunkExists(at[0] >> 4, at[2] >> 4), 40);
	}

	private boolean loaded(int[] at) {
		return onServer(() -> world().theChunkProviderServer.chunkExists(at[0] >> 4, at[2] >> 4));
	}

	/** With a lodestone there and a compass locked to it in hand, the player's needle comes to rest on the frame. */
	private void needleSettlesAt(int[] lodestonePos, int frame) {
		place(lodestone(), lodestonePos);
		setHeld(locked(lodestonePos, 0));
		waitUntilOnClient("the compass in hand", () -> isLodestoneCompass(mc().thePlayer.getHeldItem()), 20);
		look(0.0F, 0.0F);
		waitUntilOnClient("the held needle at frame " + frame, () -> heldFrame() == frame, 100);
		// in a screen (the hotbar, the inventory) its owner is the player too: the same frame, read in the same tick (the
		// needle still swings a little as it settles, and east sits on the edge of frames 7 and 8)
		int[] both = onClient(() -> new int[]{heldFrame(), LodestoneNeedle.frameOf(mc().thePlayer.getHeldItem().getIconIndex())});
		assertEqual(both[0], both[1], "in a screen; shown by " + onClient(LodestoneTests::shownBy));
	}

	/** Who the needle thinks is showing the compass now (null: the player), for a failure's message. */
	private static String shownBy() {
		try {
			java.lang.reflect.Field field = LodestoneNeedle.class.getDeclaredField("shownBy");
			field.setAccessible(true);
			Object owner = field.get(null);
			return owner == null ? "nobody (the player)" : owner + (owner instanceof net.minecraft.entity.Entity entity && entity.isDead ? " (dead)" : "");
		} catch (ReflectiveOperationException e) {
			return "? (" + e + ")";
		}
	}

	/** The needle frame of the compass in the player's hand, as the hand draws it. */
	private static int heldFrame() {
		ItemStack held = mc().thePlayer.getHeldItem();
		return held == null ? -1 : LodestoneNeedle.frameOf(mc().thePlayer.getItemIcon(held, 0));
	}

	/** The held needle's frames over these ticks, one look a tick. */
	private Set<Integer> heldFramesOver(int ticks) {
		waitUntilOnClient("the compass in hand", () -> isLodestoneCompass(mc().thePlayer.getHeldItem()), 20);
		Set<Integer> seen = new TreeSet<>();
		for (int tick = 0; tick < ticks; tick++) {
			seen.add(onClient(() -> heldFrame()));
		}
		return seen;
	}

	/** 26.3's frame for a needle value: 0 is frame 16, straight up, and each 1/32 turn one frame on. */
	private static int needleFrame(double value) {
		double turn = (value % 1.0D + 1.0D) % 1.0D;
		return ((int) Math.floor(turn * 32.0D + 0.5D) + 16) % 32;
	}

	/** 26.3's needle for an owner that doesn't smooth it: at (x, z) with this visual yaw, the lodestone's centre at (tx, tz). */
	private static int pointing(double x, double z, double yaw, double tx, double tz) {
		double toTarget = Math.atan2(tz - z, tx - x) / (2.0D * Math.PI);
		double facing = (yaw / 360.0D % 1.0D + 1.0D) % 1.0D;
		return needleFrame(0.5D - (facing - 0.25D - toTarget));
	}
}
