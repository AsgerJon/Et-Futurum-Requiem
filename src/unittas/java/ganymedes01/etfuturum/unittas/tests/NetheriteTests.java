package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Netherite and ancient debris as our own (SPEC-modern-movement.md §88.4, step C1), as 26.3's: ours, EFR's classes gone;
 * EFR's ids kept (`netherite_spade`); 26.3's numbers for the tools (2031 uses, tier 4, enchantability 15) and armour (37
 * times the slot's base, defence 3, 8, 6, 3, enchantability 15), repaired with the ingot; the block and debris (50 and
 * 30 hard, 1200 against explosions, a diamond pickaxe, their sounds, the block a beacon base, the ingot a payment); 26.3's
 * fire-resistant set (`Item.Properties.fireResistant`), whose item entities fire and lava don't hurt, and no other item's;
 * 26.3's recipes (`netherite_recipes_26_3.txt`, from its jar). A row at z 80 on the flat world.
 */
@TasSuite("netherite")
public class NetheriteTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 80;
	private static final String[] ITEMS = {"netherite_scrap", "netherite_ingot", "netherite_sword", "netherite_pickaxe", "netherite_axe",
		"netherite_spade", "netherite_hoe", "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"};
	private static final String[] BLOCKS = {"netherite_block", "netherite_stairs", "ancient_debris"};
	private static final String[] EFR_CLASSES = {"items.ItemNetheriteIngot", "items.equipment.ItemEFRArmour", "items.equipment.ItemEFRPickaxe",
		"items.equipment.ItemEFRAxe", "items.equipment.ItemEFRSpade", "items.equipment.ItemEFRHoe", "items.equipment.ItemEFRSword",
		"blocks.BlockNetherite", "blocks.BlockNetheriteStairs", "blocks.BlockAncientDebris", "mixins.early.uninflammableitem.MixinEntityItem"};

	@Override
	protected void setUp() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	@Override
	protected void tearDown() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	private void clear() {
		onServer(() -> {
			for (int x = X - 4; x <= X + 19; x++) {
				for (int y = Y; y <= Y + 3; y++) {
					for (int z = Z - 4; z <= Z + 4; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem) {
					((EntityItem) entity).setDead();
				}
			}
		});
	}

	private static Item item(String name) {
		Object item = Item.itemRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(item instanceof Item, "no " + McFly.DOMAIN + ":" + name);
		return (Item) item;
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	// ---------------------------------------------------------------- ours

	/** Every netherite item and block McFly's, under EFR's ids; none of EFR's classes for them left. */
	public void testNetheriteIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : ITEMS) {
			String type = item(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : BLOCKS) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "netherite's classes");
	}

	// ---------------------------------------------------------------- the numbers

	/** 26.3's tools (`ToolMaterial.NETHERITE`: 2031 uses, tier 4, enchantability 15) and armour (`ArmorMaterials.NETHERITE`). */
	public void testToolsAndArmourAs26_3() {
		StringBuilder got = new StringBuilder();
		for (String name : new String[]{"netherite_sword", "netherite_pickaxe", "netherite_axe", "netherite_spade", "netherite_hoe"}) {
			Item tool = item(name);
			ItemStack stack = new ItemStack(tool);
			got.append(name).append(' ').append(tool.getMaxDamage()).append(' ').append(tool.getItemEnchantability());
			if (tool instanceof ItemTool) {
				got.append(" tier ").append(((ItemTool) tool).func_150913_i().getHarvestLevel());
			}
			got.append(tool.getIsRepairable(stack, new ItemStack(item("netherite_ingot"))) ? " ingot" : " -").append("; ");
		}
		// Forge's harvest levels, as a player's breaking asks (1.7.10's own pickaxe check wants exactly 3 for obsidian)
		got.append("pickaxe on obsidian ").append(net.minecraftforge.common.ForgeHooks.canToolHarvestBlock(Blocks.obsidian, 0, new ItemStack(item("netherite_pickaxe"))))
			.append(", its speed ").append(item("netherite_pickaxe").getDigSpeed(new ItemStack(item("netherite_pickaxe")), Blocks.stone, 0)).append("; ");
		for (String name : new String[]{"netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"}) {
			ItemArmor armour = (ItemArmor) item(name);
			got.append(name).append(' ').append(armour.getMaxDamage()).append(' ').append(armour.damageReduceAmount).append(' ')
				.append(armour.getItemEnchantability()).append(armour.getIsRepairable(new ItemStack(armour), new ItemStack(item("netherite_ingot"))) ? " ingot" : " -")
				.append("; ");
		}
		assertEqual("netherite_sword 2031 15 ingot; netherite_pickaxe 2031 15 tier 4 ingot; netherite_axe 2031 15 tier 4 ingot; "
			+ "netherite_spade 2031 15 tier 4 ingot; netherite_hoe 2031 15 ingot; pickaxe on obsidian true, its speed 9.0; "
			+ "netherite_helmet 407 3 15 ingot; netherite_chestplate 592 8 15 ingot; netherite_leggings 555 6 15 ingot; netherite_boots 481 3 15 ingot; ",
			got.toString(), "uses, enchantability, tier and repair; uses, defence, enchantability and repair");
	}

	/** The block and debris: 26.3's strength (50 and 30, 1200), a diamond pickaxe, their sounds; the block a beacon base. */
	public void testBlocksAs26_3() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (String name : new String[]{"netherite_block", "ancient_debris"}) {
				Block block = block(name);
				world().setBlock(X, Y, Z, block, 0, 3);
				out.append(name).append(' ').append(block.getBlockHardness(world(), X, Y, Z)).append(' ')
					.append(block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z)).append(' ').append(block.getHarvestTool(0)).append(' ')
					.append(block.getHarvestLevel(0)).append(' ').append(block.stepSound.getBreakSound()).append(' ')
					.append(block.isBeaconBase(world(), X, Y, Z, X, Y + 1, Z)).append("; ");
			}
			world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			out.append("ingot a payment ").append(item("netherite_ingot").isBeaconPayment(new ItemStack(item("netherite_ingot"))));
			return out.toString();
		});
		assertEqual("netherite_block 50.0 1200.0 pickaxe 3 " + McFly.MC_ASSETS + ":block.netherite_block.break true; ancient_debris 30.0 1200.0 pickaxe 3 "
			+ McFly.MC_ASSETS + ":block.ancient_debris.break false; ingot a payment true", got, "the blocks");
	}

	// ---------------------------------------------------------------- fire resistant

	/**
	 * 26.3's fire-resistant set: the scrap, ingot, block, debris, tools and armour (and EFR's stairs); their item entities in
	 * lava aren't hurt (a diamond's is, and burns away), and nothing else is made fire resistant by its name.
	 */
	public void testFireResistantItemsSurviveLava() {
		List<String> names = new ArrayList<>();
		java.util.Collections.addAll(names, ITEMS);
		java.util.Collections.addAll(names, BLOCKS);
		onServer(() -> {
			int i = 0;
			for (String name : names) {
				put(X + i, new ItemStack(Item.itemRegistry.getObject(McFly.DOMAIN + ":" + name) instanceof Item ? item(name) : Item.getItemFromBlock(block(name))));
				i++;
			}
			put(X + i, new ItemStack(Items.diamond));
		});
		serverTicks(60);
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (Object entity : world().loadedEntityList) {
				if (entity instanceof EntityItem && !((EntityItem) entity).isDead && Math.abs(((EntityItem) entity).posZ - (Z + 0.5)) < 1.0) {
					ItemStack stack = ((EntityItem) entity).getEntityItem();
					out.append(stack.getItem() == Items.diamond ? "diamond" : Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", ""))
						.append(' ');
				}
			}
			return out.toString().trim();
		});
		List<String> left = new ArrayList<>(java.util.Arrays.asList(got.split(" ")));
		java.util.Collections.sort(left);
		List<String> want = new ArrayList<>(names);
		java.util.Collections.sort(want);
		assertEqual(want.toString(), left.toString(), "what's left of the items in lava after 60 ticks (the diamond burnt)");
	}

	/** An item entity of this stack in a lava source at (x, Y, Z), held in place. */
	private static void put(int x, ItemStack stack) {
		world().setBlock(x, Y - 1, Z, Blocks.stone, 0, 3);
		world().setBlock(x, Y, Z, Blocks.lava, 0, 3);
		world().setBlock(x, Y + 1, Z, Blocks.glass, 0, 3);
		EntityItem item = new EntityItem(world(), x + 0.5, Y + 0.3, Z + 0.5, stack);
		item.motionX = item.motionY = item.motionZ = 0;
		item.delayBeforeCanPickup = 1000;
		world().spawnEntityInWorld(item);
	}

	// ---------------------------------------------------------------- the stored form and recipes

	/** EFR's ids, read by 26.3's names: the spade is 26.3's netherite shovel; the block and debris. */
	public void testStoredForm() {
		String got = onServer(() -> {
			BlockIds.Found block = BlockIds.find(block("netherite_block"), 0);
			BlockIds.Found debris = BlockIds.find(block("ancient_debris"), 0);
			ganymedes01.etfuturum.command.modern.arguments.ItemIds.Entry shovel = ganymedes01.etfuturum.command.modern.arguments.ItemIds.byId("minecraft:netherite_shovel");
			return (block == null ? "?" : block.type().id) + " " + (debris == null ? "?" : debris.type().id) + " "
				+ (shovel == null ? "?" : Item.itemRegistry.getNameForObject(shovel.item()));
		});
		assertEqual("minecraft:netherite_block minecraft:ancient_debris " + McFly.DOMAIN + ":netherite_spade", got, "26.3's names for EFR's ids");
	}

	/** 26.3's netherite recipes (its jar's, `netherite_recipes_26_3.txt`), each giving 26.3's result here. */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("netherite_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(4, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
