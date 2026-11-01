package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.item.ItemComponents;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A parsed item: the id's item and damage and its decoded components, made into a stack when the command runs (spec §45.2). */
public final class ItemInput {

	private static final Dynamic2CommandExceptionType OVERSTACKED = new Dynamic2CommandExceptionType(
		(item, count) -> ComponentMessage.translatable("arguments.item.overstacked", item, count));
	private static final DynamicCommandExceptionType MALFORMED = new DynamicCommandExceptionType(
		reason -> ComponentMessage.translatable("arguments.item.malformed", reason));

	private final ItemIds.Entry entry;
	private final String id;
	private final List<ItemComponents.Decoded> components;

	ItemInput(ItemIds.Entry entry, String id, List<ItemComponents.Decoded> components) {
		this.entry = entry;
		this.id = id;
		this.components = new ArrayList<>(components);
		this.components.sort(Comparator.comparingInt(component -> component.step));
	}

	public ItemStack createItemStack(int count) throws CommandSyntaxException {
		ItemStack stack = entry.createStack(count);
		for (ItemComponents.Decoded component : components) {
			component.applier.apply(stack);
		}
		if (count > stack.getMaxStackSize()) {
			throw OVERSTACKED.create(id, stack.getMaxStackSize());
		}
		if (stack.isItemStackDamageable() && stack.getItemDamage() > stack.getMaxDamage()) {
			throw MALFORMED.create("Item damage " + stack.getItemDamage() + " is higher than the maximum of " + stack.getMaxDamage());
		}
		return stack;
	}
}
