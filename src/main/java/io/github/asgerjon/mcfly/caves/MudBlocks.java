package io.github.asgerjon.mcfly.caves;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyMetaBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Mud as 26.3's (SPEC-modern-movement.md §90, step C), EFR's ids: mud; packed mud and mud bricks in one id (`packed_mud`
 * 0 and 1); a water bottle (drinkable, holding water) on dirt or coarse dirt from any face but below makes mud, a
 * dispenser's too (26.3's `PotionItem.useOn` and its dispenser behaviour).
 */
public final class MudBlocks {

	public static final MudBlocks EVENTS = new MudBlocks();

	private MudBlocks() {
	}

	/** Mud: dirt's 0.5 and 0.5, a shovel, its own sound, 26.3's terracotta cyan as 1.7.10's gray; 14 pixels high to walk on; plants grow on it as on dirt. */
	public static final class Mud extends McFlyBlock {

		public Mud() {
			super(Material.ground);
			setNames("mud");
			strength(0.5F, 0.5F);
			tool("shovel", 0);
			sound(McFlySoundType.MUD);
			colour(MapColor.grayColor);
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 14.0D / 16.0D, z + 1);
		}

		@Override
		public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
			return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plantable);
		}
	}

	/** Packed mud (0: 1 and 3, dropped by hand, 26.3's no tool rule) and mud bricks (1: 1.5 and 3, a pickaxe), the bricks' sound by EFR's per-variant table. */
	public static final class PackedMud extends McFlyMetaBlock {

		public PackedMud() {
			super(Material.rock, McFlySoundType.PACKED_MUD, new Variant("packed_mud", 1.0F, 3.0F, MapColor.dirtColor),
				new Variant("mud_bricks", 1.5F, 3.0F, MapColor.dirtColor));
			tool("pickaxe", 0);
		}

		@Override
		public boolean canHarvestBlock(EntityPlayer player, int meta) {
			return meta == 0 || super.canHarvestBlock(player, meta);
		}
	}

	// ---------------------------------------------------------------- dirt into mud

	/** 26.3's `#convertible_to_mud`: dirt and coarse dirt (1.7.10's dirt 0 and 1, EFR's coarse dirt), not podzol. */
	public static boolean convertible(Block block, int meta) {
		return block == Blocks.dirt && meta != 2 || block == McFlyBlocks.ROOTED_DIRT.get() || Block.blockRegistry.getNameForObject(block) != null
			&& (McFly.DOMAIN + ":coarse_dirt").equals(Block.blockRegistry.getNameForObject(block));
	}

	/** A drinkable bottle of water (26.3's `PotionContents.is(Potions.WATER)`). */
	public static boolean isWaterBottle(ItemStack stack) {
		if (stack == null || stack.getItem() != Items.potionitem || ganymedes01.etfuturum.potion.PotionContents.isSplash(stack)) {
			return false;
		}
		ganymedes01.etfuturum.potion.NamedPotion potion = ganymedes01.etfuturum.potion.PotionContents.of(stack).potion();
		if (potion != null) {
			return "minecraft:water".equals(potion.id());
		}
		// 1.7.10's water bottle: damage 0, nothing in it
		net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
		return stack.getItemDamage() == 0 && (tag == null || !tag.hasKey("CustomPotionEffects") && !tag.hasKey("Potion"));
	}

	/** The block becomes mud with 26.3's splash, 5 splash particles above it and the bottle's sound (on the server). */
	private static void toMud(World world, int x, int y, int z, boolean splash) {
		if (splash) {
			world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":entity.generic.splash", 1.0F, 1.0F);
		}
		if (world instanceof WorldServer server) {
			for (int i = 0; i < 5; i++) {
				server.func_147487_a("splash", x + world.rand.nextDouble(), y + 1, z + world.rand.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0);
			}
		}
		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":item.bottle.empty", 1.0F, 1.0F);
		world.setBlock(x, y, z, McFlyBlocks.MUD.get());
	}

	/**
	 * A water bottle used on a block's face (the potion's own use, ahead of drinking it): dirt or coarse dirt from any face
	 * but below becomes mud, the bottle given back (a stack of them shrinks and the empty one goes to the inventory).
	 */
	public static boolean useWaterBottle(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side) {
		if (!McFlyBlocks.MUD.isEnabled() || side == 0 || !isWaterBottle(stack) || !convertible(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z))) {
			return false;
		}
		if (!world.isRemote) {
			toMud(world, x, y, z, true);
			if (!player.capabilities.isCreativeMode) {
				ItemStack bottle = new ItemStack(Items.glass_bottle);
				if (--stack.stackSize <= 0) {
					player.inventory.setInventorySlotContents(player.inventory.currentItem, bottle);
				} else if (!player.inventory.addItemStackToInventory(bottle)) {
					player.dropPlayerItemWithRandomChoice(bottle, false);
				}
			}
		}
		return true;
	}

	/** In init: the dispenser's water bottle onto dirt (26.3's), before 1.7.10's potion behaviour; ExtraUtils' watering can (EFR's). */
	public static void register() {
		if (!McFlyBlocks.MUD.isEnabled()) {
			return;
		}
		IBehaviorDispenseItem potions = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(Items.potionitem);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Items.potionitem, new IBehaviorDispenseItem() {
			@Override
			public ItemStack dispense(IBlockSource source, ItemStack stack) {
				EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
				World world = source.getWorld();
				int x = source.getXInt() + facing.getFrontOffsetX(), y = source.getYInt() + facing.getFrontOffsetY(), z = source.getZInt() + facing.getFrontOffsetZ();
				if (!isWaterBottle(stack) || !convertible(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z))) {
					return potions.dispense(source, stack);
				}
				toMud(world, x, y, z, false);
				// 26.3's `consumeWithRemainder`: the empty bottle in the slot, or put away, or dropped
				if (--stack.stackSize <= 0) {
					return new ItemStack(Items.glass_bottle);
				}
				if (((TileEntityDispenser) source.getBlockTileEntity()).func_146019_a(new ItemStack(Items.glass_bottle)) < 0) {
					new BehaviorDefaultDispenseItem().dispense(source, new ItemStack(Items.glass_bottle));
				}
				return stack;
			}
		});
		MinecraftForge.EVENT_BUS.register(EVENTS);
	}

	/** ExtraUtils' watering can (damage 0 or 3) on dirt from any face but below, as EFR's handler had it (the can kept). */
	@SubscribeEvent
	public void interact(PlayerInteractEvent event) {
		if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.world.isRemote || event.face == 0) {
			return;
		}
		ItemStack held = event.entityPlayer.getHeldItem();
		Item can = GameRegistry.findItem("ExtraUtilities", "watering_can");
		if (held == null || can == null || held.getItem() != can || held.getItemDamage() != 0 && held.getItemDamage() != 3
			|| !convertible(event.world.getBlock(event.x, event.y, event.z), event.world.getBlockMetadata(event.x, event.y, event.z))) {
			return;
		}
		toMud(event.world, event.x, event.y, event.z, true);
		event.entityPlayer.swingItem();
	}
}
