package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.loot.LootTableHolder;
import ganymedes01.etfuturum.loot.ModernLoot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10's chest holding 26.3's loot table until it's first opened (SPEC-modern-movement.md §76.12): as 26.3's
 * RandomizableContainer, any use of its slots on the server (opening it, a hopper, a comparator, breaking it) rolls
 * the table into it first (no luck: 1.7.10 has none).
 */
@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest extends net.minecraft.tileentity.TileEntity implements LootTableHolder {

	@Unique
	private String etfu$lootTable;
	@Unique
	private long etfu$lootTableSeed;

	@Override
	public void etfu$setLootTable(String table, long seed) {
		etfu$lootTable = table;
		etfu$lootTableSeed = seed;
	}

	@Override
	public String etfu$lootTable() {
		return etfu$lootTable;
	}

	@Override
	public long etfu$lootTableSeed() {
		return etfu$lootTableSeed;
	}

	@Override
	public void etfu$clearLootTable() {
		etfu$lootTable = null;
		etfu$lootTableSeed = 0L;
	}

	@Unique
	private void etfu$unpackLootTable() {
		if (etfu$lootTable != null && worldObj != null && !worldObj.isRemote) {
			String table = etfu$lootTable;
			long seed = etfu$lootTableSeed;
			etfu$clearLootTable();
			ModernLoot.table(table).fill((TileEntityChest) (Object) this, seed, 0.0F);
			markDirty();
		}
	}

	@Inject(method = "getStackInSlot", at = @At("HEAD"))
	private void etfu$unpackOnGet(int slot, CallbackInfoReturnable<ItemStack> cir) {
		etfu$unpackLootTable();
	}

	@Inject(method = "decrStackSize", at = @At("HEAD"))
	private void etfu$unpackOnTake(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
		etfu$unpackLootTable();
	}

	@Inject(method = "getStackInSlotOnClosing", at = @At("HEAD"))
	private void etfu$unpackOnClose(int slot, CallbackInfoReturnable<ItemStack> cir) {
		etfu$unpackLootTable();
	}

	@Inject(method = "setInventorySlotContents", at = @At("HEAD"))
	private void etfu$unpackOnSet(int slot, ItemStack stack, CallbackInfo ci) {
		etfu$unpackLootTable();
	}

	@Inject(method = "openInventory", at = @At("HEAD"))
	private void etfu$unpackOnOpen(CallbackInfo ci) {
		etfu$unpackLootTable();
	}

	@Inject(method = "readFromNBT", at = @At("TAIL"))
	private void etfu$readLootTable(NBTTagCompound tag, CallbackInfo ci) {
		etfu$lootTable = tag.hasKey("LootTable", 8) ? tag.getString("LootTable") : null;
		etfu$lootTableSeed = tag.getLong("LootTableSeed");
	}

	@Inject(method = "writeToNBT", at = @At("TAIL"))
	private void etfu$writeLootTable(NBTTagCompound tag, CallbackInfo ci) {
		if (etfu$lootTable != null) {
			tag.setString("LootTable", etfu$lootTable);
			if (etfu$lootTableSeed != 0L) {
				tag.setLong("LootTableSeed", etfu$lootTableSeed);
			}
		}
	}
}
