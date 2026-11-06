package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import io.github.asgerjon.mcfly.boat.BoatTypes;
import io.github.asgerjon.mcfly.boat.McFlyChestBoat;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartFurnace;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Vehicles drop as one (SPEC-modern-movement.md §59), as since 1.19: a chest boat gives its chest boat item and spills
 * what its chest held (EFR's option for it, `dropVehiclesTogether`, was never read, so it gave a boat and a chest);
 * minecarts with a chest, furnace, hopper or TNT give their own item. Spruce boats, as the oak boat's item changes
 * with `replaceOldBoats`.
 */
@TasSuite("quick")
public class VehicleTests extends TasTestCase {

	/** In survival: a creative player's hit removes a boat or a minecart with no drops. */
	@Override
	protected void setUp() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		ticks(2);
	}

	public void testChestBoatDropsAsOne() {
		List<Item> drops = breakChestBoat();
		assertEqual(1, count(drops, McFlyItems.SPRUCE_CHEST_BOAT.get()), "spruce chest boats among " + drops);
		assertEqual(0, count(drops, McFlyItems.SPRUCE_BOAT.get()), "spruce boats among " + drops);
		assertEqual(0, count(drops, Item.getItemFromBlock(Blocks.chest)), "chests among " + drops);
		assertEqual(1, count(drops, Items.diamond), "diamonds among " + drops);
	}

	/** With the option off, EFR's old way: a boat and a chest apart, and what the chest held. */
	public void testWithTheOptionOffABoatAndAChest() {
		boolean before = McFlyConfig.dropVehiclesTogether;
		McFlyConfig.dropVehiclesTogether = false;
		try {
			List<Item> drops = breakChestBoat();
			assertEqual(0, count(drops, McFlyItems.SPRUCE_CHEST_BOAT.get()), "spruce chest boats among " + drops);
			assertEqual(1, count(drops, McFlyItems.SPRUCE_BOAT.get()), "spruce boats among " + drops);
			assertEqual(1, count(drops, Item.getItemFromBlock(Blocks.chest)), "chests among " + drops);
			assertEqual(1, count(drops, Items.diamond), "diamonds among " + drops);
		} finally {
			McFlyConfig.dropVehiclesTogether = before;
		}
	}

	// ---------------------------------------------------------------- minecarts

	public void testChestMinecartDropsAsOne() {
		List<Item> drops = breakCart(world -> new EntityMinecartChest(world, 0.5D, 4.0D, 3.5D), PUNCH);
		assertEqual(1, count(drops, Items.chest_minecart), "chest minecarts among " + drops);
		assertEqual(0, count(drops, Items.minecart), "minecarts among " + drops);
		assertEqual(0, count(drops, Item.getItemFromBlock(Blocks.chest)), "chests among " + drops);
		assertEqual(1, count(drops, Items.diamond), "diamonds among " + drops);
	}

	public void testHopperMinecartDropsAsOne() {
		List<Item> drops = breakCart(world -> new EntityMinecartHopper(world, 0.5D, 4.0D, 3.5D), PUNCH);
		assertEqual(1, count(drops, Items.hopper_minecart), "hopper minecarts among " + drops);
		assertEqual(0, count(drops, Items.minecart), "minecarts among " + drops);
		assertEqual(0, count(drops, Item.getItemFromBlock(Blocks.hopper)), "hoppers among " + drops);
		assertEqual(1, count(drops, Items.diamond), "diamonds among " + drops);
	}

	public void testFurnaceMinecartDropsAsOne() {
		List<Item> drops = breakCart(world -> new EntityMinecartFurnace(world, 0.5D, 4.0D, 3.5D), PUNCH);
		assertEqual(1, count(drops, Items.furnace_minecart), "furnace minecarts among " + drops);
		assertEqual(0, count(drops, Items.minecart), "minecarts among " + drops);
		assertEqual(0, count(drops, Item.getItemFromBlock(Blocks.furnace)), "furnaces among " + drops);
	}

	/** Standing still and punched: its own item, and it doesn't go off. */
	public void testStillTntMinecartDropsAsOne() {
		List<Item> drops = breakCart(world -> new EntityMinecartTNT(world, 0.5D, 4.0D, 3.5D), PUNCH);
		assertEqual(1, count(drops, Items.tnt_minecart), "TNT minecarts among " + drops);
		assertEqual(0, count(drops, Items.minecart), "minecarts among " + drops);
		assertEqual(0, count(drops, Item.getItemFromBlock(Blocks.tnt)), "TNT among " + drops);
	}

	/**
	 * Broken by fire it goes off and drops nothing (1.7.10 still dropped its minecart); with modernTntMinecarts after its
	 * short fuse (§61.1). Far out: it blows a crater.
	 */
	public void testTntMinecartThatGoesOffDropsNothing() {
		List<Item> drops = breakCart(world -> new EntityMinecartTNT(world, 40.5D, 4.0D, 40.5D), world -> DamageSource.inFire);
		assertEqual(0, count(drops, Items.tnt_minecart), "TNT minecarts among " + drops);
		assertEqual(0, count(drops, Items.minecart), "minecarts among " + drops);
		assertEqual(0, count(drops, Item.getItemFromBlock(Blocks.tnt)), "TNT among " + drops);
	}

	/** With the option off, 1.7.10's way: a minecart and a chest apart, and what the chest held. */
	public void testWithTheOptionOffAMinecartAndAChest() {
		boolean before = McFlyConfig.dropVehiclesTogether;
		McFlyConfig.dropVehiclesTogether = false;
		try {
			List<Item> drops = breakCart(world -> new EntityMinecartChest(world, 0.5D, 4.0D, 3.5D), PUNCH);
			assertEqual(0, count(drops, Items.chest_minecart), "chest minecarts among " + drops);
			assertEqual(1, count(drops, Items.minecart), "minecarts among " + drops);
			assertEqual(1, count(drops, Item.getItemFromBlock(Blocks.chest)), "chests among " + drops);
			assertEqual(1, count(drops, Items.diamond), "diamonds among " + drops);
		} finally {
			McFlyConfig.dropVehiclesTogether = before;
		}
	}

	/** One hit of the survival player: 5 damage counts 50, past a vehicle's 40. On the server. */
	private static final Function<World, DamageSource> PUNCH = world -> DamageSource.causePlayerDamage(player());

	/**
	 * The minecart made where it stands (a diamond in its first slot if it holds things), broken by one hit of this
	 * source; every item that then lies within 3 blocks of it, one per count.
	 */
	private List<Item> breakCart(Function<World, EntityMinecart> make, Function<World, DamageSource> source) {
		EntityMinecart cart = onServer(() -> {
			EntityMinecart made = make.apply(world());
			if (made instanceof IInventory inventory) {
				inventory.setInventorySlotContents(0, new ItemStack(Items.diamond));
			}
			world().spawnEntityInWorld(made);
			return made;
		});
		ticks(2);
		double x = cart.posX, z = cart.posZ;
		onServer(() -> cart.attackEntityFrom(source.apply(world()), 5.0F));
		// a lit TNT cart goes off up to 38 ticks later
		waitUntil("the minecart broken", () -> cart.isDead, 45);
		ticks(3);
		return onServer(() -> dropsNear(x, z));
	}

	/**
	 * A spruce chest boat with a diamond in its chest, three ahead on the ground, broken by one hit of the survival
	 * player (5 damage counts 50, past the boat's 40); every item that then lies within 3 blocks, one per count.
	 */
	private List<Item> breakChestBoat() {
		if (!McFlyItems.SPRUCE_CHEST_BOAT.isEnabled()) {
			skipTest("the new boats are turned off (enableNewBoats)");
		}
		McFlyChestBoat boat = onServer(() -> {
			// made after any change to the option: the boat picks its drop when its wood is set
			McFlyChestBoat made = new McFlyChestBoat(world());
			made.setType(BoatTypes.SPRUCE);
			made.setPosition(0.5D, 4.0D, 3.5D);
			made.setInventorySlotContents(0, new ItemStack(Items.diamond));
			world().spawnEntityInWorld(made);
			return made;
		});
		ticks(2);
		onServer(() -> boat.attackEntityFrom(DamageSource.causePlayerDamage(player()), 5.0F));
		waitUntil("the boat broken", () -> boat.isDead, 10);
		ticks(3);
		return onServer(() -> dropsNear(0.5D, 3.5D));
	}

	/** Every dropped item within 3 blocks of x z, one per count; on the server. */
	private static List<Item> dropsNear(double x, double z) {
		List<Item> found = new ArrayList<>();
		for (Object loaded : world().loadedEntityList) {
			if (loaded instanceof EntityItem item && !item.isDead
				&& (item.posX - x) * (item.posX - x) + (item.posZ - z) * (item.posZ - z) <= 9.0D) {
				for (int i = 0; i < item.getEntityItem().stackSize; i++) {
					found.add(item.getEntityItem().getItem());
				}
			}
		}
		return found;
	}

	private static int count(List<Item> items, Item kind) {
		int count = 0;
		for (Item item : items) {
			if (item == kind) {
				count++;
			}
		}
		return count;
	}
}
