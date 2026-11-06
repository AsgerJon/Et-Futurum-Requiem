package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * McFly's sculk, sculk vein and sculk catalyst (SPEC-modern-movement.md §90.9, steps H3a and H3b), as 26.3's
 * (`SculkBlock`, `SculkVeinBlock`, `SculkCatalystBlock` and its block entity, `SculkSpreader`): EFR's ids, McFly's
 * classes, EFR's gone; their strength, drops and experience; the catalyst taking a dying mob's experience as cursors (no
 * orbs), its bloom of 8 ticks, its cursors saved; the charge spreading sculk and veins over stone, within 24; a vein's
 * faces off over sculk; sculk growing a sensor. On a stone floor at y 7 (x 6 to 30, z 6 to 14); the player on the flat ground at (20.5, 4, -14.5), out of the
 * catalyst's 8.
 */
@TasSuite("sculk")
public class SculkTests extends TasTestCase {

	private static final int X = 10, Y = 8, Z = 10;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(20.5D, 4.0D, -14.5D, 0.0F, 0.0F);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	private static void clear() {
		for (int x = X - 4; x <= X + 20; x++) {
			for (int z = Z - 4; z <= Z + 4; z++) {
				for (int y = Y - 1; y <= Y + 4; y++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 21, Y + 6, Z + 5))) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((Entity) entity).setDead();
			}
		}
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject("etfuturum:" + name);
		return block instanceof Block && block != Blocks.air ? (Block) block : null;
	}

	private void needBlocks() {
		assertTrue(block("sculk") != null && block("sculk_catalyst") != null, "no sculk and sculk_catalyst blocks");
	}

	private static NBTTagCompound saved(int x, int y, int z) {
		TileEntity tile = world().getTileEntity(x, y, z);
		NBTTagCompound tag = new NBTTagCompound();
		if (tile != null) {
			tile.writeToNBT(tag);
		}
		return tag;
	}

	/** The cursors' charge, summed, from the catalyst's saved form. */
	private static int charge(NBTTagCompound tag) {
		NBTTagList cursors = tag.getTagList("cursors", 10);
		int charge = 0;
		for (int i = 0; i < cursors.tagCount(); i++) {
			charge += cursors.getCompoundTagAt(i).getInteger("charge");
		}
		return charge;
	}

	/** A pig at (x, y, z) killed by the player (so that 1.7.10 would drop its orbs, were they not taken). */
	private static EntityPig killedPig(double x, double y, double z) {
		EntityPig pig = new EntityPig(world());
		pig.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
		world().spawnEntityInWorld(pig);
		pig.attackEntityFrom(DamageSource.causePlayerDamage(player()), 100.0F);
		return pig;
	}

	// ---------------------------------------------------------------- ours

	/** McFly's classes under EFR's ids; EFR's blocks, tile entity and event handler gone. */
	public void testOursAndEfrsGone() {
		needBlocks();
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"sculk", "sculk_catalyst"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : new String[]{"blocks.BlockSculk", "blocks.BlockSculkCatalyst", "tileentities.TileEntitySculkCatalyst", "core.handlers.SculkEventHandler"}) {
			try {
				if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
					wrong.add(name + " still there");
				}
			} catch (java.io.IOException e) {
				wrong.add(name + " " + e);
			}
		}
		String tile = onServer(() -> {
			world().setBlock(X, Y - 1, Z, block("sculk_catalyst"), 0, 3);
			return saved(X, Y - 1, Z).getString("id");
		});
		assertEqual("[] etfuturum.sculk_catalyst", wrong + " " + tile, "sculk's classes, the catalyst's block entity id");
	}

	/**
	 * 26.3's: sculk 0.2, the catalyst 3 and light 6, the hoe for both; to Silk Touch they drop themselves, else nothing
	 * but experience (sculk 1, the catalyst 5).
	 */
	public void testProperties() {
		needBlocks();
		String got = onServer(() -> {
			Block sculk = block("sculk"), catalyst = block("sculk_catalyst");
			world().setBlock(X, Y - 1, Z, sculk, 0, 3);
			world().setBlock(X + 1, Y - 1, Z, catalyst, 0, 3);
			return sculk.getBlockHardness(world(), X, Y - 1, Z) + " " + sculk.getHarvestTool(0) + " " + sculk.getExpDrop(world(), 0, 0) + " drops "
				+ sculk.getDrops(world(), X, Y - 1, Z, 0, 0).size() + " silk " + sculk.canSilkHarvest(world(), player(), X, Y - 1, Z, 0) + "; "
				+ catalyst.getBlockHardness(world(), X + 1, Y - 1, Z) + " " + catalyst.getLightValue() + " " + catalyst.getHarvestTool(0) + " "
				+ catalyst.getExpDrop(world(), 0, 0) + " drops " + catalyst.getDrops(world(), X + 1, Y - 1, Z, 0, 0).size() + " silk "
				+ catalyst.canSilkHarvest(world(), player(), X + 1, Y - 1, Z, 0);
		});
		assertEqual("0.2 hoe 1 drops 0 silk true; 3.0 6 hoe 5 drops 0 silk true", got, "strength, drops, experience");
	}

	// ---------------------------------------------------------------- the catalyst

	/**
	 * A pig dying 3 off a catalyst (26.3's `CatalystListener`, `entity_die` out to 8): its experience as cursors at the
	 * block above where it died (1 to 3, a pig's), the bloom (meta 1, a tick due in 8); no orbs after its death's 20
	 * ticks. A pig dying far off drops its orbs as ever. Both killed by the player, as 1.7.10 asks for orbs.
	 */
	public void testCatalystTakesExperience() {
		needBlocks();
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, block("sculk_catalyst"), 0, 3);
			killedPig(X + 3.5D, Y, Z + 0.5D);
			NBTTagCompound tag = saved(X, Y - 1, Z);
			NBTTagList cursors = tag.getTagList("cursors", 10);
			int[] pos = cursors.tagCount() == 0 ? new int[0] : cursors.getCompoundTagAt(0).getIntArray("pos");
			int charge = charge(tag);
			return cursors.tagCount() + " cursor at " + java.util.Arrays.toString(pos) + ", charge " + (charge >= 1 && charge <= 3 ? "1 to 3" : String.valueOf(charge))
				+ ", bloom " + world().getBlockMetadata(X, Y - 1, Z);
		});
		assertEqual("1 cursor at [" + (X + 3) + ", " + Y + ", " + Z + "], charge 1 to 3, bloom 1", got, "the catalyst took it");
		waitUntil("the bloom's end", () -> onServer(() -> world().getBlockMetadata(X, Y - 1, Z) == 0), 30);
		onServer(() -> {
			EntityPig far = new EntityPig(world());
			far.setLocationAndAngles(X + 19.5D, Y, Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(far);
			far.attackEntityFrom(DamageSource.causePlayerDamage(player()), 100.0F);
		});
		serverTicks(30);
		String orbs = onServer(() -> {
			int near = world().getEntitiesWithinAABB(EntityXPOrb.class, AxisAlignedBB.getBoundingBox(X - 2, Y - 2, Z - 4, X + 8, Y + 4, Z + 4)).size();
			int farOrbs = world().getEntitiesWithinAABB(EntityXPOrb.class, AxisAlignedBB.getBoundingBox(X + 14, Y - 2, Z - 4, X + 24, Y + 4, Z + 4)).size();
			return "near " + near + ", far " + (farOrbs > 0 ? "some" : "none");
		});
		assertEqual("near 0, far some", orbs, "orbs");
	}

	/** The catalyst's cursors saved (26.3's `cursors`: `pos`, `charge`, `decay_delay`, `update_delay`) and loaded back the same. */
	public void testCursorsSaved() {
		needBlocks();
		String got = onServer(() -> {
			world().setBlock(X, Y - 1, Z, block("sculk_catalyst"), 0, 3);
			killedPig(X + 2.5D, Y, Z + 0.5D);
			NBTTagCompound tag = saved(X, Y - 1, Z);
			NBTTagCompound cursor = tag.getTagList("cursors", 10).getCompoundTagAt(0);
			TileEntity loaded = TileEntity.createAndLoadEntity(tag);
			NBTTagCompound again = new NBTTagCompound();
			if (loaded != null) {
				loaded.writeToNBT(again);
			}
			return cursor.hasKey("pos") + " " + cursor.hasKey("charge") + " " + cursor.hasKey("decay_delay") + " " + cursor.hasKey("update_delay") + "; loaded "
				+ (charge(again) == charge(tag) && charge(tag) > 0 ? "the same" : charge(again) + " for " + charge(tag));
		});
		assertEqual("true true true true; loaded the same", got, "the cursors saved");
	}

	// ---------------------------------------------------------------- spreading (H3b)

	private void needVein() {
		assertTrue(block("sculk_vein") != null, "no sculk_vein block");
	}

	private static io.github.asgerjon.mcfly.sculk.SculkCatalystBlockEntity catalyst(int x, int y, int z) {
		TileEntity tile = world().getTileEntity(x, y, z);
		return tile instanceof io.github.asgerjon.mcfly.sculk.SculkCatalystBlockEntity ? (io.github.asgerjon.mcfly.sculk.SculkCatalystBlockEntity) tile : null;
	}

	/** 26.3's vein: McFly's, 0.2, the hoe, no collision; nothing dropped but to Silk Touch, nor to shears. */
	public void testVeinProperties() {
		needBlocks();
		needVein();
		String got = onServer(() -> {
			Block vein = block("sculk_vein");
			io.github.asgerjon.mcfly.base.McFlyMultiface multiface = (io.github.asgerjon.mcfly.base.McFlyMultiface) vein;
			multiface.addFace(world(), X, Y, Z, 0, 3);
			return vein.getClass().getName().startsWith("io.github.asgerjon.mcfly.") + " " + vein.getBlockHardness(world(), X, Y, Z) + " " + vein.getHarvestTool(0)
				+ " collision " + vein.getCollisionBoundingBoxFromPool(world(), X, Y, Z) + " drops " + vein.getDrops(world(), X, Y, Z, 0, 0).size() + " shears "
				+ ((net.minecraftforge.common.IShearable) vein).isShearable(new net.minecraft.item.ItemStack(net.minecraft.init.Items.shears), world(), X, Y, Z);
		});
		assertEqual("true 0.2 hoe collision null drops 0 shears false", got, "the vein");
	}

	/**
	 * 26.3's `SculkVeinBlock.onDischarged`: a vein's faces against sculk go, the rest stay; none left, it's gone. A vein
	 * on the floor and a wall, the floor turned to sculk: the wall's face kept; one on the floor only: gone.
	 */
	public void testVeinFacesOffOverSculk() {
		needBlocks();
		needVein();
		String got = onServer(() -> {
			io.github.asgerjon.mcfly.sculk.SculkVeinBlock vein = (io.github.asgerjon.mcfly.sculk.SculkVeinBlock) block("sculk_vein");
			world().setBlock(X, Y, Z - 1, Blocks.stone, 0, 2);
			vein.addFace(world(), X, Y, Z, 0, 3);
			vein.addFace(world(), X, Y, Z, 2, 3);
			vein.addFace(world(), X + 2, Y, Z, 0, 3);
			world().setBlock(X, Y - 1, Z, block("sculk"), 0, 2);
			world().setBlock(X + 2, Y - 1, Z, block("sculk"), 0, 2);
			vein.onDischarged(world(), X, Y, Z, new Random(1));
			vein.onDischarged(world(), X + 2, Y, Z, new Random(1));
			return "two faces: " + (world().getBlock(X, Y, Z) == vein ? "faces " + io.github.asgerjon.mcfly.base.McFlyMultiface.faces(world(), X, Y, Z) : "gone")
				+ ", the floor's only: " + (world().getBlock(X + 2, Y, Z) == vein ? "still there" : "gone");
		});
		assertEqual("two faces: faces 4, the floor's only: gone", got, "faces over sculk");
	}

	/**
	 * A catalyst's charge of 80 dropped 6 off on a stone floor: it spreads until spent (26.3's `updateCursors`),
	 * turning floor stone to sculk, with veins at the edge; none of it past 24 from the catalyst.
	 */
	public void testSpreading() {
		needBlocks();
		needVein();
		onServer(() -> {
			world().setBlock(X, Y - 1, Z, block("sculk_catalyst"), 0, 3);
			catalyst(X, Y - 1, Z).spreader().addCursors(X + 6, Y, Z, 80);
		});
		waitUntil("the charge spent", () -> onServer(() -> catalyst(X, Y - 1, Z).spreader().cursors().isEmpty()), 1200);
		String got = onServer(() -> {
			int sculk = 0, beyond = 0;
			Block block = block("sculk");
			for (int x = X - 26; x <= X + 26; x++) {
				for (int z = Z - 26; z <= Z + 26; z++) {
					for (int y = Y - 3; y <= Y + 3; y++) {
						if (world().getBlock(x, y, z) == block) {
							sculk++;
							if (Math.max(Math.abs(x - X), Math.abs(z - Z)) > 24) {
								beyond++;
							}
						}
					}
				}
			}
			return (sculk >= 3 ? "sculk" : "sculk " + sculk) + ", " + beyond + " past 24";
		});
		assertEqual("sculk, 0 past 24", got, "the spread");
	}

	/**
	 * 26.3's `SculkBlock.attemptUseCharge` and its growth: sculk 8 off the catalyst with air above and a charge of 20, on
	 * a roll that decays (1 in 10), grows (under the charge) and isn't a shrieker (1 in 11): a sensor above it, the charge
	 * less the growth's 10.
	 */
	public void testGrowth() {
		needBlocks();
		needVein();
		long seed = 0;
		for (long s = 0; s < 10000; s++) {
			Random roll = new Random(s);
			if (roll.nextInt(10) == 0 && roll.nextInt(10) < 20 && roll.nextInt(11) != 0) {
				seed = s;
				break;
			}
		}
		long chosen = seed;
		String got = onServer(() -> {
			io.github.asgerjon.mcfly.sculk.SculkBlock sculk = (io.github.asgerjon.mcfly.sculk.SculkBlock) block("sculk");
			world().setBlock(X, Y - 1, Z, block("sculk_catalyst"), 0, 3);
			world().setBlock(X + 8, Y - 1, Z, sculk, 0, 3);
			io.github.asgerjon.mcfly.sculk.SculkSpreader spreader = io.github.asgerjon.mcfly.sculk.SculkSpreader.createLevelSpreader();
			io.github.asgerjon.mcfly.sculk.SculkSpreader.ChargeCursor cursor = new io.github.asgerjon.mcfly.sculk.SculkSpreader.ChargeCursor(X + 8, Y - 1, Z, 20);
			int left = sculk.attemptUseCharge(cursor, world(), X, Y - 1, Z, new Random(chosen), spreader, true);
			Block above = world().getBlock(X + 8, Y, Z);
			return "left " + left + ", above " + Block.blockRegistry.getNameForObject(above);
		});
		assertEqual("left 10, above etfuturum:sculk_sensor", got, "growth");
	}

	// ---------------------------------------------------------------- the charge's particles (H3c)

	/** The client's sculk particles, counted by kind from its effect renderer's lit layer (3, McFly's). */
	private static String sculkParticles() {
		try {
			java.lang.reflect.Field layers = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("fxLayers");
			layers.setAccessible(true);
			List<?>[] fx = (List<?>[]) layers.get(mc().effectRenderer);
			int count = 0;
			for (Object particle : fx[3]) {
				if (particle.getClass().getName().equals("io.github.asgerjon.mcfly.sculk.client.SculkParticle")) {
					count++;
				}
			}
			return count > 0 ? "some" : "none";
		} catch (ReflectiveOperationException e) {
			return e.toString();
		}
	}

	/** 26.3's level event 3006 on the client: a charge of 3 on a face (its `sculk_charge`), then a pop over sculk (40 `sculk_charge_pop`). */
	public void testChargeParticles() {
		needBlocks();
		onServer(() -> {
			player().playerNetServerHandler.setPlayerLocation(X + 0.5D, Y, Z - 3.5D, 0.0F, 0.0F);
			world().setBlock(X, Y - 1, Z, block("sculk"), 0, 3);
		});
		waitUntilOnClient("at the floor", () -> Math.abs(mc().thePlayer.posZ - (Z - 3.5D)) < 0.01, 100);
		onServer(() -> {
			io.github.asgerjon.mcfly.network.LevelEventMessage.sendAround(world(), io.github.asgerjon.mcfly.network.LevelEventMessage.SCULK_CHARGE, X, Y, Z, 3 << 6 | 1);
			io.github.asgerjon.mcfly.network.LevelEventMessage.sendAround(world(), io.github.asgerjon.mcfly.network.LevelEventMessage.SCULK_CHARGE, X, Y - 1, Z, 0);
		});
		ticks(2);
		String got = onClient(SculkTests::sculkParticles);
		assertEqual("some", got, "sculk particles on the client");
	}
}
