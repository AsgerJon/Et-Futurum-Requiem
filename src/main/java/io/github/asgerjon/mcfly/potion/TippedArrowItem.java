package io.github.asgerjon.mcfly.potion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tipped arrow (SPEC-modern-movement.md §82; 26.3's `TippedArrowItem`): an arrow with a potion's effects, an eighth as
 * long. Stored as EFR stored it (`etfuturum:tipped_arrow`: 1.7.10's damage, a one-effect `Potion` compound,
 * `CustomPotionEffects`, or a potion by name, §69); drawn as its shaft with the tip in the potion's colour.
 */
public class TippedArrowItem extends Item {

	/** 1.7.10's arrows by damage for the creative list: each effect plain, long and strong, made once. */
	private static final Map<List<PotionEffect>, Integer> BY_DAMAGE = new LinkedHashMap<>();
	private final Map<Integer, List<PotionEffect>> cache = new HashMap<>();
	private IIcon tip;

	public TippedArrowItem() {
		setHasSubtypes(true);
		setTextureName("tipped_arrow");
		setUnlocalizedName(McFly.name("tipped_arrow"));
		setCreativeTab(McFlyTab.TAB);
		if (McFlyConfig.enableTippedArrows) {
			BlockDispenser.dispenseBehaviorRegistry.putObject(this, new TippedArrowDispense());
		}
	}

	private static boolean byName() {
		return McFlyConfig.potionsByName && NamedPotions.isReady();
	}

	/** 1.7.10's potion of that damage, its effects an eighth as long. */
	private static List<PotionEffect> byDamage(int damage) {
		@SuppressWarnings("unchecked")
		List<PotionEffect> effects = PotionHelper.getPotionEffects(damage, false);
		if (effects != null) {
			for (int i = 0; i < effects.size(); i++) {
				PotionEffect effect = effects.get(i);
				effects.set(i, new PotionEffect(effect.getPotionID(), effect.getDuration() / 8, effect.getAmplifier()));
			}
		}
		return effects;
	}

	/** Its effects: a potion by name's at an eighth; a `Potion` compound's one; `CustomPotionEffects`; by its damage. */
	public List<PotionEffect> getEffects(ItemStack stack) {
		if (byName()) {
			PotionContents contents = PotionContents.of(stack);
			if (contents.potion() != null) {
				return contents.scaledEffects(PotionContents.durationScale(stack));
			}
		}
		if (stack.hasTagCompound()) {
			if (stack.getTagCompound().hasKey("Potion", 10)) {
				List<PotionEffect> one = new ArrayList<>();
				PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(stack.getTagCompound().getCompoundTag("Potion"));
				if (effect != null) {
					one.add(effect);
				}
				return one;
			}
			if (stack.getTagCompound().hasKey("CustomPotionEffects", 9)) {
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
		}
		return cache.computeIfAbsent(stack.getItemDamage(), TippedArrowItem::byDamage);
	}

	/** 26.3's: an arrow of every offered potion by name in its order, then 1.7.10's unnamed ones. */
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		List<ItemStack> old = new ArrayList<>();
		if (BY_DAMAGE.isEmpty()) {
			for (int effect = 1; effect <= 15; effect++) {
				for (int variant : new int[]{0, 32, 64}) {
					int damage = effect | 8192 | variant;
					List<PotionEffect> effects = byDamage(damage);
					if (effects != null && !effects.isEmpty()) {
						BY_DAMAGE.put(effects, damage);
					}
				}
			}
		}
		for (int damage : BY_DAMAGE.values()) {
			old.add(new ItemStack(item, 1, damage));
		}
		if (!byName()) {
			list.addAll(old);
			return;
		}
		for (NamedPotion potion : NamedPotions.available()) {
			ItemStack arrow = new ItemStack(item, 1, 0);
			PotionContents.write(arrow, false, potion, Collections.<PotionEffect>emptyList(), null, null);
			list.add(arrow);
		}
		for (ItemStack arrow : old) {
			if (PotionContents.of(arrow).potion() == null) {
				list.add(arrow);
			}
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		if (byName()) {
			PotionContents contents = PotionContents.of(stack);
			if (contents.potion() != null || contents.customName() != null) {
				String key = contents.nameKey("tipped_arrow");
				if (StatCollector.canTranslate(key)) {
					return StatCollector.translateToLocal(key).trim();
				}
			}
		}
		return super.getItemStackDisplayName(stack);
	}

	/** 1.7.10's naming for an arrow by damage or `Potion`: `item.etfuturum.tipped_arrow.<effect>`; empty as such. */
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		String base = "item." + McFly.name("tipped_arrow");
		List<PotionEffect> effects = getEffects(stack);
		if (effects != null && !effects.isEmpty()) {
			if (stack.getItemDamage() > 0 || stack.hasTagCompound() && stack.getTagCompound().hasKey("Potion", 10)) {
				return base + "." + effects.get(0).getEffectName();
			}
		} else if (stack.getItemDamage() == 0) {
			return base + ".effect.empty";
		}
		return base;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		PotionTooltip.add(getEffects(stack), lines);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		itemIcon = register.registerIcon(getIconString() + "_base");
		tip = register.registerIcon(getIconString() + "_head");
	}

	@Override
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	/** Its tip in the potion's colour (pass 0); the shaft as it is. */
	@Override
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if (pass == 0 && stack.getItemDamage() == 0 && stack.hasTagCompound() && stack.getTagCompound().hasKey("Potion", 10)) {
			PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(stack.getTagCompound().getCompoundTag("Potion"));
			if (effect != null && effect.getPotionID() >= 0 && effect.getPotionID() < Potion.potionTypes.length && Potion.potionTypes[effect.getPotionID()] != null) {
				return Potion.potionTypes[effect.getPotionID()].getLiquidColor();
			}
		}
		return Items.potionitem.getColorFromItemStack(stack, pass);
	}

	@Override
	public IIcon getIcon(ItemStack stack, int pass) {
		return pass == 0 ? tip : super.getIcon(stack, pass);
	}

	/** One tipped arrow of a stack's potion, for an arrow entity to give back when picked up. */
	public static ItemStack one(ItemStack arrow) {
		ItemStack one = arrow.copy();
		one.stackSize = 1;
		return one;
	}

	static NBTTagCompound tagOf(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
	}
}
