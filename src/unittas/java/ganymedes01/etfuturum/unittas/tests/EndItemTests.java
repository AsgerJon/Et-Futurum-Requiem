package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * The end crystal item and dragon's breath as our own (SPEC-modern-movement.md §85), as 26.3 has them, under EFR's ids.
 * On the flat world at x 500, z 500.
 */
@TasSuite("enditems")
public class EndItemTests extends TasTestCase {

	private static final int GROUND = 4;
	private static final int X = 500;
	private static final int Z = 500;
	private static final String[] EFR_CLASSES = {"ganymedes01.etfuturum.items.ItemEndCrystal", "ganymedes01.etfuturum.entities.EntityPlacedEndCrystal",
		"ganymedes01.etfuturum.client.renderer.entity.PlacedEndCrystalRenderer"};

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (int x = X - 3; x <= X + 6; x++) {
				for (int z = Z - 3; z <= Z + 3; z++) {
					for (int y = GROUND + 4; y >= GROUND; y--) {
						world().setBlockToAir(x, y, z);
					}
				}
			}
			for (Object entity : world().getEntitiesWithinAABB(net.minecraft.entity.Entity.class, AxisAlignedBB.getBoundingBox(X - 8, GROUND - 2, Z - 8, X + 10, GROUND + 10, Z + 8))) {
				if (!(entity instanceof EntityPlayer)) {
					((net.minecraft.entity.Entity) entity).setDead();
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.clearInventory(null, -1);
		});
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject("etfuturum:" + name);
		assertTrue(item != null, "no item etfuturum:" + name);
		return (Item) item;
	}

	/** The crystal item and dragon's breath McFly's under EFR's ids; EFR's old placed crystal's id read as McFly's. */
	public void testItemsAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (Object thing : new Object[]{Item.itemRegistry.getObject("etfuturum:end_crystal"), Item.itemRegistry.getObject("etfuturum:dragon_breath"),
			net.minecraft.entity.EntityList.stringToClassMapping.get("etfuturum.end_crystal")}) {
			String type = thing == null ? "nothing" : thing instanceof Class ? ((Class<?>) thing).getName() : thing.getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(type);
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the End's items' classes");
	}

	/**
	 * 26.3's `EndCrystalItem`: a crystal stands on obsidian or bedrock with the block above empty and no entity in the 1
	 * by 2 above it (its base hidden); on stone nothing; used up in survival.
	 */
	public void testCrystalPlacement() {
		Item crystal = item("end_crystal");
		String found = onServer(() -> {
			world().setBlock(X, GROUND, Z, Blocks.obsidian);
			world().setBlock(X + 3, GROUND, Z, Blocks.bedrock);
			world().setBlock(X + 6, GROUND, Z, Blocks.stone);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			ItemStack stack = new ItemStack(crystal, 3);
			List<String> results = new ArrayList<>();
			for (int dx : new int[]{0, 3, 6}) {
				results.add(String.valueOf(stack.getItem().onItemUse(stack, player(), world(), X + dx, GROUND, Z, 1, 0.5F, 1.0F, 0.5F)));
			}
			int standing = 0;
			for (Object entity : world().getEntitiesWithinAABB(EntityEnderCrystal.class, AxisAlignedBB.getBoundingBox(X - 1, GROUND, Z - 1, X + 8, GROUND + 4, Z + 2))) {
				standing++;
			}
			return results + " " + standing + " crystals, " + stack.stackSize + " left";
		});
		assertEqual("[true, true, false] 2 crystals, 1 left", found, "on obsidian, bedrock, stone");
	}

	/**
	 * Dragon's breath as 26.3's: a glass bottle used within 2 of a cloud the dragon made takes it (the cloud shrinking 0.5,
	 * `item.bottle.fill_dragonbreath`); near a cloud of another's, nothing.
	 */
	public void testBottlingDragonsBreath() {
		Item breath = item("dragon_breath");
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(X + 0.5, GROUND, Z + 0.5, 0.0F, 0.0F));
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - (X + 0.5)) < 0.01, 60);
		clearSounds();
		String found = onServer(() -> {
			EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(world(), X + 1.5, GROUND, Z + 0.5);
			cloud.setRadius(3.0F);
			cloud.setDuration(600);
			cloud.setOwner(new EntityDragon(world()));
			world().spawnEntityInWorld(cloud);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			ItemStack bottles = new ItemStack(Items.glass_bottle, 2);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, bottles);
			ItemStack after = bottles.useItemRightClick(world(), player());
			boolean got = false;
			for (ItemStack stack : player().inventory.mainInventory) {
				got |= stack != null && stack.getItem() == breath;
			}
			got |= after != null && after.getItem() == breath;
			String result = got + " " + cloud.getRadius();
			cloud.setDead();
			return result;
		});
		assertEqual("true 2.5", found, "dragon's breath taken, the cloud's radius after");
		waitForSound(McFly.MC_ASSETS + ":item.bottle.fill_dragonbreath", 20);
		String other = onServer(() -> {
			EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(world(), X + 1.5, GROUND, Z + 0.5);
			cloud.setRadius(3.0F);
			cloud.setDuration(600);
			world().spawnEntityInWorld(cloud);
			ItemStack bottles = new ItemStack(Items.glass_bottle, 1);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, bottles);
			ItemStack after = bottles.useItemRightClick(world(), player());
			String result = (after == null ? "nothing" : Item.itemRegistry.getNameForObject(after.getItem())) + " " + cloud.getRadius();
			cloud.setDead();
			return result;
		});
		assertEqual("minecraft:glass_bottle 3.0", other, "near a cloud no dragon made");
	}

	/** 26.3 has no recipe for dragon's breath (EFR's: an awkward potion and two chorus fruit). */
	public void testNoCraftingRecipe() {
		Item fruit = item("chorus_fruit");
		String found = onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			grid.setInventorySlotContents(0, new ItemStack(Items.potionitem, 1, 8195));
			grid.setInventorySlotContents(1, new ItemStack(fruit));
			grid.setInventorySlotContents(2, new ItemStack(fruit));
			ItemStack result = CraftingManager.getInstance().findMatchingRecipe(grid, world());
			return result == null ? "nothing" : Item.itemRegistry.getNameForObject(result.getItem());
		});
		assertEqual("nothing", found, "EFR's dragon's breath recipe");
	}
}
