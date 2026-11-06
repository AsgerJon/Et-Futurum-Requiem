package ganymedes01.etfuturum.world.structure.modern;

import io.github.asgerjon.mcfly.banner.BannerBlockEntity;
import io.github.asgerjon.mcfly.banner.BannerPatterns;
import io.github.asgerjon.mcfly.banner.BannerPattern;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 26.3's block states as 1.7.10's blocks (SPEC-modern-movement.md §76.12): by name, the blocks the structures here
 * use; their block entities from 26.3's data. One it doesn't know goes in as air, logged once.
 */
public final class ModernBlockStates {

	private static final Logger LOG = LogManager.getLogger("etfuturum");
	private static final Set<String> UNKNOWN = new HashSet<>();
	/** 26.3's colours in wool order (1.7.10's wool and glass metadata). */
	private static final String[] COLOURS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
		"purple", "blue", "brown", "green", "red", "black"};

	private ModernBlockStates() {
	}

	/** A block and its metadata. */
	public static final class Placed {
		public final Block block;
		public final int meta;

		Placed(Block block, int meta) {
			this.block = block;
			this.meta = meta;
		}
	}

	/** The wool metadata of a 26.3 colour, or -1. */
	public static int woolMeta(String colour) {
		for (int i = 0; i < COLOURS.length; i++) {
			if (COLOURS[i].equals(colour)) {
				return i;
			}
		}
		return -1;
	}

	/** 1.7.10's chest, ladder, wall sign and wall banner metadata for a horizontal facing. */
	private static int wallMeta(String facing) {
		return switch (facing) {
			case "south" -> 3;
			case "west" -> 4;
			case "east" -> 5;
			default -> 2;
		};
	}

	/** ForgeDirection's order (EFR's end rod): down, up, north, south, west, east. */
	private static int directionMeta(String facing) {
		return switch (facing) {
			case "down" -> 0;
			case "up" -> 1;
			case "north" -> 2;
			case "south" -> 3;
			case "west" -> 4;
			default -> 5;
		};
	}

	/** 1.7.10's stairs: facing east 0, west 1, south 2, north 3; upside down 4 more. */
	private static int stairsMeta(ModernState state) {
		int meta = switch (state.get("facing", "east")) {
			case "west" -> 1;
			case "south" -> 2;
			case "north" -> 3;
			default -> 0;
		};
		return "top".equals(state.get("half")) ? meta | 4 : meta;
	}

	/** 1.7.10's pillars (BlockRotatedPillar): y 0, x 4, z 8. */
	private static int axisMeta(ModernState state) {
		return switch (state.get("axis", "y")) {
			case "x" -> 4;
			case "z" -> 8;
			default -> 0;
		};
	}

	/** The state as 1.7.10's block and metadata, or null if there's none here. */
	public static Placed convert(ModernState state) {
		String id = state.id;
		if (state.isAir()) {
			return new Placed(Blocks.air, 0);
		}
		switch (id) {
			case "minecraft:purpur_block":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_BLOCK.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_BLOCK.isEnabled(), 0);
			case "minecraft:purpur_pillar":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_PILLAR.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_PILLAR.isEnabled(), axisMeta(state));
			case "minecraft:purpur_stairs":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_STAIRS.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_STAIRS.isEnabled(), stairsMeta(state));
			case "minecraft:purpur_slab":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_SLAB.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_SLAB.isEnabled(), "top".equals(state.get("type")) ? 8 : 0);
			case "minecraft:end_stone_bricks":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.END_BRICKS.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.END_BRICKS.isEnabled(), 0);
			case "minecraft:end_rod":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.isEnabled(), directionMeta(state.get("facing", "up")));
			case "minecraft:end_stone":
				return new Placed(Blocks.end_stone, 0);
			case "minecraft:chorus_plant":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.isEnabled(), 0);
			case "minecraft:chorus_flower":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.isEnabled(), Integer.parseInt(state.get("age", "0")));
			case "minecraft:end_gateway":
				return enabled(io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.get(), io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.isEnabled(), 0);
			case "minecraft:obsidian":
				return new Placed(Blocks.obsidian, 0);
			case "minecraft:bedrock":
				return new Placed(Blocks.bedrock, 0);
			case "minecraft:ladder":
				return new Placed(Blocks.ladder, wallMeta(state.get("facing", "north")));
			case "minecraft:chest":
				return new Placed(Blocks.chest, wallMeta(state.get("facing", "north")));
			case "minecraft:ender_chest":
				return new Placed(Blocks.ender_chest, wallMeta(state.get("facing", "north")));
			case "minecraft:brewing_stand":
				return new Placed(Blocks.brewing_stand, ("true".equals(state.get("has_bottle_0")) ? 1 : 0) | ("true".equals(state.get("has_bottle_1")) ? 2 : 0)
					| ("true".equals(state.get("has_bottle_2")) ? 4 : 0));
			case "minecraft:dragon_wall_head":
			case "minecraft:skeleton_wall_skull":
			case "minecraft:wither_skeleton_wall_skull":
			case "minecraft:zombie_wall_head":
			case "minecraft:player_wall_head":
			case "minecraft:creeper_wall_head":
				return new Placed(Blocks.skull, wallMeta(state.get("facing", "north")));
			case "minecraft:dragon_head":
			case "minecraft:skeleton_skull":
			case "minecraft:wither_skeleton_skull":
			case "minecraft:zombie_head":
			case "minecraft:player_head":
			case "minecraft:creeper_head":
				return new Placed(Blocks.skull, 1);
			default:
				break;
		}
		if (id.endsWith("_stained_glass")) {
			int colour = woolMeta(id.substring("minecraft:".length(), id.length() - "_stained_glass".length()));
			return colour < 0 ? null : new Placed(Blocks.stained_glass, colour);
		}
		if (id.endsWith("_wall_banner")) {
			return McFlyBlocks.BANNER.isEnabled() ? new Placed(McFlyBlocks.BANNER.get(), wallMeta(state.get("facing", "north"))) : null;
		}
		if (id.endsWith("_banner")) {
			return McFlyBlocks.BANNER.isEnabled() ? new Placed(McFlyBlocks.BANNER.get(), Integer.parseInt(state.get("rotation", "0"))) : null;
		}
		return null;
	}

	private static Placed enabled(Block block, boolean enabled, int meta) {
		return enabled ? new Placed(block, meta) : null;
	}

	/** 26.3's skull types as 1.7.10's TileEntitySkull's (dragon 5: §76.12's 7d). */
	private static int skullType(String id) {
		if (id.contains("wither_skeleton")) {
			return 1;
		}
		if (id.contains("zombie")) {
			return 2;
		}
		if (id.contains("player")) {
			return 3;
		}
		if (id.contains("creeper")) {
			return 4;
		}
		if (id.contains("dragon")) {
			return 5;
		}
		return 0;
	}

	/**
	 * The state at the place (flag 2), its metadata set again after (some blocks turn themselves as they're added), its
	 * block entity filled from 26.3's data. A container's loot seed is drawn first, as 26.3's template draws it.
	 */
	public static boolean place(World world, int x, int y, int z, ModernState state, NBTTagCompound nbt, Random random) {
		Placed placed = convert(state);
		if (placed == null) {
			if (UNKNOWN.add(state.id)) {
				LOG.warn("No 1.7.10 block for 26.3's {}: left as air", state.id);
			}
			placed = new Placed(Blocks.air, 0);
		}
		if (!world.setBlock(x, y, z, placed.block, placed.meta, 2)) {
			return false;
		}
		if (world.getBlockMetadata(x, y, z) != placed.meta) {
			world.setBlockMetadataWithNotify(x, y, z, placed.meta, 2);
		}
		if (nbt != null) {
			if (placed.block == Blocks.chest) {
				// 26.3's RandomizableContainer gets a LootTableSeed as it's placed (its table comes from a marker)
				random.nextLong();
			}
			loadBlockEntity(world.getTileEntity(x, y, z), state, nbt);
		}
		return true;
	}

	private static void loadBlockEntity(TileEntity entity, ModernState state, NBTTagCompound nbt) {
		if (entity instanceof BannerBlockEntity banner) {
			NBTTagCompound tag = new NBTTagCompound();
			banner.writeToNBT(tag);
			String colour = state.id.substring("minecraft:".length(), state.id.indexOf(state.id.endsWith("_wall_banner") ? "_wall_banner" : "_banner"));
			tag.setInteger("Base", 15 - Math.max(0, woolMeta(colour)));
			tag.setBoolean("IsStanding", !state.id.endsWith("_wall_banner"));
			NBTTagList patterns = new NBTTagList();
			NBTTagList modern = nbt.getTagList("patterns", 10);
			for (int i = 0; i < modern.tagCount(); i++) {
				NBTTagCompound layer = modern.getCompoundTagAt(i);
				String name = layer.getString("pattern");
				BannerPattern pattern = BannerPatterns.byName(name.contains(":") ? name.substring(name.indexOf(':') + 1) : name);
				if (pattern != null) {
					NBTTagCompound old = new NBTTagCompound();
					old.setString("Pattern", pattern.shortId());
					old.setInteger("Color", 15 - Math.max(0, woolMeta(layer.getString("color"))));
					patterns.appendTag(old);
				}
			}
			tag.setTag("Patterns", patterns);
			banner.readFromNBT(tag);
		} else if (entity instanceof TileEntityBrewingStand stand) {
			NBTTagList items = nbt.getTagList("Items", 10);
			for (int i = 0; i < items.tagCount(); i++) {
				NBTTagCompound item = items.getCompoundTagAt(i);
				int slot = item.getByte("Slot") & 255;
				ItemStack stack = potion(item);
				if (stack != null && slot < 3) {
					stand.setInventorySlotContents(slot, stack);
				}
			}
		} else if (entity instanceof TileEntitySkull skull) {
			skull.func_152107_a(skullType(state.id));
		}
	}

	/** A 26.3 potion item as 1.7.10's (by its named potion's damage), or null. */
	private static ItemStack potion(NBTTagCompound item) {
		if (!"minecraft:potion".equals(item.getString("id"))) {
			return null;
		}
		String name = item.getCompoundTag("components").getCompoundTag("minecraft:potion_contents").getString("potion");
		NamedPotion potion = NamedPotions.byId(name.contains(":") ? name.substring(name.indexOf(':') + 1) : name);
		return potion == null || potion.legacyDamage() <= 0 ? null : new ItemStack(Items.potionitem, Math.max(1, item.getInteger("count")), potion.legacyDamage());
	}
}
