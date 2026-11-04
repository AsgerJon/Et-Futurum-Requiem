package ganymedes01.etfuturum.items;

import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class ItemSuspiciousStew extends ItemSoup {

	public static final String effectsList = "Effects";
	/** An old stew's effect number; stews are written by name now ({@link #stewEffectName}). */
	public static final String stewEffect = "EffectId";
	/** 26.3's name of the effect in a stew's entry. */
	public static final String stewEffectName = "id";
	public static final String stewEffectDuration = "EffectDuration";
	public static final String stewEffectLevel = "EffectLevel";
	//The potion ID should be set as a byte
	//The potion duration should be set as an int
	//The potion level should be set as a byte

	public ItemSuspiciousStew() {
		super(6);
		setTextureName("suspicious_stew");
		setUnlocalizedName(Utils.getUnlocalisedName("suspicious_stew"));
		setCreativeTab(null);
		setAlwaysEdible();
	}

	@Override
	public ItemStack onEaten(ItemStack p_77654_1_, World p_77654_2_, EntityPlayer p_77654_3_) {
		applyPotionEffects(p_77654_1_, p_77654_3_);
		return super.onEaten(p_77654_1_, p_77654_2_, p_77654_3_);
	}

	public void applyPotionEffects(ItemStack stack, EntityPlayer player) {
		if (hasPotion(stack) && player != null) {
			NBTTagCompound nbtBase = stack.getTagCompound();
			if (nbtBase.hasKey(effectsList)) {
				NBTTagList list = (NBTTagList) nbtBase.getTag(effectsList);
				for (int i = 0; i < list.tagCount(); i++) {
					NBTTagCompound nbt = list.getCompoundTagAt(i);
					Potion effect = effectOf(nbt);
					if (effect == null)
						continue;
					int effectDuration = 160;
					if (nbt.hasKey(stewEffectDuration)) {
						effectDuration = nbt.getInteger(stewEffectDuration);
					}
					effectDuration = effectDuration > 1 ? effectDuration : 1;
					player.addPotionEffect(new PotionEffect(effect.id, effectDuration, nbt.getByte(stewEffectLevel)));
				}
			}
		}
	}

	/** Adds an effect to the stew, by its name (26.3's `suspicious_stew_effects`: {id, duration}; spec §70, stage B). */
	public static void addEffect(ItemStack stew, Potion potion, int duration) {
		if (!stew.hasTagCompound()) {
			stew.setTagCompound(new NBTTagCompound());
		}
		if (!stew.getTagCompound().hasKey(effectsList, 9)) {
			stew.getTagCompound().setTag(effectsList, new NBTTagList());
		}
		NBTTagCompound entry = new NBTTagCompound();
		String name = ganymedes01.etfuturum.potion.EffectRegistry.key(potion);
		if (name != null) {
			entry.setString(stewEffectName, name);
		} else {
			entry.setByte(stewEffect, (byte) potion.id);
		}
		entry.setInteger(stewEffectDuration, duration);
		stew.getTagCompound().getTagList(effectsList, 10).appendTag(entry);
	}

	/** The effect of one of the stew's entries, or null: by its name, or an old stew's number. */
	public static Potion effectOf(NBTTagCompound entry) {
		if (entry.hasKey(stewEffectName, 8)) {
			return ganymedes01.etfuturum.potion.EffectRegistry.get(entry.getString(stewEffectName));
		}
		int id = entry.getByte(stewEffect);
		return id > 0 && id < Potion.potionTypes.length ? Potion.potionTypes[id] : null;
	}

	public boolean hasPotion(ItemStack stack) {
		if (!stack.hasTagCompound())
			return false;
		return stack.getTagCompound().hasKey(effectsList);
	}

	@Override
	public boolean getShareTag() {
		return true;
	}
}
