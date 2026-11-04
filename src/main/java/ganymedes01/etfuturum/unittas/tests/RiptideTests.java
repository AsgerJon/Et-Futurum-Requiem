package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.EnchantmentIds;
import ganymedes01.etfuturum.command.modern.arguments.EntityTypeIds;
import ganymedes01.etfuturum.command.modern.arguments.ItemIds;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.trident.Riptide;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import java.util.Arrays;

/**
 * Riptide, the trident's stage 2 (SPEC-modern-movement.md §58.2): only in water or rain, a launch along the look of
 * 1.5, 2.25 or 3.0 by level instead of a throw, and a spin of up to 20 ticks that hits the first mob in the way. The
 * enchantment is found by its modern id, `minecraft:riptide`; the spin through `trident.Riptide`, a stand-in until the
 * code. The test world's floor is plains, where it rains.
 */
public class RiptideTests extends TasTestCase {

	private static final String TRIDENT = "minecraft:trident";
	private static final String RIPTIDE = "minecraft:riptide";
	private static final String SOUND = Reference.MCAssetVer + ":item.trident.riptide_";

	@Override
	protected void tearDown() {
		weather(false);
		onServer(() -> {
			if (player().ridingEntity != null) {
				player().mountEntity(null);
			}
		});
	}

	// ---------------------------------------------------------------- the enchantment

	/** Levels I-III, weight 2, enchanting cost 17 + 7 a level above I to 50; for tridents only. */
	public void testEnchantment() {
		Enchantment riptide = riptide();
		assertEqual(1, riptide.getMinLevel());
		assertEqual(3, riptide.getMaxLevel());
		assertEqual(2, riptide.getWeight());
		assertEqual(17, riptide.getMinEnchantability(1));
		assertEqual(31, riptide.getMinEnchantability(3));
		assertEqual(50, riptide.getMaxEnchantability(1));
		assertEqual(50, riptide.getMaxEnchantability(3));
		assertTrue(riptide.canApply(trident(0)), "can't go on a trident");
		assertFalse(riptide.canApply(new ItemStack(Items.iron_sword)), "goes on a sword");
	}

	// ---------------------------------------------------------------- when it works

	/** On dry land without rain it doesn't even start charging: no launch, no use taken. */
	public void testDryUseFails() {
		survival();
		holdTrident(trident(1));
		look(0.0F, 0.0F);
		keyDown(mc().gameSettings.keyBindUseItem);
		try {
			ticks(5);
			assertFalse(onClient(() -> mc().thePlayer.isUsingItem()), "a riptide trident charges on dry land");
		} finally {
			keyUp(mc().gameSettings.keyBindUseItem);
			ticks(3);
		}
		assertFalse(onServer(() -> Riptide.isSpinning(player())), "spinning on dry land");
		assertEqual(0, held().getItemDamage());
	}

	/** In water: launched, spinning on both sides, the riptide sound of its level, 1 use taken, nothing thrown. */
	public void testLaunchInWater() {
		pool(true);
		try {
			survival();
			holdTrident(trident(1));
			look(0.0F, 0.0F);
			clearSounds();
			charge(15);
			waitUntil("the spin on the server", () -> Riptide.isSpinning(player()), 5);
			waitUntilOnClient("the spin on the client", () -> Riptide.isSpinning(mc().thePlayer), 5);
			waitForSound(SOUND + "1", 20);
			assertEqual(1, held().getItemDamage());
			assertEqual(0, onServer(() -> thrownTridents()), "a riptide trident was thrown");
		} finally {
			pool(false);
		}
	}

	/** Spinning, the player is 0.6 high (the swimming size); the spin is over within 20 ticks. */
	public void testSpinPoseAndLength() {
		pool(true);
		try {
			holdTrident(trident(1));
			look(0.0F, -30.0F);
			charge(15);
			waitUntil("the spin", () -> Riptide.isSpinning(player()), 5);
			assertAlmostEqual(0.6, onServer(() -> player().boundingBox.maxY - player().boundingBox.minY), 0.01);
			waitUntil("the spin over", () -> !Riptide.isSpinning(player()), 25);
		} finally {
			pool(false);
		}
	}

	/** Level III launches about twice as far as level I (strength 3.0 against 1.5), along the look. */
	public void testStrengthByLevel() {
		pool(true);
		try {
			double first = launchDistance(1);
			double third = launchDistance(3);
			assertGreater(first, 2.0);
			double ratio = third / first;
			assertTrue(ratio > 1.6 && ratio < 2.4, "level III went " + third + ", level I " + first + " (about twice)");
			waitForSound(SOUND + "3", 20);
		} finally {
			pool(false);
		}
	}

	/** In the rain under the open sky it works on land. */
	public void testWorksInRain() {
		weather(true);
		holdTrident(trident(3));
		look(0.0F, -30.0F);
		charge(15);
		waitUntil("the spin in the rain", () -> Riptide.isSpinning(player()), 5);
	}

	/** Under a roof the rain doesn't count. */
	public void testNotUnderARoof() {
		onServer(() -> world().setBlock(0, 6, 0, Blocks.glass, 0, 3));
		try {
			weather(true);
			holdTrident(trident(3));
			look(0.0F, -30.0F);
			charge(15);
			serverTicks(5);
			assertFalse(onServer(() -> Riptide.isSpinning(player())), "spinning under a roof");
		} finally {
			onServer(() -> world().setBlock(0, 6, 0, Blocks.air, 0, 3));
		}
	}

	/** Where the biome gets no rain (a desert) the rain doesn't count. */
	public void testNotInADesert() {
		byte[] before = biomes((byte) BiomeGenBase.desert.biomeID);
		try {
			weather(true);
			holdTrident(trident(3));
			look(0.0F, -30.0F);
			charge(15);
			serverTicks(5);
			assertFalse(onServer(() -> Riptide.isSpinning(player())), "spinning in a desert's rain");
		} finally {
			setBiomes(before);
		}
	}

	/** Riding, letting go launches nothing. */
	public void testNotWhileRiding() {
		EntityPig pig = spawn(EntityPig::new, 0.5D, 4.0D, 0.5D);
		onServer(() -> player().mountEntity(pig));
		ticks(3);
		weather(true);
		holdTrident(trident(3));
		look(0.0F, -30.0F);
		charge(15);
		serverTicks(5);
		assertFalse(onServer(() -> Riptide.isSpinning(player())), "spinning while riding");
	}

	// ---------------------------------------------------------------- the spin attack

	/**
	 * The first mob in the way takes a melee hit; the spin ends and the player bounces back. On land in the rain (in
	 * water a villager floats above a spinning player), past creative's 6-block mob reach (in reach, right-clicking
	 * would trade).
	 */
	public void testSpinHitsTheFirstMob() {
		EntityVillager villager = spawn(EntityVillager::new, 0.5D, 4.0D, 7.5D);
		onServer(() -> villager.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D));
		weather(true);
		holdTrident(trident(3));
		look(0.0F, 0.0F);
		charge(15);
		waitUntil("the villager hit by the spin", () -> villager.getHealth() < villager.getMaxHealth(), 20);
		waitUntil("the spin over after the hit", () -> !Riptide.isSpinning(player()), 3);
		waitUntilOnClient("the player bounced back", () -> mc().thePlayer.motionZ < 0.0D, 5);
	}

	// ---------------------------------------------------------------- helpers

	private static Enchantment riptide() {
		Enchantment riptide = EnchantmentIds.byId(RIPTIDE);
		assertIsNotNone(riptide, "EFR has no Riptide (" + RIPTIDE + ")");
		return riptide;
	}

	private static Item tridentItem() {
		ItemIds.Entry entry = ItemIds.byId(TRIDENT);
		assertTrue(entry != null && entry.item() != null, "EFR has no trident (" + TRIDENT + ")");
		return entry.item();
	}

	/** A trident, with Riptide at this level (0 for none). */
	private static ItemStack trident(int level) {
		ItemStack trident = new ItemStack(tridentItem());
		if (level > 0) {
			trident.addEnchantment(riptide(), level);
		}
		return trident;
	}

	private void holdTrident(ItemStack trident) {
		setHeld(trident);
		waitUntilOnClient("the trident in hand", () -> mc().thePlayer.getHeldItem() != null && mc().thePlayer.getHeldItem().getItem() == tridentItem(), 40);
	}

	private void charge(int ticks) {
		hold(mc().gameSettings.keyBindUseItem, ticks);
		ticks(2);
	}

	private void survival() {
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		ticks(2);
	}

	/** How far south the player gets in 5 ticks from a launch at this level, looking south, starting still. */
	private double launchDistance(int level) {
		onServer(() -> player().setPositionAndUpdate(0.5D, 4.0D, 0.5D));
		waitUntil("the last spin over", () -> !Riptide.isSpinning(player()), 25);
		ticks(5);
		holdTrident(trident(level));
		look(0.0F, 0.0F);
		keyDown(mc().gameSettings.keyBindUseItem);
		ticks(15);
		double start = onClient(() -> mc().thePlayer.posZ);
		keyUp(mc().gameSettings.keyBindUseItem);
		ticks(5);
		return onClient(() -> mc().thePlayer.posZ) - start;
	}

	private static int thrownTridents() {
		int count = 0;
		for (Object loaded : world().loadedEntityList) {
			if (TRIDENT.equals(EntityTypeIds.idOf((Entity) loaded)) && !((Entity) loaded).isDead) {
				count++;
			}
		}
		return count;
	}

	/** Rain at full strength at once on both sides, or none. */
	private void weather(boolean rain) {
		float strength = rain ? 1.0F : 0.0F;
		onServer(() -> {
			world().getWorldInfo().setRaining(rain);
			world().getWorldInfo().setRainTime(rain ? 100000 : 0);
			world().rainingStrength = strength;
			world().prevRainingStrength = strength;
		});
		onClient(() -> {
			mc().theWorld.rainingStrength = strength;
			mc().theWorld.prevRainingStrength = strength;
		});
		ticks(2);
	}

	/** Water around the player (x -2..2, y 4..6, z -2..22) in a glass basin, or taken away. */
	private void pool(boolean fill) {
		onServer(() -> {
			for (int x = -3; x <= 3; x++) {
				for (int y = 4; y <= 6; y++) {
					for (int z = -3; z <= 23; z++) {
						boolean inside = x > -3 && x < 3 && z > -3 && z < 23;
						Block block = !fill ? Blocks.air : inside ? Blocks.water : Blocks.glass;
						world().setBlock(x, y, z, block, 0, 2);
					}
				}
			}
		});
		ticks(3);
	}

	/** Sets the biome of the player's chunk on both sides; returns what it was. */
	private byte[] biomes(byte biome) {
		byte[] before = onServer(() -> world().getChunkFromBlockCoords(0, 0).getBiomeArray().clone());
		byte[] all = new byte[256];
		Arrays.fill(all, biome);
		setBiomes(all);
		return before;
	}

	private void setBiomes(byte[] biomes) {
		onServer(() -> world().getChunkFromBlockCoords(0, 0).setBiomeArray(biomes.clone()));
		onClient(() -> chunkOf(mc().theWorld).setBiomeArray(biomes.clone()));
	}

	private static Chunk chunkOf(World world) {
		return world.getChunkFromBlockCoords(0, 0);
	}
}
