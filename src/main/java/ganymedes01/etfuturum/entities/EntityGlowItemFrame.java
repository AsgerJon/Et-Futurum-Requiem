package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import ganymedes01.etfuturum.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * The glow item frame (SPEC-modern-movement.md §67.4, 26.3's `GlowItemFrame`): an item frame whose item shows at full
 * light (its renderer), dropping itself when broken. A mod entity, so FML spawns it on clients, with the block it hangs
 * on and its facing as spawn data (1.7.10's frame packet, type 71, would make a plain frame there).
 */
public class EntityGlowItemFrame extends EntityItemFrame implements IEntityAdditionalSpawnData {

	public EntityGlowItemFrame(World world) {
		super(world);
	}

	public EntityGlowItemFrame(World world, int x, int y, int z, int direction) {
		super(world, x, y, z, direction);
	}

	/** 1.7.10's `func_146065_b` without its plain frame: the glow frame instead (not for a creative player). */
	@Override
	public void onBroken(Entity breaker) {
		if (!(breaker instanceof EntityPlayer player && player.capabilities.isCreativeMode) && McFlyItems.GLOW_ITEM_FRAME.isEnabled()) {
			entityDropItem(McFlyItems.GLOW_ITEM_FRAME.newItemStack(), 0.0F);
		}
		func_146065_b(breaker, false);
	}

	@Override
	public ItemStack getPickedResult(net.minecraft.util.MovingObjectPosition target) {
		ItemStack shown = getDisplayedItem();
		return shown != null ? shown.copy() : McFlyItems.GLOW_ITEM_FRAME.isEnabled() ? McFlyItems.GLOW_ITEM_FRAME.newItemStack() : null;
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeInt(field_146063_b);
		buffer.writeInt(field_146064_c);
		buffer.writeInt(field_146062_d);
		buffer.writeByte(hangingDirection);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		field_146063_b = buffer.readInt();
		field_146064_c = buffer.readInt();
		field_146062_d = buffer.readInt();
		setDirection(buffer.readByte());
	}
}
