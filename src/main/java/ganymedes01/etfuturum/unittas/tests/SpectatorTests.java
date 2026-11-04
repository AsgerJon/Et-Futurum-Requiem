package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Spectator mode as our own (SPEC-modern-movement.md §83): McFly's, under EFR's game type (3, `spectator`), with its
 * rules. Each test on its own spot (x 400), the player back in creative after.
 */
public class SpectatorTests extends TasTestCase {

	private static final int X = 400;
	private static final int Y = 4;
	private static final int Z = 400;

	@Override
	protected void setUp() {
		if (WorldSettings.GameType.getByID(3) == null || WorldSettings.GameType.getByID(3) == WorldSettings.GameType.SURVIVAL) {
			skipTest("spectator mode is off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().setSneaking(false);
			for (Object entity : world().getEntitiesWithinAABB(Entity.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(X - 8, Y - 2, Z - 8, X + 8, Y + 8, Z + 8))) {
				if (entity != player()) {
					((Entity) entity).setDead();
				}
			}
			for (int x = X - 3; x <= X + 3; x++) {
				for (int z = Z - 3; z <= Z + 3; z++) {
					world().setBlockToAir(x, Y, z);
					world().setBlock(x, Y - 1, z, Blocks.stone);
				}
			}
		});
		ticks(2);
	}

	private void spectate() {
		onServer(() -> {
			player().setPositionAndUpdate(X + 0.5, Y, Z + 0.5);
			player().setGameType(WorldSettings.GameType.getByID(3));
		});
		ticks(3);
	}

	/** The classes EFR's spectator mode was (spec §83): none of them is left. */
	static final String[] EFR_CLASSES = {"ganymedes01.etfuturum.spectator.SpectatorMode", "ganymedes01.etfuturum.spectator.SpectatorModeClient",
		"ganymedes01.etfuturum.mixins.early.spectator.MixinEntityPlayer", "ganymedes01.etfuturum.mixins.early.spectator.MixinWorld",
		"ganymedes01.etfuturum.mixins.early.spectator.MixinGameType", "ganymedes01.etfuturum.mixins.early.spectator.client.MixinEntityRenderer",
		"ganymedes01.etfuturum.mixins.late.spectator.MixinContainerIronChest"};

	/** Ours: McFly's spectator class, its mixins applied from McFly's package (none from EFR's), game type 3 named spectator. */
	public void testOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		try {
			Class.forName("io.github.asgerjon.mcfly.spectator.Spectator");
		} catch (ClassNotFoundException e) {
			wrong.add("no McFly Spectator");
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		Set<String> ours = new TreeSet<>();
		for (String target : new String[]{"net.minecraft.entity.player.EntityPlayer", "net.minecraft.world.World", "net.minecraft.inventory.Slot"}) {
			for (Method method : Class.forName(target).getDeclaredMethods()) {
				org.spongepowered.asm.mixin.transformer.meta.MixinMerged merged = method.getAnnotation(org.spongepowered.asm.mixin.transformer.meta.MixinMerged.class);
				if (merged == null) {
					continue;
				}
				if (merged.mixin().startsWith("ganymedes01.etfuturum.mixins.early.spectator.")) {
					wrong.add("EFR's " + merged.mixin().substring(merged.mixin().lastIndexOf('.') + 1) + " in " + target.substring(target.lastIndexOf('.') + 1));
				} else if (merged.mixin().startsWith("io.github.asgerjon.mcfly.mixins.early.spectator.")) {
					ours.add(target.substring(target.lastIndexOf('.') + 1));
				}
			}
		}
		assertEqual("[] [EntityPlayer, Slot, World] spectator", wrong + " " + ours + " " + WorldSettings.GameType.getByID(3).getName(), "spectator mode's classes");
	}

	/** `/gamemode spectator`, its short name and its number all make the player a spectator. */
	public void testGameModeNames() {
		List<String> got = new ArrayList<>();
		for (String name : new String[]{"spectator", "sp", "3"}) {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			command("/gamemode " + name);
			got.add(name + " " + onServer(() -> player().theItemInWorldManager.getGameType().getID()));
		}
		assertEqual("[spectator 3, sp 3, 3 3]", got.toString(), "the game mode by name");
	}

	/** A spectator flies through blocks unseen; back in creative, it doesn't. */
	public void testNoclipAndInvisible() {
		spectate();
		String as = onServer(() -> "noclip " + player().noClip + " invisible " + player().isInvisible() + " flying " + player().capabilities.isFlying);
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
		ticks(3);
		String after = onServer(() -> "noclip " + player().noClip + " invisible " + player().isInvisible());
		assertEqual("noclip true invisible true flying true, then noclip false invisible false", as + ", then " + after, "a spectator's state");
	}

	/** A spectator picks nothing up and breaks nothing. */
	public void testNoPickUpNoBreaking() {
		spectate();
		onServer(() -> {
			world().setBlock(X + 2, Y, Z, Blocks.stone);
			EntityItem item = new EntityItem(world(), player().posX, player().posY, player().posZ, new ItemStack(Items.diamond));
			item.delayBeforeCanPickup = 0;
			world().spawnEntityInWorld(item);
			player().inventory.clearInventory(null, -1);
		});
		serverTicks(10);
		String got = onServer(() -> {
			boolean broke = player().theItemInWorldManager.tryHarvestBlock(X + 2, Y, Z);
			return "picked up " + player().inventory.hasItem(Items.diamond) + ", broke " + broke + " " + (world().getBlock(X + 2, Y, Z) != Blocks.stone);
		});
		assertEqual("picked up false, broke false false", got, "pick-up and breaking");
	}

	/** A spectator opens a chest and sees it, but taking from it is refused. */
	public void testChestSeenNotChanged() {
		onServer(() -> {
			world().setBlock(X + 1, Y, Z, Blocks.chest);
			((TileEntityChest) world().getTileEntity(X + 1, Y, Z)).setInventorySlotContents(0, new ItemStack(Items.emerald, 3));
		});
		spectate();
		onServer(() -> Blocks.chest.onBlockActivated(world(), X + 1, Y, Z, player(), 1, 0.5F, 1.0F, 0.5F));
		ticks(3);
		String got = onServer(() -> {
			boolean open = player().openContainer instanceof ContainerChest;
			if (open) {
				player().openContainer.slotClick(0, 0, 0, player());
			}
			ItemStack left = ((TileEntityChest) world().getTileEntity(X + 1, Y, Z)).getStackInSlot(0);
			player().closeScreen();
			return "open " + open + ", left " + (left == null ? 0 : left.stackSize) + ", holding " + (player().inventory.getItemStack() != null);
		});
		assertEqual("open true, left 3, holding false", got, "the chest");
	}

	/** Mobs looking for the closest player don't find a spectator. */
	public void testNotTheClosestPlayer() {
		spectate();
		boolean found = onServer(() -> world().getClosestPlayer(X + 0.5, Y, Z + 0.5, 8.0) == player());
		assertFalse(found, "the spectator was found as the closest player");
	}

	/** Attacking an entity follows it (its eyes), sneaking lets go. */
	public void testFollowingAndLettingGo() {
		spectate();
		int[] pig = new int[1];
		onServer(() -> {
			EntityPig p = new EntityPig(world());
			p.setPosition(X + 3.5, Y, Z + 0.5);
			world().spawnEntityInWorld(p);
			pig[0] = p.getEntityId();
		});
		ticks(3);
		onClient(() -> mc().playerController.attackEntity(mc().thePlayer, mc().theWorld.getEntityByID(pig[0])));
		serverTicks(5);
		double near = onServer(() -> player().getDistanceToEntity(world().getEntityByID(pig[0])));
		onServer(() -> player().setSneaking(true));
		serverTicks(3);
		onServer(() -> {
			player().setSneaking(false);
			player().setPositionAndUpdate(X + 0.5, Y + 3, Z - 2.5);
		});
		serverTicks(5);
		double after = onServer(() -> player().getDistanceToEntity(world().getEntityByID(pig[0])));
		assertEqual("following true, let go true", "following " + (near < 1.5) + ", let go " + (after > 3.0), "following an entity (" + near + ", " + after + ")");
	}
}
