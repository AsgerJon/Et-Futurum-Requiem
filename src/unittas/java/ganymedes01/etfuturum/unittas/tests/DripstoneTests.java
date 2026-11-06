package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Dripstone as our own (SPEC-modern-movement.md §90.7, step F), as 26.3's (`SpeleothemBlock`, `PointedDripstoneBlock`):
 * the dripstone block; pointed dripstone in EFR's metas (meta % 5 the thickness, 5 and up pointing up), its thickness
 * from its neighbours, placing, a stalactite falling and hurting, a stalagmite's fall damage, growth under water, drips
 * into cauldrons (water and lava) and mud to clay, drip particles, a trident breaking it, the recipe. At z 420, y 8 and
 * up; the drips at x -3, z 9, by the player.
 */
@TasSuite("caves")
public class DripstoneTests extends TasTestCase {

	private static final int X = 40, Y = 8, Z = 420;
	private static final int SEEN_X = -3, SEEN_Y = 8, SEEN_Z = 9;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setSneaking(false);
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** x 36 to 44, y 6 to 22, z 416 to 424 emptied, a stone floor at y 7, its items and mobs gone. */
	private void clear() {
		for (int x = X - 4; x <= X + 4; x++) {
			for (int y = Y - 2; y <= Y + 14; y++) {
				for (int z = Z - 4; z <= Z + 4; z++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(net.minecraft.entity.Entity.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 3, Z - 5, X + 6, Y + 16, Z + 6))) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((net.minecraft.entity.Entity) entity).setDead();
			}
		}
	}

	private String dropped() {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 3, Z - 5, X + 6, Y + 16, Z + 6))) {
			EntityItem item = (EntityItem) entity;
			if (!item.isDead) {
				ItemStack stack = item.getEntityItem();
				counts.merge(Item.itemRegistry.getNameForObject(stack.getItem()).replace(McFly.DOMAIN + ":", "") + ":" + stack.getItemDamage(), stack.stackSize, Integer::sum);
				item.setDead();
			}
		}
		return counts.toString();
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static Block pointed() {
		return block("pointed_dripstone");
	}

	/** The 26.3 block it reads as, with its tip direction and thickness. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		String id = found.type().id.replace("minecraft:", "");
		return state.containsKey("thickness") ? id + "," + state.get("vertical_direction") + "," + state.get("thickness") : id;
	}

	/** The pointed dripstone column at x, z from y0 to y1, as tip direction and thickness (or "-" for none). */
	private String column(int y0, int y1) {
		StringBuilder names = new StringBuilder();
		for (int y = y1; y >= y0; y--) {
			Block there = world().getBlock(X, y, Z);
			names.append(names.length() == 0 ? "" : " ").append(there == pointed() ? name(there, world().getBlockMetadata(X, y, Z)).replace("pointed_dripstone,", "") : "-");
		}
		return names.toString();
	}

	/** The player uses a pointed dripstone on a face of a block, looking up or down. */
	private void place(int x, int y, int z, int side, float pitch) {
		ItemStack stack = new ItemStack(pointed(), 4);
		player().inventory.setInventorySlotContents(player().inventory.currentItem, stack);
		player().rotationPitch = pitch;
		stack.getItem().onItemUse(stack, player(), world(), x, y, z, side, 0.5F, side == 0 ? 0.0F : 1.0F, 0.5F);
	}

	// ---------------------------------------------------------------- ours, stored form, properties

	public void testDripstoneIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"dripstone_block", "pointed_dripstone"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockPointedDripstone", "entities.EntityFallingDripstone", "client.renderer.block.BlockPointedDripstoneRenderer",
			"client.renderer.entity.FallingDripstoneRenderer"}) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "dripstone's classes");
	}

	/** EFR's metas by 26.3's names: meta % 5 the thickness (tip, frustum, middle, base, tip_merge), 0 to 4 down, 5 to 9 up. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder names = new StringBuilder(name(block("dripstone_block"), 0));
			for (int meta = 0; meta < 10; meta++) {
				names.append(" ").append(name(pointed(), meta).replace("pointed_dripstone,", ""));
			}
			return names.toString();
		});
		assertEqual("dripstone_block down,tip down,frustum down,middle down,base down,tip_merge up,tip up,frustum up,middle up,base up,tip_merge", got,
			"26.3's names");
	}

	/** 26.3's strength (1.5 and 1, 1.5 and 3), a pickaxe, their sounds; terracotta brown as 1.7.10's brown; not burning. */
	public void testProperties() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (Block block : new Block[]{block("dripstone_block"), pointed()}) {
				world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y, Z, block, block == pointed() ? 0 : 0, 2);
				result.append(result.length() == 0 ? "" : "; ").append(block.getBlockHardness(world(), X, Y, Z)).append(" ").append(block.getExplosionResistance(null))
					.append(" ").append(block.getHarvestTool(0)).append(" ").append(block.stepSound.getBreakSound().replace(McFly.MC_ASSETS + ":", "")).append(" ")
					.append(block.getMapColor(0) == net.minecraft.block.material.MapColor.brownColor ? "brown" : block.getMapColor(0).colorIndex).append(" ")
					.append(block.isFlammable(world(), X, Y, Z, net.minecraftforge.common.util.ForgeDirection.UP));
			}
			return result.toString();
		});
		assertEqual("1.5 1.0 pickaxe block.dripstone_block.break brown false; 1.5 3.0 pickaxe block.pointed_dripstone.break brown false", got, "properties");
	}

	// ---------------------------------------------------------------- placing and thickness

	/**
	 * Under a ceiling, looking up, four placed one under another: base, middle, frustum, tip; the lowest broken, the three
	 * left are base, frustum, tip. On the floor, looking down: a stalagmite. A stalagmite met by a stalactite: both tip_merge;
	 * sneaking, the placed one a plain tip.
	 */
	public void testPlacingAndThickness() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 8, Z, Blocks.stone, 0, 2);
			for (int i = 0; i < 4; i++) {
				place(X, Y + 8 - i, Z, 0, -60.0F);
			}
			String four = column(Y + 4, Y + 7);
			world().setBlock(X, Y + 4, Z, Blocks.air, 0, 3);
			String three = column(Y + 5, Y + 7);
			place(X, Y - 1, Z, 1, 60.0F);
			String floor = column(Y, Y);
			// the floor's stalagmite built up to just under the stalactite's tip (now y + 5): the two tips meet
			place(X, Y, Z, 1, 60.0F);
			place(X, Y + 1, Z, 1, 60.0F);
			place(X, Y + 2, Z, 1, 60.0F);
			place(X, Y + 3, Z, 1, 60.0F);
			String met = column(Y + 3, Y + 5);
			world().setBlock(X + 2, Y + 3, Z, Blocks.stone, 0, 2);
			world().setBlock(X + 2, Y, Z, Blocks.stone, 0, 2);
			world().setBlock(X + 2, Y + 1, Z, pointed(), 5, 3);
			player().setSneaking(true);
			place(X + 2, Y + 3, Z, 0, -60.0F);
			player().setSneaking(false);
			String sneaked = name(world().getBlock(X + 2, Y + 2, Z), world().getBlockMetadata(X + 2, Y + 2, Z)).replace("pointed_dripstone,", "") + " over "
				+ name(world().getBlock(X + 2, Y + 1, Z), world().getBlockMetadata(X + 2, Y + 1, Z)).replace("pointed_dripstone,", "");
			return "four: " + four + "; three: " + three + "; floor: " + floor + "; met: " + met + "; sneaked: " + sneaked;
		});
		assertEqual("four: down,base down,middle down,frustum down,tip; three: down,base down,frustum down,tip; floor: up,tip; "
			+ "met: down,tip_merge up,tip_merge up,frustum; sneaked: down,tip over up,tip", got, "placing and thickness");
		// three long, 26.3's thickness is base, frustum, tip (frustum: the one before a tip), as four's lowest three
	}

	// ---------------------------------------------------------------- falling and damage

	/** Public, as Forge's event bus calls it from its own generated class. */
	public static final class Hurts {
		final List<String> entries = new ArrayList<>();
		final int id;

		Hurts(int id) {
			this.id = id;
		}

		@SubscribeEvent
		public void onHurt(LivingHurtEvent event) {
			if (event.entityLiving.getEntityId() == id) {
				entries.add(event.source.getDamageType() + " " + event.ammount);
			}
		}
	}

	/** The falling tests by the player (x 5, z -5): 1.7.10 moves an entity only with the chunks round it loaded. */
	private static final int NEAR_X = 5, NEAR_Z = -5;

	/** x 4 to 6, y 7 to 21, z -6 to -4 by the player: a stone floor at y 7, air over it; its items and mobs gone. */
	private void clearNear() {
		for (int x = NEAR_X - 1; x <= NEAR_X + 1; x++) {
			for (int y = Y - 1; y <= Y + 13; y++) {
				for (int z = NEAR_Z - 1; z <= NEAR_Z + 1; z++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(net.minecraft.entity.Entity.class, nearBox())) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((net.minecraft.entity.Entity) entity).setDead();
			}
		}
	}

	private static AxisAlignedBB nearBox() {
		return AxisAlignedBB.getBoundingBox(NEAR_X - 2, Y - 2, NEAR_Z - 2, NEAR_X + 3, Y + 15, NEAR_Z + 3);
	}

	/** The pointed dripstone dropped by the player's spot (a dead pig's pork aside). */
	private String droppedNear() {
		int count = 0;
		for (Object entity : world().getEntitiesWithinAABB(EntityItem.class, nearBox())) {
			EntityItem item = (EntityItem) entity;
			if (!item.isDead && item.getEntityItem().getItem() == Item.getItemFromBlock(pointed())) {
				count += item.getEntityItem().stackSize;
			}
		}
		return count + " pointed dripstone";
	}

	/**
	 * A three-long stalactite 8 over a pig, its ceiling gone: it falls whole (2 ticks on), its tip hurting the pig as
	 * `fallingStalactite` (1 a block times its size, at least 6, at most 40), and breaks on landing, dropping all three.
	 */
	public void testStalactiteFallsAndHurts() {
		EntityPig pig = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			clearNear();
			world().setBlock(NEAR_X, Y + 12, NEAR_Z, Blocks.stone, 0, 2);
			for (int i = 0; i < 3; i++) {
				place(NEAR_X, Y + 12 - i, NEAR_Z, 0, -60.0F);
			}
			EntityPig spawned = new EntityPig(world());
			spawned.setLocationAndAngles(NEAR_X + 0.5D, Y, NEAR_Z + 0.5D, 0.0F, 0.0F);
			spawned.setHealth(10.0F);
			world().spawnEntityInWorld(spawned);
			return spawned;
		});
		Hurts hurts = new Hurts(pig.getEntityId());
		MinecraftForge.EVENT_BUS.register(hurts);
		try {
			onServer(() -> world().setBlock(NEAR_X, Y + 12, NEAR_Z, Blocks.air, 0, 3));
			serverTicks(60);
			String got = onServer(() -> {
				int left = 0;
				for (int y = Y + 9; y <= Y + 11; y++) {
					left += world().getBlock(NEAR_X, y, NEAR_Z) == pointed() ? 1 : 0;
				}
				return left + " left, hurt " + (hurts.entries.isEmpty() ? "none" : hurts.entries.get(0).split(" ")[0]) + ", drops " + droppedNear();
			});
			assertEqual("0 left, hurt fallingStalactite, drops 3 pointed dripstone", got, "the fallen stalactite");
		} finally {
			MinecraftForge.EVENT_BUS.unregister(hurts);
			onServer(this::clearNear);
		}
	}

	/**
	 * A pig dropped 4.25 onto a stalagmite's tip: 26.3's fall of 2.5 more at twice the damage (`calculateFallDamage`:
	 * floor((fall + 1e-6 - 3) x modifier)), floor((4.25 + 2.5 - 3) x 2) = 7, as `stalagmite` (a fall between whole numbers,
	 * so no rounding at the floor).
	 */
	public void testStalagmiteFallDamage() {
		EntityPig pig = onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			clearNear();
			place(NEAR_X, Y - 1, NEAR_Z, 1, 60.0F);
			EntityPig spawned = new EntityPig(world());
			spawned.setLocationAndAngles(NEAR_X + 0.5D, Y + 11.0D / 16.0D + 4.25D, NEAR_Z + 0.5D, 0.0F, 0.0F);
			spawned.setHealth(10.0F);
			world().spawnEntityInWorld(spawned);
			return spawned;
		});
		Hurts hurts = new Hurts(pig.getEntityId());
		MinecraftForge.EVENT_BUS.register(hurts);
		try {
			serverTicks(40);
			assertEqual("[stalagmite 7.0]", onServer(() -> hurts.entries.toString()), "landing on a stalagmite's tip");
		} finally {
			MinecraftForge.EVENT_BUS.unregister(hurts);
			onServer(this::clearNear);
		}
	}

	// ---------------------------------------------------------------- growth, cauldrons, mud

	/** Random ticks (seeded) on a stalactite hanging from a dripstone block under water: it grows down or a stalagmite grows below; without the water, none. */
	public void testGrowsUnderWater() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (boolean water : new boolean[]{true, false}) {
				clear();
				world().setBlock(X, Y + 7, Z, water ? Blocks.water : Blocks.stone, 0, 2);
				for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					world().setBlock(X + side[0], Y + 7, Z + side[1], Blocks.stone, 0, 2);
				}
				world().setBlock(X, Y + 8, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y + 6, Z, block("dripstone_block"), 0, 2);
				place(X, Y + 6, Z, 0, -60.0F);
				Random random = new Random(5);
				for (int i = 0; i < 3000; i++) {
					((io.github.asgerjon.mcfly.base.RandomTicking) pointed()).randomTick(world(), X, Y + 5, Z, random);
				}
				int count = 0;
				for (int y = Y; y <= Y + 5; y++) {
					count += world().getBlock(X, y, Z) == pointed() ? 1 : 0;
				}
				result.append(result.length() == 0 ? "" : "; ").append(water ? "under water " : "dry ").append(count > 1 ? "grew" : "stayed " + count);
			}
			return result.toString();
		});
		assertEqual("under water grew; dry stayed 1", got, "growth");
	}

	/**
	 * A stalactite's tip 3 over an empty cauldron, a fluid two over its top: a drip (the random value forced) fills the
	 * cauldron 50 + 3 ticks on, water a level, lava a full lava cauldron; mud there instead turns to clay.
	 */
	public void testDripsIntoCauldronsAndMudToClay() {
		StringBuilder result = new StringBuilder();
		for (Block source : new Block[]{Blocks.water, Blocks.lava, block("mud")}) {
			onServer(() -> {
				clear();
				world().setBlock(X, Y, Z, Blocks.cauldron, 0, 2);
				world().setBlock(X, Y + 5, Z, source, 0, 2);
				for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					world().setBlock(X + side[0], Y + 5, Z + side[1], Blocks.stone, 0, 2);
				}
				world().setBlock(X, Y + 6, Z, Blocks.stone, 0, 2);
				world().setBlock(X, Y + 4, Z, Blocks.stone, 0, 2);
				place(X, Y + 4, Z, 0, -60.0F);
				Random forced = new Random() {
					@Override
					public float nextFloat() {
						return 0.0F;
					}
				};
				((io.github.asgerjon.mcfly.base.RandomTicking) pointed()).randomTick(world(), X, Y + 3, Z, forced);
			});
			serverTicks(70);
			String cauldron = onServer(() -> {
				Block there = world().getBlock(X, Y, Z);
				String name = Block.blockRegistry.getNameForObject(there).replace(McFly.DOMAIN + ":", "").replace("minecraft:", "");
				return name + ":" + world().getBlockMetadata(X, Y, Z) + ", above " + Block.blockRegistry.getNameForObject(world().getBlock(X, Y + 5, Z))
					.replace(McFly.DOMAIN + ":", "").replace("minecraft:", "");
			});
			result.append(result.length() == 0 ? "" : "; ").append(cauldron);
		}
		assertEqual("cauldron:1, above water; lava_cauldron:0, above lava; cauldron:0, above clay", result.toString(), "drips into cauldrons");
	}

	// ---------------------------------------------------------------- drips, tridents, recipes

	/**
	 * A free-hanging tip's drips (26.3's `animateTick`, seeded, the particle setting All): under water, one each display
	 * tick whose value is 0.12 or less; dry, those under 0.02 only.
	 */
	public void testDripParticles() {
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(0.5D, 4.0D, 0.5D, 0.0F, 0.0F);
			for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				world().setBlock(SEEN_X + side[0], SEEN_Y + 2, SEEN_Z + side[1], Blocks.stone, 0, 2);
			}
			world().setBlock(SEEN_X, SEEN_Y + 3, SEEN_Z, Blocks.stone, 0, 2);
			world().setBlock(SEEN_X, SEEN_Y + 2, SEEN_Z, Blocks.water, 0, 2);
			world().setBlock(SEEN_X, SEEN_Y + 1, SEEN_Z, Blocks.stone, 0, 2);
			world().setBlock(SEEN_X, SEEN_Y, SEEN_Z, pointed(), 0, 2);
			world().setBlock(SEEN_X, SEEN_Y - 1, SEEN_Z, Blocks.air, 0, 2);
		});
		try {
			waitUntilOnClient("the client's tip under water", () -> mc().theWorld.getBlock(SEEN_X, SEEN_Y, SEEN_Z) == pointed()
				&& mc().theWorld.getBlock(SEEN_X, SEEN_Y + 2, SEEN_Z) == Blocks.water, 60);
			String wet = onClient(() -> drips(3L, 0.12F, true));
			onServer(() -> world().setBlock(SEEN_X, SEEN_Y + 2, SEEN_Z, Blocks.stone, 0, 2));
			waitUntilOnClient("the client's tip dry", () -> mc().theWorld.getBlock(SEEN_X, SEEN_Y + 2, SEEN_Z) == Blocks.stone, 40);
			String dry = onClient(() -> drips(4L, 0.02F, false));
			assertEqual("as 26.3's", wet, "drips under water");
			assertEqual("as 26.3's", dry, "drips dry");
		} finally {
			onServer(() -> {
				for (int y = SEEN_Y - 1; y <= SEEN_Y + 3; y++) {
					world().setBlock(SEEN_X, y, SEEN_Z, Blocks.air, 0, 2);
				}
				for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					world().setBlock(SEEN_X + side[0], SEEN_Y + 2, SEEN_Z + side[1], Blocks.air, 0, 2);
				}
			});
		}
	}

	private String drips(long seed, float chance, boolean inclusive) {
		int expected = 0;
		Random replay = new Random(seed);
		for (int i = 0; i < 2000; i++) {
			float value = replay.nextFloat();
			expected += (inclusive ? value <= chance : value < chance) ? 1 : 0;
		}
		int setting = mc().gameSettings.particleSetting;
		List<EntityFX> before = particlesAtTheTip();
		try {
			mc().gameSettings.particleSetting = 0;
			Random random = new Random(seed);
			for (int i = 0; i < 2000; i++) {
				pointed().randomDisplayTick(mc().theWorld, SEEN_X, SEEN_Y, SEEN_Z, random);
			}
		} finally {
			mc().gameSettings.particleSetting = setting;
		}
		List<EntityFX> made = particlesAtTheTip();
		made.removeAll(before);
		return made.size() == expected ? "as 26.3's" : made.size() + " of " + expected;
	}

	@SuppressWarnings("unchecked")
	private List<EntityFX> particlesAtTheTip() {
		List<?>[] layers = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(net.minecraft.client.particle.EffectRenderer.class, mc().effectRenderer,
			"fxLayers", "field_78876_b");
		List<EntityFX> found = new ArrayList<>();
		for (List<?> layer : layers) {
			for (Object entry : layer) {
				EntityFX particle = (EntityFX) entry;
				if (Math.abs(particle.posX - (SEEN_X + 0.5D)) < 0.2D && Math.abs(particle.posZ - (SEEN_Z + 0.5D)) < 0.2D && particle.posY > SEEN_Y - 0.1D
					&& particle.posY < SEEN_Y + 0.5D) {
					found.add(particle);
				}
			}
		}
		return found;
	}

	/** A thrown trident faster than 0.6 breaks the stalagmite tip it hits (26.3's `onProjectileHit`), dropping it; a slower one doesn't. */
	public void testTridentBreaksATip() {
		String got = onServer(() -> {
			StringBuilder result = new StringBuilder();
			for (double speed : new double[]{1.5D, 0.3D}) {
				clear();
				place(X, Y - 1, Z, 1, 60.0F);
				ganymedes01.etfuturum.entities.EntityThrownTrident thrown = new ganymedes01.etfuturum.entities.EntityThrownTrident(world());
				thrown.setItem(new ItemStack(ganymedes01.etfuturum.command.modern.arguments.ItemIds.byId("minecraft:trident").item()));
				thrown.setPosition(X + 0.5D, Y + 0.4D, Z - 0.5D);
				thrown.motionZ = speed;
				world().spawnEntityInWorld(thrown);
				for (int i = 0; i < 6; i++) {
					thrown.onUpdate();
				}
				result.append(result.length() == 0 ? "" : "; ").append(speed).append(": ").append(world().getBlock(X, Y, Z) == pointed() ? "stands" : "broken")
					.append(" ").append(dropped().contains("pointed_dripstone") ? "dropped" : "no drop");
				thrown.setDead();
			}
			return result.toString();
		});
		assertEqual("1.5: broken dropped; 0.3: stands no drop", got, "a trident at a tip");
	}

	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("dripstone_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(1, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
