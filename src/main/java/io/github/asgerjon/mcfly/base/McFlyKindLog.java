package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;

/**
 * Logs of several kinds in one id (SPEC-modern-movement.md §91.3, step A): each kind one of 26.3's log blocks
 * (`RotatedPillarBlock` with `logProperties`), in 1.7.10's `BlockLog` form: meta & 3 the kind (read modulo the kinds,
 * as EFR did), meta & 12 the axis (0 y, 4 x, 8 z). 2.0, an axe, fire 5 and 5. A kind's map colour is its upright one
 * standing, its lying one on its side (26.3's by axis). Stripping is {@link io.github.asgerjon.mcfly.wood.Stripping}'s.
 */
public class McFlyKindLog extends BlockLog implements NamedMetas {

	/** A kind: 26.3's name, its map colours upright and lying, its side and end textures (a wood's end is its side). */
	public static final class Kind {
		final String name, side, end;
		final MapColor upright, lying;

		public Kind(String name, MapColor upright, MapColor lying, String side, String end) {
			this.name = name;
			this.upright = upright;
			this.lying = lying;
			this.side = side;
			this.end = end;
		}

		/** A wood: one colour, bark all round. */
		public static Kind wood(String name, MapColor colour, String bark) {
			return new Kind(name, colour, colour, bark, bark);
		}

		/** A log of one colour on either axis, its end `<side>_top`. */
		public static Kind log(String name, MapColor colour) {
			return new Kind(name, colour, colour, name, name + "_top");
		}
	}

	private final Kind[] kinds;

	public McFlyKindLog(String id, Block.SoundType sound, Kind... kinds) {
		this.kinds = kinds;
		setBlockName(McFly.name(id));
		setHardness(2.0F);
		this.blockResistance = 2.0F * 5.0F;
		setStepSound(sound);
		setHarvestLevel("axe", 0);
		setCreativeTab(McFlyTab.TAB);
	}

	private int kindOf(int meta) {
		return (meta & 3) % kinds.length;
	}

	public int kinds() {
		return kinds.length;
	}

	@Override
	public String nameOf(int meta) {
		return kinds[kindOf(meta)].name;
	}

	@Override
	public boolean isVariant(int meta) {
		return (meta & 12) == 0 && (meta & 3) < kinds.length;
	}

	@Override
	public int damageDropped(int meta) {
		return kindOf(meta);
	}

	@Override
	public MapColor getMapColor(int meta) {
		Kind kind = kinds[kindOf(meta)];
		return (meta & 12) == 0 ? kind.upright : kind.lying;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return true;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 5;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 5;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int i = 0; i < kinds.length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected IIcon getSideIcon(int meta) {
		return field_150167_a[kindOf(meta)];
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected IIcon getTopIcon(int meta) {
		return field_150166_b[kindOf(meta)];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		field_150167_a = new IIcon[kinds.length];
		field_150166_b = new IIcon[kinds.length];
		for (int i = 0; i < kinds.length; i++) {
			field_150167_a[i] = register.registerIcon(kinds[i].side);
			field_150166_b[i] = kinds[i].end.equals(kinds[i].side) ? field_150167_a[i] : register.registerIcon(kinds[i].end);
		}
		blockIcon = field_150167_a[0];
	}
}
