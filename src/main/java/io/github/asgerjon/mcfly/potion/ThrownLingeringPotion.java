package io.github.asgerjon.mcfly.potion;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import ganymedes01.etfuturum.entities.EntityAreaEffectCloud;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.potion.ThrownPotions;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A thrown lingering potion (SPEC-modern-movement.md §82; 26.3's `ThrownLingeringPotion`): where it lands it splashes
 * and leaves 26.3's area effect cloud of its effects (radius 3, waiting 10 ticks, 600 ticks long, shrinking to nothing
 * over them and by 0.5 a use, the effects given at a quarter), in its colour; a water one puts out fire instead.
 * Saved with its potion as EFR saved it (`Potion`).
 */
public class ThrownLingeringPotion extends EntityPotion implements IEntityAdditionalSpawnData {

	private ItemStack potion;

	public ThrownLingeringPotion(World world) {
		super(world);
	}

	public ThrownLingeringPotion(World world, EntityLivingBase thrower, ItemStack potion) {
		super(world, thrower, potion);
		this.potion = potion;
	}

	public ThrownLingeringPotion(World world, double x, double y, double z, ItemStack potion) {
		super(world, x, y, z, potion);
		this.potion = potion;
	}

	public ItemStack potion() {
		return potion;
	}

	/** The potion's effects as drinking it gives them (the cloud gives them at a quarter). */
	private List<PotionEffect> fullEffects() {
		if (McFlyConfig.potionsByName && NamedPotions.isReady()) {
			PotionContents contents = PotionContents.of(potion);
			if (contents.potion() != null) {
				return contents.allEffects();
			}
		}
		if (potion.hasTagCompound() && potion.getTagCompound().hasKey("CustomPotionEffects", 9)) {
			List<PotionEffect> effects = new ArrayList<>();
			NBTTagList list = potion.getTagCompound().getTagList("CustomPotionEffects", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
				if (effect != null) {
					effects.add(effect);
				}
			}
			return effects;
		}
		@SuppressWarnings("unchecked")
		List<PotionEffect> byDamage = Items.potionitem.getEffects(potion.getItemDamage());
		return byDamage == null ? new ArrayList<>() : byDamage;
	}

	private int colour() {
		PotionContents contents = PotionContents.of(potion);
		if (McFlyConfig.potionsByName && NamedPotions.isReady() && (contents.potion() != null || contents.customColor() != null)) {
			return contents.color();
		}
		return PotionHelper.func_77915_a(potion.getItemDamage(), false);
	}

	@Override
	protected void onImpact(MovingObjectPosition hit) {
		if (worldObj.isRemote) {
			return;
		}
		if (potion != null) {
			List<PotionEffect> effects = fullEffects();
			if (effects.isEmpty()) {
				putOutFire(hit);
			} else {
				EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(worldObj, posX, posY, posZ);
				if (getThrower() != null) {
					cloud.setOwner(getThrower());
				}
				cloud.setRadius(3.0F);
				cloud.setRadiusOnUse(-0.5F);
				cloud.setDuration(600);
				cloud.setWaitTime(10);
				cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
				cloud.setPotionDurationScale(0.25F);
				for (PotionEffect effect : effects) {
					cloud.addEffect(new PotionEffect(effect));
				}
				cloud.setColour(colour());
				worldObj.spawnEntityInWorld(cloud);
			}
			worldObj.playAuxSFX(2002, (int) Math.round(posX), (int) Math.round(posY), (int) Math.round(posZ), ThrownPotions.splashData(potion));
		}
		setDead();
	}

	/** 26.3's water bottle: the fire where it lands and beside it goes out, and burning things near it stop burning. */
	private void putOutFire(MovingObjectPosition hit) {
		int x = hit.blockX;
		int y = hit.blockY;
		int z = hit.blockZ;
		if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			x += new int[]{0, 0, 0, 0, -1, 1}[hit.sideHit];
			y += new int[]{-1, 1, 0, 0, 0, 0}[hit.sideHit];
			z += new int[]{0, 0, -1, 1, 0, 0}[hit.sideHit];
		} else {
			x = MathHelper.floor_double(posX);
			y = MathHelper.floor_double(posY);
			z = MathHelper.floor_double(posZ);
		}
		int[][] around = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int[] at : around) {
			if (worldObj.getBlock(x + at[0], y + at[1], z + at[2]) == Blocks.fire) {
				worldObj.setBlockToAir(x + at[0], y + at[1], z + at[2]);
			}
		}
		for (Object found : worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(posX - 4, posY - 2, posZ - 4, posX + 4, posY + 2, posZ + 4))) {
			Entity entity = (Entity) found;
			if (entity.isBurning() && getDistanceSqToEntity(entity) < 16.0) {
				entity.extinguish();
			}
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		ByteBufUtils.writeItemStack(buffer, potion);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		potion = ByteBufUtils.readItemStack(buffer);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		potion = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("Potion"));
		if (potion == null) {
			setDead();
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		if (potion != null) {
			nbt.setTag("Potion", potion.writeToNBT(new NBTTagCompound()));
		}
	}
}
