package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 26.3's stripping (SPEC-modern-movement.md §91.3, step A; its `BlockTransformers.AXE`, `axeStrippables`): an axe used
 * on a log, wood, stem or hyphae makes it the stripped kind, its axis kept, with `item.axe.strip`, the axe worn once
 * (not in creative), the arm swung and `block_change` from the player. One table for every log here: 1.7.10's (its
 * six-sided bark, metas 12 to 15, is a wood), EFR's ids (McFly's now), McFly's mangrove log and stems, and Biomes O'
 * Plenty's and Witchery's as EFR's `StrippedLogRegistry` had them. By the right click, on the server; the client swings.
 */
public final class Stripping {

	public static final Stripping INSTANCE = new Stripping();

	/** Per log block, per kind (meta & 3): the stripped block and kind, or null. */
	private static final Map<Block, Target[]> TABLE = new IdentityHashMap<>();
	/** Per vanilla log block: the stripped woods its six-sided bark (12 to 15) becomes. */
	private static final Map<Block, Block> SIX_SIDED = new IdentityHashMap<>();

	private static final class Target {
		final Block block;
		final int kind;

		Target(Block block, int kind) {
			this.block = block;
			this.kind = kind;
		}
	}

	private Stripping() {
	}

	/** The block's kind stripped into that block's kind. */
	public static void add(Block from, int kind, Block to, int toKind) {
		if (from == null || to == null) {
			return;
		}
		TABLE.computeIfAbsent(from, block -> new Target[4])[kind & 3] = new Target(to, toKind);
	}

	public static void register() {
		McFlyBlocks[] logs = {McFlyBlocks.LOG_STRIPPED, McFlyBlocks.LOG2_STRIPPED};
		McFlyBlocks[] woods = {McFlyBlocks.BARK, McFlyBlocks.BARK2};
		McFlyBlocks[] strippedWoods = {McFlyBlocks.WOOD_STRIPPED, McFlyBlocks.WOOD2_STRIPPED};
		Block[] vanilla = {Blocks.log, Blocks.log2};
		for (int set = 0; set < 2; set++) {
			for (int i = 0; i < (set == 0 ? 4 : 2); i++) {
				if (logs[set].isEnabled()) {
					add(vanilla[set], i, logs[set].get(), i);
				}
				if (woods[set].isEnabled() && strippedWoods[set].isEnabled()) {
					add(woods[set].get(), i, strippedWoods[set].get(), i);
				}
			}
			if (strippedWoods[set].isEnabled()) {
				SIX_SIDED.put(vanilla[set], strippedWoods[set].get());
			}
		}
		// one wood's four kinds in an id: log to stripped log, wood to stripped wood
		for (McFlyBlocks log : new McFlyBlocks[]{McFlyBlocks.CHERRY_LOG, McFlyBlocks.MANGROVE_LOG, McFlyBlocks.CRIMSON_STEM, McFlyBlocks.WARPED_STEM}) {
			if (log.isEnabled()) {
				add(log.get(), 0, log.get(), 2);
				add(log.get(), 1, log.get(), 3);
			}
		}
		if (McFlyBlocks.BAMBOO_BLOCK.isEnabled()) {
			add(McFlyBlocks.BAMBOO_BLOCK.get(), 0, McFlyBlocks.BAMBOO_BLOCK.get(), 1);
		}
		// Biomes O' Plenty's and Witchery's, as EFR had them
		McFlyBlocks[] bopLogs = {McFlyBlocks.BOP_LOG_STRIPPED, McFlyBlocks.BOP_LOG_STRIPPED2, McFlyBlocks.BOP_LOG_STRIPPED3, McFlyBlocks.BOP_LOG_STRIPPED4};
		McFlyBlocks[] bopWoods = {McFlyBlocks.BOP_WOOD, McFlyBlocks.BOP_WOOD2, McFlyBlocks.BOP_WOOD3, McFlyBlocks.BOP_WOOD4};
		McFlyBlocks[] bopStrippedWoods = {McFlyBlocks.BOP_WOOD_STRIPPED, McFlyBlocks.BOP_WOOD_STRIPPED2, McFlyBlocks.BOP_WOOD_STRIPPED3, McFlyBlocks.BOP_WOOD_STRIPPED4};
		for (int n = 0; n < 4; n++) {
			for (int i = 0; i < 4; i++) {
				if (bopLogs[n].isEnabled()) {
					add(GameRegistry.findBlock("BiomesOPlenty", "logs" + (n + 1)), i, bopLogs[n].get(), i);
				}
				if (bopWoods[n].isEnabled() && bopStrippedWoods[n].isEnabled()) {
					add(bopWoods[n].get(), i, bopStrippedWoods[n].get(), i);
				}
			}
		}
		for (int i = 0; i < 3; i++) {
			if (McFlyBlocks.WITCHERY_LOG_STRIPPED.isEnabled()) {
				add(GameRegistry.findBlock("witchery", "witchlog"), i, McFlyBlocks.WITCHERY_LOG_STRIPPED.get(), i);
			}
			if (McFlyBlocks.WITCHERY_WOOD.isEnabled() && McFlyBlocks.WITCHERY_WOOD_STRIPPED.isEnabled()) {
				add(McFlyBlocks.WITCHERY_WOOD.get(), i, McFlyBlocks.WITCHERY_WOOD_STRIPPED.get(), i);
			}
		}
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/** What the block at this meta strips into, as {block, meta}, or null: the axis kept (a six-sided bark's y). */
	public static Object[] stripped(Block block, int meta) {
		if ((meta & 12) == 12 && SIX_SIDED.containsKey(block)) {
			return new Object[]{SIX_SIDED.get(block), meta & 3};
		}
		Target[] row = TABLE.get(block);
		Target target = row == null ? null : row[meta & 3];
		return target == null ? null : new Object[]{target.block, target.kind | (meta & 12)};
	}

	private static boolean isAxe(ItemStack held) {
		if (held == null || held.getItem() == null) {
			return false;
		}
		Set<String> classes = held.getItem().getToolClasses(held);
		return classes != null && classes.contains("axe");
	}

	@SubscribeEvent
	public void interact(PlayerInteractEvent event) {
		if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		EntityPlayer player = event.entityPlayer;
		ItemStack held = player.getHeldItem();
		World world = event.world;
		if (!isAxe(held)) {
			return;
		}
		Object[] stripped = stripped(world.getBlock(event.x, event.y, event.z), world.getBlockMetadata(event.x, event.y, event.z));
		if (stripped == null) {
			return;
		}
		player.swingItem();
		if (world.isRemote) {
			return;
		}
		Block to = (Block) stripped[0];
		int meta = (Integer) stripped[1];
		world.setBlock(event.x, event.y, event.z, to, meta, 3);
		world.playSoundEffect(event.x + 0.5D, event.y + 0.5D, event.z + 0.5D, McFly.MC_ASSETS + ":item.axe.strip", 1.0F, 1.0F);
		GameEvents.postAtBlock(world, "block_change", event.x, event.y, event.z, player, to, meta);
		if (!player.capabilities.isCreativeMode) {
			held.damageItem(1, player);
			if (held.stackSize <= 0) {
				player.destroyCurrentEquippedItem();
			}
		}
		event.setCanceled(true);
	}
}
