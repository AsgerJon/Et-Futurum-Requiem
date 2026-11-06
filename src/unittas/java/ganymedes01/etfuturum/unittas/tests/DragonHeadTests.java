package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ducks.IDragonHead;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.3's dragon head (SPEC-modern-movement.md §76.12, 7d) as 1.7.10's sixth skull: its item (damage 5) is named and
 * listed, placing it makes a skull of type 5 that drops it again, and its jaw works only while powered (on the client,
 * as 26.3's).
 */
@TasSuite("end")
public class DragonHeadTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off (the dragon head comes with it)");
		}
	}

	public void testItem() {
		String found = onServer(() -> {
			ItemStack head = new ItemStack(Items.skull, 1, 5);
			List<ItemStack> list = new ArrayList<>();
			Items.skull.getSubItems(Items.skull, null, list);
			boolean listed = false;
			for (ItemStack stack : list) {
				listed |= stack.getItemDamage() == 5;
			}
			return head.getDisplayName() + ", listed " + listed;
		});
		assertEqual("Dragon Head, listed true", found, "the dragon head's item");
	}

	public void testPlacedAndDropped() {
		String found = onServer(() -> {
			int x = (int) Math.floor(player().posX) + 2;
			int y = (int) Math.floor(player().posY);
			int z = (int) Math.floor(player().posZ);
			ItemStack head = new ItemStack(Items.skull, 1, 5);
			head.getItem().onItemUse(head, player(), player().worldObj, x, y - 1, z, 1, 0.5F, 1.0F, 0.5F);
			TileEntity entity = player().worldObj.getTileEntity(x, y, z);
			int type = entity instanceof TileEntitySkull skull ? skull.func_145904_a() : -1;
			List<ItemStack> drops = Blocks.skull.getDrops(player().worldObj, x, y, z, player().worldObj.getBlockMetadata(x, y, z), 0);
			player().worldObj.setBlockToAir(x, y, z);
			return "type " + type + ", drops " + (drops.isEmpty() ? "none" : drops.get(0).getItemDamage());
		});
		assertEqual("type 5, drops 5", found, "a placed dragon head");
	}

	/** Powered, the client's head counts ticks (its jaw moves); unpowered it stops. */
	public void testJawWhilePowered() {
		int[] at = onServer(() -> {
			int x = (int) Math.floor(player().posX) + 3;
			int y = (int) Math.floor(player().posY);
			int z = (int) Math.floor(player().posZ) + 1;
			player().worldObj.setBlock(x, y, z, Blocks.skull, 1, 3);
			((TileEntitySkull) player().worldObj.getTileEntity(x, y, z)).func_152107_a(5);
			player().worldObj.markBlockForUpdate(x, y, z);
			return new int[] {x, y, z};
		});
		ticks(20);
		float still = onClient(() -> ((IDragonHead) mc().theWorld.getTileEntity(at[0], at[1], at[2])).etfu$animation(0.0F));
		onServer(() -> player().worldObj.setBlock(at[0] + 1, at[1], at[2], Blocks.redstone_block));
		ticks(20);
		float moving = onClient(() -> ((IDragonHead) mc().theWorld.getTileEntity(at[0], at[1], at[2])).etfu$animation(0.0F));
		onServer(() -> {
			player().worldObj.setBlockToAir(at[0] + 1, at[1], at[2]);
			player().worldObj.setBlockToAir(at[0], at[1], at[2]);
		});
		assertTrue(still == 0.0F && moving >= 10.0F, "the jaw's ticks: unpowered " + still + ", powered 20 ticks " + moving);
	}
}
