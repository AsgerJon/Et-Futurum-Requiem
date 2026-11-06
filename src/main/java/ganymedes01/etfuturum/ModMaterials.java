package ganymedes01.etfuturum;

import net.minecraft.item.ItemArmor;
import net.minecraftforge.common.util.EnumHelper;

public class ModMaterials {
	/**
	 * 26.3's `TURTLE_SCUTE`: 25 a slot's factor (275 uses for the helmet), the helmet 2 armour, enchantability 9.
	 * Mended with scutes by `ItemTurtleShell.getIsRepairable`. Netherite's are McFly's now (spec §88.4,
	 * `io.github.asgerjon.mcfly.nether.NetheriteMaterials`).
	 */
	public static final ItemArmor.ArmorMaterial TURTLE_ARMOUR = EnumHelper.addArmorMaterial("Turtle_Scute", 25, new int[]{2, 6, 5, 2}, 9);
}
