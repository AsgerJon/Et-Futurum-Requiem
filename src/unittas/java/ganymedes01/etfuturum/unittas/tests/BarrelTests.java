package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The barrel (SPEC-modern-movement.md 92.4, step B), as 26.3's `BarrelBlock` and `BarrelBlockEntity` on EFR's id and
 * NBT: facing back along the player's nearest look, open while anyone views it (26.3's `ContainerOpenersCounter`, its
 * sounds at the lid, its game events, the 5-tick recheck, no spectators), its name kept in its drop, 27 slots. At z
 * 820, y 8, on stone. Read through block names, metas, tile entity NBT, class names and McFly's game events (which
 * EFR's barrel never posts), so it runs on EFR's (and fails there).
 */
@TasSuite("workstations")
public class BarrelTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 820;

	/** What a recording heard: the barrel's sounds (with where) and McFly's game events. */
	private final List<String> sounds = Collections.synchronizedList(new ArrayList<>());
	private final List<String> events = Collections.synchronizedList(new ArrayList<>());
	private IWorldAccess soundRecorder;
	private GameEventListener eventRecorder;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z - 2.5D, 0.0F, 0.0F);
		});
		serverTicks(2);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			stopRecording();
			player().closeScreen();
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	/** x 35 to 45, y 6 to 11, z 815 to 825: stone at y 6 and 7, air above; no items. */
	private static void clear() {
		for (int x = X - 5; x <= X + 5; x++) {
			for (int z = Z - 5; z <= Z + 5; z++) {
				for (int y = Y - 2; y <= Y + 3; y++) {
					world().setBlock(x, y, z, y < Y ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 6, Y - 3, Z - 6, X + 7, Y + 6, Z + 7))) {
			((Entity) entity).setDead();
		}
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject("etfuturum:" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no block etfuturum:" + name);
		return (Block) block;
	}

	/** A barrel at the middle with this meta. */
	private static IInventory place(int meta) {
		world().setBlock(X, Y, Z, block("barrel"), meta, 3);
		return (IInventory) world().getTileEntity(X, Y, Z);
	}

	/**
	 * The player uses it (as a right click on its top). Placed a few ticks before: the client needs its tile entity
	 * first (EFR's barrel screen fails on the client without it, and its failure stops the run).
	 */
	private static void use() {
		world().getBlock(X, Y, Z).onBlockActivated(world(), X, Y, Z, player(), 1, 0.5F, 1.0F, 0.5F);
	}

	private static int meta() {
		return world().getBlockMetadata(X, Y, Z);
	}

	/** Its tile entity's NBT changed by this, read back into it. */
	private static void edit(java.util.function.Consumer<NBTTagCompound> change) {
		TileEntity tile = world().getTileEntity(X, Y, Z);
		NBTTagCompound tag = new NBTTagCompound();
		tile.writeToNBT(tag);
		change.accept(tag);
		tile.readFromNBT(tag);
	}

	// ---------------------------------------------------------------- recording (on the server)

	private void startRecording() {
		sounds.clear();
		events.clear();
		soundRecorder = new SoundRecorder(sounds);
		world().addWorldAccess(soundRecorder);
		eventRecorder = new EventRecorder(events);
		GameEvents.register(world(), eventRecorder);
	}

	private void stopRecording() {
		if (soundRecorder != null) {
			world().removeWorldAccess(soundRecorder);
			soundRecorder = null;
		}
		if (eventRecorder != null) {
			GameEvents.unregister(world(), eventRecorder);
			eventRecorder = null;
		}
	}

	/** What was recorded since the last call, emptied. */
	private String recorded() {
		String got = sounds + " " + events;
		sounds.clear();
		events.clear();
		return got;
	}

	/** The world's sounds with "barrel" in their name: the name without its domain, at x, y, z. */
	private static final class SoundRecorder implements IWorldAccess {
		private final List<String> heard;

		SoundRecorder(List<String> heard) {
			this.heard = heard;
		}

		@Override
		public void playSound(String name, double x, double y, double z, float volume, float pitch) {
			if (name.contains("barrel")) {
				heard.add(name.substring(name.indexOf(':') + 1) + "@" + x + "," + y + "," + z + (volume == 0.5F && pitch >= 0.9F && pitch <= 1.0F ? "" : " volume " + volume
					+ " pitch " + pitch));
			}
		}

		@Override
		public void playSoundToNearExcept(EntityPlayer except, String name, double x, double y, double z, float volume, float pitch) {
			playSound(name, x, y, z, volume, pitch);
		}

		@Override
		public void markBlockForUpdate(int x, int y, int z) {
		}

		@Override
		public void markBlockForRenderUpdate(int x, int y, int z) {
		}

		@Override
		public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
		}

		@Override
		public void spawnParticle(String name, double x, double y, double z, double dx, double dy, double dz) {
		}

		@Override
		public void onEntityCreate(Entity entity) {
		}

		@Override
		public void onEntityDestroy(Entity entity) {
		}

		@Override
		public void playRecord(String record, int x, int y, int z) {
		}

		@Override
		public void broadcastSound(int id, int x, int y, int z, int data) {
		}

		@Override
		public void playAuxSFX(EntityPlayer player, int type, int x, int y, int z, int data) {
		}

		@Override
		public void destroyBlockPartially(int breaker, int x, int y, int z, int progress) {
		}

		@Override
		public void onStaticEntitiesChanged() {
		}
	}

	/** McFly's game events within 8 of the barrel. */
	private static final class EventRecorder implements GameEventListener {
		private final List<String> heard;

		EventRecorder(List<String> heard) {
			this.heard = heard;
		}

		@Override
		public double[] position(World world) {
			return new double[]{X + 0.5D, Y + 0.5D, Z + 0.5D};
		}

		@Override
		public int radius() {
			return 8;
		}

		@Override
		public boolean handle(World world, GameEvent event, GameEvent.Context context, double x, double y, double z) {
			String name = event.name.replace("minecraft:", "");
			if (name.startsWith("container_")) {
				heard.add(name);
			}
			return true;
		}
	}

	// ---------------------------------------------------------------- ours

	public void testOurs() {
		List<String> wrong = new ArrayList<>();
		String type = block("barrel").getClass().getName();
		if (!type.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add("barrel " + type);
		}
		String tile = onServer(() -> {
			TileEntity entity = (TileEntity) place(2);
			return entity == null ? "none" : entity.getClass().getName();
		});
		if (!tile.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add("tile " + tile);
		}
		for (String name : new String[]{"blocks.BlockBarrel", "blocks.itemblocks.ItemBlockBarrel", "tileentities.TileEntityBarrel", "client.gui.inventory.GuiBarrel",
			"inventory.ContainerChestGeneric", "client.renderer.block.BlockBarrelRenderer", "items.ItemBarrelUpgrade"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the barrel's classes");
	}

	// ---------------------------------------------------------------- placing

	/**
	 * Facing back at the player along their nearest look (26.3's `getNearestLookingDirection().getOpposite()`), the
	 * player 2.5 away: down at it gives up (1), up gives down (0), mostly down (50) up, south gives north (2), west
	 * gives east (5). EFR's piston rule gives up or down only within 2 of the block.
	 */
	public void testPlacingByTheLook() {
		String got = onServer(() -> {
			StringBuilder metas = new StringBuilder();
			float[][] looks = {{0.0F, 90.0F}, {0.0F, -90.0F}, {0.0F, 50.0F}, {0.0F, 0.0F}, {90.0F, 0.0F}};
			for (float[] look : looks) {
				player().rotationYaw = look[0];
				player().rotationPitch = look[1];
				ItemStack stack = new ItemStack(block("barrel"));
				stack.tryPlaceItemIntoWorld(player(), world(), X, Y - 1, Z, 1, 0.5F, 1.0F, 0.5F);
				metas.append(metas.length() == 0 ? "" : " ").append(world().getBlock(X, Y, Z) == block("barrel") ? String.valueOf(meta()) : "none");
				world().setBlockToAir(X, Y, Z);
			}
			player().rotationYaw = 0.0F;
			player().rotationPitch = 0.0F;
			return metas.toString();
		});
		assertEqual("1 0 1 2 5", got, "facings placed looking down, up, mostly down, south, west");
	}

	// ---------------------------------------------------------------- open and closed

	/**
	 * Facing south (3): opened, it's open (11), `block.barrel.open` at the lid (the middle plus half south), volume 0.5,
	 * pitch 0.9 to 1, and `container_open`; closed, it's closed (3), `block.barrel.close` there, `container_close`.
	 */
	public void testOpenAndClose() {
		onServer(() -> place(3));
		serverTicks(3);
		onServer(() -> {
			startRecording();
			use();
		});
		serverTicks(3);
		String opened = onServer(() -> recorded() + " meta " + meta());
		onServer(() -> player().closeScreen());
		serverTicks(3);
		String closed = onServer(() -> {
			String got = recorded() + " meta " + meta();
			stopRecording();
			return got;
		});
		String lid = "@40.5,8.5,821.0";
		assertEqual("[block.barrel.open" + lid + "] [container_open] meta 11; [block.barrel.close" + lid + "] [container_close] meta 3", opened + "; " + closed,
			"opened; closed");
	}

	/** Its viewer's container swapped away without closing it: 26.3's recheck (every 5 ticks) shuts it within 10. */
	public void testRecheckShutsIt() {
		onServer(() -> place(2));
		serverTicks(3);
		onServer(BarrelTests::use);
		serverTicks(3);
		int open = onServer(BarrelTests::meta);
		onServer(() -> {
			player().openContainer = player().inventoryContainer;
		});
		serverTicks(12);
		int after = onServer(BarrelTests::meta);
		assertEqual("open 10, after 2", "open " + open + ", after " + after, "opened; its viewer gone 12 ticks");
	}

	/** A spectator looking in leaves it closed (26.3's counter skips spectators). */
	public void testSpectatorLeavesItClosed() {
		if (WorldSettings.GameType.getByID(3) == null || WorldSettings.GameType.getByID(3) == WorldSettings.GameType.SURVIVAL) {
			skipTest("spectator mode is off");
		}
		onServer(() -> place(2));
		serverTicks(3);
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.getByID(3));
			use();
		});
		serverTicks(3);
		int meta = onServer(() -> {
			int now = meta();
			player().closeScreen();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			return now;
		});
		assertEqual(2, meta, "a spectator's barrel");
	}

	// ---------------------------------------------------------------- stored form, drops

	/**
	 * EFR's tag read back: `Items` (a metal barrel's slot 40 past the 27), `Type` (iron's 1), `CustomName`. Written:
	 * the 27 slots' items and the name, no `Type`.
	 */
	public void testStoredForm() {
		String got = onServer(() -> {
			place(2);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "etfuturum.barrel");
			tag.setInteger("x", X);
			tag.setInteger("y", Y);
			tag.setInteger("z", Z);
			NBTTagList items = new NBTTagList();
			int[][] slots = {{0, 5}, {26, 1}, {40, 1}};
			ItemStack[] stacks = {new ItemStack(Items.apple, 5), new ItemStack(Items.stick), new ItemStack(Items.diamond)};
			for (int i = 0; i < slots.length; i++) {
				NBTTagCompound item = new NBTTagCompound();
				stacks[i].writeToNBT(item);
				item.setByte("Slot", (byte) slots[i][0]);
				items.appendTag(item);
			}
			tag.setTag("Items", items);
			tag.setByte("Type", (byte) 1);
			tag.setString("CustomName", "Keg");
			world().getTileEntity(X, Y, Z).readFromNBT(tag);
			NBTTagCompound saved = new NBTTagCompound();
			world().getTileEntity(X, Y, Z).writeToNBT(saved);
			Map<Integer, String> kept = new TreeMap<>();
			NBTTagList list = saved.getTagList("Items", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound item = list.getCompoundTagAt(i);
				ItemStack stack = ItemStack.loadItemStackFromNBT(item);
				kept.put(item.getByte("Slot") & 255, stack == null ? "none" : Recipes26_3.nameOf(stack) + " " + stack.stackSize);
			}
			return kept + ", size " + ((IInventory) world().getTileEntity(X, Y, Z)).getSizeInventory() + ", Type " + (saved.hasKey("Type") ? "written" : "not written")
				+ ", name " + saved.getString("CustomName");
		});
		assertEqual("{0=apple 5, 26=stick 1}, size 27, Type not written, name Keg", got, "EFR's tag read, 26.3's written");
	}

	/** Broken by the player: itself, named as it was (26.3's loot copies `custom_name`), and its contents spilled. */
	public void testDropsItselfNamed() {
		String got = onServer(() -> {
			IInventory barrel = place(2);
			barrel.setInventorySlotContents(0, new ItemStack(Items.apple, 10));
			edit(tag -> tag.setString("CustomName", "Keg"));
			player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
			return dropped();
		});
		assertEqual("[1 barrel named Keg, 10 apple]", got, "what breaking a named barrel holding 10 apples drops");
	}

	/** The items lying round the middle, sorted: "count name" and a custom name. */
	static String dropped() {
		List<String> found = new ArrayList<>();
		int apples = 0;
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 3, Y - 2, Z - 3, X + 4, Y + 4, Z + 4))) {
			ItemStack stack = ((EntityItem) entity).getEntityItem();
			if (stack.getItem() == Items.apple) {
				apples += stack.stackSize;
				continue;
			}
			found.add(stack.stackSize + " " + Recipes26_3.nameOf(stack) + (stack.hasDisplayName() ? " named " + stack.getDisplayName() : ""));
		}
		if (apples > 0) {
			found.add(apples + " apple");
		}
		Collections.sort(found);
		return found.toString();
	}

	// ---------------------------------------------------------------- the rest

	/** The comparator by fullness: 1 for a stick, 15 full. */
	public void testComparator() {
		String got = onServer(() -> {
			IInventory barrel = place(2);
			barrel.setInventorySlotContents(0, new ItemStack(Items.stick));
			int one = world().getBlock(X, Y, Z).getComparatorInputOverride(world(), X, Y, Z, 0);
			for (int i = 0; i < 27; i++) {
				barrel.setInventorySlotContents(i, new ItemStack(Items.stick, 64));
			}
			return one + " " + world().getBlock(X, Y, Z).getComparatorInputOverride(world(), X, Y, Z, 0);
		});
		assertEqual("1 15", got, "the comparator by a stick; full");
	}

	/** A hopper above fills it, one below empties it (any face, a plain container). */
	public void testHoppers() {
		onServer(() -> {
			place(2);
			world().setBlock(X, Y + 1, Z, Blocks.hopper, 0, 3);
			((TileEntityHopper) world().getTileEntity(X, Y + 1, Z)).setInventorySlotContents(0, new ItemStack(Items.apple));
			world().setBlock(X, Y - 1, Z, Blocks.hopper, 0, 3);
		});
		serverTicks(40);
		String got = onServer(() -> {
			ItemStack below = ((TileEntityHopper) world().getTileEntity(X, Y - 1, Z)).getStackInSlot(0);
			return below == null ? "nothing" : below.stackSize + " " + Recipes26_3.nameOf(below);
		});
		assertEqual("1 apple", got, "the hopper below, from the hopper above through the barrel");
	}

	/** 26.3's crafting (`workstations_barrel_recipes_26_3.txt`). */
	public void testRecipeAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("workstations_barrel_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(1, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	/** 26.3's name (`block.minecraft.barrel`), its menu's title (`container.barrel`), 2.5, an axe's, wood's sound. */
	public void testNamesAndStrength() {
		String names = onClient(() -> new ItemStack(block("barrel")).getDisplayName() + ", " + StatCollector.translateToLocal("container.barrel"));
		String title = onServer(() -> place(2).getInventoryName());
		String strength = onServer(() -> {
			Block barrel = block("barrel");
			return barrel.getBlockHardness(world(), X, Y, Z) + " " + barrel.getHarvestTool(2) + " " + barrel.stepSound.soundName;
		});
		assertEqual("Barrel, Barrel; container.barrel; 2.5 axe wood", names + "; " + title + "; " + strength, "names; title; strength, tool, sound");
	}
}
