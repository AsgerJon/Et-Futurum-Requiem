package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.banner.BannerBlockEntity;
import io.github.asgerjon.mcfly.banner.Banners;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSettings;

/**
 * Banners beside 26.3's in the comparison (SPEC-modern-movement.md §66.8): a white banner with a red bottom stripe and
 * a blue cross, standing 3 ahead facing the player (`banner.placed`), held in the main hand (`banner.held`, and from
 * the front: `banner.third_person_front`), in the survival inventory (`banner.inventory`), dropped on stone
 * (`banner.dropped`), in an item frame (`banner.item_frame`, §66.10), the four new patterns side by side (`banner.new_patterns`, §66.11), and on a shield, first person and from the front (`shield.decorated.*`, §66.9).
 */
public class BannerViewTests extends ComparisonTestCase {

	@Override
	protected void setUp() {
		if (!McFlyConfig.enableBanners) {
			skipTest("banners are off");
		}
	}

	/** The banner: white, a red bottom stripe, a blue cross (EFR's colours: white 0, red 14, blue 11). */
	static ItemStack banner() {
		ItemStack banner = McFlyBlocks.BANNER.newItemStack(1, 0);
		NBTTagCompound entity = new NBTTagCompound();
		NBTTagList layers = new NBTTagList();
		for (String[] layer : new String[][] {{"bs", "14"}, {"cr", "11"}}) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("Pattern", layer[0]);
			tag.setInteger("Color", Integer.parseInt(layer[1]));
			layers.appendTag(tag);
		}
		entity.setTag("Patterns", layers);
		entity.setInteger("Base", 0);
		banner.setTagInfo("BlockEntityTag", entity);
		return banner;
	}

	/** The dropped items and frames gone (a frame breaking drops itself and its item). */
	private void clearItemsAndFrames() {
		onServer(() -> {
			for (Object entity : new java.util.ArrayList<Object>(world().loadedEntityList)) {
				if (entity instanceof net.minecraft.entity.item.EntityItem || entity instanceof net.minecraft.entity.item.EntityItemFrame) {
					((net.minecraft.entity.Entity) entity).setDead();
				}
			}
		});
	}

	public void testScreenshots() throws Exception {
		comparisonViews(() -> {
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			look(0.0F, 0.0F);
			// standing, rotation 8: its front to the north, the player
			onServer(() -> {
				world().setBlock(0, 4, 3, McFlyBlocks.BANNER.get(), 8, 3);
				BannerBlockEntity placed = (BannerBlockEntity) world().getTileEntity(0, 4, 3);
				placed.setStanding(true);
				placed.setItemValues(banner());
				world().markBlockForUpdate(0, 4, 3);
			});
			ticks(10);
			comparisonShot("banner.placed");
			onServer(() -> world().setBlockToAir(0, 4, 3));
			// the four new patterns (spec §66.11), white banners 4 ahead from x -2 to 1: a red globe, a blue snout, a
			// lime flow, a black guster
			String[][] fourNew = {{"glb", "14"}, {"pig", "11"}, {"flw", "5"}, {"gus", "15"}};
			onServer(() -> {
				for (int i = 0; i < 4; i++) {
					world().setBlock(i - 2, 4, 4, McFlyBlocks.BANNER.get(), 8, 3);
					BannerBlockEntity placed = (BannerBlockEntity) world().getTileEntity(i - 2, 4, 4);
					placed.setStanding(true);
					placed.setItemValues(BannerTests.bannerWith(0, fourNew[i][0], Integer.parseInt(fourNew[i][1])));
					world().markBlockForUpdate(i - 2, 4, 4);
				}
			});
			ticks(10);
			comparisonShot("banner.new_patterns");
			onServer(() -> {
				for (int i = 0; i < 4; i++) {
					world().setBlockToAir(i - 2, 4, 4);
				}
			});
			setHeld(banner());
			ticks(10);
			comparisonShot("banner.held");
			// as an item (spec §66.10): held from the front, in the survival inventory, dropped on stone
			cameraView(2);
			ticks(10);
			comparisonShot("banner.third_person_front");
			cameraView(0);
			onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
			ticks(3);
			inventoryShot("banner.inventory");
			onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			setHeld(null);
			onServer(() -> {
				world().setBlock(0, 4, 2, net.minecraft.init.Blocks.stone);
				net.minecraft.entity.item.EntityItem dropped = new net.minecraft.entity.item.EntityItem(world(), 0.5, 5.2, 2.5, banner());
				dropped.motionX = dropped.motionY = dropped.motionZ = 0.0D;
				dropped.delayBeforeCanPickup = 32767;
				world().spawnEntityInWorld(dropped);
			});
			ticks(10);
			comparisonShot("banner.dropped");
			clearItemsAndFrames();
			onServer(() -> world().setBlockToAir(0, 4, 2));
			// in an item frame on a stone wall ahead, at eye height (its fixed display)
			onServer(() -> {
				world().setBlock(0, 5, 3, net.minecraft.init.Blocks.stone);
				net.minecraft.entity.item.EntityItemFrame frame = new net.minecraft.entity.item.EntityItemFrame(world(), 0, 5, 3, 2);
				frame.setDisplayedItem(banner());
				world().spawnEntityInWorld(frame);
			});
			ticks(10);
			comparisonShot("banner.item_frame");
			clearItemsAndFrames();
			onServer(() -> world().setBlockToAir(0, 5, 3));
			ticks(2);
			// the same banner on a shield (spec §66.9), first person and from the front
			if (io.github.asgerjon.mcfly.registry.McFlyItems.SHIELD.isEnabled()) {
				ItemStack shield = new ItemStack(io.github.asgerjon.mcfly.registry.McFlyItems.SHIELD.get());
				NBTTagCompound entity = (NBTTagCompound) banner().getTagCompound().getCompoundTag("BlockEntityTag").copy();
				shield.setTagInfo("BlockEntityTag", entity);
				setHeld(shield);
				ticks(10);
				comparisonShot("shield.decorated.first_person");
				cameraView(2);
				ticks(10);
				comparisonShot("shield.decorated.third_person_front");
				// raised, its face to the camera in front (the layers show: they aren't painted for measuring)
				keyDown(mc().gameSettings.keyBindUseItem);
				ticks(8);
				comparisonShot("shield.decorated.third_person_front_raised");
				keyUp(mc().gameSettings.keyBindUseItem);
				ticks(4);
				cameraView(0);
				// the survival inventory, as the shield's own inventory view
				onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
				ticks(3);
				inventoryShot("shield.decorated.inventory");
				onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
			}
			setHeld(null);
		});
	}
}
