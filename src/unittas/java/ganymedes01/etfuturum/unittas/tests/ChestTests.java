package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.blocks.ModernChests;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;

/**
 * Chests side by side, as 26.3's (SPEC-modern-movement.md §65): a chest joins a single chest of its kind at its left or
 * right with the same facing, and nothing else; sneaking keeps it single, or sneak-clicking a chest's side joins that
 * chest; any number side by side; a broken half leaves the other single; old worlds' doubles converted when loaded.
 */
@TasSuite("chests")
public class ChestTests extends TasTestCase {

	/** Where the chests go: well away from the player, on the flat world's ground (y 4). */
	private static final int X = 6;
	private static final int Y = 4;
	private static final int Z = 8;

	@Override
	protected void setUp() {
		if (!McFlyConfig.modernChestPlacement) {
			skipTest("modern chest placement is off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			player().setSneaking(false);
			for (int dx = -1; dx <= 5; dx++) {
				for (int dz = -1; dz <= 2; dz++) {
					world().setBlockToAir(X + dx, Y, Z + dz);
					world().setBlockToAir(X + dx, Y + 1, Z + dz);
				}
			}
		});
	}

	/** Two chests side by side facing apart stay single (1.7.10 joined them). */
	public void testFacingApartStaySingle() {
		place(Blocks.chest, X, Z, 180.0F, false);
		place(Blocks.chest, X + 1, Z, 90.0F, false);
		assertEqual("single single", types(X, X + 1), "the two chests");
		assertEqual(27, size(X, Z), "the first's inventory");
		assertEqual(27, size(X + 1, Z), "the second's");
	}

	/** Four in a row, one facing: two doubles (1.7.10 wouldn't let the third be placed). */
	public void testRowOfFourTwoDoubles() {
		for (int i = 0; i < 4; i++) {
			place(Blocks.chest, X + i, Z, 180.0F, false);
		}
		assertEqual("right left right left", types(X, X + 3), "the row");
		assertEqual(54, size(X, Z), "the first double");
		assertEqual(54, size(X + 2, Z), "the second double");
		boolean apart = onServer(() -> {
			IInventory first = inventory(X, Z);
			first.setInventorySlotContents(30, new ItemStack(Blocks.stone));
			ItemStack inSecond = ((TileEntityChest) world().getTileEntity(X + 1, Y, Z)).getStackInSlot(3);
			ItemStack inThird = ((TileEntityChest) world().getTileEntity(X + 2, Y, Z)).getStackInSlot(3);
			return inSecond != null && inThird == null;
		});
		assertTrue(apart, "the first double's slot 30 isn't the second chest's slot 3 alone");
	}

	/** Sneaking, a chest stays single next to a single one. */
	public void testSneakPlacedStaysSingle() {
		place(Blocks.chest, X, Z, 180.0F, false);
		place(Blocks.chest, X + 1, Z, 180.0F, true);
		assertEqual("single single", types(X, X + 1), "a chest set down sneaking");
	}

	/** Sneak-clicking a single chest's side joins it, taking its facing whatever way the player looks. */
	public void testSneakClickJoinsTheSide() {
		place(Blocks.chest, X, Z, 180.0F, false);
		onServer(() -> {
			player().rotationYaw = 90.0F;
			player().setSneaking(true);
			new ItemStack(Blocks.chest).tryPlaceItemIntoWorld(player(), world(), X, Y, Z, 5, 1.0F, 0.5F, 0.5F);
			player().setSneaking(false);
		});
		assertEqual("right left", types(X, X + 1), "the clicked chest and the new one");
		assertEqual(onServer(() -> world().getBlockMetadata(X, Y, Z)), onServer(() -> world().getBlockMetadata(X + 1, Y, Z)), "the new one's facing");
		assertEqual(54, size(X + 1, Z), "the double");
	}

	/** A chest can go next to a double (1.7.10 refused it) and stays single. */
	public void testNextToADouble() {
		place(Blocks.chest, X, Z, 180.0F, false);
		place(Blocks.chest, X + 1, Z, 180.0F, false);
		place(Blocks.chest, X + 2, Z, 180.0F, false);
		assertEqual(Blocks.chest, onServer(() -> world().getBlock(X + 2, Y, Z)), "the chest next to the double");
		assertEqual("right left single", types(X, X + 2), "the three");
	}

	/** Breaking a half leaves the other single. */
	public void testBreakingAHalf() {
		place(Blocks.chest, X, Z, 180.0F, false);
		place(Blocks.chest, X + 1, Z, 180.0F, false);
		onServer(() -> world().setBlockToAir(X + 1, Y, Z));
		assertEqual("single", types(X, X), "the half left");
		assertEqual(27, size(X, Z), "its inventory");
	}

	/** Trapped chests join trapped ones only. */
	public void testTrappedChests() {
		place(Blocks.trapped_chest, X, Z, 180.0F, false);
		place(Blocks.trapped_chest, X + 1, Z, 180.0F, false);
		place(Blocks.chest, X + 2, Z, 180.0F, false);
		assertEqual("right left single", types(X, X + 2), "two trapped chests and a chest");
	}

	/** An old world's double chest (no type saved) is converted when it loads, staying the double it was. */
	public void testOldDoubleConverted() {
		onServer(() -> {
			world().setBlock(X, Y, Z, Blocks.chest, 3, 3);
			world().setBlock(X + 1, Y, Z, Blocks.chest, 3, 3);
			for (int i = 0; i < 2; i++) {
				TileEntityChest chest = (TileEntityChest) world().getTileEntity(X + i, Y, Z);
				NBTTagCompound saved = new NBTTagCompound();
				chest.writeToNBT(saved);
				saved.removeTag("ChestType");
				chest.readFromNBT(saved);
			}
		});
		serverTicks(3);
		assertEqual("right left", types(X, X + 1), "the old double");
		assertEqual(54, size(X, Z), "still a double");
	}

	/** The type is saved with the chest and reaches the client. */
	public void testSavedAndSent() {
		place(Blocks.chest, X, Z, 180.0F, false);
		place(Blocks.chest, X + 1, Z, 180.0F, false);
		String saved = onServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			world().getTileEntity(X + 1, Y, Z).writeToNBT(tag);
			return tag.getString("ChestType");
		});
		assertEqual("left", saved, "the type saved");
		waitUntilOnClient("the client to have the type", () -> "left".equals(ModernChests.typeOf(mc().theWorld, X + 1, Y, Z)), 40);
	}

	/** For a look: a row of four (two doubles) and, in front, two singles side by side facing apart. */
	public void testTheirLook() {
		for (int i = 0; i < 4; i++) {
			place(Blocks.chest, X + i, Z, 0.0F, false);
		}
		place(Blocks.chest, X + 1, Z - 1, 0.0F, false);
		place(Blocks.chest, X + 2, Z - 1, 90.0F, false);
		onServer(() -> {
			player().setPositionAndUpdate(X + 2.0D, Y + 2.0D, Z - 4.0D);
		});
		ticks(5);
		look(0.0F, 35.0F);
		ticks(10);
		String shot = screenshot("chests.side_by_side");
		onServer(() -> player().setPositionAndUpdate(0.5D, Y, 0.5D));
		assertTrue(shot != null, "no picture taken");
	}

	// ---------------------------------------------------------------- helpers

	/** A chest placed on the ground here as the player looking this way (yaw), sneaking or not, would place it. */
	private void place(Block chest, int x, int z, float yaw, boolean sneaking) {
		onServer(() -> {
			player().rotationYaw = yaw;
			player().setSneaking(sneaking);
			new ItemStack(chest).tryPlaceItemIntoWorld(player(), world(), x, Y - 1, z, 1, 0.5F, 1.0F, 0.5F);
			player().setSneaking(false);
		});
		serverTicks(1);
	}

	private String types(int from, int to) {
		return onServer(() -> {
			StringBuilder text = new StringBuilder();
			for (int x = from; x <= to; x++) {
				text.append(x == from ? "" : " ").append(ModernChests.typeOf(world(), x, Y, Z));
			}
			return text.toString();
		});
	}

	private IInventory inventory(int x, int z) {
		Block block = world().getBlock(x, Y, z);
		return block instanceof BlockChest chest ? chest.func_149951_m(world(), x, Y, z) : null;
	}

	private int size(int x, int z) {
		return onServer(() -> {
			IInventory inventory = inventory(x, z);
			return inventory == null ? 0 : inventory.getSizeInventory();
		});
	}
}
