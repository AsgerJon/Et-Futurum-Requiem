package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.handlers.ClientEventHandler;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.IOffhandItemRenderer;
import ganymedes01.etfuturum.offhand.Offhand;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The user's report (SPEC-modern-movement.md §53.6): in creative, placing a block from the main hand made the off-hand
 * item "hit". Stone goes from the main hand onto the ground two blocks ahead, with each of a few items in the off-hand,
 * right- and left-handed, and both hands are watched tick by tick for ten ticks after the click: the off-hand must
 * neither swing nor dip, nor place anything. Every case that moved it is reported with the ticks.
 */
@TasSuite("quick")
public class OffhandSwingTests extends TasTestCase {

	private static final int X = 0, Y = 3, Z = 2;

	@Override
	protected void setUp() {
		onServer(() -> {
			for (int x = -1; x <= 1; x++) {
				for (int y = 4; y <= 6; y++) {
					for (int z = 1; z <= 3; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
			world().setBlock(X, Y, Z, Blocks.grass, 0, 2);
		});
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			McFlyConfig.mainHandLeft = false;
			ClientEventHandler.sendPlayerSettings();
		});
	}

	public void testMainHandPlacingLeavesTheOffhandStill() {
		if (!Offhand.isEnabled()) {
			skipTest("the off-hand is off");
		}
		ItemStack[] offhands = {new ItemStack(Blocks.torch), new ItemStack(Blocks.dirt), new ItemStack(Items.iron_sword), new ItemStack(Items.bow), new ItemStack(Items.bread)};
		List<String> problems = new ArrayList<>();
		for (boolean left : new boolean[]{false, true}) {
			onClient(() -> {
				McFlyConfig.mainHandLeft = left;
				ClientEventHandler.sendPlayerSettings();
			});
			waitUntilOnClient("the main hand set", () -> Offhand.isLeftHanded(mc().thePlayer) == left, 20);
			for (ItemStack offhand : offhands) {
				String problem = clickGround(new ItemStack(Blocks.stone), offhand, Blocks.stone);
				if (problem != null) {
					problems.add((left ? "left-handed" : "right-handed") + ", " + offhand.getDisplayName() + " in the off-hand: " + problem);
				}
			}
		}
		if (!problems.isEmpty()) {
			fail(String.join("\n", problems));
		}
	}

	/**
	 * What the user saw (2026-10-30, "it dips"): a click on a block that neither hand can use. Modern lowers a hand
	 * only when its use did something; EFR lowered the off-hand in creative whenever it got the click.
	 */
	public void testClickNeitherHandUsesLeavesTheOffhandStill() {
		if (!Offhand.isEnabled()) {
			skipTest("the off-hand is off");
		}
		String problem = clickGround(new ItemStack(Items.stick), new ItemStack(Items.iron_sword), null);
		if (problem != null) {
			fail("stick in the main hand, sword in the off-hand: " + problem);
		}
	}

	/** One right click on the ground ahead; null when the off-hand stayed still and the expected block (or none) is there. */
	private String clickGround(ItemStack main, ItemStack offhand, Block placed) {
		onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 3);
			world().setBlock(X, Y + 2, Z, Blocks.air, 0, 3);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, main.copy());
			Offhand.setOffhand(player(), offhand.copy());
			player().inventoryContainer.detectAndSendChanges();
		});
		waitUntilOnClient("both hands filled", () -> mc().thePlayer.getHeldItem() != null && Offhand.getOffhand(mc().thePlayer) != null
			&& Offhand.getOffhand(mc().thePlayer).getItem() == offhand.getItem(), 40);
		// from the eyes (1.62 up) to the middle of the ground block's top, two blocks ahead
		look(0.0F, (float) Math.toDegrees(Math.atan2(1.62D, 2.0D)));
		waitUntilOnClient("the crosshair on the ground ahead", () -> {
			MovingObjectPosition hit = mc().objectMouseOver;
			return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& hit.blockX == X && hit.blockY == Y && hit.blockZ == Z && hit.sideHit == 1;
		}, 20);
		waitUntilOnClient("both hands raised and no swing", () -> height(Hand.MAIN) >= 0.99F && height(Hand.OFF) >= 0.99F
			&& !mc().thePlayer.isSwingInProgress, 40);

		List<String> ticks = new ArrayList<>();
		boolean moved = false;
		keyDown(mc().gameSettings.keyBindUseItem);
		for (int tick = 1; tick <= 10; tick++) {
			ticks(1);
			if (tick == 1) {
				keyUp(mc().gameSettings.keyBindUseItem);
			}
			Object[] state = onClient(() -> {
				EntityClientPlayerMP player = mc().thePlayer;
				return new Object[]{player.isSwingInProgress ? Offhand.getSwingingHand(player) : null, height(Hand.MAIN), height(Hand.OFF)};
			});
			boolean offSwing = state[0] == Hand.OFF;
			boolean offDip = (Float) state[2] < 0.99F;
			moved |= offSwing || offDip;
			ticks.add(String.format(Locale.ROOT, "t%d swing %s, main %.2f, off %.2f", tick, state[0] == null ? "none" : state[0], state[1], state[2]));
		}
		Block there = onServer(() -> world().getBlock(X, Y + 1, Z));
		boolean rightBlock = placed == null ? there == Blocks.air : there == placed;
		boolean somethingAbove = onServer(() -> !world().isAirBlock(X, Y + 2, Z));
		// the placed block goes again: left two blocks ahead of the start, it stood between later tests and their targets
		onServer(() -> {
			world().setBlock(X, Y + 1, Z, Blocks.air, 0, 3);
			world().setBlock(X, Y + 2, Z, Blocks.air, 0, 3);
		});
		if (!moved && rightBlock && !somethingAbove) {
			return null;
		}
		return (rightBlock ? "" : "on the ground: " + there.getLocalizedName() + "; ") + (somethingAbove ? "something placed on top; " : "")
			+ String.join(" | ", ticks);
	}

	private static float height(Hand hand) {
		return ((IOffhandItemRenderer) mc().entityRenderer.itemRenderer).etfu$equippedProgress(hand);
	}
}
