package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.nether.NetherLightItem;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.block.material.MapColor;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.Supplier;

/**
 * A wood's sign (SPEC-modern-movement.md 91.8, step F; 26.3's `StandingSignBlock`, `WallSignBlock`), standing or on a
 * wall as 1.7.10's `BlockSign` (whose behaviour McFly's mixins make 26.3's for vanilla's oak sign too): 1.0, the wood's
 * sound and map colour, its block texture (26.3's `block/<wood>_sign`), the editor's picture and its item. Made in pairs
 * (`pair`): each knows the other.
 */
public class McFlySign extends BlockSign implements NetherLightItem.Flat {

	public final boolean standing;
	/** `minecraft:spruce_sign`, `biomesoplenty:sacredoak_sign`: the block texture and the item's. */
	public final String blockTexture;
	public final String itemTexture;
	public final String guiTexture;
	private final MapColor colour;
	private final Supplier<Block> planks;
	private final int planksMeta;
	private Supplier<Item> item;
	private McFlySign other;

	/**
	 * @param name        the lang name, `spruce_sign` (both of a pair)
	 * @param domain      the textures' domain (`minecraft`, a mod's)
	 * @param wood        the textures' wood name (`spruce`, `sacredoak`)
	 */
	public McFlySign(String name, boolean standing, String domain, String wood, Block.SoundType sound, MapColor colour, Supplier<Block> planks, int planksMeta) {
		super(Signs.Tile.class, standing);
		this.standing = standing;
		this.blockTexture = domain + ":" + wood + "_sign";
		this.itemTexture = domain + ":" + wood + "_sign";
		this.guiTexture = domain + ":textures/gui/signs/" + wood + ".png";
		this.colour = colour;
		this.planks = planks;
		this.planksMeta = planksMeta;
		setHardness(1.0F);
		this.blockResistance = 1.0F * 5.0F;
		setStepSound(sound);
		setBlockName(McFly.name(name));
		disableStats();
	}

	/** Links a standing sign and its wall sign, and the item both drop. */
	public static void pair(McFlySign standing, McFlySign wall, Supplier<Item> item) {
		standing.other = wall;
		wall.other = standing;
		standing.item = item;
		wall.item = item;
	}

	public McFlySign standingSign() {
		return standing ? this : other;
	}

	public McFlySign wallSign() {
		return standing ? other : this;
	}

	public Item signItem() {
		return item == null ? null : item.get();
	}

	@Override
	public String itemTexture() {
		return itemTexture;
	}

	/** Its particles are its planks' (26.3's models' `particle`). */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return planks.get().getIcon(side, planksMeta);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return colour;
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return signItem();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return signItem();
	}

	@Override
	public String getItemIconName() {
		return itemTexture;
	}
}
