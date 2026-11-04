package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.blocks.ModernChests;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.entities.EntityBrownMooshroom;
import ganymedes01.etfuturum.items.ItemArrowTipped;
import ganymedes01.etfuturum.items.ItemSuspiciousStew;
import ganymedes01.etfuturum.potion.EffectRegistry;
import ganymedes01.etfuturum.potion.ModPotions;
import ganymedes01.etfuturum.potion.NamedPotion;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.tileentities.TileEntityCauldronPotion;
import ganymedes01.etfuturum.unittas.TasLogErrors;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.storage.ThreadedFileIOBase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Old saves through the real chunk loader (SPEC-modern-movement.md §74): things built in a chunk far from the player,
 * the chunk saved and unloaded, its saved NBT rewritten into the old format on disk, then the chunk loaded again by
 * 1.7.10's chunk loader (Forge's `ChunkIOExecutor.syncChunkLoad`, where §67's frame bug was). Everything must come back
 * exactly once, and each old format be read as meant.
 */
public class OldWorldTests extends TasTestCase {

	/** Each test its own chunk, far out along x (no player near, no spawn chunk). */
	private static int nextChunk = 3000;

	private int cx;
	private final int cz = 3000;

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	@Override
	protected void setUp() {
		cx = nextChunk;
		nextChunk += 2;
		onServer(() -> world().theChunkProviderServer.loadChunk(cx, cz));
	}

	@Override
	protected void tearDown() {
		onServer(() -> world().difficultySetting = EnumDifficulty.NORMAL);
	}

	private int bx(int dx) {
		return cx * 16 + 8 + dx;
	}

	private int bz(int dz) {
		return cz * 16 + 8 + dz;
	}

	// ---------------------------------------------------------------- old formats

	/** An item frame 1.7.10 saved (no `Facing`, its turn 3 of 4): back once, on its wall, turn 6 of 8, its item kept. */
	public void testOldItemFrame() {
		UUID id = onServer(() -> {
			world().setBlock(bx(0), 5, bz(1), Blocks.stone);
			world().setBlock(bx(0), 5, bz(-1), Blocks.stone);
			EntityItemFrame frame = new EntityItemFrame(world(), bx(0), 5, bz(0), 0);
			world().spawnEntityInWorld(frame);
			frame.setDisplayedItem(new ItemStack(Items.diamond));
			return frame.getUniqueID();
		});
		roundTrip(level -> forEachEntity(level, "ItemFrame", frame -> {
			frame.removeTag("Facing");
			frame.setByte("ItemRotation", (byte) 3);
		}));
		List<Entity> back = entities(id);
		assertEqual(1, back.size(), "the frame back (times)");
		EntityItemFrame frame = (EntityItemFrame) back.get(0);
		assertEqual(6, onServer(frame::getRotation), "its turn");
		assertEqual(Items.diamond, onServer(() -> frame.getDisplayedItem() == null ? null : frame.getDisplayedItem().getItem()), "its item");
	}

	/** A double chest 1.7.10 saved (no `ChestType`): back as the same double, its contents kept. */
	public void testOldDoubleChest() {
		onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), Blocks.chest, 3, 3);
			world().setBlock(bx(1), 5, bz(0), Blocks.chest, 3, 3);
			((TileEntityChest) world().getTileEntity(bx(0), 5, bz(0))).setInventorySlotContents(0, new ItemStack(Items.apple));
			((TileEntityChest) world().getTileEntity(bx(1), 5, bz(0))).setInventorySlotContents(5, new ItemStack(Items.bread));
		});
		roundTrip(level -> forEachTile(level, "Chest", chest -> chest.removeTag("ChestType")));
		serverTicks(3);
		String types = onServer(() -> ModernChests.typeOf(world(), bx(0), 5, bz(0)) + " " + ModernChests.typeOf(world(), bx(1), 5, bz(0)));
		assertEqual("right left", types, "the old double's halves");
		int[] contents = onServer(() -> {
			IInventory inventory = ((BlockChest) Blocks.chest).func_149951_m(world(), bx(0), 5, bz(0));
			return new int[]{inventory.getSizeInventory(), inventory.getStackInSlot(0) == null ? 0 : 1, inventory.getStackInSlot(27 + 5) == null ? 0 : 1};
		});
		assertEqual(54, contents[0], "a double");
		assertEqual(2, contents[1] + contents[2], "its apple and bread kept");
	}

	/** A mob's effects saved by number (`Id` bytes in `ActiveEffects`): back with them. */
	public void testOldMobEffects() {
		UUID id = onServer(() -> {
			EntityPig pig = new EntityPig(world());
			pig.setLocationAndAngles(bx(0) + 0.5D, 4, bz(0) + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(pig);
			pig.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 6000, 1));
			if (ModPotions.slowFalling != null) {
				pig.addPotionEffect(new PotionEffect(ModPotions.slowFalling.id, 6000, 0));
			}
			return pig.getUniqueID();
		});
		roundTrip(level -> forEachEntity(level, "Pig", pig -> toNumbers(pig.getTagList("ActiveEffects", 10))));
		List<Entity> back = entities(id);
		assertEqual(1, back.size(), "the pig back (times)");
		EntityPig pig = (EntityPig) back.get(0);
		assertTrue(onServer(() -> pig.isPotionActive(Potion.moveSpeed)), "its speed");
		if (ModPotions.slowFalling != null) {
			assertTrue(onServer(() -> pig.isPotionActive(ModPotions.slowFalling)), "its slow falling");
		}
	}

	/** Potions in a chest as 1.7.10 and old EFR kept them: by damage value, custom effects by number, EFR's leaping. */
	public void testOldPotionsInAChest() {
		onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), Blocks.chest, 3, 3);
			TileEntityChest chest = (TileEntityChest) world().getTileEntity(bx(0), 5, bz(0));
			chest.setInventorySlotContents(0, new ItemStack(Items.potionitem, 1, 8258));
			ItemStack custom = new ItemStack(Items.potionitem, 1, 8194);
			NBTTagList effects = new NBTTagList();
			effects.appendTag(new PotionEffect(Potion.poison.id, 300, 0).writeCustomPotionEffectToNBT(new NBTTagCompound()));
			custom.setTagCompound(new NBTTagCompound());
			custom.getTagCompound().setTag("CustomPotionEffects", effects);
			chest.setInventorySlotContents(1, custom);
			chest.setInventorySlotContents(2, new ItemStack(Items.potionitem, 1, 8203));
		});
		roundTrip(level -> forEachTile(level, "Chest", chest -> {
			NBTTagList items = chest.getTagList("Items", 10);
			for (int i = 0; i < items.tagCount(); i++) {
				NBTTagCompound tag = items.getCompoundTagAt(i).getCompoundTag("tag");
				if (tag.hasKey("CustomPotionEffects")) {
					toNumbers(tag.getTagList("CustomPotionEffects", 10));
				}
			}
		}));
		String[] read = onServer(() -> {
			TileEntityChest chest = (TileEntityChest) world().getTileEntity(bx(0), 5, bz(0));
			return new String[]{name(chest.getStackInSlot(0)), effects(chest.getStackInSlot(1)), name(chest.getStackInSlot(2))};
		});
		assertEqual("minecraft:long_swiftness", read[0], "a 1.7.10 potion by damage");
		assertEqual("poison/300", read[1], "custom effects saved by number");
		if (ModItems.RABBIT_FOOT.isEnabled()) {
			assertEqual("minecraft:leaping", read[2], "old EFR's leaping (8203)");
		}
	}

	/** A beacon saved with 1.7.10's `Primary` and `Secondary` numbers: back with its effects. */
	public void testOldBeacon() {
		onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), Blocks.beacon);
			NBTTagCompound pyramid = new NBTTagCompound();
			TileEntityBeacon beacon = (TileEntityBeacon) world().getTileEntity(bx(0), 5, bz(0));
			beacon.writeToNBT(pyramid);
			pyramid.setInteger("Levels", 4);
			beacon.readFromNBT(pyramid);
			beacon.setPrimaryEffect(Potion.moveSpeed.id);
			beacon.setSecondaryEffect(Potion.regeneration.id);
		});
		roundTrip(level -> forEachTile(level, "Beacon", beacon -> {
			beacon.removeTag("primary_effect");
			beacon.removeTag("secondary_effect");
			beacon.setInteger("Primary", Potion.moveSpeed.id);
			beacon.setInteger("Secondary", Potion.regeneration.id);
		}));
		String effects = onServer(() -> {
			TileEntityBeacon beacon = (TileEntityBeacon) world().getTileEntity(bx(0), 5, bz(0));
			return beacon.getPrimaryEffect() + " " + beacon.getSecondaryEffect();
		});
		assertEqual(Potion.moveSpeed.id + " " + Potion.regeneration.id, effects, "the old beacon's effects");
	}

	/** A suspicious stew and a brown mooshroom saved with effect numbers (`EffectId`): their effects kept. */
	public void testOldStewAndMooshroom() {
		if (!ModItems.SUSPICIOUS_STEW.isEnabled()) {
			skipTest("suspicious stew is off");
		}
		UUID id = onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), Blocks.chest, 3, 3);
			ItemStack stew = ModItems.SUSPICIOUS_STEW.newItemStack(1, 0);
			ItemSuspiciousStew.addEffect(stew, Potion.nightVision, 100);
			((TileEntityChest) world().getTileEntity(bx(0), 5, bz(0))).setInventorySlotContents(0, stew);
			EntityBrownMooshroom mooshroom = new EntityBrownMooshroom(world());
			mooshroom.setLocationAndAngles(bx(3) + 0.5D, 4, bz(0) + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(mooshroom);
			mooshroom.getEntityData().setString("stew_effect", "minecraft:blindness");
			NBTTagCompound saved = new NBTTagCompound();
			mooshroom.writeToNBT(saved);
			mooshroom.readFromNBT(saved);
			return mooshroom.getUniqueID();
		});
		roundTrip(level -> {
			forEachTile(level, "Chest", chest -> {
				NBTTagCompound entry = chest.getTagList("Items", 10).getCompoundTagAt(0).getCompoundTag("tag")
					.getTagList(ItemSuspiciousStew.effectsList, 10).getCompoundTagAt(0);
				entry.removeTag(ItemSuspiciousStew.stewEffectName);
				entry.setByte(ItemSuspiciousStew.stewEffect, (byte) Potion.nightVision.id);
			});
			forEachEntity(level, "etfuturum.brown_mooshroom", mooshroom -> {
				NBTTagCompound data = mooshroom.getCompoundTag("ForgeData");
				data.removeTag("stew_effect");
				data.setByte("EffectId", (byte) Potion.blindness.id);
			});
		});
		String stew = onServer(() -> {
			ItemStack stack = ((TileEntityChest) world().getTileEntity(bx(0), 5, bz(0))).getStackInSlot(0);
			Potion effect = ItemSuspiciousStew.effectOf(stack.getTagCompound().getTagList(ItemSuspiciousStew.effectsList, 10).getCompoundTagAt(0));
			return EffectRegistry.key(effect);
		});
		assertEqual("minecraft:night_vision", stew, "the old stew's effect");
		List<Entity> back = entities(id);
		assertEqual(1, back.size(), "the mooshroom back (times)");
		String stored = onServer(() -> {
			NBTTagCompound saved = new NBTTagCompound();
			back.get(0).writeToNBT(saved);
			return saved.getCompoundTag("ForgeData").getString("stew_effect");
		});
		assertEqual("minecraft:blindness", stored, "its stored flower effect, now by name");
	}

	/** EFR's one-effect tipped arrows (a stray's: an NBT `Potion` compound, the effect by number): their effect kept. */
	public void testOldTippedArrow() {
		if (!ModItems.TIPPED_ARROW.isEnabled()) {
			skipTest("tipped arrows are off");
		}
		onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), Blocks.chest, 3, 3);
			ItemStack arrow = ModItems.TIPPED_ARROW.newItemStack(1, 0);
			arrow.setTagCompound(new NBTTagCompound());
			arrow.getTagCompound().setTag("Potion", new PotionEffect(Potion.moveSlowdown.id, 600, 0).writeCustomPotionEffectToNBT(new NBTTagCompound()));
			((TileEntityChest) world().getTileEntity(bx(0), 5, bz(0))).setInventorySlotContents(0, arrow);
		});
		roundTrip(level -> forEachTile(level, "Chest", chest -> {
			NBTTagCompound potion = chest.getTagList("Items", 10).getCompoundTagAt(0).getCompoundTag("tag").getCompoundTag("Potion");
			toNumber(potion);
		}));
		String effect = onServer(() -> {
			ItemStack arrow = ((TileEntityChest) world().getTileEntity(bx(0), 5, bz(0))).getStackInSlot(0);
			List<PotionEffect> list = ((ItemArrowTipped) arrow.getItem()).getEffects(arrow);
			return list == null || list.isEmpty() ? "none" : EffectRegistry.key(Potion.potionTypes[list.get(0).getPotionID()]);
		});
		assertEqual("minecraft:slowness", effect, "the old arrow's effect");
	}

	/** A sign saved before §67's sign data: its text back. */
	public void testOldSign() {
		onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), Blocks.standing_sign, 0, 3);
			TileEntitySign sign = (TileEntitySign) world().getTileEntity(bx(0), 5, bz(0));
			sign.signText = new String[]{"old", "sign", "", "text"};
		});
		roundTrip(level -> forEachTile(level, "Sign", sign -> {
			for (String key : new String[]{"Color", "GlowingText", "Waxed", "front_text", "back_text"}) {
				sign.removeTag(key);
			}
			sign.setString("Text1", "old");
			sign.setString("Text2", "sign");
			sign.setString("Text4", "text");
		}));
		String text = onServer(() -> {
			TileEntity tile = world().getTileEntity(bx(0), 5, bz(0));
			return tile instanceof TileEntitySign sign ? String.join("|", sign.signText) : "no sign";
		});
		assertEqual("old|sign||text", text, "the old sign's text");
	}

	/**
	 * A potion cauldron whose potion doesn't load (its item gone from the game): a plain cauldron again, set after the
	 * chunk loading (1.7.10's EFR set the block from reading the NBT, with no world yet: an exception, the cauldron kept).
	 */
	public void testOldPotionCauldronLostItsPotion() {
		if (!ModBlocks.POTION_CAULDRON.isEnabled()) {
			skipTest("potion cauldrons are off");
		}
		onServer(() -> {
			world().setBlock(bx(0), 5, bz(0), ModBlocks.POTION_CAULDRON.get(), 2, 3);
			((TileEntityCauldronPotion) world().getTileEntity(bx(0), 5, bz(0))).potion = new ItemStack(Items.potionitem);
		});
		// an item number no item has
		roundTrip(level -> forEachTile(level, "etfuturum.potion_cauldron", cauldron -> cauldron.getCompoundTag("Potion").setShort("id", (short) 31999)));
		serverTicks(2);
		assertEqual(Blocks.cauldron, onServer(() -> world().getBlock(bx(0), 5, bz(0))), "the cauldron");
		assertEqual(0, onServer(() -> world().getBlockMetadata(bx(0), 5, bz(0))), "its water");
	}

	/**
	 * A vanilla boat EFR swaps for its own as the chunk loads, saved just before a pig: the new boat (with the old
	 * one's UUID) and the pig are both in the world. EFR took the old boat out of the chunk's list while Forge went
	 * through that list by index, so the entity after it was skipped: never in the world (frozen and unseen) but still
	 * in the chunk, saved again (2026-11-03, found by §75's vanilla worlds: two mobs a world).
	 */
	public void testReplacedEntityLeavesTheNextOne() {
		if (!(ConfigBlocksItems.enableNewBoats && ConfigBlocksItems.replaceOldBoats)) {
			skipTest("EFR's boats don't replace vanilla's");
		}
		UUID pig = onServer(() -> {
			EntityPig made = new EntityPig(world());
			made.setLocationAndAngles(bx(2) + 0.5, 4.0, bz(0) + 0.5, 0.0F, 0.0F);
			made.func_110163_bv();
			world().spawnEntityInWorld(made);
			return made.getUniqueID();
		});
		UUID boat = UUID.randomUUID();
		roundTrip(level -> {
			NBTTagList saved = level.getTagList("Entities", 10);
			NBTTagList list = new NBTTagList();
			NBTTagCompound old = new NBTTagCompound();
			old.setString("id", "Boat");
			old.setTag("Pos", doubles(bx(0) + 0.5, 4.0, bz(0) + 0.5));
			old.setTag("Motion", doubles(0.0, 0.0, 0.0));
			NBTTagList rotation = new NBTTagList();
			rotation.appendTag(new net.minecraft.nbt.NBTTagFloat(0.0F));
			rotation.appendTag(new net.minecraft.nbt.NBTTagFloat(0.0F));
			old.setTag("Rotation", rotation);
			old.setLong("UUIDMost", boat.getMostSignificantBits());
			old.setLong("UUIDLeast", boat.getLeastSignificantBits());
			// the old boat first, the pig after it in the same list
			list.appendTag(old);
			for (int i = 0; i < saved.tagCount(); i++) {
				list.appendTag(saved.getCompoundTagAt(i));
			}
			level.setTag("Entities", list);
		});
		serverTicks(2);
		assertEqual(1, inWorld(pig), "the pig saved after the old boat, in the world (times)");
		assertEqual(1, inWorld(boat), "the boat, in the world (times)");
		assertTrue(onServer(() -> {
			for (Object loaded : world().loadedEntityList) {
				if (((Entity) loaded).getUniqueID().equals(boat)) {
					return loaded instanceof ganymedes01.etfuturum.entities.EntityNewBoat;
				}
			}
			return false;
		}), "the boat is EFR's");
	}

	// ---------------------------------------------------------------- the sweeps

	/** Every EFR entity type (and the vanilla ones EFR changes) through a save and reload: back once each. */
	public void testEveryEntityComesBackOnce() {
		onServer(() -> world().difficultySetting = EnumDifficulty.NORMAL);
		Map<UUID, String> spawned = onServer(() -> {
			Map<UUID, String> made = new HashMap<>();
			int i = 0;
			for (Object key : new ArrayList<Object>(EntityList.stringToClassMapping.keySet())) {
				String name = (String) key;
				Class<?> type = (Class<?>) EntityList.stringToClassMapping.get(name);
				boolean efr = type.getName().startsWith("ganymedes01.etfuturum");
				boolean changed = name.equals("EntityHorse") || name.equals("Squid") || name.equals("Pig") || name.equals("Zombie")
					|| name.equals("Skeleton") || name.equals("MinecartChest") || name.equals("Boat");
				if (!efr && !changed) {
					continue;
				}
				Entity entity;
				try {
					entity = EntityList.createEntityByName(name, world());
				} catch (RuntimeException e) {
					continue;
				}
				if (!(entity instanceof EntityLiving || entity instanceof EntityMinecart || entity instanceof EntityBoat)) {
					continue;
				}
				if (entity instanceof EntityLiving living) {
					// no despawning while the chunk waits to be saved, far from the player
					living.func_110163_bv();
				}
				entity.setLocationAndAngles(bx(-6 + i % 13), 4.0D, bz(-6 + i / 13), 0.0F, 0.0F);
				if (world().spawnEntityInWorld(entity)) {
					made.put(entity.getUniqueID(), name);
				}
				i++;
			}
			return made;
		});
		serverTicks(1);
		// only those still there just before the save count (some die or leave in their first tick on purpose)
		Map<UUID, String> there = new HashMap<>();
		for (Map.Entry<UUID, String> entry : spawned.entrySet()) {
			if (entities(entry.getKey()).size() == 1) {
				there.put(entry.getKey(), entry.getValue());
			}
		}
		assertTrue(there.size() > 10, "only " + there.size() + " entity types made and there");
		spawned = there;
		roundTrip(level -> {
		});
		List<String> wrong = new ArrayList<>();
		for (Map.Entry<UUID, String> entry : spawned.entrySet()) {
			// in its chunk's list and in the world: an entity only in the chunk is frozen and unseen (2026-11-03)
			int times = Math.max(entities(entry.getKey()).size(), inWorld(entry.getKey()));
			if (entities(entry.getKey()).size() != inWorld(entry.getKey())) {
				wrong.add(entry.getValue() + " in its chunk " + entities(entry.getKey()).size() + " times, in the world " + inWorld(entry.getKey()));
			}
			if (times != 1) {
				wrong.add(entry.getValue() + " x" + times);
			}
		}
		assertEqual("[]", wrong.toString(), "entities not back exactly once");
	}

	/** Every EFR block with a tile entity (and chests, signs, beacons) through a save and reload: its tile entity back. */
	public void testEveryTileEntityComesBack() {
		Map<String, String> placed = onServer(() -> {
			Map<String, String> made = new HashMap<>();
			int i = 0;
			List<Block> blocks = new ArrayList<>();
			for (ModBlocks block : ModBlocks.values()) {
				if (block.isEnabled() && block.get() != null && block.get().hasTileEntity(0)) {
					blocks.add(block.get());
				}
			}
			for (io.github.asgerjon.mcfly.registry.McFlyBlocks block : io.github.asgerjon.mcfly.registry.McFlyBlocks.values()) {
				if (block.isEnabled() && block.get().hasTileEntity(0)) {
					blocks.add(block.get());
				}
			}
			blocks.add(Blocks.chest);
			blocks.add(Blocks.standing_sign);
			blocks.add(Blocks.beacon);
			for (Block block : blocks) {
				int x = bx(-6 + i % 13);
				int z = bz(-6 + i / 13);
				// a floor and a roof: signs stand, cave vines hang
				world().setBlock(x, 5, z, Blocks.stone);
				world().setBlock(x, 7, z, Blocks.stone);
				world().setBlock(x, 6, z, block, 0, 3);
				TileEntity tile = world().getTileEntity(x, 6, z);
				if (tile instanceof TileEntityCauldronPotion cauldron) {
					// as a real one: never without its potion
					cauldron.potion = new ItemStack(Items.potionitem);
				}
				if (tile != null) {
					made.put(x + " " + z, tile.getClass().getName());
				}
				i++;
			}
			return made;
		});
		serverTicks(1);
		// only those still standing a tick later count (a block that can't stand there pops off)
		Map<String, String> standing = onServer(() -> {
			Map<String, String> still = new HashMap<>();
			for (Map.Entry<String, String> entry : placed.entrySet()) {
				String[] at = entry.getKey().split(" ");
				TileEntity tile = world().getTileEntity(Integer.parseInt(at[0]), 6, Integer.parseInt(at[1]));
				if (tile != null && tile.getClass().getName().equals(entry.getValue())) {
					still.put(entry.getKey(), entry.getValue());
				}
			}
			return still;
		});
		assertTrue(standing.size() > 5, "only " + standing.size() + " tile entities made and standing");
		roundTrip(level -> {
		});
		List<String> wrong = onServer(() -> {
			List<String> missing = new ArrayList<>();
			for (Map.Entry<String, String> entry : standing.entrySet()) {
				String[] at = entry.getKey().split(" ");
				TileEntity tile = world().getTileEntity(Integer.parseInt(at[0]), 6, Integer.parseInt(at[1]));
				if (tile == null || !tile.getClass().getName().equals(entry.getValue())) {
					missing.add(entry.getValue() + (tile == null ? " gone" : " now " + tile.getClass().getName()));
				}
			}
			return missing;
		});
		assertEqual("[]", wrong.toString(), "tile entities not back");
	}

	// ---------------------------------------------------------------- the round trip

	/**
	 * The chunk saved and unloaded, its saved NBT changed (the old format), and loaded again by the chunk loader. The
	 * file writer is waited for both ways, so the loader reads what is on disk, not a copy waiting in memory.
	 */
	private void roundTrip(Consumer<NBTTagCompound> oldFormat) {
		TasLogErrors errors = new TasLogErrors();
		onServer(() -> {
			world().theChunkProviderServer.unloadChunksIfNotNearSpawn(cx, cz);
			world().theChunkProviderServer.unloadQueuedChunks();
		});
		assertTrue(!onServer(() -> world().theChunkProviderServer.chunkExists(cx, cz)), "the chunk didn't unload");
		waitForTheWriter();
		onServer(() -> {
			try {
				DataInputStream in = RegionFileCache.getChunkInputStream(world().getChunkSaveLocation(), cx, cz);
				assertTrue(in != null, "the chunk isn't on disk");
				NBTTagCompound root = CompressedStreamTools.read(in);
				in.close();
				oldFormat.accept(root.getCompoundTag("Level"));
				DataOutputStream out = RegionFileCache.getChunkOutputStream(world().getChunkSaveLocation(), cx, cz);
				CompressedStreamTools.write(root, out);
				out.close();
			} catch (java.io.IOException e) {
				throw new AssertionError("couldn't rewrite the chunk: " + e);
			}
		});
		// it must hear FML's logger and the root (it checks with a probe through each)
		errors.listen();
		try {
			onServer(() -> world().theChunkProviderServer.loadChunk(cx, cz));
			serverTicks(2);
		} finally {
			errors.quiet();
		}
		// a thing that throws while it loads is lost, though the game may make a fresh one in its place
		assertEqual("[]", errors.errors.toString(), "errors logged while the chunk loaded");
	}

	private void waitForTheWriter() {
		try {
			ThreadedFileIOBase.threadedIOInstance.waitForFinish();
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
	}

	/** The entities in the test's chunk with this UUID. */
	@SuppressWarnings("unchecked")
	private List<Entity> entities(UUID id) {
		return onServer(() -> {
			List<Entity> found = new ArrayList<>();
			Chunk chunk = world().getChunkFromChunkCoords(cx, cz);
			for (List<Entity> section : chunk.entityLists) {
				for (Entity entity : section) {
					if (entity.getUniqueID().equals(id) && !entity.isDead) {
						found.add(entity);
					}
				}
			}
			return found;
		});
	}

	/** How many times an entity is in the world's list (what ticks and is sent to players), not dead. */
	private int inWorld(UUID id) {
		return onServer(() -> {
			int times = 0;
			for (Object loaded : world().loadedEntityList) {
				Entity entity = (Entity) loaded;
				if (entity.getUniqueID().equals(id) && !entity.isDead) {
					times++;
				}
			}
			return times;
		});
	}

	private static NBTTagList doubles(double... values) {
		NBTTagList list = new NBTTagList();
		for (double value : values) {
			list.appendTag(new net.minecraft.nbt.NBTTagDouble(value));
		}
		return list;
	}

	private static void forEachEntity(NBTTagCompound level, String id, Consumer<NBTTagCompound> change) {
		NBTTagList list = level.getTagList("Entities", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			if (id.equals(list.getCompoundTagAt(i).getString("id"))) {
				change.accept(list.getCompoundTagAt(i));
			}
		}
	}

	private static void forEachTile(NBTTagCompound level, String id, Consumer<NBTTagCompound> change) {
		NBTTagList list = level.getTagList("TileEntities", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			if (id.equals(list.getCompoundTagAt(i).getString("id"))) {
				change.accept(list.getCompoundTagAt(i));
			}
		}
	}

	/** Effect instances as 1.7.10 saved them: `Id`, a byte of the effect's number, for 26.3's `id` name. */
	private static void toNumbers(NBTTagList effects) {
		for (int i = 0; i < effects.tagCount(); i++) {
			toNumber(effects.getCompoundTagAt(i));
		}
	}

	private static void toNumber(NBTTagCompound effect) {
		if (effect.hasKey("id", 8)) {
			Potion potion = EffectRegistry.get(effect.getString("id"));
			effect.removeTag("id");
			if (potion != null) {
				effect.setByte("Id", (byte) potion.id);
			}
		}
	}

	private static String name(ItemStack stack) {
		NamedPotion potion = PotionContents.of(stack).potion();
		return potion == null ? null : potion.id();
	}

	@SuppressWarnings("unchecked")
	private static String effects(ItemStack stack) {
		List<PotionEffect> list = Items.potionitem.getEffects(stack);
		StringBuilder text = new StringBuilder();
		for (PotionEffect effect : list) {
			String name = EffectRegistry.key(Potion.potionTypes[effect.getPotionID()]);
			text.append(text.length() == 0 ? "" : " ").append(name.substring(name.indexOf(':') + 1)).append('/').append(effect.getDuration());
		}
		return text.toString();
	}
}
