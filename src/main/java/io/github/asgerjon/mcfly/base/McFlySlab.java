package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.BlockSlab;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A slab of ours (SPEC-modern-movement.md §84): 1.7.10's slab (meta 8 the top half) with its double, both looking like
 * their full block, the double dropping two of the single (26.3's loot `set_count` 2 for `type=double`). The single is
 * made first, its double from it.
 */
public class McFlySlab extends BlockSlab {

	private final Block full;
	private final String name;
	private final McFlySlab single;
	private McFlySlab doubled;

	/** The single slab, of this full block. */
	public McFlySlab(Block full, String name) {
		this(full, name, null);
	}

	/** The double of this single slab. */
	public McFlySlab(McFlySlab single) {
		this(single.full, single.name, single);
		single.doubled = this;
	}

	private McFlySlab(Block full, String name, McFlySlab single) {
		super(single != null, full.getMaterial());
		this.full = full;
		this.name = name;
		this.single = single != null ? single : this;
		// 26.3 has no double block: the double goes by the single's name, as its item does
		setBlockName(McFly.name(name));
		// the double takes its single's sound (set after it was made: mud bricks' on packed mud's id)
		setStepSound(single != null ? single.stepSound : full.stepSound);
		// the double takes its single's tool (set after it was made, for a slab of a vanilla block)
		Block toolOf = single != null ? single : full;
		setHarvestLevel(toolOf.getHarvestTool(0), toolOf.getHarvestLevel(0));
		// lit by its neighbours, as 1.7.10's own slabs are (Block.registerBlocks sets it for vanilla's alone): it stops
		// all light, so its own space reads 0 and a face inside it (a bottom slab's top) would draw dark
		this.useNeighborBrightness = true;
		if (single == null) {
			setCreativeTab(McFlyTab.TAB);
		}
	}

	/** 26.3's strength: 1.7.10's explosion resistance is kept as 26.3's number (1.7.10 divides the field by 5). */
	public McFlySlab strength(float hardness, float resistance) {
		setHardness(hardness);
		this.blockResistance = resistance * 5.0F;
		return this;
	}

	/** The tool and its level (a slab of a vanilla block: Forge gives vanilla's theirs only after ours are made). */
	public McFlySlab tool(String tool, int level) {
		setHarvestLevel(tool, level);
		return this;
	}

	/** Its own sound (a slab of a block of variants whose first isn't its own: mud bricks'). */
	public McFlySlab sound(SoundType sound) {
		setStepSound(sound);
		return this;
	}

	public McFlySlab single() {
		return single;
	}

	public McFlySlab doubled() {
		return doubled;
	}

	public boolean isDouble() {
		return field_150004_a;
	}

	@Override
	public String func_150002_b(int meta) {
		return single.getUnlocalizedName();
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(single);
	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(single, isDouble() ? 2 : 1, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(single);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return full.getIcon(side, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
	}

	@Override
	public MapColor getMapColor(int meta) {
		return full.getMapColor(0);
	}
}
