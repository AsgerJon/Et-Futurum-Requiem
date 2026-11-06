package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.relauncher.ReflectionHelper;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;

import java.util.Map;

/**
 * McFly's plants in 1.7.10's flower pot (SPEC-modern-movement.md §90, step D2), as 26.3's potted blocks: azalea and
 * flowering azalea (`potted_azalea_bush`, `potted_flowering_azalea_bush`), and §88's fungi and roots (EFR's pot item
 * went with EFR's blocks); nether sprouts and other blocks not (26.3 has no potted ones). In the pot by the item's id
 * and damage, given back when the pot breaks; drawn as 26.3's potted model, the pot with it. At z 360, y 8; the drawn
 * ones at x -3, z -3, by the player, where the client has the chunk.
 */
@TasSuite("caves")
public class FlowerPotTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 360;
	private static final int DRAWN_X = -3, DRAWN_Y = 4, DRAWN_Z = -3;

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

	private void clear() {
		for (int y = Y - 1; y <= Y + 1; y++) {
			world().setBlock(X, y, Z, Blocks.air, 0, 2);
		}
		world().setBlock(DRAWN_X, DRAWN_Y, DRAWN_Z, Blocks.air, 0, 2);
		world().setBlock(DRAWN_X, DRAWN_Y + 1, DRAWN_Z, Blocks.air, 0, 2);
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 2, Y - 2, Z - 2, X + 3, Y + 3, Z + 3))) {
			((EntityItem) entity).setDead();
		}
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static String name(Item item) {
		return Item.itemRegistry.getNameForObject(item).replace(McFly.DOMAIN + ":", "").replace("minecraft:", "");
	}

	/** A pot on stone, the player holding two of the plant uses it on the pot: used or not, what the pot holds, what's left held. */
	private String pot(Block plant, int meta) {
		return onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 2);
			// a new pot: setting a pot over a pot keeps the old one, its plant too
			world().setBlock(X, Y, Z, Blocks.air, 0, 2);
			world().setBlock(X, Y, Z, Blocks.flower_pot, 0, 2);
			ItemStack held = new ItemStack(plant, 2, meta);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, held);
			boolean used = Blocks.flower_pot.onBlockActivated(world(), X, Y, Z, player(), 1, 0.5F, 0.5F, 0.5F);
			TileEntityFlowerPot pot = (TileEntityFlowerPot) world().getTileEntity(X, Y, Z);
			String holds = pot.getFlowerPotItem() == null ? "empty" : name(pot.getFlowerPotItem()) + ":" + pot.getFlowerPotData();
			return used + " " + holds + " held " + held.stackSize;
		});
	}

	/** The pot broken by hand: what drops. */
	private String broken() {
		return onServer(() -> {
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			player().theItemInWorldManager.tryHarvestBlock(X, Y, Z);
			Map<String, Integer> counts = new java.util.TreeMap<>();
			for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 2, Y - 2, Z - 2, X + 3, Y + 3, Z + 3))) {
				EntityItem item = (EntityItem) entity;
				if (!item.isDead) {
					ItemStack stack = item.getEntityItem();
					counts.merge(name(stack.getItem()) + ":" + stack.getItemDamage(), stack.stackSize, Integer::sum);
					item.setDead();
				}
			}
			return counts.toString();
		});
	}

	public void testAzaleasGoInAPot() {
		assertEqual("true azalea:0 held 1", pot(block("azalea"), 0), "azalea into a pot");
		assertEqual("{azalea:0=1, flower_pot:0=1}", broken(), "the potted azalea broken");
		assertEqual("true azalea:1 held 1", pot(block("azalea"), 1), "flowering azalea into a pot");
		assertEqual("{azalea:1=1, flower_pot:0=1}", broken(), "the potted flowering azalea broken");
	}

	/** §88's fungi and roots, each kind; the sprouts, moss carpet and hanging roots not (26.3 pots none of them). */
	public void testNetherPlantsGoInAPot() {
		assertEqual("true nether_fungus:0 held 1", pot(block("nether_fungus"), 0), "crimson fungus into a pot");
		assertEqual("true nether_fungus:1 held 1", pot(block("nether_fungus"), 1), "warped fungus into a pot");
		assertEqual("true nether_roots:0 held 1", pot(block("nether_roots"), 0), "crimson roots into a pot");
		assertEqual("true nether_roots:1 held 1", pot(block("nether_roots"), 1), "warped roots into a pot");
		assertEqual("{flower_pot:0=1, nether_roots:1=1}", broken(), "the potted warped roots broken");
		assertEqual("false empty held 2", pot(block("nether_sprouts"), 0), "nether sprouts into a pot");
		assertEqual("false empty held 2", pot(block("moss_carpet"), 0), "a moss carpet into a pot");
		assertEqual("false empty held 2", pot(block("hanging_roots"), 0), "hanging roots into a pot");
	}

	/** Step E's (spec §90.6): the propagule and the cherry sapling, each kind; a hanging propagule's damage isn't an item's. */
	public void testSaplingsGoInAPot() {
		assertEqual("true sapling:0 held 1", pot(block("sapling"), 0), "a mangrove propagule into a pot");
		assertEqual("true sapling:1 held 1", pot(block("sapling"), 1), "a cherry sapling into a pot");
		assertEqual("{flower_pot:0=1, sapling:1=1}", broken(), "the potted cherry sapling broken");
	}

	/**
	 * Drawn as 26.3's potted model, on grass with air above (its five `cullface: down` faces culled): the azalea's
	 * template, 36 faces, 31 drawn, 124 vertices; a fungus's `flower_pot_cross`, 26 faces, 21 drawn, 84 vertices.
	 */
	public void testPottedModelsDrawn() {
		assertEqual(124, drawn(block("azalea"), 0), "vertices of the potted azalea");
		assertEqual(124, drawn(block("azalea"), 1), "vertices of the potted flowering azalea");
		assertEqual(84, drawn(block("nether_fungus"), 0), "vertices of the potted crimson fungus");
		assertEqual(84, drawn(block("nether_roots"), 1), "vertices of the potted warped roots");
		// step E's: `potted_mangrove_propagule` (its own model, 30 faces, 25 drawn) and `potted_cherry_sapling` (the cross)
		assertEqual(100, drawn(block("sapling"), 0), "vertices of the potted mangrove propagule");
		assertEqual(84, drawn(block("sapling"), 1), "vertices of the potted cherry sapling");
	}

	/** A pot by the player holding the plant, once the client has it: the vertices 1.7.10's chunk renderer gets for it. */
	private int drawn(Block plant, int meta) {
		Item item = Item.getItemFromBlock(plant);
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			world().setBlock(DRAWN_X, DRAWN_Y - 1, DRAWN_Z, Blocks.grass, 0, 2);
			world().setBlock(DRAWN_X, DRAWN_Y, DRAWN_Z, Blocks.flower_pot, 0, 2);
			TileEntityFlowerPot pot = (TileEntityFlowerPot) world().getTileEntity(DRAWN_X, DRAWN_Y, DRAWN_Z);
			pot.func_145964_a(item, meta);
			pot.markDirty();
			world().setBlockMetadataWithNotify(DRAWN_X, DRAWN_Y, DRAWN_Z, meta, 2);
			world().markBlockForUpdate(DRAWN_X, DRAWN_Y, DRAWN_Z);
		});
		waitUntilOnClient("the client's pot holding " + name(item) + ":" + meta, () -> mc().theWorld.getTileEntity(DRAWN_X, DRAWN_Y, DRAWN_Z) instanceof TileEntityFlowerPot pot
			&& pot.getFlowerPotItem() == item && pot.getFlowerPotData() == meta, 40);
		return onClient(() -> {
			Tessellator tessellator = Tessellator.instance;
			tessellator.startDrawingQuads();
			new RenderBlocks(mc().theWorld).renderBlockByRenderType(Blocks.flower_pot, DRAWN_X, DRAWN_Y, DRAWN_Z);
			int vertices = ReflectionHelper.getPrivateValue(Tessellator.class, tessellator, "vertexCount", "field_78406_i");
			tessellator.draw();
			return vertices;
		});
	}
}
