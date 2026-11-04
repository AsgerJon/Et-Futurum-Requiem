package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSettings;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EFR's banners as they are, before the loom (SPEC-modern-movement.md §66.5, stage 0): older EFR code, unverified until
 * tested. 26.3's recipe (six wool of one colour and a stick, a banner of that colour: the user's word, §66.4), and
 * EFR's crafting-grid patterns off by default (26.3 has none), with copying a banner kept.
 */
public class BannerTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!ConfigBlocksItems.enableBanners) {
			skipTest("banners are off");
		}
	}

	/** The 16 colours by wool's numbers (white 0 to black 15). */
	private static final String[] COLOURS = {"White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray",
		"Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"};

	/** Each colour's six wool and a stick: a banner of that colour, named so ("White Banner" from white wool). */
	public void testRecipeGivesTheWoolsColour() {
		for (int wool = 0; wool < 16; wool++) {
			int colour = wool;
			ItemStack made = onServer(() -> craft(woolRow(colour), woolRow(colour), new ItemStack[] {null, new ItemStack(Items.stick), null}));
			String woolName = COLOURS[colour] + " wool";
			assertTrue(made != null && made.getItem() == Item.getItemFromBlock(ModBlocks.BANNER.get()), woolName + " made " + made);
			assertEqual(colour, TileEntityBanner.getBaseColor(made), "the colour of the banner from " + woolName);
			assertEqual(COLOURS[colour] + " Banner", made.getDisplayName(), "the banner from " + woolName);
		}
	}

	/** Wool of two colours makes no banner: 26.3's recipe takes six of one colour. */
	public void testMixedWoolMakesNothing() {
		ItemStack[] mixed = {new ItemStack(Blocks.wool, 1, 0), new ItemStack(Blocks.wool, 1, 14), new ItemStack(Blocks.wool, 1, 0)};
		ItemStack made = onServer(() -> craft(woolRow(0), mixed, new ItemStack[] {null, new ItemStack(Items.stick), null}));
		assertEqual(null, made, "white and red wool with a stick");
	}

	/**
	 * EFR's crafting-grid patterns are off by default (26.3 has none, the loom adds them; §66.4): a banner under three
	 * dyes in a row (EFR's top stripe) makes nothing; with the option on, it makes the stripe as EFR always did.
	 */
	public void testGridPatternsOffByDefault() {
		assertFalse(McFlyConfig.bannerGridPatterns, "the option's default (bannerGridPatterns)");
		ItemStack[] dyes = {new ItemStack(Items.dye, 1, 1), new ItemStack(Items.dye, 1, 1), new ItemStack(Items.dye, 1, 1)};
		ItemStack[] banner = {null, ModBlocks.BANNER.newItemStack(1, 0), null};
		assertEqual(null, onServer(() -> craft(dyes, banner, new ItemStack[3])), "a banner under three red dyes, by default");
		McFlyConfig.bannerGridPatterns = true;
		try {
			ItemStack made = onServer(() -> craft(dyes, banner, new ItemStack[3]));
			assertTrue(made != null, "with the option on, the stripe is made");
			assertEqual("ts", layer(made, 0), "the layer added");
		} finally {
			McFlyConfig.bannerGridPatterns = false;
		}
	}

	/** Copying a banner's layers onto a plain banner of its colour stays (26.3's BannerDuplicateRecipe), the option off. */
	public void testCopyingStays() {
		ItemStack patterned = ModBlocks.BANNER.newItemStack(1, 0);
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagList layers = new NBTTagList();
		NBTTagCompound layer = new NBTTagCompound();
		layer.setString("Pattern", "bs");
		layer.setInteger("Color", 14);
		layers.appendTag(layer);
		tag.setTag("Patterns", layers);
		tag.setInteger("Base", 0);
		patterned.setTagInfo("BlockEntityTag", tag);
		ItemStack made = onServer(() -> craft(new ItemStack[] {patterned, ModBlocks.BANNER.newItemStack(1, 0), null}, new ItemStack[3], new ItemStack[3]));
		assertTrue(made != null, "a patterned and a plain white banner made nothing");
		assertEqual("bs", layer(made, 0), "the copy's layer");
	}

	// ---------------------------------------------------------------- colours as 26.3's (spec §66.8)

	/** 26.3's `DyeColor.getTextureDiffuseColor`, by its ids (white 0 to black 15), read from its code. */
	private static final int[] DYE_TEXTURE_COLOURS = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA,
		0x474F52, 0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

	/** 26.3's way: a pattern's texel (the banner's shading) times the dye's colour, channel by channel. */
	private static int shaded(int texel, int colour) {
		int r = (int) ((texel >> 16 & 255) * (float) (colour >> 16 & 255) / 255.0F);
		int g = (int) ((texel >> 8 & 255) * (float) (colour >> 8 & 255) / 255.0F);
		int b = (int) ((texel & 255) * (float) (colour & 255) / 255.0F);
		return r << 16 | g << 8 | b;
	}

	/**
	 * A banner's canvas in 26.3's colours, as the renderer makes it: each of the 16 as a banner's base, and a red bottom
	 * stripe on white, are the pattern's shading times 26.3's dye colour (EFR mixed 1.7.10's map colours in: its red
	 * 0x993333 for 0xB02E26).
	 */
	public void testCanvasColoursAre26_3s() {
		List<String> wrong = onClient(() -> {
			List<String> found = new ArrayList<>();
			IResourceManager resources = mc().getResourceManager();
			java.awt.image.BufferedImage base = texture(resources, "base");
			for (int colour = 0; colour < 16; colour++) {
				java.awt.image.BufferedImage canvas = canvas(resources, bannerWith(colour));
				int expected = shaded(base.getRGB(5, 5), DYE_TEXTURE_COLOURS[colour]);
				int got = canvas.getRGB(5, 5) & 0xFFFFFF;
				if (expected != got) {
					found.add(String.format("%s base: %06X, 26.3's %06X", COLOURS[colour], got, expected));
				}
			}
			java.awt.image.BufferedImage stripe = texture(resources, "stripe_bottom");
			java.awt.image.BufferedImage canvas = canvas(resources, bannerWith(0, "bs", 14));
			for (int y = 1; y <= 40; y++) {
				if ((stripe.getRGB(10, y) >>> 24) == 255) {
					int expected = shaded(stripe.getRGB(10, y), DYE_TEXTURE_COLOURS[14]);
					int got = canvas.getRGB(10, y) & 0xFFFFFF;
					if (expected != got) {
						found.add(String.format("the red stripe at 10 %d: %06X, 26.3's %06X", y, got, expected));
					}
					break;
				}
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "the canvas's colours");
	}

	/** 26.3's banner texture by its name (spec §66.11: `etfuturum:textures/entity/banner/`, as RGBA). */
	static java.awt.image.BufferedImage texture(IResourceManager resources, String name) throws java.io.IOException {
		return javax.imageio.ImageIO.read(resources.getResource(new ResourceLocation(McFly.DOMAIN, "textures/entity/banner/" + name + ".png")).getInputStream());
	}

	/** The canvas the renderer makes for that banner. */
	static java.awt.image.BufferedImage canvas(IResourceManager resources, ItemStack banner) throws java.io.IOException {
		TileEntityBanner entity = new TileEntityBanner();
		entity.setItemValues(banner);
		return ganymedes01.etfuturum.client.renderer.tileentity.TileEntityBannerRenderer.canvasImage(resources, entity);
	}

	/** A banner of that colour (white 0 to black 15) with layers (pattern id and colour, bottom first). */
	static ItemStack bannerWith(int colour, Object... layers) {
		ItemStack banner = ModBlocks.BANNER.newItemStack(1, colour);
		if (layers.length > 0) {
			NBTTagCompound entity = new NBTTagCompound();
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < layers.length; i += 2) {
				NBTTagCompound layer = new NBTTagCompound();
				layer.setString("Pattern", (String) layers[i]);
				layer.setInteger("Color", (Integer) layers[i + 1]);
				list.appendTag(layer);
			}
			entity.setTag("Patterns", list);
			entity.setInteger("Base", colour);
			banner.setTagInfo("BlockEntityTag", entity);
		}
		return banner;
	}

	// ---------------------------------------------------------------- stage 5: the four new patterns (spec §66.11)

	/** 26.3's four patterns 1.8 hadn't: its name, EFR's short id, the layer's name. */
	private static final String[][] NEW_PATTERNS = {{"globe", "glb", "Globe"}, {"piglin", "pig", "Snout"},
		{"flow", "flw", "Flow"}, {"guster", "gus", "Guster"}};

	/**
	 * Each by its name and id, with its texture; none made in the crafting grid, none among the 32 the loom offers
	 * without a pattern item (26.3 has them from pattern items only).
	 */
	public void testTheFourNewPatterns() {
		List<String> wrong = new ArrayList<>();
		for (String[] row : NEW_PATTERNS) {
			TileEntityBanner.EnumBannerPattern pattern = ganymedes01.etfuturum.blocks.ModernBanners.byName(row[0]);
			if (pattern == null) {
				wrong.add("no pattern " + row[0]);
				continue;
			}
			if (!row[1].equals(pattern.getPatternID()) || TileEntityBanner.EnumBannerPattern.getPatternByID(row[1]) != pattern) {
				wrong.add(row[0] + "'s id is " + pattern.getPatternID());
			}
			if (pattern.hasValidCrafting()) {
				wrong.add(row[0] + " has a crafting-grid recipe");
			}
			if (ganymedes01.etfuturum.blocks.ModernBanners.NO_ITEM_REQUIRED.contains(pattern)) {
				wrong.add(row[0] + " is offered without its item");
			}
			boolean drawn = onClient(() -> {
				try {
					return texture(mc().getResourceManager(), row[0]) != null;
				} catch (java.io.IOException missing) {
					return false;
				}
			});
			if (!drawn) {
				wrong.add(row[0] + " has no texture");
			}
		}
		assertEqual("[]", wrong.toString(), "the four new patterns");
	}

	/** Their layers named as 26.3's on a banner's tooltip ("Red Globe", "Blue Snout"). */
	@SuppressWarnings("unchecked")
	public void testTheirLayerNames() {
		ItemStack banner = bannerWith(0, "glb", 14, "pig", 11, "flw", 5, "gus", 15);
		List<String> lines = onClient(() -> new ArrayList<String>(banner.getTooltip(mc().thePlayer, false)));
		List<String> layers = new ArrayList<>();
		for (String line : lines.subList(1, lines.size())) {
			layers.add(net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(line));
		}
		assertEqual("[Red Globe, Blue Snout, Lime Flow, Black Guster]", layers.toString(), "the layer lines");
	}

	/**
	 * The snout drawn by 26.3's rule: where its own shading differs from the banner's base, the canvas is the snout
	 * texture's shading times the dye (1.8's masks took the base's shading, which the snout's isn't).
	 */
	public void testTheSnoutsOwnShading() {
		List<String> wrong = onClient(() -> {
			List<String> found = new ArrayList<>();
			IResourceManager resources = mc().getResourceManager();
			java.awt.image.BufferedImage snout = texture(resources, "piglin");
			java.awt.image.BufferedImage base = texture(resources, "base");
			java.awt.image.BufferedImage canvas = canvas(resources, bannerWith(0, "pig", 14));
			int checked = 0;
			for (int y = 0; y < 64 && found.size() < 3; y++) {
				for (int x = 0; x < 64 && found.size() < 3; x++) {
					int texel = snout.getRGB(x, y);
					if (texel >>> 24 == 255 && (texel & 0xFFFFFF) != (base.getRGB(x, y) & 0xFFFFFF)) {
						checked++;
						int expected = shaded(texel, DYE_TEXTURE_COLOURS[14]);
						int got = canvas.getRGB(x, y) & 0xFFFFFF;
						if (got != expected) {
							found.add(String.format("at %d %d: %06X, 26.3's %06X", x, y, got, expected));
						}
					}
				}
			}
			if (checked == 0) {
				found.add("no texel of the snout's own shading");
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "the red snout's canvas");
	}

	/**
	 * A pattern another mod adds through EFR's API (`BannerPatternHelper`) keeps working as before: drawn from its own
	 * mask where such a mod puts it (`minecraft:textures/entity/banner/<name>.png`, 1.8's format), while 26.3's are
	 * drawn from 26.3's textures; on a shield it's left off (it has no shield texture: the canvas would fail whole).
	 */
	public void testAnotherModsPattern() throws Exception {
		if (TileEntityBanner.EnumBannerPattern.getPatternByID("etfuapi") == null) {
			ganymedes01.etfuturum.api.BannerPatternHelper.addPattern("etfu_test_api", "etfuapi", "###", "# #", "###");
		}
		TileEntityBanner entity = new TileEntityBanner();
		entity.setItemValues(bannerWith(0, "glb", 14, "etfuapi", 11));
		assertEqual("[etfuturum:textures/entity/banner/base.png, etfuturum:textures/entity/banner/globe.png, textures/entity/banner/etfu_test_api.png]",
			onClient(() -> ganymedes01.etfuturum.client.renderer.tileentity.TileEntityBannerRenderer.layerTextures(entity)).toString(),
			"the layers' textures");
		if (io.github.asgerjon.mcfly.registry.McFlyItems.SHIELD.isEnabled()) {
			ItemStack shield = new ItemStack(io.github.asgerjon.mcfly.registry.McFlyItems.SHIELD.get());
			shield.setTagInfo("BlockEntityTag", (NBTTagCompound) bannerWith(0, "glb", 14, "etfuapi", 11).getTagCompound().getCompoundTag("BlockEntityTag").copy());
			String canvas = onClient(() -> ganymedes01.etfuturum.client.renderer.item.ItemShieldRenderer.textureFor(shield).getResourcePath());
			assertFalse(canvas.contains("etfuapi"), "the shield's canvas (" + canvas + ") has the other mod's layer");
			assertTrue(canvas.contains("glb"), "the shield's canvas (" + canvas + ") has the globe");
		}
	}

	// ---------------------------------------------------------------- stage 3: washing (spec §66.7)

	private static final int CX = 2;
	private static final int CY = 4;
	private static final int CZ = 3;

	/** A water cauldron at that level, the player in survival holding the stack, everything else cleared. */
	private void cauldronWith(int level, ItemStack held, boolean creative) {
		onServer(() -> {
			world().setBlock(CX, CY, CZ, Blocks.cauldron, level, 3);
			world().setBlockToAir(CX, CY + 1, CZ);
			player().inventory.clearInventory(null, -1);
			player().setGameType(creative ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL);
			player().inventory.currentItem = 0;
			player().inventory.setInventorySlotContents(0, held);
			player().inventoryContainer.detectAndSendChanges();
		});
	}

	/** The player right-clicks the cauldron's top with what they hold, as the server takes a right click. */
	private void useOnCauldron() {
		onServer(() -> {
			ItemStack held = player().getHeldItem();
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), held, CX, CY, CZ, 1, 0.5F, 1.0F, 0.5F);
			if (held != null && held.stackSize <= 0) {
				player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
			}
			player().inventoryContainer.detectAndSendChanges();
		});
	}

	private int cauldronLevel() {
		return onServer(() -> world().getBlock(CX, CY, CZ) == Blocks.cauldron ? world().getBlockMetadata(CX, CY, CZ) : -1);
	}

	/** A white banner with layers (bottom first) and maybe a name. */
	private static ItemStack patterned(int count, String name, String... layers) {
		ItemStack banner = ModBlocks.BANNER.newItemStack(count, 0);
		NBTTagCompound entity = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		for (String id : layers) {
			NBTTagCompound layer = new NBTTagCompound();
			layer.setString("Pattern", id);
			layer.setInteger("Color", 14);
			list.appendTag(layer);
		}
		entity.setTag("Patterns", list);
		banner.setTagInfo("BlockEntityTag", entity);
		if (name != null) {
			banner.setStackDisplayName(name);
		}
		return banner;
	}

	private static int layers(ItemStack banner) {
		return banner == null || !banner.hasTagCompound() ? 0
			: banner.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).tagCount();
	}

	/** Washed banners elsewhere in the inventory than the held slot: (count, layers of the first). */
	private int[] washedElsewhere() {
		return onServer(() -> {
			int count = 0, layers = -1;
			for (int slot = 1; slot < player().inventory.mainInventory.length; slot++) {
				ItemStack stack = player().inventory.mainInventory[slot];
				if (stack != null && stack.getItem() == Item.getItemFromBlock(ModBlocks.BANNER.get())) {
					count += stack.stackSize;
					if (layers < 0) {
						layers = layers(stack);
					}
				}
			}
			return new int[] {count, layers};
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setBlockToAir(CX, CY, CZ);
			world().setBlockToAir(CX, CY + 1, CZ);
			player().inventory.clearInventory(null, -1);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	/** 26.3's wash takes the top layer only (not every layer, and not the name) and one level of water. */
	public void testWashingTakesTheTopLayer() {
		cauldronWith(3, patterned(1, "Flag", "bs", "ts"), false);
		useOnCauldron();
		ItemStack held = onServer(() -> player().getHeldItem());
		assertEqual(1, layers(held), "the layers left");
		assertEqual("bs", layer(held, 0), "the one left (the bottom stripe)");
		assertEqual("Flag", held.getDisplayName(), "its name");
		assertEqual(2, cauldronLevel(), "the water left");
	}

	/** From a stack, one banner is washed: the rest stay in the hand, the washed one goes into the inventory. */
	public void testWashingOneOfAStack() {
		cauldronWith(3, patterned(3, null, "bs"), false);
		useOnCauldron();
		ItemStack held = onServer(() -> player().getHeldItem());
		assertEqual(2, held == null ? 0 : held.stackSize, "banners left in the hand");
		assertEqual(1, layers(held), "and still patterned");
		assertEqual("1 0", washedElsewhere()[0] + " " + washedElsewhere()[1], "one washed banner in the inventory (count, layers)");
		assertEqual(2, cauldronLevel(), "the water left");
	}

	/** A plain banner takes no water: it's used as on any block (placed on top). */
	public void testPlainBannerTakesNoWater() {
		cauldronWith(3, ModBlocks.BANNER.newItemStack(1, 0), false);
		useOnCauldron();
		assertEqual(3, cauldronLevel(), "the water left");
	}

	/** In creative the patterned banner stays and a washed copy is added (26.3's ItemUtils.createFilledResult). */
	public void testWashingInCreative() {
		cauldronWith(3, patterned(1, null, "bs", "ts"), true);
		useOnCauldron();
		ItemStack held = onServer(() -> player().getHeldItem());
		assertEqual(2, layers(held), "the held banner's layers (unchanged)");
		assertEqual("1 1", washedElsewhere()[0] + " " + washedElsewhere()[1], "a washed copy in the inventory (count, layers)");
		assertEqual(2, cauldronLevel(), "the water left");
	}

	/** An empty cauldron washes nothing. */
	public void testEmptyCauldronWashesNothing() {
		cauldronWith(0, patterned(2, null, "bs"), false);
		useOnCauldron();
		assertEqual(0, cauldronLevel(), "the cauldron");
		assertEqual(0, washedElsewhere()[0], "washed banners");
	}

	// ---------------------------------------------------------------- helpers

	private static ItemStack[] woolRow(int colour) {
		return new ItemStack[] {new ItemStack(Blocks.wool, 1, colour), new ItemStack(Blocks.wool, 1, colour), new ItemStack(Blocks.wool, 1, colour)};
	}

	/** What the crafting grid makes from three rows. Call on the server. */
	static ItemStack craft(ItemStack[] top, ItemStack[] middle, ItemStack[] bottom) {
		InventoryCrafting grid = new InventoryCrafting(new Container() {
			@Override
			public boolean canInteractWith(EntityPlayer player) {
				return true;
			}
		}, 3, 3);
		ItemStack[][] rows = {top, middle, bottom};
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				if (rows[row][column] != null) {
					grid.setInventorySlotContents(row * 3 + column, rows[row][column].copy());
				}
			}
		}
		return CraftingManager.getInstance().findMatchingRecipe(grid, world());
	}

	/** A banner's layer's pattern id (EFR's short ids), or null. */
	static String layer(ItemStack banner, int index) {
		if (banner == null || !banner.hasTagCompound()) {
			return null;
		}
		NBTTagList layers = banner.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10);
		return index < layers.tagCount() ? layers.getCompoundTagAt(index).getString("Pattern") : null;
	}
}
