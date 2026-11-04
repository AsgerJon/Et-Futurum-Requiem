package io.github.asgerjon.mcfly.potion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.potion.BrewingTable;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.BlockDispenser;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A lingering potion (SPEC-modern-movement.md §82; 26.3's `LingeringPotionItem`): thrown, it leaves a cloud of its
 * effects. Stored as EFR stored it (`etfuturum:lingering_potion`, 1.7.10's damage, or a potion by name, §69); its
 * effects a quarter as long as the drinkable potion's.
 */
public class LingeringPotionItem extends ItemPotion {

	private IIcon bottle;

	public LingeringPotionItem() {
		setTextureName("potion");
		setUnlocalizedName(McFly.name("lingering_potion"));
		setCreativeTab(McFlyTab.TAB);
		if (McFlyConfig.enableLingeringPotions) {
			BlockDispenser.dispenseBehaviorRegistry.putObject(this, new PotionDispense(false));
		}
	}

	private static boolean byName() {
		return McFlyConfig.potionsByName && NamedPotions.isReady();
	}

	@Override
	public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
		return stack;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 0;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.none;
	}

	/** Thrown as 26.3 throws it, with its sound. */
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		ItemStack thrown = stack.copy();
		thrown.stackSize = 1;
		if (!player.capabilities.isCreativeMode) {
			stack.stackSize--;
		}
		world.playSoundAtEntity(player, McFly.MC_ASSETS + ":entity.lingering_potion.throw", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
		if (!world.isRemote) {
			world.spawnEntityInWorld(new ThrownLingeringPotion(world, player, thrown));
		}
		return stack;
	}

	/** 1.7.10's potion by its damage, its lasting effects a quarter as long. */
	@Override
	public List<PotionEffect> getEffects(int damage) {
		List<PotionEffect> effects = new ArrayList<>();
		List<PotionEffect> drinkable = super.getEffects(damage);
		if (drinkable != null) {
			for (PotionEffect effect : drinkable) {
				if (Potion.potionTypes[effect.getPotionID()].isInstant()) {
					effects.add(new PotionEffect(effect));
				} else {
					PotionEffect quarter = new PotionEffect(effect.getPotionID(), effect.getDuration() / 4, effect.getAmplifier(), effect.getIsAmbient());
					quarter.setCurativeItems(effect.getCurativeItems());
					effects.add(quarter);
				}
			}
		}
		return effects;
	}

	/** A potion by name's 26.3 effects at a quarter; else `CustomPotionEffects` as they are; else by its damage. */
	@Override
	public List<PotionEffect> getEffects(ItemStack stack) {
		if (byName()) {
			PotionContents contents = PotionContents.of(stack);
			if (contents.potion() != null) {
				return contents.scaledEffects(PotionContents.durationScale(stack));
			}
		}
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("CustomPotionEffects", 9)) {
			List<PotionEffect> effects = new ArrayList<>();
			NBTTagList list = stack.getTagCompound().getTagList("CustomPotionEffects", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
				if (effect != null) {
					effects.add(effect);
				}
			}
			return effects;
		}
		return getEffects(stack.getItemDamage());
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		if (byName()) {
			PotionContents contents = PotionContents.of(stack);
			if (contents.potion() != null || contents.customName() != null) {
				String key = contents.nameKey("lingering_potion");
				if (StatCollector.canTranslate(key)) {
					return StatCollector.translateToLocal(key).trim();
				}
			}
		}
		if (stack.getItemDamage() == 0) {
			return StatCollector.translateToLocal("item.emptyPotion.name").trim();
		}
		List<PotionEffect> effects = getEffects(stack);
		if (effects != null && !effects.isEmpty()) {
			return StatCollector.translateToLocal("potion.prefix.lingering").trim() + " "
				+ StatCollector.translateToLocal(effects.get(0).getEffectName() + ".postfix").trim();
		}
		return StatCollector.translateToLocal(PotionHelper.func_77905_c(stack.getItemDamage())).trim() + " " + super.getItemStackDisplayName(stack);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		if (stack.getItemDamage() != 0 || PotionContents.of(stack).potion() != null) {
			PotionTooltip.add(getEffects(stack), lines);
		}
	}

	/** 26.3's: each offered potion by name in its order, then 1.7.10's unnamed ones (no splash damages). */
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		List<ItemStack> potions = new ArrayList<>();
		super.getSubItems(item, tab, potions);
		boolean named = byName();
		if (named) {
			for (NamedPotion potion : NamedPotions.available()) {
				list.add(BrewingTable.create(BrewingTable.Container.LINGERING, potion));
			}
		}
		for (ItemStack potion : potions) {
			if (!isSplash(potion.getItemDamage()) && (!named || PotionContents.of(potion).potion() == null)) {
				list.add(potion);
			}
		}
	}

	@Override
	public IIcon getIconFromDamage(int damage) {
		return bottle;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		super.registerIcons(register);
		bottle = register.registerIcon(getIconString() + "_bottle_lingering");
	}

	@Override
	public boolean hasEffect(ItemStack stack, int pass) {
		return super.hasEffect(stack, pass) && pass == 0;
	}
}
