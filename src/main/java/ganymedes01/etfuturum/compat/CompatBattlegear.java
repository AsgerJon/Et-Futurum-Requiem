package ganymedes01.etfuturum.compat;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
import mods.battlegear2.api.quiver.IArrowFireHandler;
import mods.battlegear2.api.quiver.QuiverArrowRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class CompatBattlegear {
    public static void init() {
        boolean tippedArrowHandlerRegistered = QuiverArrowRegistry.addArrowFireHandler(new IArrowFireHandler() {
            @Override
            public boolean canFireArrow(ItemStack itemStack, World world, EntityPlayer entityPlayer, float charge) {
                return itemStack.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem;
            }

            @Override
            public EntityArrow getFiredArrow(ItemStack tippedArrow, World world, EntityPlayer player, float charge) {
                if(!(tippedArrow.getItem() instanceof io.github.asgerjon.mcfly.potion.TippedArrowItem)) return null;

                io.github.asgerjon.mcfly.potion.TippedArrow arrowEntity = new io.github.asgerjon.mcfly.potion.TippedArrow(world, player, charge);
                arrowEntity.setArrow(tippedArrow);
                return arrowEntity;
            }
        });

        if(tippedArrowHandlerRegistered) {
            QuiverArrowRegistry.addArrowToRegistry(McFlyItems.TIPPED_ARROW.get(), null);
        }
    }
}
