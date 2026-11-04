package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.entities.EntityGlowItemFrame;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Item frames as 26.3 and glow item frames (SPEC-modern-movement.md §67.3, §67.4; 26.3's `ItemFrame`, `ItemFrameItem`,
 * `ComparatorBlock`, `GlowItemFrame`). A stone block in the air at (12, 5, -8) for floors and ceilings (its top and
 * bottom faces), a wall of stone at z -7 for walls and the comparator.
 */
public class ItemFrameTests extends TasTestCase {

	private static final int BX = 12, BY = 5, BZ = -8;
	/** 1.7.10's `hangingDirection` and the port's two new ones. */
	private static final int SOUTH = 0, NORTH = 2, FLOOR = 4, CEILING = 5;
	private static final String MODERN = McFly.MC_ASSETS + ":";

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	@Override
	protected void setUp() {
		if (!McFlyConfig.modernItemFrames) {
			skipTest("modernItemFrames is off");
		}
		onServer(() -> world().setBlock(BX, BY, BZ, Blocks.stone));
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (EntityItemFrame frame : entitiesNear(EntityItemFrame.class, 60.0D)) {
				frame.setDead();
			}
			for (EntityItem item : entitiesNear(EntityItem.class, 60.0D)) {
				item.setDead();
			}
			for (int x = 8; x <= 18; x++) {
				for (int y = 4; y <= 8; y++) {
					for (int z = -12; z <= -3; z++) {
						world().setBlockToAir(x, y, z);
					}
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	// ---------------------------------------------------------------- placing

	/** An item frame goes on a block's top (facing up) and bottom (facing down), and is used up. */
	public void testPlacedOnTopAndBottom() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		setHeld(new ItemStack(Items.item_frame, 2));
		assertTrue(place(1), "the frame wasn't placed on the block's top");
		assertTrue(place(0), "the frame wasn't placed on the block's bottom");
		List<Integer> ways = onServer(() -> {
			List<Integer> found = new ArrayList<>();
			for (EntityItemFrame frame : entitiesNear(EntityItemFrame.class, 60.0D)) {
				found.add(frame.hangingDirection);
			}
			return found;
		});
		assertTrue(ways.contains(FLOOR) && ways.contains(CEILING), "the frames' facings " + ways);
		// the item's use is called straight here: 1.7.10 leaves a used-up stack at 0 (the server's packet handler clears it)
		assertEqual(0, onServer(() -> player().getHeldItem() == null ? 0 : player().getHeldItem().stackSize), "frames left");
		double[] floor = onServer(() -> box(frameFacing(FLOOR)));
		assertTrue(floor[1] >= BY + 1.0D && floor[4] <= BY + 1.1D && floor[0] > BX && floor[3] < BX + 1.0D, "the floor frame's box " + java.util.Arrays.toString(floor));
	}

	/** A floor frame stays while its block does and drops (as an item frame) when the block goes. */
	public void testStaysAndDrops() {
		EntityItemFrame frame = spawnFrame(BX, BY, BZ, FLOOR);
		serverTicks(110);
		assertFalse(onServer(() -> frame.isDead), "the frame dropped from a solid block");
		onServer(() -> world().setBlockToAir(BX, BY, BZ));
		waitUntil("the frame gone", () -> frame.isDead, 120);
		assertTrue(onServer(() -> {
			for (EntityItem item : entitiesNear(EntityItem.class, 60.0D)) {
				if (item.getEntityItem().getItem() == Items.item_frame) {
					return true;
				}
			}
			return false;
		}), "no item frame dropped");
	}

	// ---------------------------------------------------------------- turning

	/** Used with an item in it, a frame turns 8 times (45 degrees each), the 8th back to 0; a floor frame too. */
	public void testEightTurns() {
		for (int facing : new int[]{NORTH, FLOOR}) {
			EntityItemFrame frame = facing == FLOOR ? spawnFrame(BX, BY, BZ, FLOOR) : spawnFrame(BX, BY, BZ, NORTH);
			onServer(() -> frame.setDisplayedItem(new ItemStack(Items.diamond)));
			List<Integer> turns = onServer(() -> {
				List<Integer> seen = new ArrayList<>();
				for (int i = 0; i < 8; i++) {
					frame.interactFirst(player());
					seen.add(frame.getRotation());
				}
				return seen;
			});
			assertEqual("[1, 2, 3, 4, 5, 6, 7, 0]", turns.toString(), "the turns of a frame facing " + facing);
			onServer(frame::setDead);
		}
	}

	/**
	 * A comparator reads a frame through a block (26.3's `ComparatorBlock`): rotation + 1 with an item, 0 without; and
	 * nothing with two frames there.
	 */
	public void testComparatorReadsAFrame() {
		// comparator at z -6 facing north (its input at z -7, the stone); the frame on the stone's north face, at z -8
		onServer(() -> {
			world().setBlock(14, 4, -7, Blocks.stone);
			world().setBlock(14, 4, -6, Blocks.unpowered_comparator, NORTH, 3);
		});
		EntityItemFrame frame = spawnFrame(14, 4, -7, NORTH);
		assertEqual(0, signal(), "an empty frame's signal");
		onServer(() -> frame.setDisplayedItem(new ItemStack(Items.diamond)));
		assertEqual(1, signal(), "the signal at rotation 0");
		onServer(() -> {
			for (int i = 0; i < 5; i++) {
				frame.interactFirst(player());
			}
		});
		assertEqual(6, signal(), "the signal at rotation 5");
		EntityItemFrame second = spawnFrame(14, 4, -7, NORTH);
		onServer(() -> second.setDisplayedItem(new ItemStack(Items.diamond)));
		assertEqual(0, signal(), "the signal with two frames");
	}

	// ---------------------------------------------------------------- saved and sent

	/** Saved with `Facing` (26.3's 6 ways) and read back on a floor and a ceiling, with their 8-way rotation. */
	public void testSavedAndReadBack() {
		for (int facing : new int[]{FLOOR, CEILING}) {
			EntityItemFrame frame = spawnFrame(BX, BY, BZ, facing);
			int[] read = onServer(() -> {
				frame.setDisplayedItem(new ItemStack(Items.diamond));
				frame.setItemRotation(5);
				NBTTagCompound tag = new NBTTagCompound();
				frame.writeToNBT(tag);
				EntityItemFrame copy = new EntityItemFrame(world());
				copy.readFromNBT(tag);
				return new int[]{tag.hasKey("Facing") ? tag.getByte("Facing") : -1, copy.hangingDirection, copy.getRotation()};
			});
			assertEqual(facing == FLOOR ? 1 : 0, read[0], "the saved Facing");
			assertEqual(facing, read[1], "the facing read back");
			assertEqual(5, read[2], "the rotation read back");
			onServer(frame::setDead);
		}
	}

	/**
	 * Reading a frame from its NBT touches no block (26.3's `setRotation(rotation, false)` there): chunk loading reads
	 * frames before their chunk is in the world, and telling comparators of the read turn loaded the chunk again from
	 * inside its own loading, so the frame couldn't be restored and was lost, and the nested loads added the chunk's
	 * entities again (the user's world, 2026-11-03: dozens of copies of each frame, the End's server thread stuck). It
	 * was a frame 1.7.10 saved (no `Facing`): its turn is converted after its place is read. One saved far out, where
	 * no chunk is loaded, read back: the chunks there stay unloaded.
	 */
	public void testReadingTouchesNoBlock() {
		EntityItemFrame frame = spawnFrame(BX, BY, BZ, SOUTH);
		boolean loaded = onServer(() -> {
			frame.setDisplayedItem(new ItemStack(Items.diamond));
			frame.setItemRotation(3);
			NBTTagCompound tag = new NBTTagCompound();
			frame.writeToNBT(tag);
			int far = 40000;
			tag.removeTag("Facing");
			tag.setInteger("TileX", far);
			tag.setInteger("TileZ", far);
			net.minecraft.nbt.NBTTagList pos = new net.minecraft.nbt.NBTTagList();
			pos.appendTag(new net.minecraft.nbt.NBTTagDouble(far + 0.5D));
			pos.appendTag(new net.minecraft.nbt.NBTTagDouble(BY + 0.5D));
			pos.appendTag(new net.minecraft.nbt.NBTTagDouble(far + 0.5D));
			tag.setTag("Pos", pos);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (world().getChunkProvider().chunkExists((far >> 4) + dx, (far >> 4) + dz)) {
						return null;
					}
				}
			}
			EntityItemFrame copy = new EntityItemFrame(world());
			copy.readFromNBT(tag);
			boolean any = false;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					any |= world().getChunkProvider().chunkExists((far >> 4) + dx, (far >> 4) + dz);
				}
			}
			return any;
		});
		onServer(frame::setDead);
		assertEqual(false, loaded, "reading a frame loaded a chunk around it (null: one was loaded already)");
	}

	/** A frame 1.7.10 saved (no `Facing`; `ItemRotation` 3 of its 4) is read on its wall with rotation 6 (the same look). */
	public void testOldFrameConverted() {
		EntityItemFrame frame = spawnFrame(BX, BY, BZ, SOUTH);
		int[] read = onServer(() -> {
			frame.setDisplayedItem(new ItemStack(Items.diamond));
			NBTTagCompound tag = new NBTTagCompound();
			frame.writeToNBT(tag);
			tag.removeTag("Facing");
			tag.setByte("ItemRotation", (byte) 3);
			EntityItemFrame copy = new EntityItemFrame(world());
			copy.readFromNBT(tag);
			return new int[]{copy.hangingDirection, copy.getRotation()};
		});
		assertEqual(SOUTH, read[0], "an old frame's wall");
		assertEqual(6, read[1], "an old frame's rotation");
	}

	/** On the client: the floor frame's facing and rotation. */
	public void testOnTheClient() {
		EntityItemFrame frame = spawnFrame(BX, BY, BZ, FLOOR);
		onServer(() -> {
			frame.setDisplayedItem(new ItemStack(Items.diamond));
			frame.setItemRotation(3);
		});
		waitUntilOnClient("the client's floor frame turned 3", () -> {
			Entity seen = mc().theWorld.getEntityByID(frame.getEntityId());
			return seen instanceof EntityItemFrame client && client.hangingDirection == FLOOR && client.getRotation() == 3;
		}, 40);
	}

	// ---------------------------------------------------------------- glow item frames

	/** Shapeless: an item frame and a glow ink sac. */
	public void testGlowFrameRecipe() {
		needGlowFrames();
		ItemStack made = onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			grid.setInventorySlotContents(0, new ItemStack(Items.item_frame));
			grid.setInventorySlotContents(4, McFlyItems.GLOW_INK_SAC.newItemStack());
			return CraftingManager.getInstance().findMatchingRecipe(grid, world());
		});
		assertIsNotNone(made, "the recipe makes nothing");
		assertEqual(McFlyItems.GLOW_ITEM_FRAME.get(), made.getItem(), "the recipe's item");
	}

	/** A glow item frame placed (a floor too), a glow frame on the client, with its place sound. */
	public void testGlowFramePlaced() {
		needGlowFrames();
		setHeld(McFlyItems.GLOW_ITEM_FRAME.newItemStack(1));
		clearSounds();
		assertTrue(place(1), "the glow frame wasn't placed");
		EntityItemFrame frame = onServer(() -> frameFacing(FLOOR));
		assertTrue(frame instanceof EntityGlowItemFrame, "a plain frame placed: " + frame);
		waitUntilOnClient("a glow frame on the client", () -> mc().theWorld.getEntityByID(frame.getEntityId()) instanceof EntityGlowItemFrame client
			&& client.hangingDirection == FLOOR, 40);
		if (ganymedes01.etfuturum.configuration.configs.ConfigSounds.paintingItemFramePlacing) {
			waitForSound(MODERN + "entity.glow_item_frame.place", 10);
		}
	}

	/** Broken, a glow frame drops a glow item frame (and not a plain one). */
	public void testGlowFrameDropsItsOwn() {
		needGlowFrames();
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		EntityGlowItemFrame frame = onServer(() -> {
			EntityGlowItemFrame made = new EntityGlowItemFrame(world(), BX, BY, BZ, NORTH);
			world().spawnEntityInWorld(made);
			return made;
		});
		onServer(() -> frame.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		String dropped = onServer(() -> {
			StringBuilder found = new StringBuilder();
			for (EntityItem item : entitiesNear(EntityItem.class, 60.0D)) {
				found.append(id(item.getEntityItem())).append(' ');
			}
			return found.toString().trim();
		});
		assertEqual(id(McFlyItems.GLOW_ITEM_FRAME.newItemStack()), dropped, "what the glow frame dropped");
	}

	// ---------------------------------------------------------------- helpers

	private static void needGlowFrames() {
		if (!McFlyItems.GLOW_ITEM_FRAME.isEnabled()) {
			skipTest("glow item frames are off");
		}
	}

	/** The player uses what they hold on the stone's face (0 bottom, 1 top); whether it was used. */
	private boolean place(int side) {
		return onServer(() -> player().getHeldItem().getItem().onItemUse(player().getHeldItem(), player(), world(), BX, BY, BZ, side, 0.5F, 0.5F, 0.5F));
	}

	private EntityItemFrame spawnFrame(int x, int y, int z, int facing) {
		// 1.7.10's renderer can't draw a floor or ceiling frame (its direction tables have 4 entries): none without the port
		assertTrue(facing < FLOOR || ganymedes01.etfuturum.entities.ModernFrames.active(), "no floor or ceiling frames");
		return onServer(() -> {
			EntityItemFrame frame = new EntityItemFrame(world(), x, y, z, facing);
			world().spawnEntityInWorld(frame);
			return frame;
		});
	}

	private static EntityItemFrame frameFacing(int facing) {
		for (EntityItemFrame frame : entitiesNear(EntityItemFrame.class, 60.0D)) {
			if (frame.hangingDirection == facing) {
				return frame;
			}
		}
		return null;
	}

	private static double[] box(EntityItemFrame frame) {
		return new double[]{frame.boundingBox.minX, frame.boundingBox.minY, frame.boundingBox.minZ, frame.boundingBox.maxX, frame.boundingBox.maxY, frame.boundingBox.maxZ};
	}

	/** The comparator's output a few ticks on. */
	private int signal() {
		serverTicks(6);
		return onServer(() -> ((TileEntityComparator) world().getTileEntity(14, 4, -6)).getOutputSignal());
	}
}
