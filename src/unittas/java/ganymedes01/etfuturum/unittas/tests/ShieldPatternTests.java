package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.banner.client.BannerCanvas;
import io.github.asgerjon.mcfly.dye.DyeColor;
import io.github.asgerjon.mcfly.banner.Banners;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldSettings;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Banners on shields, as 26.3's `ShieldDecorationRecipe`, `ShieldItem` and `ShieldSpecialRenderer`
 * (SPEC-modern-movement.md §66.9, stage 4): a clear shield and a banner make the shield with the banner's colour and
 * layers, its own data kept; named for the colour, its layers in its tooltip; drawn with them; not washed (as Java).
 */
@TasSuite("banners")
public class ShieldPatternTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!McFlyConfig.enableBanners || !McFlyItems.SHIELD.isEnabled()) {
			skipTest("banners or the shield are off");
		}
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			world().setBlockToAir(2, 4, 3);
			player().inventory.clearInventory(null, -1);
			player().setGameType(WorldSettings.GameType.CREATIVE);
		});
	}

	private static ItemStack shield() {
		return new ItemStack(McFlyItems.SHIELD.get());
	}

	/** A banner of that colour (white 0 to black 15) with layers (pattern id and colour, bottom first). */
	private static ItemStack banner(int colour, Object... layers) {
		ItemStack banner = McFlyBlocks.BANNER.newItemStack(1, colour);
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

	private static ItemStack craft(ItemStack a, ItemStack b) {
		return BannerTests.craft(new ItemStack[] {a, b, null}, new ItemStack[3], new ItemStack[3]);
	}

	private static int base(ItemStack shield) {
		NBTTagCompound entity = shield.hasTagCompound() ? shield.getTagCompound().getCompoundTag("BlockEntityTag") : null;
		return entity == null || !entity.hasKey("Base") ? -1 : entity.getInteger("Base");
	}

	private static String layers(ItemStack shield) {
		List<String> names = new ArrayList<>();
		if (shield.hasTagCompound()) {
			NBTTagList list = shield.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				names.add(list.getCompoundTagAt(i).getString("Pattern") + "/" + list.getCompoundTagAt(i).getInteger("Color"));
			}
		}
		return names.toString();
	}

	/** A clear shield and a patterned banner: the shield with the banner's colour and layers. */
	public void testDecorating() {
		ItemStack made = onServer(() -> craft(shield(), banner(0, "bs", 14, "cr", 11)));
		assertTrue(made != null && made.getItem() == McFlyItems.SHIELD.get(), "a shield and a banner made " + made);
		assertEqual(0, base(made), "its base colour (white)");
		assertEqual("[bs/14, cr/11]", layers(made), "its layers");
	}

	/** A plain banner gives its colour alone (26.3's recipe takes any banner), and the shield is named for it. */
	public void testPlainBannerGivesItsColour() {
		ItemStack made = onServer(() -> craft(banner(14), shield()));
		assertTrue(made != null, "a shield and a red banner made nothing");
		assertEqual(14, base(made), "its base colour (red)");
		assertEqual("[]", layers(made), "its layers");
		assertEqual("Red Shield", onClient(made::getDisplayName), "its name");
		assertEqual("Shield", onClient(() -> shield().getDisplayName()), "a plain shield's name");
	}

	/** Only a shield with no layers, and exactly one banner and one shield. */
	public void testOnlyAClearShield() {
		ItemStack decorated = onServer(() -> craft(shield(), banner(0, "bs", 14)));
		assertEqual(null, onServer(() -> craft(decorated, banner(4))), "a shield with layers and a banner");
		assertEqual(null, onServer(() -> BannerTests.craft(new ItemStack[] {shield(), banner(0), banner(1)}, new ItemStack[3], new ItemStack[3])), "a shield and two banners");
		assertEqual(null, onServer(() -> BannerTests.craft(new ItemStack[] {shield(), shield(), banner(0)}, new ItemStack[3], new ItemStack[3])), "two shields and a banner");
		assertEqual(null, onServer(() -> craft(shield(), new ItemStack(Blocks.wool))), "a shield and wool");
		ItemStack coloured = onServer(() -> craft(shield(), banner(14)));
		assertTrue(onServer(() -> craft(coloured, banner(0, "bs", 14))) != null, "a coloured shield without layers takes a banner (26.3 checks the layers only)");
	}

	/** The shield keeps its own data: wear, name, enchantments (26.3's createWithOriginalComponents). */
	public void testKeepsTheShieldsOwnData() {
		ItemStack own = shield();
		own.setItemDamage(100);
		own.setStackDisplayName("My Shield");
		own.addEnchantment(Enchantment.unbreaking, 3);
		ItemStack made = onServer(() -> craft(own, banner(0, "bs", 14)));
		assertTrue(made != null, "a worn, named, enchanted shield and a banner made nothing");
		assertEqual(100, made.getItemDamage(), "its wear");
		assertEqual("My Shield", onClient(made::getDisplayName), "its name");
		assertEqual(3, net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, made), "its Unbreaking");
		assertEqual("[bs/14]", layers(made), "its layers");
	}

	/** Its tooltip lists its layers as the banner's does (26.3's BannerPatternLayers is a tooltip provider). */
	@SuppressWarnings("unchecked")
	public void testTooltipListsTheLayers() {
		ItemStack banner = banner(0, "bs", 14, "cr", 11);
		ItemStack made = onServer(() -> craft(shield(), banner));
		List<String> bannerLines = onClient(() -> new ArrayList<String>(banner.getTooltip(mc().thePlayer, false)));
		List<String> shieldLines = onClient(() -> new ArrayList<String>(made.getTooltip(mc().thePlayer, false)));
		assertEqual(bannerLines.subList(1, bannerLines.size()).toString(), shieldLines.subList(1, shieldLines.size()).toString(), "the layer lines");
	}

	/** Not washed in a cauldron (as Java; Bedrock washes shields): the layers and the water stay. */
	public void testDecoratedShieldNotWashed() {
		ItemStack made = onServer(() -> craft(shield(), banner(0, "bs", 14)));
		onServer(() -> {
			world().setBlock(2, 4, 3, Blocks.cauldron, 3, 3);
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().inventory.currentItem = 0;
			player().inventory.setInventorySlotContents(0, made);
			ItemStack held = player().getHeldItem();
			player().theItemInWorldManager.activateBlockOrUseItem(player(), world(), held, 2, 4, 3, 1, 0.5F, 1.0F, 0.5F);
		});
		assertEqual("[bs/14]", onServer(() -> layers(player().getHeldItem())), "the shield's layers");
		assertEqual(3, onServer(() -> world().getBlockMetadata(2, 4, 3)), "the water");
	}

	/**
	 * The shield's canvas, as 26.3 draws it: on the frame (26.3's shield_base), the face in the base colour, then the
	 * layers, each the face's shading times 26.3's dye colour (the banner's rule, spec §66.8).
	 */
	public void testShieldCanvasColours() {
		List<String> wrong = onClient(() -> {
			List<String> found = new ArrayList<>();
			IResourceManager resources = mc().getResourceManager();
			ResourceLocation frame = new ResourceLocation(McFly.DOMAIN, "textures/entity/shield/base.png");
			BufferedImage base = javax.imageio.ImageIO.read(resources.getResource(frame).getInputStream());
			BufferedImage canvas = BannerCanvas.compose(resources, frame,
				Arrays.asList(new BannerCanvas.Part(Banners.shieldMask("base"), DyeColor.byId(11).textureColour(), true),
					new BannerCanvas.Part(Banners.shieldMask("stripe_bottom"), DyeColor.byId(14).textureColour(), true)));
			// 26.3's rule: each layer is its own pattern texture's shading times the dye (not the frame's: a pack painting
			// the frame mustn't tint the layers)
			BufferedImage baseMask = javax.imageio.ImageIO.read(resources.getResource(new ResourceLocation(Banners.shieldMask("base"))).getInputStream());
			BufferedImage stripeMask = javax.imageio.ImageIO.read(resources.getResource(new ResourceLocation(Banners.shieldMask("stripe_bottom"))).getInputStream());
			// the plate's front face is u 1 to 12, v 1 to 22: high up the base colour (blue), at the bottom the stripe (red)
			int top = canvas.getRGB(6, 3) & 0xFFFFFF;
			int wantTop = shaded(baseMask.getRGB(6, 3), DyeColor.byId(11).textureColour());
			if (top != wantTop) {
				found.add(String.format("the face's top: %06X, 26.3's %06X", top, wantTop));
			}
			int bottom = canvas.getRGB(6, 21) & 0xFFFFFF;
			int wantBottom = shaded(stripeMask.getRGB(6, 21), DyeColor.byId(14).textureColour());
			if (bottom != wantBottom) {
				found.add(String.format("the face's bottom: %06X, 26.3's %06X", bottom, wantBottom));
			}
			// the handle is the frame's, untinted
			if ((canvas.getRGB(28, 8) & 0xFFFFFF) != (base.getRGB(28, 8) & 0xFFFFFF)) {
				found.add("the handle is tinted");
			}
			return found;
		});
		assertEqual("[]", wrong.toString(), "the shield's canvas");
	}

	private static int shaded(int texel, int colour) {
		int r = (int) ((texel >> 16 & 255) * (float) (colour >> 16 & 255) / 255.0F);
		int g = (int) ((texel >> 8 & 255) * (float) (colour >> 8 & 255) / 255.0F);
		int b = (int) ((texel & 255) * (float) (colour & 255) / 255.0F);
		return r << 16 | g << 8 | b;
	}
}
