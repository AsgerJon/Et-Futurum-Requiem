package io.github.asgerjon.mcfly.base;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

/**
 * 26.3's `BlockSetType` (SPEC-modern-movement.md §91.6, step D): what a set's doors, trapdoors, buttons and pressure
 * plates share. Whether a hand opens its doors and trapdoors, whether a wind burst does, whether arrows press its buttons,
 * what its plates sense, its sound type, and its sounds (`<door>.open`, `.close`; a plate's and a button's `.click_on`,
 * `.click_off`).
 */
public final class McFlySetType {

	public final String name;
	public final boolean byHand;
	public final boolean byWind;
	public final boolean byArrows;
	/** Its plates sense everything (wood's, iron's); else mobs alone (stone's). */
	public final boolean plateSensesAll;
	public final Block.SoundType sound;
	private final String door, trapdoor, plate, button;

	private McFlySetType(String name, boolean byHand, boolean byWind, boolean byArrows, boolean plateSensesAll, Block.SoundType sound, String door, String trapdoor,
						 String plate, String button) {
		this.name = name;
		this.byHand = byHand;
		this.byWind = byWind;
		this.byArrows = byArrows;
		this.plateSensesAll = plateSensesAll;
		this.sound = sound;
		this.door = door;
		this.trapdoor = trapdoor;
		this.plate = plate;
		this.button = button;
	}

	private static McFlySetType wood(String name, Block.SoundType sound, String prefix) {
		return new McFlySetType(name, true, true, true, true, sound, "block." + prefix + "_door", "block." + prefix + "_trapdoor", "block." + prefix + "_pressure_plate",
			"block." + prefix + "_button");
	}

	/** The vanilla woods', mangrove's, the mods' woods' (26.3's `BlockSetType(name)`: oak's sounds). */
	public static final McFlySetType WOOD = new McFlySetType("oak", true, true, true, true, Block.soundTypeWood, "block.wooden_door", "block.wooden_trapdoor",
		"block.wooden_pressure_plate", "block.wooden_button");
	public static final McFlySetType NETHER_WOOD = wood("crimson", McFlySoundType.NETHER_WOOD, "nether_wood");
	public static final McFlySetType CHERRY = wood("cherry", McFlySoundType.CHERRY_WOOD, "cherry_wood");
	public static final McFlySetType BAMBOO = wood("bamboo", McFlySoundType.BAMBOO_WOOD, "bamboo_wood");
	public static final McFlySetType IRON = new McFlySetType("iron", false, false, false, true, McFlySoundType.IRON, "block.iron_door", "block.iron_trapdoor",
		"block.metal_pressure_plate", "block.stone_button");
	public static final McFlySetType COPPER = new McFlySetType("copper", true, true, false, true, McFlySoundType.COPPER, "block.copper_door",
		"block.copper_trapdoor", "block.metal_pressure_plate", "block.stone_button");
	public static final McFlySetType STONE = new McFlySetType("stone", true, true, false, false, Block.soundTypePiston, "block.iron_door", "block.iron_trapdoor",
		"block.stone_pressure_plate", "block.stone_button");

	/** A planks' wood's set (crimson 0 to bamboo 4). */
	public static McFlySetType ofWood(int wood) {
		return wood < 2 ? NETHER_WOOD : wood == 3 ? CHERRY : wood == 4 ? BAMBOO : WOOD;
	}

	/**
	 * A block's set: ours by its own; another button by its time (1.7.10's wooden 30 ticks, stone 20); another block by
	 * its material (iron's, stone's, else wood's).
	 */
	public static McFlySetType of(Block block) {
		if (block instanceof Typed typed) {
			return typed.setType();
		}
		if (block instanceof net.minecraft.block.BlockButton) {
			return block.tickRate(null) == 30 ? WOOD : STONE;
		}
		return block.getMaterial() == Material.iron ? IRON : block.getMaterial() == Material.rock ? STONE : WOOD;
	}

	/** A block of a set. */
	public interface Typed {
		McFlySetType setType();
	}

	private static void play(World world, int x, int y, int z, String event) {
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":" + event, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
	}

	/** 26.3's door sound by the new state, pitch 0.9 to 1.0 (on the server: everyone near hears it). */
	public void playDoor(World world, int x, int y, int z, boolean opened) {
		play(world, x, y, z, door + (opened ? ".open" : ".close"));
	}

	public void playTrapdoor(World world, int x, int y, int z, boolean opened) {
		play(world, x, y, z, trapdoor + (opened ? ".open" : ".close"));
	}

	public String plateSound(boolean on) {
		return plate + (on ? ".click_on" : ".click_off");
	}

	public String buttonSound(boolean on) {
		return button + (on ? ".click_on" : ".click_off");
	}
}
