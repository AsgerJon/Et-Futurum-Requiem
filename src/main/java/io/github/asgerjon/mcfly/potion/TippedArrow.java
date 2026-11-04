package io.github.asgerjon.mcfly.potion;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import ganymedes01.etfuturum.entities.ITotemUser;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.io.IOException;

/**
 * A tipped arrow in flight (SPEC-modern-movement.md §82; 26.3's `Arrow` with potion contents): an arrow carrying its
 * item (the effects are the item's, an eighth as long), giving them after its hit lands (`tippedarrows.MixinEntityArrow`,
 * spec §46.10), trailing the potion's colour (2 specks a tick in flight, 1 every 5 in the ground), picked up as its item.
 * Saved as EFR saved it (`Potion`, or the old `potionValue` damage).
 */
public class TippedArrow extends EntityArrow implements IEntityAdditionalSpawnData {

	private ItemStack arrow;

	public TippedArrow(World world) {
		super(world);
	}

	public TippedArrow(World world, EntityLivingBase shooter, float speed) {
		super(world, shooter, speed);
	}

	public TippedArrow(World world, double x, double y, double z) {
		super(world, x, y, z);
	}

	public TippedArrow(World world, EntityLivingBase shooter, EntityLivingBase target, float speed, float spread) {
		super(world, shooter, target, speed, spread);
	}

	public void setArrow(ItemStack arrow) {
		this.arrow = arrow == null ? null : TippedArrowItem.one(arrow);
	}

	public ItemStack getArrow() {
		return arrow;
	}

	/**
	 * Its effects on what it has just hurt (spec §46.10, 26.3's doPostHurtEffects): after the hit, instant ones as their
	 * own hit; from no one when it had no shooter. With `arrowHarmingAfterTotemFix` no Harming follows a hit that has
	 * just used up a totem (MC-206307).
	 */
	public void applyEffectsTo(EntityLivingBase target) {
		if (arrow == null || !(arrow.getItem() instanceof TippedArrowItem item)) {
			return;
		}
		boolean totemJustUsed = McFlyConfig.arrowHarmingAfterTotemFix && target instanceof ITotemUser user && user.etfu$usedTotemThisTick();
		EntityLivingBase shooter = shootingEntity instanceof EntityLivingBase living ? living : null;
		for (PotionEffect effect : item.getEffects(arrow)) {
			Potion potion = Potion.potionTypes[effect.getPotionID()];
			if (totemJustUsed && potion == Potion.harm) {
				continue;
			}
			if (potion.isInstant()) {
				potion.affectEntity(shooter, target, effect.getAmplifier(), 1.0D);
			} else {
				target.addPotionEffect(new PotionEffect(effect.getPotionID(), effect.getDuration(), effect.getAmplifier()));
			}
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (worldObj.isRemote && arrow != null) {
			if (inGround) {
				if (ticksInGround % 5 == 0) {
					specks(1);
				}
			} else {
				specks(2);
			}
		}
	}

	private void specks(int count) {
		int colour = arrow.getItem().getColorFromItemStack(arrow, 0);
		for (int i = 0; i < count; i++) {
			worldObj.spawnParticle("mobSpell", posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height - yOffset,
				posZ + (rand.nextDouble() - 0.5) * width, (colour >> 16 & 0xFF) / 255.0, (colour >> 8 & 0xFF) / 255.0, (colour & 0xFF) / 255.0);
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		if (tag.hasKey("Potion", 10)) {
			arrow = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Potion"));
		} else if (McFlyItems.TIPPED_ARROW.isEnabled()) {
			arrow = McFlyItems.TIPPED_ARROW.newItemStack(1, tag.getInteger("potionValue"));
		}
		if (arrow == null) {
			setDead();
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		if (arrow != null) {
			tag.setTag("Potion", arrow.writeToNBT(new NBTTagCompound()));
		}
	}

	/** Picked up as its tipped arrow (in creative, when shot in creative, nothing to pick up). */
	@Override
	public void onCollideWithPlayer(EntityPlayer player) {
		if (worldObj.isRemote || !inGround || arrowShake > 0 || arrow == null) {
			return;
		}
		boolean take = canBePickedUp == 1 || canBePickedUp == 2 && player.capabilities.isCreativeMode;
		if (canBePickedUp == 1 && !player.inventory.addItemStackToInventory(TippedArrowItem.one(arrow))) {
			take = false;
		}
		if (take) {
			playSound("random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			player.onItemPickup(this, 1);
			setDead();
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeFloat(rotationYaw);
		buffer.writeInt(shootingEntity == null ? getEntityId() : shootingEntity.getEntityId());
		buffer.writeDouble(motionX);
		buffer.writeDouble(motionY);
		buffer.writeDouble(motionZ);
		try {
			new PacketBuffer(buffer).writeItemStackToBuffer(arrow);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		rotationYaw = buffer.readFloat();
		shootingEntity = worldObj.getEntityByID(buffer.readInt());
		motionX = buffer.readDouble();
		motionY = buffer.readDouble();
		motionZ = buffer.readDouble();
		// 1.7.10's arrow starts a little in front of and below its shooter's eyes
		posX -= MathHelper.cos(rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
		posY -= 0.1F;
		posZ -= MathHelper.sin(rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
		try {
			arrow = new PacketBuffer(buffer).readItemStackFromBuffer();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
