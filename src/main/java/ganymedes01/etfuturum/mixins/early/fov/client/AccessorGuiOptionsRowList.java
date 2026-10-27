package ganymedes01.etfuturum.mixins.early.fov.client;

import net.minecraft.client.gui.GuiOptionsRowList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GuiOptionsRowList.class)
public interface AccessorGuiOptionsRowList {
	@Accessor("field_148184_k")
	List<GuiOptionsRowList.Row> etfu$rows();
}
