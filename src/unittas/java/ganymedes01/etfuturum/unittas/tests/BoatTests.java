package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The wood sets' boats and rafts (SPEC-modern-movement.md 91.12, step J), as 26.3's `AbstractBoat` on EFR's entity ids
 * and NBT (`etfuturum.new_boat`, `etfuturum.chest_boat`; `Type`, `Domain`, `Items`, a name kept): floating, hits, no
 * breaking into planks after a fall, snow kept and lily pads broken, mobs getting in, bubble columns, the item and
 * dispensers, fuel, stacks, recipes, names. At z 760, y 8, over a 5 by 5 pool of water 2 deep (its surface at y 8).
 * Read through entity ids, NBT and names, so it runs on EFR's boats (and fails there).
 */
@TasSuite("woods")
public class BoatTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 760;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z - 5.5D, 0.0F, 0.0F);
		});
		serverTicks(2);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 33 to 47, y 8 to 24, z 753 to 767 emptied over stone; a pool at x 38 to 42, z 758 to 762, y 6 and 7. */
	private void clear() {
		for (Entity entity : near(Entity.class)) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				entity.setDead();
			}
		}
		for (int x = X - 7; x <= X + 7; x++) {
			for (int z = Z - 7; z <= Z + 7; z++) {
				for (int y = Y; y <= Y + 16; y++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
				boolean pool = Math.abs(x - X) <= 2 && Math.abs(z - Z) <= 2;
				world().setBlock(x, Y - 1, z, pool ? Blocks.water : Blocks.stone, 0, 2);
				world().setBlock(x, Y - 2, z, pool ? Blocks.water : Blocks.stone, 0, 2);
				world().setBlock(x, Y - 3, z, Blocks.stone, 0, 2);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Entity> List<E> near(Class<E> type) {
		List<E> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(type, AxisAlignedBB.getBoundingBox(X - 8, Y - 4, Z - 8, X + 9, Y + 20, Z + 9))) {
			if (!((Entity) entity).isDead) {
				found.add((E) entity);
			}
		}
		return found;
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (EntityItem item : near(EntityItem.class)) {
			ItemStack stack = item.getEntityItem();
			String name = Recipes26_3.nameOf(stack) + (stack.hasDisplayName() ? "(" + stack.getDisplayName() + ")" : "");
			counts.merge(name, stack.stackSize, Integer::sum);
			item.setDead();
		}
		return counts.toString();
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(item instanceof Item, "no item " + name);
		return (Item) item;
	}

	/** A boat as EFR wrote one: `id`, `Pos`, `Motion`, `Rotation`, `Type`, `Domain`, the name if given. */
	private static NBTTagCompound boatTag(String id, String wood, double x, double y, double z, String name) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("id", id);
		NBTTagList pos = new NBTTagList();
		pos.appendTag(new NBTTagDouble(x));
		pos.appendTag(new NBTTagDouble(y));
		pos.appendTag(new NBTTagDouble(z));
		tag.setTag("Pos", pos);
		NBTTagList motion = new NBTTagList();
		for (int i = 0; i < 3; i++) {
			motion.appendTag(new NBTTagDouble(0.0D));
		}
		tag.setTag("Motion", motion);
		NBTTagList rotation = new NBTTagList();
		rotation.appendTag(new NBTTagFloat(0.0F));
		rotation.appendTag(new NBTTagFloat(0.0F));
		tag.setTag("Rotation", rotation);
		tag.setString("Type", wood);
		tag.setString("Domain", "minecraft");
		if (name != null) {
			tag.setString("CustomName", name);
		}
		return tag;
	}

	/** Spawns it from the tag (as a chunk loads it); null when the id is unknown. */
	private static Entity spawn(NBTTagCompound tag) {
		Entity entity = EntityList.createEntityFromNBT(tag, world());
		if (entity != null) {
			world().spawnEntityInWorld(entity);
		}
		return entity;
	}

	private Entity boat(String wood, double x, double y, double z) {
		return onServer(() -> spawn(boatTag("etfuturum.new_boat", wood, x, y, z, null)));
	}

	// ---------------------------------------------------------------- ours, stored form

	public void testBoatsAreOurs() {
		List<String> wrong = new ArrayList<>();
		String got = onServer(() -> {
			StringBuilder classes = new StringBuilder();
			for (String id : new String[]{"etfuturum.new_boat", "etfuturum.chest_boat"}) {
				Entity entity = EntityList.createEntityFromNBT(boatTag(id, "oak", X + 0.5D, Y + 5, Z + 0.5D, null), world());
				classes.append(entity == null ? id + " unknown; " : entity.getClass().getName().startsWith("io.github.asgerjon.mcfly.") ? "" : entity.getClass().getName() + "; ");
			}
			return classes.toString();
		});
		if (!got.isEmpty()) {
			wrong.add(got);
		}
		for (String name : new String[]{"entities.EntityNewBoat", "entities.EntityNewBoatWithChest", "entities.EntityNewBoatSeat", "items.ItemNewBoat",
			"client.renderer.entity.NewBoatRenderer", "client.renderer.entity.ChestBoatRenderer", "client.model.ModelNewBoat", "network.BoatMoveMessage",
			"network.ChestBoatOpenInventoryMessage"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the boats' classes");
	}

	/** EFR's NBT read back: `Type`, `Domain`; a name kept through a save and a load (EFR read it as a list: lost). */
	public void testStoredForm() {
		String got = onServer(() -> {
			Entity boat = spawn(boatTag("etfuturum.new_boat", "spruce", X + 0.5D, Y + 5, Z + 0.5D, "Boaty"));
			if (boat == null) {
				return "unknown id";
			}
			NBTTagCompound saved = new NBTTagCompound();
			boat.writeToNBTOptional(saved);
			boat.setDead();
			Entity again = EntityList.createEntityFromNBT(saved, world());
			NBTTagCompound twice = new NBTTagCompound();
			if (again != null) {
				again.writeToNBTOptional(twice);
			}
			return saved.getString("Type") + " " + saved.getString("Domain") + " " + twice.getString("Type") + " name " + twice.getString("CustomName");
		});
		assertEqual("spruce minecraft spruce name Boaty", got, "Type, Domain; after a save and a load: Type, name");
	}

	/** A chest boat's `Items` read back. */
	public void testChestBoatItems() {
		String got = onServer(() -> {
			NBTTagCompound tag = boatTag("etfuturum.chest_boat", "birch", X + 0.5D, Y + 5, Z + 0.5D, null);
			NBTTagList items = new NBTTagList();
			NBTTagCompound entry = new NBTTagCompound();
			new ItemStack(Items.diamond, 5).writeToNBT(entry);
			entry.setByte("Slot", (byte) 26);
			items.appendTag(entry);
			tag.setTag("Items", items);
			Entity boat = spawn(tag);
			if (boat == null) {
				return "unknown id";
			}
			NBTTagCompound saved = new NBTTagCompound();
			boat.writeToNBTOptional(saved);
			NBTTagCompound first = saved.getTagList("Items", 10).getCompoundTagAt(0);
			ItemStack stack = ItemStack.loadItemStackFromNBT(first);
			return (first.getByte("Slot") & 255) + " " + (stack == null ? "none" : stack.stackSize + " " + Recipes26_3.nameOf(stack));
		});
		assertEqual("26 5 diamond", got, "the last slot's diamonds");
	}

	// ---------------------------------------------------------------- in the world

	/** Dropped into the pool, it settles at the surface (y 8, the water's top a little under). */
	public void testFloats() {
		Entity boat = boat("oak", X + 0.5D, Y + 3, Z + 0.5D);
		assertIsNotNone(boat, "no boat");
		serverTicks(120);
		double[] got = onServer(() -> new double[]{boat.posY, boat.motionY});
		assertTrue(got[0] > Y - 0.7D && got[0] < Y + 0.1D && Math.abs(got[1]) < 0.05D, "the boat's y " + got[0] + ", motion " + got[1]);
	}

	/** Hits: five of 1 by a player break it (damage past 40; it drops its item with its name); in creative one. */
	public void testHits() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			Entity boat = spawn(boatTag("etfuturum.new_boat", "jungle", X + 3.5D, Y, Z + 0.5D, "Mine"));
			for (int i = 0; i < 4; i++) {
				boat.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			}
			result.append(boat.isDead ? "broken after 4" : "whole after 4");
			boat.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			result.append(boat.isDead ? ", broken after 5 " : ", whole after 5 ").append(dropped());
			player().setGameType(WorldSettings.GameType.CREATIVE);
			Entity other = spawn(boatTag("etfuturum.new_boat", "jungle", X + 3.5D, Y, Z + 0.5D, null));
			other.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F);
			result.append(other.isDead ? "; creative: gone " : "; creative: whole ").append(dropped());
			return result.toString();
		});
		assertEqual("whole after 4, broken after 5 {jungle_boat(Mine)=1}; creative: gone {}", got, "by a survival player, then a creative one");
	}

	/** A fall onto stone leaves it whole (EFR's 1.12 rule broke it into planks and sticks). */
	public void testFallKeepsIt() {
		Entity boat = boat("acacia", X + 4.5D, Y + 12, Z + 0.5D);
		serverTicks(60);
		String got = onServer(() -> (boat.isDead ? "broken " : "whole ") + dropped());
		assertEqual("whole {}", got, "after falling 12 blocks onto stone");
	}

	/** Snow beside it stays (EFR broke snow layers within 0.2); a lily pad it touches breaks (26.3's `LilyPadBlock`). */
	public void testSnowAndLilyPads() {
		onServer(() -> {
			world().setBlock(X + 5, Y, Z, Blocks.snow_layer, 0, 3);
			world().setBlock(X, Y, Z, Blocks.waterlily, 0, 3);
		});
		// its box (half 0.6875 wide) reaches x 45.09: within EFR's 0.2 of the snow at x 45
		boat("oak", X + 4.4D, Y, Z + 0.5D);
		boat("oak", X + 0.5D, Y - 0.2D, Z + 0.5D);
		serverTicks(20);
		String got = onServer(() -> (world().getBlock(X + 5, Y, Z) == Blocks.snow_layer ? "snow kept" : "snow gone") + " "
			+ (world().getBlock(X, Y, Z) == Blocks.waterlily ? "lily pad kept" : "lily pad gone"));
		assertEqual("snow kept lily pad gone", got, "a boat beside snow; one on a lily pad");
	}

	/** A chicken pushed against an empty boat gets in (26.3's `tick`: a living thing that fits, no player steering). */
	public void testMobGetsIn() {
		Entity boat = boat("birch", X + 5.0D, Y, Z + 0.5D);
		EntityChicken chicken = onServer(() -> {
			EntityChicken mob = new EntityChicken(world());
			mob.setLocationAndAngles(X + 5.0D, Y, Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			return mob;
		});
		serverTicks(20);
		assertTrue(onServer(() -> chicken.ridingEntity != null), "the chicken got in");
	}

	/** Over an upward bubble column (soul sand under the water), after 60 ticks it's thrown up out of the water. */
	public void testBubbleColumnLifts() {
		onServer(() -> {
			world().setBlock(X + 2, Y - 3, Z + 2, Blocks.soul_sand, 0, 3);
			world().setBlock(X + 2, Y - 2, Z + 2, Blocks.water, 0, 3);
			world().setBlock(X + 2, Y - 1, Z + 2, Blocks.water, 0, 3);
		});
		serverTicks(20);
		Entity boat = boat("oak", X + 2.5D, Y - 0.3D, Z + 2.5D);
		double highest = Y - 1;
		for (int i = 0; i < 12; i++) {
			serverTicks(10);
			highest = Math.max(highest, onServer(() -> boat.posY));
		}
		assertGreater(highest, Y + 0.5D, "the boat's highest y over the column in 120 ticks");
	}

	/** A dispenser facing water puts a boat in it; facing land with no water under, it drops the item. */
	public void testDispenser() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			// facing south (meta 3) into the pool's north edge
			int[][] spots = {{X, Z - 3}, {X + 5, Z - 3}};
			for (int[] spot : spots) {
				world().setBlock(spot[0], Y - 1, spot[1], Blocks.dispenser, 3, 3);
				TileEntityDispenser dispenser = (TileEntityDispenser) world().getTileEntity(spot[0], Y - 1, spot[1]);
				dispenser.setInventorySlotContents(0, new ItemStack(item("spruce_boat")));
				Blocks.dispenser.updateTick(world(), spot[0], Y - 1, spot[1], new Random());
				int boats = 0;
				for (Entity entity : near(Entity.class)) {
					String id = EntityList.getEntityString(entity);
					if (id != null && id.contains("boat") && entity.getDistanceSq(spot[0] + 0.5D, Y - 1, spot[1] + 1.5D) < 4.0D) {
						boats++;
						entity.setDead();
					}
				}
				result.append(result.length() == 0 ? "" : "; ").append(boats).append(" boat ").append(dropped());
				world().setBlock(spot[0], Y - 1, spot[1], Blocks.stone, 0, 3);
			}
			return result.toString();
		});
		assertEqual("1 boat {}; 0 boat {spruce_boat=1}", got, "facing the pool; facing land");
	}

	// ---------------------------------------------------------------- items

	/** 26.3's: fuel 1200, stacks of 1. */
	public void testFuelAndStacks() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"spruce_boat", "cherry_chest_boat", "bamboo_raft"}) {
			ItemStack stack = new ItemStack(item(name));
			got.append(got.length() == 0 ? "" : ", ").append(name).append(" ").append(stack.getMaxStackSize()).append(" ").append(TileEntityFurnace.getItemBurnTime(stack));
		}
		assertEqual("spruce_boat 1 1200, cherry_chest_boat 1 1200, bamboo_raft 1 1200", got.toString(), "stack size, burn time");
	}

	/** 26.3's (its jar's, `woods_boats_recipes_26_3.txt`): boats and rafts of 5 planks, chest boats of a boat and a chest. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("woods_boats_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(18, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String name : new String[]{"spruce_boat", "mangrove_oak_boat", "cherry_chest_boat", "bamboo_raft", "bamboo_chest_raft"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(item(name)).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Spruce Boat, Mangrove Boat, Cherry Boat with Chest, Bamboo Raft, Bamboo Raft with Chest", got, "names");
	}
}
