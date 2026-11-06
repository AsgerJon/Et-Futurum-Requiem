package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.mappings.MultiBlockSoundContainer;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trapdoors as capable as 26.3's (SPEC-modern-movement.md §91.6, step D; the user: "make trapdoors as capable as the
 * 26.3"): the wood sets', EFR's iron one and vanilla's oak one. EFR's ids and metas, 26.3's powered state (in a block
 * entity: 1.7.10's four meta bits are all used), strength, sounds by the set type and the new state, redstone by the
 * powered state, a wind burst, 26.3's placing, climbing a ladder of the same facing; drops, fuel, ore names and names.
 * At z 620, y 8.
 */
@TasSuite("woods")
public class TrapdoorTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 620;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final String[] WOODS = {"crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 36 to 44, y 7 to 11, z 616 to 624 emptied, a stone floor under it, its items gone. */
	private void clear() {
		for (int x = X - 4; x <= X + 4; x++) {
			for (int z = Z - 4; z <= Z + 4; z++) {
				for (int y = Y; y <= Y + 3; y++) {
					world().setBlock(x, y, z, Blocks.air, 0, 2);
				}
				world().setBlock(x, Y - 1, z, Blocks.stone, 0, 2);
			}
		}
		for (EntityItem item : items()) {
			item.setDead();
		}
	}

	private List<EntityItem> items() {
		List<EntityItem> found = new ArrayList<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 6, Y + 5, Z + 6))) {
			if (!((EntityItem) entity).isDead) {
				found.add((EntityItem) entity);
			}
		}
		return found;
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (EntityItem item : items()) {
			ItemStack stack = item.getEntityItem();
			counts.merge(Recipes26_3.nameOf(stack), stack.stackSize, Integer::sum);
			item.setDead();
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(name.contains(":") ? name : McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + name);
		return (Block) block;
	}

	/** The 26.3 block it reads as: a facing but north, the top half, open. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		if (state.containsKey("facing") && !"north".equals(state.get("facing"))) {
			name.append(",facing=").append(state.get("facing"));
		}
		if ("top".equals(state.get("half"))) {
			name.append(",top");
		}
		if ("true".equals(state.get("open"))) {
			name.append(",open");
		}
		return name.toString();
	}

	private String breakSound(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		String own = block.stepSound.getBreakSound();
		Object variants = MultiBlockSoundRegistry.multiBlockSounds.get(block);
		if (variants == null) {
			return own;
		}
		String got = ((MultiBlockSoundContainer) variants).getSound(world(), x, y, z, own, MultiBlockSoundRegistry.BlockSoundType.BREAK);
		return got != null ? got : own;
	}

	private void use(int x, int y, int z) {
		player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		world().getBlock(x, y, z).onBlockActivated(world(), x, y, z, player(), 1, 0.5F, 0.5F, 0.5F);
	}

	private int meta() {
		return world().getBlockMetadata(X, Y, Z);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testTrapdoorsAreOurs() {
		List<String> wrong = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (String wood : OLD) {
			names.add("trapdoor_" + wood);
		}
		for (String wood : WOODS) {
			names.add(wood + "_trapdoor");
		}
		names.add("iron_trapdoor");
		for (String name : names) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BaseTrapdoor", "blocks.BlockIronTrapdoor", "mixins.early.laddertrapdoors.MixinBlockTrapdoor",
			"mixins.early.laddertrapdoors.MixinBlockLadder"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		assertEqual("[]", wrong.toString(), "the trapdoors' classes");
	}

	/** EFR's metas by 26.3's names: `& 3` the facing (0 north, 1 south, 2 west, 3 east), 4 open, 8 the top half. */
	public void testStoredForm() {
		Object[][] metas = {{"trapdoor_birch", 0}, {"trapdoor_birch", 5}, {"trapdoor_birch", 12}, {"iron_trapdoor", 3}, {"crimson_trapdoor", 10}};
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder();
			for (Object[] entry : metas) {
				names.append(names.length() == 0 ? "" : " ").append(name(block((String) entry[0]), (Integer) entry[1]));
			}
			return names.toString();
		});
		assertEqual("birch_trapdoor birch_trapdoor,facing=south,open birch_trapdoor,top,open iron_trapdoor,facing=east crimson_trapdoor,facing=west,top", got,
			"26.3's names");
	}

	/** 26.3's: wooden 3 and 3, an axe, no fire, the wood's sound and colour; iron's 5 and 5, a pickaxe, iron's sound (26.3's `SoundType.IRON`). */
	public void testProperties() {
		String[] newSounds = {"block.nether_wood.break", "block.nether_wood.break", "dig.wood", "block.cherry_wood.break", "block.bamboo_wood.break"};
		MapColor[] newColours = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
		MapColor[] oldColours = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
		List<Object[]> want = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			want.add(new Object[]{WOODS[i] + "_trapdoor", "3.0 3.0 0 0 axe " + newSounds[i], newColours[i]});
			want.add(new Object[]{"trapdoor_" + OLD[i], "3.0 3.0 0 0 axe dig.wood", oldColours[i]});
		}
		want.add(new Object[]{"iron_trapdoor", "5.0 5.0 0 0 pickaxe block.iron.break", MapColor.ironColor});
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			for (Object[] entry : want) {
				Block block = block((String) entry[0]);
				world().setBlock(X, Y, Z, block, 0, 2);
				String got = block.getBlockHardness(world(), X, Y, Z) + " " + block.getExplosionResistance(null) + " "
					+ block.getFireSpreadSpeed(world(), X, Y, Z, ForgeDirection.UP) + " " + block.getFlammability(world(), X, Y, Z, ForgeDirection.UP) + " "
					+ block.getHarvestTool(0) + " " + breakSound(X, Y, Z).replace(MODERN, "");
				if (!entry[1].equals(got) || block.getMapColor(0) != entry[2]) {
					found.add(entry[0] + " " + got + " colour " + block.getMapColor(0).colorIndex);
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "hardness, resistance, fire, tool, sound, colour (want: as listed)");
	}

	// ---------------------------------------------------------------- use, sounds, redstone, wind

	/** Used: a wooden one opens and closes; iron's not by hand. */
	public void testUse() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("cherry_trapdoor"), 0, 2);
			use(X, Y, Z);
			int opened = meta();
			use(X, Y, Z);
			int closed = meta();
			world().setBlock(X, Y, Z, block("iron_trapdoor"), 0, 2);
			use(X, Y, Z);
			return opened + " " + closed + " " + meta();
		});
		assertEqual("4 0 0", got, "metas: opened, closed; iron by hand");
	}

	/** The set type's sounds by the new state (EFR's at random): cherry's; the oak set's for vanilla's trapdoor. */
	public void testSounds() {
		for (String[] trapdoor : new String[][]{{"cherry_trapdoor", "block.cherry_wood_trapdoor."}, {"minecraft:trapdoor", "block.wooden_trapdoor."}}) {
			onServer(() -> {
				player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
				world().setBlock(X, Y, Z, block(trapdoor[0]), 0, 2);
			});
			serverTicks(2);
			for (String want : new String[]{"open", "close", "open", "close"}) {
				clearSounds();
				onServer(() -> use(X, Y, Z));
				waitForSound(MODERN + trapdoor[1] + want, 20);
				String other = MODERN + trapdoor[1] + (want.equals("open") ? "close" : "open");
				assertFalse(soundsHeard().contains(other), trapdoor[0] + "'s " + want + " also played " + other + ": " + soundsHeard());
			}
		}
	}

	/**
	 * 26.3's powered state: a redstone block beside opens it; the player closes it, powered still; a neighbour's update
	 * leaves it; the power gone, it stays closed. Opened by hand with no power, an unpowered lever put beside leaves it open
	 * (1.7.10 closed it). Ours, vanilla's oak trapdoor; iron's opened and closed by redstone alone.
	 */
	public void testRedstone() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (String name : new String[]{"mangrove_trapdoor", "minecraft:trapdoor"}) {
				world().setBlock(X, Y, Z, block(name), 0, 3);
				world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
				int powered = meta();
				use(X, Y, Z);
				int closedByHand = meta();
				world().setBlock(X, Y, Z - 1, Blocks.stone, 0, 3);
				int afterUpdate = meta();
				world().setBlock(X + 1, Y, Z, Blocks.air, 0, 3);
				int powerGone = meta();
				use(X, Y, Z);
				world().setBlock(X - 1, Y, Z, Blocks.lever, 1, 3);
				result.append(name).append(": ").append(powered).append(" ").append(closedByHand).append(" ").append(afterUpdate).append(" ").append(powerGone)
					.append(" ").append(meta()).append("; ");
				world().setBlock(X, Y, Z - 1, Blocks.air, 0, 3);
				world().setBlock(X - 1, Y, Z, Blocks.air, 0, 3);
			}
			world().setBlock(X, Y, Z, block("iron_trapdoor"), 0, 3);
			world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
			int powered = meta();
			world().setBlock(X + 1, Y, Z, Blocks.air, 0, 3);
			return result + "iron: " + powered + " " + meta();
		});
		assertEqual("mangrove_trapdoor: 4 0 0 0 4; minecraft:trapdoor: 4 0 0 0 4; iron: 4 0", got,
			"metas: powered, closed by hand, after an update, the power gone, opened by hand with an unpowered lever put beside");
	}

	/** The powered state is saved with it (its block entity's `powered`), and read back. */
	public void testPoweredIsSaved() {
		String got = onServer(() -> {
			world().setBlock(X, Y, Z, block("trapdoor_spruce"), 0, 3);
			world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
			TileEntity tile = world().getTileEntity(X, Y, Z);
			if (tile == null) {
				return "no block entity";
			}
			NBTTagCompound tag = new NBTTagCompound();
			tile.writeToNBT(tag);
			TileEntity read = TileEntity.createAndLoadEntity(tag);
			NBTTagCompound again = new NBTTagCompound();
			read.writeToNBT(again);
			return tag.getString("id") + " " + tag.getBoolean("powered") + " " + again.getBoolean("powered");
		});
		assertEqual("etfuturum.trapdoor true true", got, "the block entity's id and powered, saved and read back");
	}

	/** A wind burst toggles an unpowered wooden trapdoor; not a powered one, nor iron's. */
	public void testWindBurst() throws Exception {
		java.lang.reflect.Method trigger = ganymedes01.etfuturum.potion.ModernEffects.class.getDeclaredMethod("trigger", World.class, int.class, int.class, int.class);
		trigger.setAccessible(true);
		String got = onServer(() -> {
			try {
				world().setBlock(X, Y, Z, block("trapdoor_jungle"), 0, 3);
				trigger.invoke(null, world(), X, Y, Z);
				int toggled = meta();
				world().setBlock(X + 1, Y, Z, Blocks.redstone_block, 0, 3);
				int powered = meta();
				trigger.invoke(null, world(), X, Y, Z);
				int burst = meta();
				world().setBlock(X + 1, Y, Z, Blocks.air, 0, 3);
				world().setBlock(X, Y, Z, block("iron_trapdoor"), 0, 3);
				trigger.invoke(null, world(), X, Y, Z);
				return toggled + " " + powered + " " + burst + " " + meta();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		assertEqual("4 4 4 0", got, "metas: toggled open; powered; a burst on the powered one; a burst on iron's");
	}

	/**
	 * 26.3's placing: on a block's side, facing that side, the half by where the click is; on the floor's top the bottom
	 * half, on a block's underside the top half, both facing away from the player (it faces north: south).
	 */
	public void testPlacing() {
		String got = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z + 2.5D, 180.0F, 0.0F);
			player().rotationYaw = 180.0F;
			StringBuilder result = new StringBuilder();
			Object[][] clicks = {{X - 1, Y, Z, 5, 0.75F}, {X - 1, Y, Z, 5, 0.25F}, {X, Y - 1, Z, 1, 0.5F}, {X, Y + 1, Z, 0, 0.5F}};
			for (Object[] click : clicks) {
				int cx = (Integer) click[0], cy = (Integer) click[1], cz = (Integer) click[2];
				world().setBlock(cx, cy, cz, Blocks.stone, 0, 2);
				ItemStack trapdoor = new ItemStack(block("cherry_trapdoor"));
				player().inventory.setInventorySlotContents(player().inventory.currentItem, trapdoor);
				player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), trapdoor, cx, cy, cz, (Integer) click[3], 1.0F, (Float) click[4], 0.5F);
				result.append(result.length() == 0 ? "" : " ").append(name(world().getBlock(X, Y, Z), world().getBlockMetadata(X, Y, Z)));
				world().setBlock(X, Y, Z, Blocks.air, 0, 2);
				if (cy != Y - 1) {
					world().setBlock(cx, cy, cz, Blocks.air, 0, 2);
				}
			}
			return result.toString();
		});
		assertEqual("cherry_trapdoor,facing=east,top cherry_trapdoor,facing=east cherry_trapdoor,facing=south cherry_trapdoor,facing=south,top", got,
			"placed: on a side high, low; on the floor; under a block");
	}

	/**
	 * 26.3's `trapdoorUsableAsLadder`: an open trapdoor over a ladder of the same facing climbs; another facing or closed
	 * doesn't; vanilla's trapdoor the same. The ladder 3/16 thick.
	 */
	public void testLadder() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			world().setBlock(X, Y, Z + 1, Blocks.stone, 0, 2);
			world().setBlock(X, Y + 1, Z + 1, Blocks.stone, 0, 2);
			world().setBlock(X, Y, Z, Blocks.ladder, 2, 2);
			for (String name : new String[]{"cherry_trapdoor", "minecraft:trapdoor"}) {
				for (int meta : new int[]{4, 5, 0}) {
					world().setBlock(X, Y + 1, Z, block(name), meta, 2);
					result.append(block(name).isLadder(world(), X, Y + 1, Z, player()) ? "1" : "0");
				}
				result.append(" ");
			}
			Blocks.ladder.setBlockBoundsBasedOnState(world(), X, Y, Z);
			return result + "" + (1.0D - Blocks.ladder.getBlockBoundsMinZ());
		});
		assertEqual("100 100 0.1875", got, "climbable: open same facing, open other facing, closed (ours, vanilla's); the ladder's depth");
	}

	// ---------------------------------------------------------------- drops, fuel, ore names, names

	public void testDrops() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Object[] entry : new Object[][]{{"trapdoor_acacia", 12, Items.iron_axe}, {"bamboo_trapdoor", 3, Items.iron_axe}, {"iron_trapdoor", 0, Items.iron_pickaxe},
				{"iron_trapdoor", 0, null}}) {
				world().setBlock(X, Y, Z, block((String) entry[0]), (Integer) entry[1], 2);
				player().inventory.setInventorySlotContents(player().inventory.currentItem, entry[2] == null ? null : new ItemStack((Item) entry[2]));
				player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
				result.append(result.length() == 0 ? "" : " ").append(dropped());
			}
			return result.toString();
		});
		assertEqual("{acacia_trapdoor=1} {bamboo_trapdoor=1} {iron_trapdoor=1} {}", got, "drops: wood by an axe, iron by a pickaxe, iron by hand");
	}

	/** 26.3's fuel: 300, crimson's and warped's none, iron's none. */
	public void testFuel() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"crimson_trapdoor", "cherry_trapdoor", "trapdoor_spruce", "iron_trapdoor"}) {
			got.append(got.length() == 0 ? "" : " ").append(TileEntityFurnace.getItemBurnTime(new ItemStack(block(name))));
		}
		assertEqual("0 300 300 0", got.toString(), "burn times");
	}

	public void testOreNames() {
		List<String> missing = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			for (String name : new String[]{WOODS[i] + "_trapdoor", "trapdoor_" + OLD[i]}) {
				boolean has = false;
				for (int id : OreDictionary.getOreIDs(new ItemStack(block(name)))) {
					has |= "trapdoorWood".equals(OreDictionary.getOreName(id));
				}
				if (!has) {
					missing.add(name);
				}
			}
		}
		assertEqual("[]", missing.toString(), "trapdoors without trapdoorWood");
	}

	public void testNames() {
		String got = onClient(() -> {
			StringBuilder names = new StringBuilder();
			for (String name : new String[]{"trapdoor_spruce", "trapdoor_dark_oak", "warped_trapdoor", "iron_trapdoor"}) {
				names.append(names.length() == 0 ? "" : ", ").append(new ItemStack(block(name)).getDisplayName());
			}
			return names.toString();
		});
		assertEqual("Spruce Trapdoor, Dark Oak Trapdoor, Warped Trapdoor, Iron Trapdoor", got, "names");
	}
}
