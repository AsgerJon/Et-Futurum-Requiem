package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.FuelBurnTimeEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 26.3's fuel times for the wood sets' items (SPEC-modern-movement.md §91.4, step B; its `ContextIntProviders`): wood
 * blocks 300, slabs 150, and nothing for crimson's and warped's (`#non_flammable_wood`). On Forge's
 * `FuelBurnTimeEvent`, which a furnace asks before 1.7.10's own rule (any wooden block 300: a slab too).
 */
public final class WoodFuel {

	public static final WoodFuel INSTANCE = new WoodFuel();

	/** Per block: its burn time by item damage (the last for any damage past the array). */
	private static final Map<Block, int[]> TIMES = new IdentityHashMap<>();

	private WoodFuel() {
	}

	private static void put(McFlyBlocks entry, int... times) {
		if (entry.isEnabled()) {
			TIMES.put(entry.get(), times);
		}
	}

	public static void register() {
		put(McFlyBlocks.WOOD_PLANKS, 0, 0, 300, 300, 300);
		put(McFlyBlocks.WOOD_SLAB, 0, 0, 150, 150, 150);
		put(McFlyBlocks.CRIMSON_STAIRS, 0);
		put(McFlyBlocks.WARPED_STAIRS, 0);
		put(McFlyBlocks.MANGROVE_STAIRS, 300);
		put(McFlyBlocks.CHERRY_STAIRS, 300);
		put(McFlyBlocks.BAMBOO_STAIRS, 300);
		put(McFlyBlocks.BAMBOO_MOSAIC, 300);
		// the fences and gates (step C): 300, crimson's and warped's none
		put(McFlyBlocks.WOOD_FENCE, 0, 0, 300, 300, 300);
		put(McFlyBlocks.CRIMSON_FENCE_GATE, 0);
		put(McFlyBlocks.WARPED_FENCE_GATE, 0);
		put(McFlyBlocks.BAMBOO_MOSAIC_SLAB, 150);
		put(McFlyBlocks.BAMBOO_MOSAIC_STAIRS, 300);
		// the doors and trapdoors (step D): doors 200, trapdoors 300, crimson's and warped's none (iron's aren't wood)
		for (McFlyBlocks door : new McFlyBlocks[]{McFlyBlocks.DOOR_SPRUCE, McFlyBlocks.DOOR_BIRCH, McFlyBlocks.DOOR_JUNGLE, McFlyBlocks.DOOR_ACACIA,
			McFlyBlocks.DOOR_DARK_OAK, McFlyBlocks.MANGROVE_DOOR, McFlyBlocks.CHERRY_DOOR, McFlyBlocks.BAMBOO_DOOR}) {
			put(door, 200);
		}
		put(McFlyBlocks.CRIMSON_DOOR, 0);
		put(McFlyBlocks.WARPED_DOOR, 0);
		put(McFlyBlocks.CRIMSON_TRAPDOOR, 0);
		put(McFlyBlocks.WARPED_TRAPDOOR, 0);
		// the buttons and plates (step E): buttons 100, plates 300, crimson's and warped's none
		for (McFlyBlocks button : new McFlyBlocks[]{McFlyBlocks.BUTTON_SPRUCE, McFlyBlocks.BUTTON_BIRCH, McFlyBlocks.BUTTON_JUNGLE, McFlyBlocks.BUTTON_ACACIA,
			McFlyBlocks.BUTTON_DARK_OAK, McFlyBlocks.MANGROVE_BUTTON, McFlyBlocks.CHERRY_BUTTON, McFlyBlocks.BAMBOO_BUTTON}) {
			put(button, 100);
		}
		put(McFlyBlocks.CRIMSON_BUTTON, 0);
		put(McFlyBlocks.WARPED_BUTTON, 0);
		put(McFlyBlocks.CRIMSON_PRESSURE_PLATE, 0);
		put(McFlyBlocks.WARPED_PRESSURE_PLATE, 0);
		// vanilla's oak button (1.7.10's isn't of wood, so it had none)
		TIMES.put(net.minecraft.init.Blocks.wooden_button, new int[]{100});
		// the signs (step F): 200, crimson's and warped's none (their items are the standing signs'); the old woods' items below
		for (McFlyBlocks sign : new McFlyBlocks[]{McFlyBlocks.MANGROVE_SIGN, McFlyBlocks.CHERRY_SIGN, McFlyBlocks.BAMBOO_SIGN}) {
			put(sign, 200);
		}
		put(McFlyBlocks.CRIMSON_SIGN, 0);
		put(McFlyBlocks.WARPED_SIGN, 0);
		// the hanging signs (step G): 800, crimson's and warped's none
		for (String wood : WoodSigns.HANGING) {
			put(McFlyBlocks.valueOf(wood.toUpperCase() + "_HANGING_SIGN"), wood.equals("crimson") || wood.equals("warped") ? 0 : 800);
		}
		// the shelves (step H): 300, crimson's and warped's none
		for (String wood : WoodSigns.HANGING) {
			put(McFlyBlocks.valueOf(wood.toUpperCase() + "_SHELF"), wood.equals("crimson") || wood.equals("warped") ? 0 : 300);
		}
		// the mods' signs: 200 too (their items are their standing signs'; 1.7.10's rule gave a wooden block's 300)
		for (McFlyBlocks entry : McFlyBlocks.VALUES) {
			String name = entry.name();
			if ((name.startsWith("BOP_") || name.startsWith("WITCHERY_")) && name.endsWith("_SIGN") && !name.endsWith("_WALL_SIGN")) {
				put(entry, 200);
			}
		}
		// the stems and hyphae (spec §88.7): wood that doesn't burn
		put(McFlyBlocks.CRIMSON_STEM, 0);
		put(McFlyBlocks.WARPED_STEM, 0);
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/** The burn time 26.3 gives the stack, or -1 when it's none of these. */
	public static int burnTime(ItemStack stack) {
		if (stack != null && (stack.getItem() instanceof io.github.asgerjon.mcfly.boat.BoatItem || stack.getItem() == net.minecraft.init.Items.boat)) {
			// the boats and rafts (step J), vanilla's too: 26.3's 1200
			return 1200;
		}
		if (stack != null && stack.getItem() instanceof Bamboo.BambooItem) {
			// bamboo: 26.3's 50
			return 50;
		}
		if (stack != null && (stack.getItem() == net.minecraft.init.Items.wooden_door || stack.getItem() == net.minecraft.init.Items.sign
			|| stack.getItem() instanceof SignItem)) {
			// vanilla's oak door and sign, the old woods' sign items: 26.3's 200 (1.7.10's aren't a block's items, so they had none)
			return 200;
		}
		Block block = stack == null || stack.getItem() == null ? null : Block.getBlockFromItem(stack.getItem());
		int[] times = block == null ? null : TIMES.get(block);
		if (times == null) {
			return -1;
		}
		int damage = stack.getItemDamage();
		return times[damage >= 0 && damage < times.length ? damage : times.length - 1];
	}

	@SubscribeEvent
	public void burnTime(FuelBurnTimeEvent event) {
		int time = burnTime(event.fuel);
		if (time >= 0) {
			event.burnTime = time;
			event.setResult(time > 0 ? Event.Result.ALLOW : Event.Result.DENY);
		}
	}
}
