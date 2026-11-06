package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.Spyglass;
import ganymedes01.etfuturum.offhand.Offhand;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.WorldSettings;

/**
 * The spyglass (SPEC-modern-movement.md §64.5; 26.3 {@code SpyglassItem}, {@code Player.isScoping}): its recipe, using
 * it, the zoom (0.1 of the field of view, first person only), the slower mouse, the scope's easing scale, the hidden
 * hands, the arm raised to the eye.
 */
@TasSuite("quick5")
public class SpyglassTests extends TasTestCase {

	private static final String USE = McFly.MC_ASSETS + ":item.spyglass.use";
	private static final String STOP = McFly.MC_ASSETS + ":item.spyglass.stop_using";

	@Override
	protected void setUp() {
		if (!McFlyItems.SPYGLASS.isEnabled()) {
			skipTest("the spyglass is off");
		}
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
		});
	}

	@Override
	protected void tearDown() {
		keyUp(mc().gameSettings.keyBindUseItem);
		cameraView(0);
		onClient(() -> {
			mc().gameSettings.smoothCamera = false;
		});
		setHeld(null);
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
	}

	/** An amethyst shard over two copper ingots, in one column. */
	public void testRecipe() {
		ItemStack made = onServer(() -> {
			InventoryCrafting grid = new InventoryCrafting(new Container() {
				@Override
				public boolean canInteractWith(EntityPlayer player) {
					return true;
				}
			}, 3, 3);
			grid.setInventorySlotContents(1, io.github.asgerjon.mcfly.registry.McFlyItems.AMETHYST_SHARD.newItemStack());
			grid.setInventorySlotContents(4, io.github.asgerjon.mcfly.registry.McFlyItems.COPPER_INGOT.newItemStack());
			grid.setInventorySlotContents(7, io.github.asgerjon.mcfly.registry.McFlyItems.COPPER_INGOT.newItemStack());
			return CraftingManager.getInstance().findMatchingRecipe(grid, world());
		});
		assertIsNotNone(made, "the recipe makes nothing");
		assertEqual(McFlyItems.SPYGLASS.get(), made.getItem(), "the recipe's item");
		assertEqual(1, made.getMaxStackSize(), "the spyglass's stack size");
	}

	/** Held right click scopes (on both sides), with the use sound; letting go stops, with its sound. */
	public void testUseAndSounds() {
		setHeld(McFlyItems.SPYGLASS.newItemStack());
		clearSounds();
		keyDown(mc().gameSettings.keyBindUseItem);
		waitUntilOnClient("scoping", () -> Spyglass.isScoping(mc().thePlayer), 10);
		waitUntil("scoping on the server", () -> Spyglass.isScoping(player()), 10);
		waitForSound(USE, 10);
		assertEqual(1200, onServer(() -> McFlyItems.SPYGLASS.get().getMaxItemUseDuration(McFlyItems.SPYGLASS.newItemStack())), "its use time");
		keyUp(mc().gameSettings.keyBindUseItem);
		waitUntilOnClient("not scoping", () -> !Spyglass.isScoping(mc().thePlayer), 10);
		waitForSound(STOP, 10);
	}

	/** Scoping in first person the field of view is 0.1 of normal; in third person it isn't. */
	public void testZoom() {
		setHeld(McFlyItems.SPYGLASS.newItemStack());
		float normal = onClient(() -> mc().thePlayer.getFOVMultiplier());
		keyDown(mc().gameSettings.keyBindUseItem);
		waitUntilOnClient("scoping", () -> Spyglass.isScoping(mc().thePlayer), 10);
		ticks(2);
		assertAlmostEqual(0.1D, onClient(() -> mc().thePlayer.getFOVMultiplier()), 1.0E-6D);
		cameraView(1);
		ticks(2);
		assertAlmostEqual(normal, onClient(() -> mc().thePlayer.getFOVMultiplier()), 1.0E-6D);
	}

	/** The mouse turns an eighth as far while scoping in first person; not with the cinematic camera, not in third person. */
	public void testSlowerMouse() {
		setHeld(McFlyItems.SPYGLASS.newItemStack());
		assertAlmostEqual(1.0D, onClient(Spyglass::turnScale), 1.0E-6D);
		keyDown(mc().gameSettings.keyBindUseItem);
		waitUntilOnClient("scoping", () -> Spyglass.isScoping(mc().thePlayer), 10);
		assertAlmostEqual(0.125D, onClient(Spyglass::turnScale), 1.0E-6D);
		onClient(() -> {
			mc().gameSettings.smoothCamera = true;
		});
		assertAlmostEqual(1.0D, onClient(Spyglass::turnScale), 1.0E-6D);
		onClient(() -> {
			mc().gameSettings.smoothCamera = false;
		});
		cameraView(1);
		assertAlmostEqual(1.0D, onClient(Spyglass::turnScale), 1.0E-6D);
	}

	/** The scope eases from half to 1.125 of the screen's short side; the hands are hidden while it's up. */
	public void testScopeAndHands() {
		setHeld(McFlyItems.SPYGLASS.newItemStack());
		assertFalse(onClient(Spyglass::hidesHands), "hands hidden before scoping");
		keyDown(mc().gameSettings.keyBindUseItem);
		waitUntilOnClient("scoping", () -> Spyglass.isScoping(mc().thePlayer), 10);
		ticks(1);
		float early = onClient(Spyglass::scopeScale);
		ticks(20);
		float late = onClient(Spyglass::scopeScale);
		assertTrue(early < 1.1F, "the scope at its full size at once (" + early + ")");
		assertAlmostEqual(1.125D, late, 0.005D);
		assertTrue(onClient(Spyglass::hidesHands), "the hands shown while scoping");
		keyUp(mc().gameSettings.keyBindUseItem);
		ticks(3);
		assertAlmostEqual(0.5D, onClient(Spyglass::scopeScale), 1.0E-6D);
		assertFalse(onClient(Spyglass::hidesHands), "hands hidden after scoping");
	}

	/**
	 * The main hand's arm raised to the eye (head straight): x -1.9198622, y -pi/12 on the right (+ on the left), no bob.
	 * Which arm is the main hand's comes from the player's setting (the user's is left).
	 */
	public void testArmRaisedToTheEye() {
		setHeld(McFlyItems.SPYGLASS.newItemStack());
		keyDown(mc().gameSettings.keyBindUseItem);
		waitUntilOnClient("scoping", () -> Spyglass.isScoping(mc().thePlayer), 10);
		boolean left = onClient(() -> Offhand.isEnabled() && Offhand.isLeftHanded(mc().thePlayer));
		float[] arm = onClient(() -> {
			ModelBiped model = new ModelBiped();
			model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, mc().thePlayer);
			ModelRenderer raised = left ? model.bipedLeftArm : model.bipedRightArm;
			return new float[]{raised.rotateAngleX, raised.rotateAngleY, raised.rotateAngleZ};
		});
		double y = (left ? 1.0D : -1.0D) * Math.PI / 12.0D;
		String seen = "the " + (left ? "left" : "right") + " arm " + arm[0] + " " + arm[1] + " " + arm[2];
		assertTrue(Math.abs(arm[0] + 1.9198622D) < 1.0E-4D, seen + " (x wanted -1.9198622)");
		assertTrue(Math.abs(arm[1] - y) < 1.0E-4D, seen + " (y wanted " + (float) y + ")");
		assertTrue(Math.abs(arm[2]) < 1.0E-4D, seen + " (no bob)");
	}

	/** Another item used doesn't scope. */
	public void testOnlyTheSpyglassScopes() {
		setHeld(new ItemStack(Items.bow));
		onServer(() -> player().inventory.addItemStackToInventory(new ItemStack(Items.arrow)));
		keyDown(mc().gameSettings.keyBindUseItem);
		ticks(5);
		assertFalse(onClient(() -> Spyglass.isScoping(mc().thePlayer)), "drawing a bow scopes");
		assertFalse(onClient(Spyglass::hidesHands), "drawing a bow hides the hands");
	}
}
