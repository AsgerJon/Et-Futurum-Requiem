package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Copper's world side (SPEC-modern-movement.md §87): honeycomb's wax and the axe's wax off and scrape by a right click
 * (26.3's item uses; a door or trapdoor opens instead unless the player sneaks, as 26.3's block action comes first);
 * honeycomb from a dispenser; iron and gold ores' raw drops (26.3's: 1, Fortune's ore bonus on top; 1.7.10's ores
 * drop themselves).
 */
public final class CopperEvents {

	public static final CopperEvents INSTANCE = new CopperEvents();

	private CopperEvents() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(INSTANCE);
		Item honeycomb = honeycomb();
		if (honeycomb != null) {
			BlockDispenser.dispenseBehaviorRegistry.putObject(honeycomb, new BehaviorDefaultDispenseItem() {
				@Override
				protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
					EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
					World world = source.getWorld();
					int x = source.getXInt() + facing.getFrontOffsetX();
					int y = source.getYInt() + facing.getFrontOffsetY();
					int z = source.getZInt() + facing.getFrontOffsetZ();
					if (Weathering.wax(world, x, y, z)) {
						stack.splitStack(1);
						return stack;
					}
					return super.dispenseStack(source, stack);
				}
			});
		}
	}

	/** EFR's honeycomb, until the bees' item makes it ours. */
	public static Item honeycomb() {
		return (Item) Item.itemRegistry.getObject(McFly.DOMAIN + ":honeycomb");
	}

	@SubscribeEvent
	public void interact(PlayerInteractEvent event) {
		if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.world.isRemote) {
			return;
		}
		EntityPlayer player = event.entityPlayer;
		ItemStack held = player.getHeldItem();
		if (held == null) {
			return;
		}
		Block block = event.world.getBlock(event.x, event.y, event.z);
		if (!(block instanceof WeatheringCopper) || (block instanceof BlockDoor || block instanceof BlockTrapDoor) && !player.isSneaking()) {
			return;
		}
		if (held.getItem() == honeycomb()) {
			if (Weathering.wax(event.world, event.x, event.y, event.z)) {
				if (!player.capabilities.isCreativeMode) {
					held.stackSize--;
					if (held.stackSize <= 0) {
						player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
					}
				}
				player.swingItem();
				event.setCanceled(true);
			}
		} else if (held.getItem().getToolClasses(held).contains("axe")) {
			if (Weathering.axe(event.world, event.x, event.y, event.z)) {
				held.damageItem(1, player);
				if (held.stackSize <= 0) {
					player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
				}
				player.swingItem();
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void drops(BlockEvent.HarvestDropsEvent event) {
		if (event.isSilkTouching || !McFlyItems.RAW_ORE.isEnabled()) {
			return;
		}
		int raw = rawOf(event.block);
		if (raw < 0) {
			return;
		}
		event.drops.clear();
		event.drops.add(new ItemStack(McFlyItems.RAW_ORE.get(), CopperOreBlock.oreBonus(event.fortuneLevel, event.world.rand), raw));
	}

	/** The raw ore an iron or gold ore drops (EFR's deepslate ones too, till deepslate's item), else -1. */
	private static int rawOf(Block block) {
		String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
		if (block == Blocks.iron_ore || name.equals(McFly.DOMAIN + ":deepslate_iron_ore")) {
			return RawOreItem.IRON;
		}
		if (block == Blocks.gold_ore || name.equals(McFly.DOMAIN + ":deepslate_gold_ore")) {
			return RawOreItem.GOLD;
		}
		return -1;
	}
}
