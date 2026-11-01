package ganymedes01.etfuturum.mixins.early.combat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A sword's material, which 1.7.10 keeps private; modern's sword damage is 3 plus its bonus (spec §46.2). */
@Mixin(ItemSword.class)
public interface AccessorItemSword {

	@Accessor("field_150933_b")
	Item.ToolMaterial etfu$getMaterial();
}
