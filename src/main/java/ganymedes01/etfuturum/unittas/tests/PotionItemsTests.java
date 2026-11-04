package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.command.modern.arguments.EffectIds;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.entities.EntityLingeringEffect;
import ganymedes01.etfuturum.entities.EntityTippedArrow;
import ganymedes01.etfuturum.items.ItemArrowTipped;
import ganymedes01.etfuturum.items.ItemLingeringPotion;
import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.potion.ThrownPotions;
import ganymedes01.etfuturum.recipes.crafting.RecipeTippedArrow;
import ganymedes01.etfuturum.tileentities.TileEntityCauldronPotion;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Potions by name, stage C (SPEC-modern-movement.md §69.3, §69.7): the lingering potion, its cloud, tipped arrows
 * (their recipe, their hit) and the potion cauldron carry the potion's name, with 26.3's names, colours, creative
 * lists and duration scales (lingering 1/4, arrows 1/8); a thrown potion kept by name shows its colour in flight and
 * in its splash.
 */
public class PotionItemsTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!McFlyConfig.potionsByName) {
			skipTest("potions by name are off");
		}
	}

	// ---------------------------------------------------------------- the lingering potion

	/** A lingering potion by name: 26.3's name, its effects at 1/4 (26.3's `potion_duration_scale`), its colour. */
	public void testLingeringPotion() {
		lingeringOn();
		ItemStack slowFalling = lingering("slow_falling");
		assertEqual("Lingering Potion of Slow Falling", slowFalling.getDisplayName());
		assertEqual("slow_falling/450/0", effects(lingeringItem().getEffects(slowFalling)), "its effects");
		ItemStack swiftness = lingering("long_swiftness");
		assertEqual("Lingering Potion of Swiftness", swiftness.getDisplayName());
		assertEqual("speed/2400/0", effects(lingeringItem().getEffects(swiftness)), "long swiftness's effects");
		assertEqual("Lingering Water Bottle", lingering("water").getDisplayName());
		assertEqual(0xF3CFB9, onClient(() -> slowFalling.getItem().getColorFromItemStack(slowFalling, 0) & 0xFFFFFF), "its colour");
	}

	/** The lingering potions' creative list: every offered potion in 26.3's order. */
	@SuppressWarnings("unchecked")
	public void testLingeringCreativeList() {
		lingeringOn();
		List<String> expected = offeredIds();
		List<String> listed = onClient(() -> {
			List<ItemStack> stacks = new ArrayList<>();
			lingeringItem().getSubItems(lingeringItem(), CreativeTabs.tabBrewing, stacks);
			return ids(stacks);
		});
		assertEqual(expected, listed.subList(0, Math.min(listed.size(), expected.size())), "the list's start");
	}

	/** Its cloud gives the named potion's effect at 1/4 (450 for slow falling), not the drinkable potion's 1800. */
	public void testLingeringCloud() {
		lingeringOn();
		EntityZombie zombie = stillZombie(3.0D);
		onServer(() -> {
			EntityLingeringEffect cloud = new EntityLingeringEffect(world(), lingering("slow_falling"), player());
			cloud.setPosition(zombie.posX, zombie.posY, zombie.posZ);
			world().spawnEntityInWorld(cloud);
		});
		serverTicks(40);
		int duration = onServer(() -> {
			PotionEffect effect = zombie.getActivePotionEffect(EffectIds.byId("minecraft:slow_falling"));
			zombie.setDead();
			return effect == null ? 0 : effect.getDuration();
		});
		assertTrue(duration > 0 && duration <= 450, "the zombie in the cloud has slow falling for " + duration + " (26.3's: up to 450)");
	}

	// ---------------------------------------------------------------- tipped arrows

	/** A tipped arrow by name: 26.3's name, its effects at 1/8, its colour. */
	public void testTippedArrow() {
		arrowsOn();
		ItemStack arrow = arrow("slow_falling");
		assertEqual("Arrow of Slow Falling", arrow.getDisplayName());
		assertEqual("slow_falling/225/0", effects(arrowItem().getEffects(arrow)), "its effects");
		assertEqual("Arrow of Leaping", arrow("strong_leaping").getDisplayName());
		assertEqual("jump_boost/225/1", effects(arrowItem().getEffects(arrow("strong_leaping"))), "strong leaping's effects");
		assertEqual(0xF3CFB9, onClient(() -> arrow.getItem().getColorFromItemStack(arrow, 0) & 0xFFFFFF), "its colour");
	}

	/** The tipped arrows' creative list: every offered potion in 26.3's order. */
	@SuppressWarnings("unchecked")
	public void testTippedArrowCreativeList() {
		arrowsOn();
		List<String> expected = offeredIds();
		List<String> listed = onClient(() -> {
			List<ItemStack> stacks = new ArrayList<>();
			arrowItem().getSubItems(arrowItem(), CreativeTabs.tabCombat, stacks);
			return ids(stacks);
		});
		assertEqual(expected, listed.subList(0, Math.min(listed.size(), expected.size())), "the list's start");
	}

	/** Eight arrows around a lingering potion by name make eight arrows of it. */
	public void testTippedArrowRecipe() {
		lingeringOn();
		arrowsOn();
		ItemStack made = onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			for (int i = 0; i < 9; i++) {
				grid.setInventorySlotContents(i, i == 4 ? lingering("slow_falling") : new ItemStack(Items.arrow));
			}
			return new RecipeTippedArrow(ModItems.TIPPED_ARROW.newItemStack(8, 0), "AAA", "ALA", "AAA", 'A', Items.arrow,
				'L', ModItems.LINGERING_POTION.get()).getCraftingResult(grid);
		});
		assertTrue(made != null, "nothing made");
		assertEqual(8, made.stackSize, "arrows made");
		assertEqual("minecraft:slow_falling", idOf(made), "their potion");
	}

	/** An arrow of slow falling's hit gives slow falling at 1/8 (225). */
	public void testTippedArrowHit() {
		arrowsOn();
		EntityZombie zombie = stillZombie(3.0D);
		int duration = onServer(() -> {
			EntityTippedArrow shot = new EntityTippedArrow(world(), zombie.posX, zombie.posY + 3.0D, zombie.posZ);
			shot.setArrow(arrow("slow_falling"));
			shot.applyEffectsTo(zombie);
			PotionEffect effect = zombie.getActivePotionEffect(EffectIds.byId("minecraft:slow_falling"));
			zombie.setDead();
			return effect == null ? 0 : effect.getDuration();
		});
		assertEqual(225, duration, "the arrow's slow falling");
	}

	// ---------------------------------------------------------------- the potion cauldron

	/** A cauldron of slow falling: its colour, a bottle of it back out, arrows dipped in it. */
	public void testPotionCauldron() {
		if (!ModBlocks.POTION_CAULDRON.isEnabled()) {
			skipTest("potion cauldrons are off");
		}
		arrowsOn();
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			world().setBlock(2, 4, 2, ModBlocks.POTION_CAULDRON.get(), 2, 3);
			TileEntityCauldronPotion cauldron = (TileEntityCauldronPotion) world().getTileEntity(2, 4, 2);
			cauldron.potion = create("slow_falling", false);
		});
		ItemStack bottled = onServer(() -> {
			player().inventory.mainInventory[player().inventory.currentItem] = new ItemStack(Items.glass_bottle);
			ModBlocks.POTION_CAULDRON.get().onBlockActivated(world(), 2, 4, 2, player(), 1, 0.5F, 1.0F, 0.5F);
			return player().getHeldItem();
		});
		assertEqual("minecraft:slow_falling", idOf(bottled), "the bottle filled from it");
		ItemStack dipped = onServer(() -> {
			player().inventory.mainInventory[player().inventory.currentItem] = new ItemStack(Items.arrow, 4);
			ModBlocks.POTION_CAULDRON.get().onBlockActivated(world(), 2, 4, 2, player(), 1, 0.5F, 1.0F, 0.5F);
			for (ItemStack stack : player().inventory.mainInventory) {
				if (stack != null && stack.getItem() == ModItems.TIPPED_ARROW.get()) {
					return stack;
				}
			}
			return null;
		});
		assertTrue(dipped != null, "no tipped arrows from the cauldron");
		assertEqual("minecraft:slow_falling", idOf(dipped), "the dipped arrows' potion");
		onServer(() -> {
			player().inventory.clearInventory(null, -1);
			world().setBlockToAir(2, 4, 2);
		});
	}

	// ---------------------------------------------------------------- thrown

	/** A splash potion kept by name shows its colour in flight (1.7.10's client knew only its damage value). */
	public void testThrownColour() {
		EntityPotion thrown = onServer(() -> {
			EntityPotion potion = new EntityPotion(world(), player().posX + 2.0D, player().posY + 6.0D, player().posZ + 2.0D, create("slow_falling", true));
			potion.motionX = potion.motionY = potion.motionZ = 0.0D;
			world().spawnEntityInWorld(potion);
			return potion;
		});
		ticks(5);
		int colour = onClient(() -> {
			net.minecraft.entity.Entity seen = mc().theWorld.getEntityByID(thrown.getEntityId());
			return seen instanceof EntityPotion potion ? ThrownPotions.colourOf(potion) : -1;
		});
		onServer(thrown::setDead);
		assertEqual(0xF3CFB9, colour, "the thrown potion's colour on the client");
	}

	/** Its splash's particles: the colour and whether it's instant travel with the splash event. */
	public void testSplashParticles() {
		int slowFalling = ThrownPotions.splashData(create("slow_falling", true));
		assertEqual(0xF3CFB9, ThrownPotions.splashColour(slowFalling), "slow falling's splash colour");
		assertEqual(false, ThrownPotions.splashInstant(slowFalling), "slow falling instant");
		int healing = ThrownPotions.splashData(new ItemStack(Items.potionitem, 1, 8229 & ~8192 | 16384));
		assertEqual(0xF82423, ThrownPotions.splashColour(healing), "strong healing's splash colour");
		assertEqual(true, ThrownPotions.splashInstant(healing), "strong healing instant");
	}

	// ---------------------------------------------------------------- helpers

	private void lingeringOn() {
		if (!ModItems.LINGERING_POTION.isEnabled()) {
			skipTest("lingering potions are off");
		}
	}

	private void arrowsOn() {
		if (!ModItems.TIPPED_ARROW.isEnabled()) {
			skipTest("tipped arrows are off");
		}
	}

	private static ItemLingeringPotion lingeringItem() {
		return (ItemLingeringPotion) ModItems.LINGERING_POTION.get();
	}

	private static ItemArrowTipped arrowItem() {
		return (ItemArrowTipped) ModItems.TIPPED_ARROW.get();
	}

	private static NamedPotion named(String name) {
		NamedPotion potion = NamedPotions.byId("minecraft:" + name);
		assertTrue(potion != null, "no potion " + name);
		return potion;
	}

	private static ItemStack create(String name, boolean splash) {
		return PotionContents.create(Items.potionitem, splash, named(name));
	}

	private static ItemStack lingering(String name) {
		ItemStack stack = BrewingTable.create(BrewingTable.Container.LINGERING, named(name));
		assertTrue(stack != null, "no lingering " + name);
		return stack;
	}

	private static ItemStack arrow(String name) {
		ItemStack stack = ModItems.TIPPED_ARROW.newItemStack(1, 0);
		PotionContents.write(stack, false, named(name), new ArrayList<>(), null, null);
		return stack;
	}

	private EntityZombie stillZombie(double ahead) {
		return onServer(() -> {
			EntityZombie mob = new EntityZombie(world());
			mob.tasks.taskEntries.clear();
			mob.targetTasks.taskEntries.clear();
			mob.setLocationAndAngles(player().posX + ahead, player().posY, player().posZ, 0.0F, 0.0F);
			world().spawnEntityInWorld(mob);
			return mob;
		});
	}

	private static List<String> offeredIds() {
		List<String> list = new ArrayList<>();
		for (NamedPotion potion : NamedPotions.available()) {
			list.add(potion.id());
		}
		return list;
	}

	private static List<String> ids(List<ItemStack> stacks) {
		List<String> list = new ArrayList<>();
		for (ItemStack stack : stacks) {
			String id = idOf(stack);
			list.add(id == null ? "unnamed " + stack.getItemDamage() : id);
		}
		return list;
	}

	private static String idOf(ItemStack stack) {
		NamedPotion potion = PotionContents.of(stack).potion();
		return potion == null ? null : potion.id();
	}

	private static String effects(List<PotionEffect> effects) {
		List<String> parts = new ArrayList<>();
		if (effects != null) {
			for (PotionEffect effect : effects) {
				String name = EffectIds.idOf(Potion.potionTypes[effect.getPotionID()]);
				parts.add(name.substring(name.indexOf(':') + 1) + "/" + effect.getDuration() + "/" + effect.getAmplifier());
			}
		}
		return String.join(" ", parts);
	}
}
