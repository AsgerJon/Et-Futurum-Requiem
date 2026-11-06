package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The chain, the lanterns and the soul torch as our own (SPEC-modern-movement.md §88.5, step D), as 26.3's: ours, EFR's
 * classes gone; the stored form EFR wrote, read as before; strength, light and sounds; the lantern placed hanging or
 * standing by where the player looks (26.3's `getStateForPlacement`), held by what supports the middle of the face it
 * touches and gone when that goes; the chain along the clicked face's axis; the soul torch's flame every display tick;
 * 26.3's recipes (`netherlights_recipes_26_3.txt`). At z 120 on the flat world.
 */
@TasSuite("netherlights")
public class NetherLightTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 120;
	private static final String[] EFR_CLASSES = {"blocks.BlockChain", "client.renderer.block.BlockChainRenderer", "blocks.BlockLantern",
		"blocks.itemblocks.ItemBlockLantern", "client.renderer.block.BlockLanternRenderer", "blocks.BlockSoulTorch", "client.particle.SoulFlameFX"};

	@Override
	protected void setUp() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
	}

	@Override
	protected void tearDown() {
		clear();
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	private void clear() {
		onServer(() -> {
			for (int x = X - 3; x <= X + 6; x++) {
				for (int y = Y; y <= Y + 4; y++) {
					for (int z = Z - 3; z <= Z + 3; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			for (Object entity : new ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof EntityItem) {
					((EntityItem) entity).setDead();
				}
			}
		});
	}

	private static Block block(String name) {
		Object block = Block.blockRegistry.getObject(McFly.DOMAIN + ":" + name);
		assertTrue(block instanceof Block && block != Blocks.air, "no " + McFly.DOMAIN + ":" + name);
		return (Block) block;
	}

	private static String props(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		StringBuilder out = new StringBuilder(found.type().id.replace("minecraft:", ""));
		for (Map.Entry<String, String> entry : found.state().properties().entrySet()) {
			out.append(',').append(entry.getKey()).append('=').append(entry.getValue());
		}
		return out.toString();
	}

	// ---------------------------------------------------------------- ours

	public void testLightsAreOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : new String[]{"chain", "lantern", "soul_lantern", "soul_torch"}) {
			String type = block(name).getClass().getName();
			if (!type.startsWith("io.github.asgerjon.mcfly.")) {
				wrong.add(name + " " + type);
			}
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("ganymedes01.etfuturum." + name) != null) {
				wrong.add(name + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the lights' classes");
	}

	/** EFR's metas by 26.3's names: the chain's axis, a lantern's hanging, the soul torch on the floor and on each wall. */
	public void testStoredForm() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (int meta = 0; meta < 3; meta++) {
				out.append(props(block("chain"), meta)).append("; ");
			}
			out.append(props(block("lantern"), 0)).append("; ").append(props(block("lantern"), 1)).append("; ");
			out.append(props(block("soul_lantern"), 1)).append("; ");
			out.append(props(block("soul_torch"), 5)).append("; ");
			for (int meta = 1; meta <= 4; meta++) {
				String vanilla = props(Blocks.torch, meta).replace("wall_torch", "soul_wall_torch");
				String ours = props(block("soul_torch"), meta);
				out.append(ours.equals(vanilla) ? "wall " + meta + " as the torch's" : ours + " not " + vanilla).append("; ");
			}
			return out.toString();
		});
		assertEqual("iron_chain,axis=y; iron_chain,axis=x; iron_chain,axis=z; lantern,hanging=false; lantern,hanging=true; soul_lantern,hanging=true; "
			+ "soul_torch; wall 1 as the torch's; wall 2 as the torch's; wall 3 as the torch's; wall 4 as the torch's; ", got, "26.3's names and states");
	}

	/** 26.3's strength, the tool for drops ("-": none), light and sounds. */
	public void testStrengthLightAndSounds() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			for (String name : new String[]{"chain", "lantern", "soul_lantern", "soul_torch"}) {
				Block block = block(name);
				world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
				world().setBlock(X, Y, Z, block, name.equals("soul_torch") ? 5 : 0, 3);
				boolean byHand = block.canHarvestBlock(player(), world().getBlockMetadata(X, Y, Z));
				out.append(name).append(' ').append(block.getBlockHardness(world(), X, Y, Z)).append(' ')
					.append(block.getExplosionResistance(null, world(), X, Y, Z, X, Y, Z)).append(byHand ? " hand" : " tool").append(" light ")
					.append(block.getLightValue()).append(' ').append(block.stepSound.getBreakSound()).append("; ");
				world().setBlock(X, Y, Z, Blocks.air, 0, 3);
			}
			return out.toString();
		});
		assertEqual("chain 5.0 6.0 tool light 0 " + McFly.MC_ASSETS + ":block.chain.break; lantern 3.5 3.5 hand light 15 " + McFly.MC_ASSETS
			+ ":block.lantern.break; soul_lantern 3.5 3.5 hand light 10 " + McFly.MC_ASSETS + ":block.lantern.break; soul_torch 0.0 0.0 hand light 10 dig.wood; ",
			got, "strength, by hand, light, break sound");
	}

	// ---------------------------------------------------------------- placing

	/** The item used on a face, the player looking at this pitch: what it placed where, as 26.3's name and state. */
	private static String use(String name, int x, int y, int z, int side, float pitch) {
		player().rotationPitch = pitch;
		ItemStack stack = new ItemStack(block(name));
		boolean used = stack.getItem().onItemUse(stack, player(), world(), x, y, z, side, 0.5F, 0.5F, 0.5F);
		int[] offset = {0, -1, 0, 0, 1, 0, 0, 0, -1, 0, 0, 1, -1, 0, 0, 1, 0, 0};
		int px = x + offset[side * 3], py = y + offset[side * 3 + 1], pz = z + offset[side * 3 + 2];
		return used ? props(world().getBlock(px, py, pz), world().getBlockMetadata(px, py, pz)) : "refused";
	}

	/**
	 * A lantern under a stone, the player looking up: it hangs; on a stone, looking down: it stands; looking down under a
	 * stone with nothing below: it hangs (the first look direction it can't survive is passed over); under a chain: it
	 * hangs; the stone under a standing one taken away: it's gone.
	 */
	public void testLanternPlacement() {
		String got = onServer(() -> {
			StringBuilder out = new StringBuilder();
			world().setBlock(X, Y + 3, Z, Blocks.stone, 0, 3);
			out.append("under, looking up ").append(use("lantern", X, Y + 3, Z, 0, -60.0F)).append("; ");
			world().setBlock(X + 2, Y, Z, Blocks.stone, 0, 3);
			out.append("on, looking down ").append(use("lantern", X + 2, Y, Z, 1, 60.0F)).append("; ");
			world().setBlock(X + 4, Y + 3, Z, Blocks.stone, 0, 3);
			out.append("under, looking down ").append(use("lantern", X + 4, Y + 3, Z, 0, 60.0F)).append("; ");
			world().setBlock(X, Y + 3, Z - 2, Blocks.stone, 0, 3);
			world().setBlock(X, Y + 2, Z - 2, block("chain"), 0, 3);
			out.append("under a chain ").append(use("lantern", X, Y + 2, Z - 2, 0, -60.0F)).append("; ");
			world().setBlock(X + 2, Y, Z, Blocks.air, 0, 3);
			out.append("its stone gone ").append(props(world().getBlock(X + 2, Y + 1, Z), world().getBlockMetadata(X + 2, Y + 1, Z)));
			return out.toString();
		});
		assertEqual("under, looking up lantern,hanging=true; on, looking down lantern,hanging=false; under, looking down lantern,hanging=true; "
			+ "under a chain lantern,hanging=true; its stone gone air", got, "where lanterns go");
	}

	/** The chain along the clicked face's axis. */
	public void testChainAxis() {
		String got = onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.stone, 0, 3);
			return use("chain", X, Y + 1, Z, 1, 0.0F) + " " + use("chain", X, Y + 1, Z, 5, 0.0F) + " " + use("chain", X, Y + 1, Z, 3, 0.0F);
		});
		assertEqual("iron_chain,axis=y iron_chain,axis=x iron_chain,axis=z", got, "on the top, the east and the south face");
	}

	/** The soul torch: a soul fire flame every display tick (26.3's `TorchBlock.animateTick`, `soul_fire_flame`). */
	public void testSoulTorchFlames() throws Exception {
		onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
			world().setBlock(X, Y, Z, block("soul_torch"), 5, 3);
			player().playerNetServerHandler.setPlayerLocation(X + 0.5, Y, Z + 3.5, 180.0F, 0.0F);
		});
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posZ - (Z + 3.5)) < 0.01D, 60);
		ticks(2);
		Block torch = block("soul_torch");
		int made = onClient(() -> {
			java.util.Random random = new java.util.Random(1);
			int before = particles("Soul");
			for (int i = 0; i < 50; i++) {
				torch.randomDisplayTick(mc().theWorld, X, Y, Z, random);
			}
			return particles("Soul") - before;
		});
		assertEqual(50, made, "soul flames from 50 display ticks");
	}

	@SuppressWarnings("rawtypes")
	private static int particles(String name) {
		try {
			java.lang.reflect.Field field;
			try {
				field = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("fxLayers");
			} catch (NoSuchFieldException e) {
				field = net.minecraft.client.particle.EffectRenderer.class.getDeclaredField("field_78876_b");
			}
			field.setAccessible(true);
			int count = 0;
			for (List layer : (List[]) field.get(mc().effectRenderer)) {
				for (Object particle : layer) {
					if (particle.getClass().getSimpleName().contains(name)) {
						count++;
					}
				}
			}
			return count;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** 26.3's recipes for the chain and the lights (its jar's, `netherlights_recipes_26_3.txt`). */
	public void testRecipesAs26_3() throws Exception {
		List<String> lines = Recipes26_3.lines("netherlights_recipes_26_3.txt");
		List<String> wrong = onServer(() -> Recipes26_3.wrong(lines, world()));
		assertEqual(4, lines.size(), "26.3's recipe lines");
		assertEqual("[]", Recipes26_3.shown(wrong), "recipes not as 26.3's");
	}
}
