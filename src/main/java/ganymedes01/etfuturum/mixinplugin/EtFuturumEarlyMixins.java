package ganymedes01.etfuturum.mixinplugin;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.mixinplugin.McFlyMixins;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.compat.CompatMisc;
import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigTweaks;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ganymedes01.etfuturum.lib.Reference.MOD_ID;

@IFMLLoadingPlugin.Name("EtFuturumEarlyMixins")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EtFuturumEarlyMixins implements IFMLLoadingPlugin, IEarlyMixinLoader {

	public static final MixinEnvironment.Side side = MixinEnvironment.getCurrentEnvironment().getSide();

	public void initConfigs() {
		final String configDir = "config" + File.separator + MOD_ID;

//	  File from before Et Futurum Requiem (Not in a subdirectory)
		File olderFile = new File(Launch.minecraftHome, "config" + File.separator + "etfuturum.cfg");
//	  File from before Et Futurum Requiem 2.2.2
		File oldFile = new File(Launch.minecraftHome, configDir + File.separator + "etfuturum.cfg");

		oldFile.getParentFile().mkdirs();
		if (olderFile.exists()) {
			try {
				Files.copy(olderFile.toPath(), oldFile.toPath());
			} catch (Exception e) {
				e.printStackTrace();
			}
			olderFile.delete();
			Reference.launchConfigWarning = true;
		}

		if (oldFile.exists()) {
			Reference.launchConfigWarning = true;
		}

		// our own options first (spec §79 B2): the first time, they're carried out of EFR's files before EFR reads them
		McFlyConfig.load(new File(Launch.minecraftHome, "config"), side == MixinEnvironment.Side.SERVER);
		ConfigBase.initializeConfigs();

//	  if(oldFile.exists()) {
//		  ConfigBase.loadBaseConfig(oldFile);
//	  }

		CompatMisc.doLwjgl3ifyCompat();
	}

	@Override
	public String getMixinConfig() {
		return "mixins." + MOD_ID + ".early.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedCoreMods) {

		initConfigs();

		List<String> mixins = new ArrayList<>();

		if (ConfigMixins.endPortalFix) {
			mixins.add("endportal.MixinBlockEndPortal");
		}

		if (ConfigMixins.fenceWallConnectFix) {
			mixins.add("fencewallconnect.MixinBlockWall");
		}

		if (ConfigMixins.stepHeightFix) {
			mixins.add("stepfix.MixinEntity");
		}

		if (McFlyConfig.thinBlockStepSoundFix || McFlyConfig.modernStepSounds) {
			McFlyMixins.EARLY.add("stepsounds.MixinEntity");
		}
		if (McFlyConfig.enableAmethyst) {
			// the crystal chime on amethyst (spec §90.4)
			McFlyMixins.EARLY.add("caves.MixinEntity");
		}
		if (McFlyConfig.enableMud) {
			// a water bottle on dirt makes mud (spec §90, step C)
			McFlyMixins.EARLY.add("caves.MixinItemPotion");
		}

		// McFly's base (spec §84): 26.3's random tick apart from the scheduled one; a chunk cache's world
		McFlyMixins.EARLY.add("base.MixinWorldServerRandomTick");
		McFlyMixins.EARLY.add("base.AccessorChunkCache");
		// no entity id 0, as 26.3 (the counter's first id 1; 0 reads as "no entity" in vanilla's spawn packets and ours)
		McFlyMixins.EARLY.add("base.MixinEntityIds");
		// McFly's plants in 1.7.10's flower pot, drawn as 26.3's potted models (spec §90, step D2)
		McFlyMixins.EARLY.add("base.MixinBlockFlowerPot");
		// McFly's game events (spec §90.9, step H1): what Forge has no event for
		McFlyMixins.EARLY.add("gameevent.MixinBlockDoor");
		McFlyMixins.EARLY.add("gameevent.MixinTileEntityNote");
		McFlyMixins.EARLY.add("gameevent.MixinItemShears");
		McFlyMixins.EARLY.add("gameevent.MixinEntity");
		McFlyMixins.EARLY.add("gameevent.MixinEntityLivingBase");
		McFlyMixins.EARLY.add("gameevent.MixinEntityPlayer");
		McFlyMixins.EARLY.add("gameevent.MixinItemStack");
		McFlyMixins.EARLY.add("gameevent.MixinBlockLever");
		McFlyMixins.EARLY.add("gameevent.MixinBlockButton");
		McFlyMixins.EARLY.add("gameevent.MixinBlockBasePressurePlate");
		McFlyMixins.EARLY.add("gameevent.MixinBlockTripWireHook");
		McFlyMixins.EARLY.add("gameevent.MixinBlockTripWire");
		McFlyMixins.EARLY.add("gameevent.MixinBlockTrapDoor");
		McFlyMixins.EARLY.add("gameevent.MixinBlockFenceGate");
		// the wood sets' fences (spec §91.5, step C): 26.3's joining for vanilla's fences too
		McFlyMixins.EARLY.add("wood.MixinBlockFence");
		// doors and trapdoors (spec §91.6, step D): vanilla's by 26.3's rules, sounds by state; trapdoors over ladders
		McFlyMixins.EARLY.add("wood.MixinBlockDoor");
		McFlyMixins.EARLY.add("wood.MixinBlockTrapDoor");
		McFlyMixins.EARLY.add("wood.MixinBlockFenceGateSound");
		McFlyMixins.EARLY.add("wood.MixinBlockLadder");
		// buttons and plates (spec §91.7, step E): every button's floor and ceiling facings, the set types' clicks
		McFlyMixins.EARLY.add("wood.MixinBlockButton");
		McFlyMixins.EARLY.add("wood.MixinBlockBasePressurePlate");
		// 26.3's signs for every sign (spec 91.8): two sides, the editor, placing, drawn as models (the applicators by modernSignText)
		McFlyMixins.EARLY.add("wood.MixinTileEntitySign");
		McFlyMixins.EARLY.add("wood.MixinBlockSign");
		McFlyMixins.EARLY.add("wood.MixinItemSign");
		McFlyMixins.EARLY.add("wood.MixinNetHandlerPlayServerSign");
		// the boats (spec 91.12, step J)
		if (McFlyConfig.enableNewBoats) {
			McFlyMixins.EARLY.add("boat.MixinBlockLilyPad");
			McFlyMixins.EARLY.add("boat.MixinBoatDismount");
			McFlyMixins.EARLY.add("boat.MixinBoatMobDismount");
			McFlyMixins.EARLY.add("boat.MixinBoatStat");
			McFlyMixins.EARLY.add("boat.MixinItemBoat");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("boat.client.MixinEntityTurn");
			}
		}
		if (side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("wood.client.MixinRenderBlocksFence");
			McFlyMixins.EARLY.add("wood.client.MixinBlockSignIcon");
			McFlyMixins.EARLY.add("wood.client.MixinMinecraftSignSwing");
		}
		McFlyMixins.EARLY.add("gameevent.MixinContainerChest");
		McFlyMixins.EARLY.add("gameevent.MixinTileEntityChest");
		McFlyMixins.EARLY.add("gameevent.MixinTileEntityEnderChest");
		McFlyMixins.EARLY.add("gameevent.MixinItemBucket");
		McFlyMixins.EARLY.add("gameevent.MixinItemGlassBottle");
		McFlyMixins.EARLY.add("gameevent.MixinBlockTNT");
		McFlyMixins.EARLY.add("gameevent.MixinEntityCreeper");
		McFlyMixins.EARLY.add("gameevent.MixinExplosion");
		McFlyMixins.EARLY.add("gameevent.MixinEntityLightningBolt");
		McFlyMixins.EARLY.add("gameevent.MixinEntityArrow");
		McFlyMixins.EARLY.add("gameevent.MixinEntityThrowable");
		McFlyMixins.EARLY.add("gameevent.MixinEntityFireball");
		McFlyMixins.EARLY.add("gameevent.MixinEntityFishHook");
		McFlyMixins.EARLY.add("gameevent.MixinItemFishingRod");
		McFlyMixins.EARLY.add("gameevent.MixinEntityEnderman");
		McFlyMixins.EARLY.add("gameevent.MixinItemMonsterPlacer");
		McFlyMixins.EARLY.add("gameevent.MixinItemMinecart");
		McFlyMixins.EARLY.add("gameevent.MixinItemBoat");
		McFlyMixins.EARLY.add("gameevent.MixinItemHangingEntity");
		McFlyMixins.EARLY.add("gameevent.MixinEntityChicken");
		McFlyMixins.EARLY.add("gameevent.MixinEntityLiving");
		McFlyMixins.EARLY.add("gameevent.MixinEntityPlayerInteract");
		McFlyMixins.EARLY.add("gameevent.MixinBlockCauldron");
		McFlyMixins.EARLY.add("gameevent.MixinBlockFarmland");
		McFlyMixins.EARLY.add("gameevent.MixinBlockCake");
		McFlyMixins.EARLY.add("gameevent.MixinBlockFlowerPotEvent");
		McFlyMixins.EARLY.add("gameevent.MixinEntityItemFrame");
		McFlyMixins.EARLY.add("gameevent.MixinItemLead");
		McFlyMixins.EARLY.add("gameevent.MixinItemDye");
		McFlyMixins.EARLY.add("gameevent.MixinMobSpawnerBaseLogic");
		McFlyMixins.EARLY.add("gameevent.MixinItemRecord");
		McFlyMixins.EARLY.add("gameevent.MixinBlockJukebox");
		McFlyMixins.EARLY.add("gameevent.MixinEntityBat");
		// sculk (spec §90.9, step H3): a death's experience taken by a catalyst drops no orbs
		McFlyMixins.EARLY.add("sculk.MixinEntityLivingBase");
		// the shrieker's warning on the player, Darkness's blend and look (step H4)
		McFlyMixins.EARLY.add("sculk.MixinEntityPlayer");
		McFlyMixins.EARLY.add("sculk.MixinEntityPlayerMP");
		McFlyMixins.EARLY.add("sculk.MixinEntityLivingBaseBlend");
		if (side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("sculk.client.MixinEntityRendererDarkness");
		}
		if (side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("base.client.MixinRenderBlocksFlowerPot");
		}

		// spectator mode (spec §83): ours, from McFly's mixin config
		if (McFlyConfig.enableSpectatorMode) {
			for (String mixin : new String[]{"MixinGameType", "MixinEntity", "MixinWorld", "MixinWorldServer", "MixinEntityPlayer",
				"MixinNetHandlerPlayServer", "MixinInventoryPlayer", "MixinContainerChest", "MixinSlot", "MixinCommandGameMode"}) {
				McFlyMixins.EARLY.add("spectator." + mixin);
			}
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("spectator.client.MixinEntityRenderer");
				McFlyMixins.EARLY.add("spectator.client.MixinEntityPlayer");
				McFlyMixins.EARLY.add("spectator.client.MixinWorldRenderer");
			}
		}

		if (ConfigMixins.avoidDroppingItemsWhenClosing) {
			mixins.add("closedrops.MixinEntityPlayerMP");
		}

		// the elytra (spec §85): ours, from McFly's mixin config
		if (McFlyConfig.enableElytra) {
			for (String mixin : new String[]{"MixinEntityPlayer", "MixinItemFirework", "MixinEntityLivingBase", "MixinNetHandlerPlayServer", "MixinEntityTrackerEntry"}) {
				McFlyMixins.EARLY.add("elytra." + mixin);
			}
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("elytra.client.MixinEntityPlayerSP");
				McFlyMixins.EARLY.add("elytra.client.MixinRenderPlayer");
				McFlyMixins.EARLY.add("elytra.client.MixinModelBiped");
			}
		}

		// The pose system; the elytra's hitbox and eye height come from it too (ModernMovement.isPoseSystemEnabled).
		if (McFlyConfig.enableModernSneaking || McFlyConfig.enableModernSwimming || McFlyConfig.enableElytra) {
			McFlyMixins.EARLY.add("modernmovement.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("modernmovement.client.MixinEntityRenderer");
				McFlyMixins.EARLY.add("modernmovement.client.MixinRenderGlobalHorizon");
				McFlyMixins.EARLY.add("modernmovement.client.MixinRenderManager");
				McFlyMixins.EARLY.add("modernmovement.client.MixinEntityPlayerSP");
				McFlyMixins.EARLY.add("modernmovement.client.MixinMovementInputFromOptions");
				McFlyMixins.EARLY.add("modernmovement.client.MixinRenderPlayer");
				McFlyMixins.EARLY.add("modernmovement.client.MixinModelBiped");
				McFlyMixins.EARLY.add("modernmovement.client.MixinItemRenderer");
			}
		}

		if (McFlyConfig.enableModernSneaking) {
			McFlyMixins.EARLY.add("modernmovement.MixinEntitySneakEdge");
		}

		if (McFlyConfig.enableModernBreathing) {
			McFlyMixins.EARLY.add("modernbreathing.MixinEntityLivingBase");
		}

		// a tipped arrow's effects after its hit, a totem it used included, as modern's
		if (McFlyConfig.enableTippedArrows) {
			McFlyMixins.EARLY.add("tippedarrows.MixinEntityArrow");
		}

		// the totem of undying where a hit's death is seen, after armour (it used to judge the hit before armour)
		if (ConfigBlocksItems.enableTotemUndying) {
			McFlyMixins.EARLY.add("totem.MixinEntityLivingBase");
			if (side == MixinEnvironment.Side.CLIENT) {
				// the sound, particles and screen animation of a used totem (spec §55.2)
				McFlyMixins.EARLY.add("totem.client.MixinNetHandlerPlayClient");
				McFlyMixins.EARLY.add("totem.client.MixinEntityRenderer");
			}
		}

		// lodestone compasses: 1.7.10's compass with the tracker (spec §57)
		if (McFlyConfig.enableLodestone) {
			McFlyMixins.EARLY.add("lodestone.MixinItem");
			McFlyMixins.EARLY.add("lodestone.MixinEntityLivingBase");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("lodestone.client.MixinItem");
				McFlyMixins.EARLY.add("lodestone.client.MixinEntityLivingBase");
				McFlyMixins.EARLY.add("lodestone.client.MixinEntityPlayer");
				McFlyMixins.EARLY.add("lodestone.client.MixinRenderItemFrame");
				McFlyMixins.EARLY.add("lodestone.client.MixinRenderItem");
			}
		}

		// powder snow and freezing (spec §62)
		if (McFlyConfig.enablePowderSnow) {
			McFlyMixins.EARLY.add("powdersnow.MixinEntity");
			McFlyMixins.EARLY.add("powdersnow.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("powdersnow.AccessorEntityLivingBase");
			McFlyMixins.EARLY.add("powdersnow.MixinEntityLiving");
			McFlyMixins.EARLY.add("powdersnow.MixinEntityEnderman");
			McFlyMixins.EARLY.add("powdersnow.MixinEntityAIFollowOwner");
			McFlyMixins.EARLY.add("powdersnow.MixinWorldServer");
			McFlyMixins.EARLY.add("powdersnow.MixinEntitySkeleton");
			McFlyMixins.EARLY.add("powdersnow.MixinEntityPlayerMP");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("powdersnow.client.MixinGuiIngameForge");
				McFlyMixins.EARLY.add("powdersnow.client.MixinRendererLivingEntity");
				McFlyMixins.EARLY.add("powdersnow.client.MixinSnowyBlocks");
			}
		}

		// quick wins batch 4 (spec §63); the mixins check their option again, so tests can turn them off
		if (McFlyConfig.modernCauldrons || McFlyConfig.modernCreativeFilling) {
			McFlyMixins.EARLY.add("quickwins4.MixinBlockCauldron");
		}
		if (McFlyConfig.modernCauldrons && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("quickwins4.client.MixinRenderBlocks");
		}
		if (McFlyConfig.modernHeldItems && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("quickwins4.client.MixinRenderBiped");
			McFlyMixins.EARLY.add("quickwins4.client.MixinRenderPlayer");
		}
		if (McFlyConfig.modernCreativeFilling) {
			McFlyMixins.EARLY.add("quickwins4.MixinItemBucket");
			McFlyMixins.EARLY.add("quickwins4.MixinItemGlassBottle");
			McFlyMixins.EARLY.add("quickwins4.MixinEntityCow");
		}
		if (McFlyConfig.modernPanic) {
			McFlyMixins.EARLY.add("quickwins4.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("quickwins4.MixinEntityAIPanic");
		}
		if (McFlyConfig.modernWeatherPace) {
			McFlyMixins.EARLY.add("quickwins4.MixinWorldServer");
		}
		if (ConfigEntities.enableStray) {
			McFlyMixins.EARLY.add("quickwins4.MixinMobSpawnerBaseLogic");
		}
		if (McFlyConfig.modernMobArms) {
			McFlyMixins.EARLY.add("quickwins4.MixinEntityZombie");
			McFlyMixins.EARLY.add("quickwins4.MixinEntityAIAttackOnCollide");
			McFlyMixins.EARLY.add("quickwins4.MixinEntityPigZombie");
			McFlyMixins.EARLY.add("quickwins4.MixinEntitySkeleton");
			McFlyMixins.EARLY.add("quickwins4.MixinEntityAIArrowAttack");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("quickwins4.client.MixinModelZombie");
				McFlyMixins.EARLY.add("quickwins4.client.MixinModelZombieVillager");
			}
		}

		// item frames as 26.3: floors and ceilings, 8 turns, read by comparators (spec §67.3); glow frames' light (§67.4)
		if (McFlyConfig.modernItemFrames) {
			McFlyMixins.EARLY.add("frames.MixinEntityHanging");
			McFlyMixins.EARLY.add("frames.MixinEntityItemFrame");
			McFlyMixins.EARLY.add("frames.MixinItemHangingEntity");
			McFlyMixins.EARLY.add("frames.MixinBlockRedstoneComparator");
		}
		if (side == MixinEnvironment.Side.CLIENT) {
			// the renderer's hooks also hold the glow frame's back and light, so with modern frames or glow frames
			if (McFlyConfig.modernItemFrames || McFlyConfig.enableGlowItemFrames) {
				McFlyMixins.EARLY.add("frames.client.MixinRenderItemFrame");
				McFlyMixins.EARLY.add("frames.client.AccessorRenderItemFrame");
			}
		}

		// the spyglass (spec §64.5): looking through it
		if (McFlyConfig.enableSpyglass && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("spyglass.client.MixinEntityPlayerSP");
			McFlyMixins.EARLY.add("spyglass.client.MixinEntityRenderer");
			McFlyMixins.EARLY.add("spyglass.client.MixinModelBiped");
			McFlyMixins.EARLY.add("spyglass.client.MixinRenderPlayer");
		}

		// strongholds in 26.3's rings (spec §64.4); the mixin checks the option again, so tests can turn it off
		if (McFlyConfig.modernStrongholds) {
			McFlyMixins.EARLY.add("strongholds.MixinMapGenStronghold");
		}

		// vehicles drop as one (spec §59); the mixins check the option again, so tests can turn it off
		if (McFlyConfig.dropVehiclesTogether) {
			McFlyMixins.EARLY.add("vehicledrops.MixinEntityMinecart");
			McFlyMixins.EARLY.add("vehicledrops.MixinEntityMinecartChest");
			McFlyMixins.EARLY.add("vehicledrops.MixinEntityMinecartFurnace");
			McFlyMixins.EARLY.add("vehicledrops.MixinEntityMinecartHopper");
		}
		// the TNT minecart's break, for both its drop (§59) and its fuse (§61.1); it checks each option itself
		if (McFlyConfig.dropVehiclesTogether || McFlyConfig.modernTntMinecarts) {
			McFlyMixins.EARLY.add("tntminecart.MixinEntityMinecartTNT");
		}
		// quick wins batch 3 (spec §61)
		if (McFlyConfig.modernWitherSkulls) {
			McFlyMixins.EARLY.add("witherskull.MixinEntityWitherSkull");
		}
		if (side == MixinEnvironment.Side.CLIENT) {
			if (McFlyConfig.modernRarity) {
				McFlyMixins.EARLY.add("modernrarity.client.MixinItemStack");
				McFlyMixins.EARLY.add("modernrarity.client.MixinGuiIngameForge");
			}
			if (McFlyConfig.modernInventoryFigure) {
				McFlyMixins.EARLY.add("inventoryfigure.client.MixinGuiInventory");
			}
		}

		// lightning as 26.3 (spec §58.3)
		if (McFlyConfig.modernLightning) {
			McFlyMixins.EARLY.add("trident.MixinEntityLightningBolt");
			McFlyMixins.EARLY.add("trident.MixinWorldServer");
			McFlyMixins.EARLY.add("trident.MixinEntityHorse");
		}

		// the trident (spec §58), which needs modern combat as the shield does
		if (McFlyConfig.enableTrident && McFlyConfig.modernCombat) {
			McFlyMixins.EARLY.add("trident.MixinEntityPlayer");
			McFlyMixins.EARLY.add("trident.MixinEnchantmentHelper");
			McFlyMixins.EARLY.add("trident.MixinEnchantmentDamage");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("trident.client.MixinModelBiped");
				McFlyMixins.EARLY.add("trident.client.MixinRenderPlayer");
			}
		}

		if (ConfigBlocksItems.enableBubbleColumns) {
			McFlyMixins.EARLY.add("bubblecolumn.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("bubblecolumn.MixinEntityItem");
			McFlyMixins.EARLY.add("bubblecolumn.MixinEntityXPOrb");
		}

		if (McFlyConfig.enableFloatingItems) {
			McFlyMixins.EARLY.add("floatingitems.MixinEntityItem");
			McFlyMixins.EARLY.add("floatingitems.MixinEntityXPOrb");
		}

		if (McFlyConfig.rightClickArmorSwap) {
			McFlyMixins.EARLY.add("armorswap.MixinItemArmor");
		}

		if (McFlyConfig.pickBlockFromInventory && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("pickblock.client.MixinForgeHooks");
		}

		if (McFlyConfig.effectPanelAlphaFix && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("effectpanel.client.MixinInventoryEffectRenderer");
		}

		if (McFlyConfig.placementFacingByLook) {
			McFlyMixins.EARLY.add("placementfacing.MixinBlockPistonBase");
		}

		// betterPistons overwrites onBlockEventReceived (backinslime.MixinBlockPistonBase) and applies this rule itself
		if (McFlyConfig.modernPistonEvents && !ConfigMixins.betterPistons) {
			McFlyMixins.EARLY.add("pistonevents.MixinBlockPistonBase");
		}

		if (McFlyConfig.modernBlockUsePackets) {
			// The server's placement check must count the placing player once the client no longer filters clicks.
			McFlyMixins.EARLY.add("blockusepacket.MixinWorld");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("blockusepacket.client.MixinPlayerControllerMP");
			}
		}

		if (McFlyConfig.shiftClickIntoCraftingGrid) {
			McFlyMixins.EARLY.add("craftinggrid.MixinContainerWorkbench");
		}

		if (McFlyConfig.itemsBackOnClose) {
			McFlyMixins.EARLY.add("containerclose.MixinContainerClose");
		}

		if (McFlyConfig.endCreditsOnce) {
			McFlyMixins.EARLY.add("endcredits.MixinEntityPlayerMP");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("endcredits.client.MixinNetHandlerPlayClient");
			}
		}

		if (McFlyConfig.modernAnvilCosts) {
			McFlyMixins.EARLY.add("anvil.MixinContainerRepair");
		}

		if (McFlyConfig.modernBedUse) {
			McFlyMixins.EARLY.add("bedspawn.MixinEntityPlayer");
			McFlyMixins.EARLY.add("bedspawn.MixinBlockBed");
		}

		if (McFlyConfig.buttonsOnFullFaces) {
			McFlyMixins.EARLY.add("fullfaces.MixinBlockButton");
			McFlyMixins.EARLY.add("fullfaces.MixinBlockLever");
		}

		if (McFlyConfig.firstPersonCameraAtEyes && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("firstpersoncamera.client.MixinEntityRenderer");
		}

		// modern options (spec §54): EFR's client options in options.txt, the title screen's icon row, and what the
		// screens that take 1.7.10's place need of them
		if (McFlyConfig.modernOptions && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("modernoptions.client.MixinGameSettings");
			McFlyMixins.EARLY.add("modernoptions.client.MixinGuiMainMenu");
			McFlyMixins.EARLY.add("modernoptions.client.AccessorGuiOptions");
			McFlyMixins.EARLY.add("modernoptions.client.AccessorGuiVideoSettings");
			McFlyMixins.EARLY.add("modernoptions.client.AccessorScreenChatOptions");
			McFlyMixins.EARLY.add("modernoptions.client.AccessorGuiControls");
			// the Hold/Toggle keys (spec §54.11)
			McFlyMixins.EARLY.add("modernoptions.client.MixinKeyBinding");
			McFlyMixins.EARLY.add("modernoptions.client.AccessorKeyBinding");
			McFlyMixins.EARLY.add("modernoptions.client.MixinMinecraft");
			McFlyMixins.EARLY.add("modernoptions.client.MixinEntityClientPlayerMP");
			// the rest of Controls and Mouse Settings (spec §54.12)
			McFlyMixins.EARLY.add("modernoptions.client.MixinEntityPlayerSPControls");
			McFlyMixins.EARLY.add("modernoptions.client.MixinEntityRenderer");
			McFlyMixins.EARLY.add("modernoptions.client.MixinGuiSlot");
			McFlyMixins.EARLY.add("modernoptions.client.MixinGuiChat");
			McFlyMixins.EARLY.add("modernoptions.client.MixinGuiAchievements");
			McFlyMixins.EARLY.add("modernoptions.client.MixinGuiContainerCreative");
		}

		if (McFlyConfig.coordinateSuggestions) {
			McFlyMixins.EARLY.add("coordinatesuggestions.MixinCommandSetBlock");
		}

		if (McFlyConfig.modernChat) {
			McFlyMixins.EARLY.add("modernchat.MixinC01PacketChatMessage");
			McFlyMixins.EARLY.add("modernchat.MixinCommandHandler");
			McFlyMixins.EARLY.add("modernchat.MixinServerConfigurationManager");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("modernchat.client.MixinC01PacketChatMessage");
				McFlyMixins.EARLY.add("modernchat.client.MixinGuiChat");
				McFlyMixins.EARLY.add("modernchat.client.MixinGuiTextField");
				McFlyMixins.EARLY.add("modernchat.client.MixinScreenChatOptions");
			}
		}

		if (McFlyConfig.modernEnd) {
			// the End as 26.3 (spec §76); Hardcore Ender Expansion's End isn't a WorldProviderModernEnd, so these stand aside
			McFlyMixins.EARLY.add("modernend.MixinTeleporter");
			McFlyMixins.EARLY.add("modernend.MixinServerConfigurationManager");
			McFlyMixins.EARLY.add("modernend.MixinEntityPlayerMP");
			McFlyMixins.EARLY.add("modernend.AccessorEntityInvulnerable");
			McFlyMixins.EARLY.add("modernend.MixinEntityEnderCrystal");
			McFlyMixins.EARLY.add("modernend.MixinEntityDragon");
			McFlyMixins.EARLY.add("modernend.AccessorEntityDragon");
			McFlyMixins.EARLY.add("modernend.AccessorLivingLerp");
			McFlyMixins.EARLY.add("modernend.AccessorEntitySize");
			McFlyMixins.EARLY.add("modernend.MixinEntityLivingDragon");
			McFlyMixins.EARLY.add("modernend.MixinEntityLivingDragonSaved");
			McFlyMixins.EARLY.add("modernend.MixinItemGlassBottle");
			McFlyMixins.EARLY.add("modernend.MixinEntityEnderPearl");
			McFlyMixins.EARLY.add("modernend.MixinTileEntityChest");
			McFlyMixins.EARLY.add("modernend.MixinTileEntitySkull");
			McFlyMixins.EARLY.add("modernend.MixinItemSkull");
			// panes traced by their bars, as 26.3 (spec §76.15): the gap under a cage's corner lets arrows through
			McFlyMixins.EARLY.add("modernend.MixinBlockPane");
		}
		if (McFlyConfig.modernCombat) {
			McFlyMixins.EARLY.add("combat.AccessorItemSword");
			McFlyMixins.EARLY.add("combat.MixinItemSword");
			McFlyMixins.EARLY.add("combat.MixinItemStack");
			McFlyMixins.EARLY.add("combat.MixinEntityPlayer");
			McFlyMixins.EARLY.add("combat.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("combat.AccessorEntity");
			McFlyMixins.EARLY.add("combat.MixinEntityArrow");
			McFlyMixins.EARLY.add("combat.MixinEntityMob");
			McFlyMixins.EARLY.add("combat.MixinEnchantmentProtection");
			McFlyMixins.EARLY.add("combat.MixinEnchantmentHelper");
			McFlyMixins.EARLY.add("combat.MixinEnchantmentDamage");
			McFlyMixins.EARLY.add("combat.MixinPotionAttackDamage");
			McFlyMixins.EARLY.add("combat.MixinExplosion");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("combat.client.MixinItemStackTooltip");
				McFlyMixins.EARLY.add("combat.client.MixinMinecraft");
				McFlyMixins.EARLY.add("combat.client.MixinPlayerControllerMP");
				McFlyMixins.EARLY.add("combat.client.MixinItemRenderer");
				McFlyMixins.EARLY.add("combat.client.MixinRenderItem");
				McFlyMixins.EARLY.add("combat.client.MixinModelBiped");
				McFlyMixins.EARLY.add("combat.client.MixinRenderItemFrame");
				McFlyMixins.EARLY.add("combat.client.MixinGuiVideoSettings");
			}
		}

		if ((McFlyConfig.chatHistoryBetweenWorlds || McFlyConfig.chatLogBetweenWorlds || McFlyConfig.chatHistoryAcrossRestarts) && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("chat.client.MixinGuiNewChat");
		}

		if (McFlyConfig.modernCreativeInventoryTab && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("creativetab.client.MixinGuiContainerCreative");
		}

		if (McFlyConfig.commandBlockCreativeOnly) {
			McFlyMixins.EARLY.add("commandblock.MixinBlockCommandBlock");
		}
		if (McFlyConfig.portalShatters) {
			McFlyMixins.EARLY.add("portalbreak.MixinBlockPortal");
		}
		if (McFlyConfig.shiftClickEquipsElytra) {
			McFlyMixins.EARLY.add("elytraequip.MixinContainerPlayer");
		}
		if (McFlyConfig.giveAbove64) {
			McFlyMixins.EARLY.add("givecount.MixinCommandGive");
		}

		// the video settings list's rows: FOV Effects' slider, the combat update's Attack Indicator
		if ((McFlyConfig.fovEffects || McFlyConfig.modernCombat) && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("fov.client.AccessorGuiOptionsRowList");
		}

		if (McFlyConfig.fovEffects && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("fov.client.MixinEntityPlayerSP");
			McFlyMixins.EARLY.add("fov.client.MixinEntityRenderer");
			McFlyMixins.EARLY.add("fov.client.MixinGuiVideoSettings");
		}

		if (McFlyConfig.guiScaleWheel && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("guiscale.client.MixinGuiVideoSettings");
			McFlyMixins.EARLY.add("guiscale.client.MixinGuiSlot");
		}

		if (McFlyConfig.offlineSkins) {
			McFlyMixins.EARLY.add("offlineskins.MixinNetHandlerLoginServer");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("offlineskins.client.MixinSession");
			}
		}

		if (McFlyConfig.skinCustomization) {
			McFlyMixins.EARLY.add("skins.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("skins.client.MixinGuiOptions");
				McFlyMixins.EARLY.add("skins.client.MixinRenderPlayer");
			}
		}

		if (McFlyConfig.beaconBeamThroughTransparentBlocks) {
			McFlyMixins.EARLY.add("beacon.MixinTileEntityBeacon");
		}

		if (McFlyConfig.modernHunger) {
			McFlyMixins.EARLY.add("hunger.MixinFoodStats");
			McFlyMixins.EARLY.add("hunger.MixinEntityPlayer");
			McFlyMixins.EARLY.add("hunger.MixinDamageSource");
			McFlyMixins.EARLY.add("hunger.MixinPotion");
			McFlyMixins.EARLY.add("hunger.MixinBlock");
		}

		if (McFlyConfig.modernPlacementShape) {
			McFlyMixins.EARLY.add("placementshape.MixinItemBlock");
			McFlyMixins.EARLY.add("placementshape.MixinBlockTrapDoor");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("placementshape.client.MixinItemBlock");
			}
		}

		if (McFlyConfig.arrowsInheritMotion) {
			McFlyMixins.EARLY.add("arrowmomentum.MixinEntityPlayer");
			McFlyMixins.EARLY.add("arrowmomentum.MixinNetHandlerPlayServer");
			McFlyMixins.EARLY.add("arrowmomentum.MixinEntityArrow");
		}

		if (McFlyConfig.modernExplosionOrder) {
			McFlyMixins.EARLY.add("explosionorder.MixinExplosion");
		}

		if (McFlyConfig.modernKeyRepeat && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("keyrepeat.client.MixinMinecraft");
		}

		if (McFlyConfig.noPotionGlint && side == MixinEnvironment.Side.CLIENT) {
			McFlyMixins.EARLY.add("potionglint.client.MixinItemPotion");
		}

		// chests side by side (spec §65)
		if (McFlyConfig.modernChestPlacement) {
			McFlyMixins.EARLY.add("modernchests.MixinBlockChest");
			McFlyMixins.EARLY.add("modernchests.MixinTileEntityChest");
		}

		// named effects (spec §70)
		if (McFlyConfig.namedEffects) {
			McFlyMixins.EARLY.add("namedeffects.MixinPotion");
			// saves by name
			McFlyMixins.EARLY.add("namedeffects.MixinPotionEffect");
			McFlyMixins.EARLY.add("namedeffects.MixinTileEntityBeacon");
			// the network, /effect by name
			McFlyMixins.EARLY.add("namedeffects.MixinS1DPacketEntityEffect");
			McFlyMixins.EARLY.add("namedeffects.MixinS1EPacketRemoveEntityEffect");
			McFlyMixins.EARLY.add("namedeffects.MixinServerConfigurationManager");
			McFlyMixins.EARLY.add("namedeffects.MixinCommandEffect");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("namedeffects.client.MixinNetHandlerPlayClient");
				McFlyMixins.EARLY.add("namedeffects.client.MixinGuiBeacon");
				McFlyMixins.EARLY.add("namedeffects.client.MixinInventoryEffectRenderer");
			}
		}

		// the effects the last potions need (spec §69.11)
		if (McFlyConfig.namedEffects && (McFlyConfig.enableLuck || McFlyConfig.enableTrialEffects)) {
			McFlyMixins.EARLY.add("moderneffects.AccessorEntitySlime");
			McFlyMixins.EARLY.add("moderneffects.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("moderneffects.MixinEntity");
			McFlyMixins.EARLY.add("moderneffects.MixinEntityFishHook");
		}

		// potions by name (spec §69)
		if (McFlyConfig.potionsByName) {
			McFlyMixins.EARLY.add("potionsbyname.MixinItemPotion");
			// 1.7.10's own brewing stand, for when EFR's is off
			McFlyMixins.EARLY.add("potionsbyname.MixinTileEntityBrewingStand");
			McFlyMixins.EARLY.add("potionsbyname.MixinContainerBrewingStandIngredient");
			// a thrown potion's colour in flight and in its splash
			McFlyMixins.EARLY.add("potionsbyname.MixinEntityPotion");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("potionsbyname.client.MixinItemPotion");
				McFlyMixins.EARLY.add("potionsbyname.client.MixinRenderSnowball");
				McFlyMixins.EARLY.add("potionsbyname.client.MixinRenderGlobal");
			}
		}

		if (McFlyConfig.enableOffhand) {
			// Backhand and Battlegear 2 are checked at runtime (Offhand.isEnabled); the mixins stay inert then.
			McFlyMixins.EARLY.add("offhand.MixinInventoryPlayer");
			McFlyMixins.EARLY.add("offhand.MixinContainerPlayer");
			McFlyMixins.EARLY.add("offhand.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("offhand.MixinEntityPlayer");
			McFlyMixins.EARLY.add("offhand.MixinNetHandlerPlayServer");
			McFlyMixins.EARLY.add("offhand.MixinItemInWorldManager");
			McFlyMixins.EARLY.add("offhand.MixinItemBow");
			McFlyMixins.EARLY.add("offhand.MixinContainer");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("offhand.client.MixinGuiContainer");
				McFlyMixins.EARLY.add("offhand.client.MixinGuiInventory");
				McFlyMixins.EARLY.add("offhand.client.MixinGuiContainerCreative");
				McFlyMixins.EARLY.add("offhand.client.MixinMinecraft");
				McFlyMixins.EARLY.add("offhand.client.MixinPlayerControllerMP");
				McFlyMixins.EARLY.add("offhand.client.MixinEntityClientPlayerMP");
				McFlyMixins.EARLY.add("offhand.client.MixinNetHandlerPlayClient");
				McFlyMixins.EARLY.add("offhand.client.MixinEntityOtherPlayerMP");
				McFlyMixins.EARLY.add("offhand.client.MixinItemRenderer");
				McFlyMixins.EARLY.add("offhand.client.MixinRenderPlayer");
				McFlyMixins.EARLY.add("offhand.client.MixinModelBiped");
				McFlyMixins.EARLY.add("offhand.client.MixinEntityPlayerIcon");
			}
		}

		if (McFlyConfig.enableModernSwimming) {
			McFlyMixins.EARLY.add("modernmovement.MixinEntity");
			McFlyMixins.EARLY.add("modernmovement.MixinEntityLivingBase");
			McFlyMixins.EARLY.add("modernmovement.AccessorBlockLiquid");
			if (side == MixinEnvironment.Side.CLIENT) {
				McFlyMixins.EARLY.add("modernmovement.client.MixinEntityRendererWaterFog");
				McFlyMixins.EARLY.add("modernmovement.client.MixinActiveRenderInfo");
			}
		}

		if (ConfigMixins.enableDoWeatherCycle) {
			mixins.add("doweathercycle.MixinCommandHandler");
			mixins.add("doweathercycle.MixinWorldInfo");
		}

		if (ConfigMixins.enableRandomTickSpeed) {
			mixins.add("randomtickspeed.MixinWorldServer");
			mixins.add("randomtickspeed.MixinGameRules");
		}

		if ((ConfigWorld.endFlashes || ConfigWorld.modernEndAmbientColor || ConfigWorld.modernBlockLightTint)
				&& side == MixinEnvironment.Side.CLIENT) {
			mixins.add("endflashes.client.MixinEntityRenderer");
		}

		if (ConfigWorld.endFlashes && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("endflashes.client.MixinRenderGlobal");
		}

		if (ConfigWorld.modernLightmapGamma && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("modernlightmap.client.MixinEntityRenderer");
		}

		if ((ConfigWorld.modernBlockLightTint || ConfigWorld.modernNightVision)
				&& side == MixinEnvironment.Side.CLIENT) {
			mixins.add("modernblocklight.client.MixinEntityRenderer");
		}

		if (ConfigMixins.creativeFlightSpeedModifier > 1 || ConfigTweaks.creativeFlightVerticalModifier > 1) {
			mixins.add("flyspeed.MixinEntityPlayer");
		}

		if (ConfigMixins.bouncyBeds) {
			mixins.add("bouncybeds.MixinBlockBed");
		}

		if (ConfigMixins.newHurtSounds) {
			mixins.add("sounds.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("sounds.client.MixinEntityClientPlayerMP");
			}
		}

		if (ConfigMixins.newMobSounds) {
			mixins.add("sounds.MixinEntitySnowman");
			mixins.add("sounds.MixinEntitySkeleton");
			mixins.add("sounds.MixinEntitySquid");
			mixins.add("sounds.MixinEntityWitch");
		}

		if (ConfigMixins.newEnderEyeSounds) {
			mixins.add("sounds.MixinItemEnderEye");
			mixins.add("sounds.MixinEntityEnderEye");
		}

		if (ConfigMixins.newEnchantingSounds) {
			mixins.add("sounds.MixinContainerEnchantment");
		}

		if (ConfigMixins.newFishingRodSounds) {
			mixins.add("sounds.MixinItemFishingRod");
		}

		if (ConfigMixins.newBeaconSounds) {
			mixins.add("sounds.MixinTileEntityBeacon");
		}

		if (ConfigMixins.hoeTilling) {
			mixins.add("sounds.MixinItemHoe");
		}

		// the observer (spec §86): ours, from McFly's mixin config
		if (McFlyConfig.enableObservers) {
			McFlyMixins.EARLY.add("redstone.MixinWorldObserverUpdates");
			McFlyMixins.EARLY.add("redstone.MixinChunkPopulating");
			McFlyMixins.EARLY.add("redstone.AccessorWorldServerTicks");
		}

		if (ConfigMixins.arrowFallingFix) {
			mixins.add("fallingarrowfix.MixinEntityArrow");
		}

		if (ConfigMixins.blockHopperInteraction) {
			mixins.add("blockinventories.MixinTileEntityHopper");
		}

		if (McFlyConfig.connectionAcceptFix) {
			McFlyMixins.EARLY.add("networktick.MixinNetworkSystem");
			McFlyMixins.EARLY.add("networktick.AccessorNetworkManager");
		}

		if (ConfigMixins.collidedThrowableFix) {
			mixins.add("projectilecollidefix.MixinEntityThrowable");
		}

		if (ConfigMixins.postTreeGenEvent && ConfigEntities.enableBees) {
			mixins.add("posttreegen.MixinWorldGenAbstractTree");
		}

		if (ConfigMixins.betterPistons) {
			mixins.add("backinslime.MixinBlockPistonBase");
		}

		// soul fire (spec §88.6): fire on a soul base is McFly's soul fire; basalt from lava
		if (McFlyConfig.enableSoulSoil || McFlyConfig.enableSoulLighting) {
			McFlyMixins.EARLY.add("soulfire.MixinBlockFire");
			McFlyMixins.EARLY.add("soulfire.MixinBlockLiquid");
		}
		// netherrack takes bonemeal next to nylium (spec §88.7)
		if (McFlyConfig.enableNetherPlants) {
			McFlyMixins.EARLY.add("nether.MixinBlockNetherrack");
		}

		if (ConfigMixins.hideSingleLevelEnchants) {
			mixins.add("singlelevel.MixinEnchantment");
		}

		// fire-resistant items (spec §88.4): 26.3's set, by McFly's marker, not EFR's name rule
		if (McFlyConfig.enableNetherite) {
			McFlyMixins.EARLY.add("fireresistant.MixinEntityItem");
		}

        if (ConfigMixins.enableJumpClimbing)
        {
            mixins.add("isLadderFix.MixinEntityLivingBase");
        }

		if (ConfigMixins.adjustedLiquidPhysics) {
			mixins.add("liquidphysics.MixinEntity");
			mixins.add("liquidphysics.MixinWorld");
			mixins.add("liquidphysics.MixinBlockLiquid");
		}

		if (ConfigMixins.liquidItemFloat) {
			mixins.add("liquidphysics.MixinEntityItem");
		}

		if (ConfigMixins.riddenHorsesInWater) {
			mixins.add("horsewater.MixinEntityHorse");
		}

		if (McFlyConfig.modernHorsesInWater) {
			McFlyMixins.EARLY.add("horses.MixinEntityLivingBase");
			if (!McFlyConfig.enableModernSwimming) {
				// with modern swimming on, its own water hooks hold these constants and ask ModernHorses
				McFlyMixins.EARLY.add("horses.MixinEntityLivingBaseWater");
			}
			McFlyMixins.EARLY.add("horses.MixinEntityAISwimming");
			McFlyMixins.EARLY.add("horses.MixinEntityHorse");
			McFlyMixins.EARLY.add("horses.MixinEntity");
		}

		if (McFlyConfig.undeadHorseLeads) {
			McFlyMixins.EARLY.add("horses.MixinEntityHorseLeads");
		}

		if (McFlyConfig.modernMobSpawnLight) {
			mixins.add("darkspawns.MixinEntityMob");
		}

		if (side == MixinEnvironment.Side.CLIENT) {
			if (ConfigMixins.dustUnderFallingBlocks) {
				mixins.add("blockfallingparticles.client.MixinBlockFalling");
			}

			if (ConfigMixins.adjustedAttenuation) {
				mixins.add("sounds.client.MixinSoundManager_Attenuation");
			}

			if (ConfigMixins.boundedBlockBreakingParticles) {
				mixins.add("boundedparticles.client.MixinEffectRenderer");
			}

			if (ConfigMixins.furnaceCrackle) {
				mixins.add("sounds.client.MixinBlockFurnace");
			}

			if (ConfigEnchantsPotions.enableSwiftSneak) {
				mixins.add("swiftsneak.client.MixinMovementInputFromOptions");
			}

			if (ConfigMixins.flowerPotFixes) {
				mixins.add("flowerpotfix.client.MixinRenderBlocks");
			}

			if (ConfigMixins.colorGrassBlockItemSides) {
				mixins.add("coloredgrassitem.client.MixinRenderBlocks");
			}

			if (ConfigMixins.worldSaveThumbnails) {
				mixins.add("worldthumbnail.client.MixinGuiSelectWorld");
				mixins.add("worldthumbnail.client.MixinGuiSelectWorldList");
			}

			if (ConfigMixins.modernLoadingScreen) {
				mixins.add("worldthumbnail.client.MixinLoadingScreenRenderer");
				mixins.add("worldthumbnail.client.MixinGuiDownloadTerrain");
				mixins.add("worldthumbnail.client.MixinMinecraft_LoadingBridge");
				mixins.add("worldthumbnail.MixinMinecraftServer");
				mixins.add("worldthumbnail.MixinChunkProviderServer");
			}
		}

		if (ConfigMixins.thinPanes) {
			mixins.add("thinpanes.MixinBlockPane");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("thinpanes.MixinRenderBlocks");
			}
		}

		if (ConfigMixins.enablePlayersSleepingPecentageGamerule) {
			mixins.add("playerssleepingpercentage.MixinWorldServer");
		}

		if (McFlyConfig.enableGlowSquids && McFlyConfig.enableGlowSquidSpawns) {
			McFlyMixins.EARLY.add("glowsquid.MixinWorldServer");
		}

		if (ConfigEntities.enableFoxes) {
			mixins.add("foxes.MixinEntityLivingBase");
			mixins.add("foxes.MixinEntityWolf");
		}

        if (ConfigEntities.enableModernWither) {
            mixins.add("wither.MixinBlockSkull_OnPlacingWitherSkull");
        }

		// unitTAS is a development dependency of its own now (its own mod and mixin config)

		// ours, chosen above into McFly's list, applied from our own mixin config (spec §79 B3)
		if (!mcflyConfigAdded) {
			mcflyConfigAdded = true;
			org.spongepowered.asm.mixin.Mixins.addConfiguration(McFlyMixins.EARLY_CONFIG);
		}

		return mixins;
	}

	private static boolean mcflyConfigAdded;

	@Override
	public String[] getASMTransformerClass() {
		return null;
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {

	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
