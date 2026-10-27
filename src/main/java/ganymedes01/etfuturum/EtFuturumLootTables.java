package ganymedes01.etfuturum;

import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.inventory.IInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

public class EtFuturumLootTables {

	private static final String COMPOSTER = "composting";
	public static final ChestGenHooks COMPOSTER_LOOT = ChestGenHooks.getInfo(COMPOSTER);

	public static void init() {
		ChestGenHooks.getInfo(COMPOSTER).setMin(1);
		ChestGenHooks.getInfo(COMPOSTER).setMax(1);
		COMPOSTER_LOOT.addItem(new WeightedRandomChestContent(new ItemStack(Items.dye, 1, 15), 1, 1, 10));
		//For some reason it is min count, max count and weight, yes weight is the last arg not the first....
	}

	/**
	 * A chest content that a chest holds at most once, as modern's bastion chests hold one upgrade template: a roll
	 * that would add a second gives nothing instead. 1.7.10 rolls every item of a chest independently.
	 */
	public static class OncePerChest extends WeightedRandomChestContent {

		public OncePerChest(Item item, int damage, int min, int max, int weight) {
			super(item, damage, min, max, weight);
		}

		@Override
		protected ItemStack[] generateChestContent(Random random, IInventory chest) {
			for (int i = 0; i < chest.getSizeInventory(); i++) {
				ItemStack stack = chest.getStackInSlot(i);
				if (stack != null && stack.getItem() == theItemId.getItem()) {
					return new ItemStack[0];
				}
			}
			return super.generateChestContent(random, chest);
		}
	}
}
