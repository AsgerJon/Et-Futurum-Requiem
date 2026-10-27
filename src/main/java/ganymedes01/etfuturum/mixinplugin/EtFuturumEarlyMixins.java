package ganymedes01.etfuturum.mixinplugin;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.compat.CompatMisc;
import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
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

		if (ConfigBlocksItems.enableNewBoats) {
			mixins.add("boatstat.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("boatcamera.client.MixinEntity");
			}
		}

		if (ConfigMixins.fenceWallConnectFix) {
			mixins.add("fencewallconnect.MixinBlockWall");
			mixins.add("fencewallconnect.MixinBlockFence");
		}

		if (ConfigMixins.stepHeightFix) {
			mixins.add("stepfix.MixinEntity");
		}

		if (ConfigMixins.enableSpectatorMode) {
			mixins.add("spectator.MixinGameType");
			mixins.add("spectator.MixinEntity");
			mixins.add("spectator.MixinWorld");
			mixins.add("spectator.MixinWorldServer");
			mixins.add("spectator.MixinEntityPlayer");
			mixins.add("spectator.MixinNetHandlerPlayServer");
			mixins.add("spectator.MixinInventoryPlayer");
			mixins.add("spectator.MixinContainerChest");
			mixins.add("spectator.MixinSlot");
			mixins.add("spectator.MixinCommandGameMode");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("spectator.client.MixinEntityRenderer");
				mixins.add("spectator.client.MixinEntityPlayer");
				mixins.add("spectator.client.MixinWorldRenderer");
			}
		}

		if (ConfigMixins.avoidDroppingItemsWhenClosing) {
			mixins.add("closedrops.MixinEntityPlayerMP");
		}

		if (ConfigMixins.enableElytra) {
			mixins.add("backlytra.MixinEntityPlayer");
			mixins.add("backlytra.MixinItemFirework");
			mixins.add("backlytra.MixinEntityLivingBase");
			mixins.add("backlytra.MixinNetHandlerPlayServer");
			mixins.add("backlytra.MixinEntityTrackerEntry");
			if (loadedCoreMods.stream().anyMatch(name -> name.contains("thaumcraft"))) {
				mixins.add("backlytra.thaumcraft.MixinEventHandlerEntity");
			}
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("backlytra.client.MixinEntityPlayerSP");
				mixins.add("backlytra.client.MixinRenderPlayer");
				mixins.add("backlytra.client.MixinModelBiped");
			}
		}

		// The pose system; the elytra's hitbox and eye height come from it too (ModernMovement.isPoseSystemEnabled).
		if (ConfigMixins.enableModernSneaking || ConfigMixins.enableModernSwimming || ConfigMixins.enableElytra) {
			mixins.add("modernmovement.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("modernmovement.client.MixinEntityRenderer");
				mixins.add("modernmovement.client.MixinRenderGlobalHorizon");
				mixins.add("modernmovement.client.MixinRenderManager");
				mixins.add("modernmovement.client.MixinEntityPlayerSP");
				mixins.add("modernmovement.client.MixinMovementInputFromOptions");
				mixins.add("modernmovement.client.MixinRenderPlayer");
				mixins.add("modernmovement.client.MixinModelBiped");
				mixins.add("modernmovement.client.MixinItemRenderer");
			}
		}

		if (ConfigMixins.enableModernSneaking) {
			mixins.add("modernmovement.MixinEntitySneakEdge");
		}

		if (ConfigMixins.enableModernBreathing) {
			mixins.add("modernbreathing.MixinEntityLivingBase");
		}

		if (ConfigBlocksItems.enableBubbleColumns) {
			mixins.add("bubblecolumn.MixinEntityLivingBase");
			mixins.add("bubblecolumn.MixinEntityItem");
			mixins.add("bubblecolumn.MixinEntityXPOrb");
		}

		if (ConfigMixins.enableFloatingItems) {
			mixins.add("floatingitems.MixinEntityItem");
			mixins.add("floatingitems.MixinEntityXPOrb");
		}

		if (ConfigMixins.rightClickArmorSwap) {
			mixins.add("armorswap.MixinItemArmor");
		}

		if (ConfigMixins.pickBlockFromInventory && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("pickblock.client.MixinForgeHooks");
		}

		if (ConfigMixins.effectPanelAlphaFix && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("effectpanel.client.MixinInventoryEffectRenderer");
		}

		if (ConfigMixins.placementFacingByLook) {
			mixins.add("placementfacing.MixinBlockPistonBase");
		}

		// betterPistons overwrites onBlockEventReceived (backinslime.MixinBlockPistonBase) and applies this rule itself
		if (ConfigMixins.modernPistonEvents && !ConfigMixins.betterPistons) {
			mixins.add("pistonevents.MixinBlockPistonBase");
		}

		if (ConfigMixins.modernBlockUsePackets) {
			// The server's placement check must count the placing player once the client no longer filters clicks.
			mixins.add("blockusepacket.MixinWorld");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("blockusepacket.client.MixinPlayerControllerMP");
			}
		}

		if (ConfigMixins.shiftClickIntoCraftingGrid) {
			mixins.add("craftinggrid.MixinContainerWorkbench");
		}

		if (ConfigMixins.itemsBackOnClose) {
			mixins.add("containerclose.MixinContainerClose");
		}

		if (ConfigMixins.setblockAtLookedBlock) {
			mixins.add("setblocklook.MixinCommandSetBlock");
		}

		if ((ConfigMixins.chatHistoryBetweenWorlds || ConfigMixins.chatLogBetweenWorlds || ConfigMixins.chatHistoryAcrossRestarts) && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("chat.client.MixinGuiNewChat");
		}

		if (ConfigMixins.modernCreativeInventoryTab && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("creativetab.client.MixinGuiContainerCreative");
		}

		if (ConfigMixins.commandBlockCreativeOnly) {
			mixins.add("commandblock.MixinBlockCommandBlock");
		}
		if (ConfigMixins.portalShatters) {
			mixins.add("portalbreak.MixinBlockPortal");
		}
		if (ConfigMixins.shiftClickEquipsElytra) {
			mixins.add("elytraequip.MixinContainerPlayer");
		}
		if (ConfigMixins.giveAbove64) {
			mixins.add("givecount.MixinCommandGive");
		}

		if (ConfigMixins.fovEffects && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("fov.client.MixinEntityPlayerSP");
			mixins.add("fov.client.MixinEntityRenderer");
			mixins.add("fov.client.AccessorGuiOptionsRowList");
			mixins.add("fov.client.MixinGuiVideoSettings");
		}

		if (ConfigMixins.guiScaleWheel && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("guiscale.client.MixinGuiVideoSettings");
			mixins.add("guiscale.client.MixinGuiSlot");
		}

		if (ConfigMixins.offlineSkins) {
			mixins.add("offlineskins.MixinNetHandlerLoginServer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("offlineskins.client.MixinSession");
			}
		}

		if (ConfigMixins.skinCustomization) {
			mixins.add("skins.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("skins.client.MixinGuiOptions");
				mixins.add("skins.client.MixinRenderPlayer");
			}
		}

		if (ConfigMixins.beaconBeamThroughTransparentBlocks) {
			mixins.add("beacon.MixinTileEntityBeacon");
		}

		if (ConfigMixins.modernHunger) {
			mixins.add("hunger.MixinFoodStats");
			mixins.add("hunger.MixinEntityPlayer");
			mixins.add("hunger.MixinDamageSource");
			mixins.add("hunger.MixinPotion");
			mixins.add("hunger.MixinBlock");
		}

		if (ConfigMixins.modernPlacementShape) {
			mixins.add("placementshape.MixinItemBlock");
			mixins.add("placementshape.MixinBlockTrapDoor");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("placementshape.client.MixinItemBlock");
			}
		}

		if (ConfigMixins.arrowsInheritMotion) {
			mixins.add("arrowmomentum.MixinEntityPlayer");
			mixins.add("arrowmomentum.MixinNetHandlerPlayServer");
			mixins.add("arrowmomentum.MixinEntityArrow");
		}

		if (ConfigMixins.modernExplosionOrder) {
			mixins.add("explosionorder.MixinExplosion");
		}

		if (ConfigMixins.modernKeyRepeat && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("keyrepeat.client.MixinMinecraft");
		}

		if (ConfigMixins.noPotionGlint && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("potionglint.client.MixinItemPotion");
		}

		if (ConfigMixins.enableOffhand) {
			// Backhand and Battlegear 2 are checked at runtime (Offhand.isEnabled); the mixins stay inert then.
			mixins.add("offhand.MixinInventoryPlayer");
			mixins.add("offhand.MixinContainerPlayer");
			mixins.add("offhand.MixinEntityLivingBase");
			mixins.add("offhand.MixinEntityPlayer");
			mixins.add("offhand.MixinNetHandlerPlayServer");
			mixins.add("offhand.MixinItemInWorldManager");
			mixins.add("offhand.MixinItemBow");
			mixins.add("offhand.MixinContainer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("offhand.client.MixinGuiContainer");
				mixins.add("offhand.client.MixinGuiInventory");
				mixins.add("offhand.client.MixinGuiContainerCreative");
				mixins.add("offhand.client.MixinMinecraft");
				mixins.add("offhand.client.MixinPlayerControllerMP");
				mixins.add("offhand.client.MixinEntityClientPlayerMP");
				mixins.add("offhand.client.MixinNetHandlerPlayClient");
				mixins.add("offhand.client.MixinEntityOtherPlayerMP");
				mixins.add("offhand.client.MixinItemRenderer");
				mixins.add("offhand.client.MixinRenderPlayer");
				mixins.add("offhand.client.MixinModelBiped");
				mixins.add("offhand.client.MixinEntityPlayerIcon");
			}
		}

		if (ConfigMixins.enableModernSwimming) {
			mixins.add("modernmovement.MixinEntity");
			mixins.add("modernmovement.MixinEntityLivingBase");
			mixins.add("modernmovement.AccessorBlockLiquid");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("modernmovement.client.MixinEntityRendererWaterFog");
				mixins.add("modernmovement.client.MixinActiveRenderInfo");
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

		if (ConfigMixins.floorCeilingButtons) {
			mixins.add("floorceilbutton.MixinBlockButton");
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

		if (ConfigMixins.enableObservers) {
			mixins.add("observer.MixinWorld");
			mixins.add("observer.MixinWorldServer");
			mixins.add("observer.MixinChunk");
		}

		if (ConfigMixins.arrowFallingFix) {
			mixins.add("fallingarrowfix.MixinEntityArrow");
		}

		if (ConfigMixins.blockHopperInteraction) {
			mixins.add("blockinventories.MixinTileEntityHopper");
		}

		if (ConfigMixins.connectionAcceptFix) {
			mixins.add("networktick.MixinNetworkSystem");
			mixins.add("networktick.AccessorNetworkManager");
		}

		if (ConfigMixins.collidedThrowableFix) {
			mixins.add("projectilecollidefix.MixinEntityThrowable");
		}

		if (ConfigMixins.postTreeGenEvent && ConfigEntities.enableBees) {
			mixins.add("posttreegen.MixinWorldGenAbstractTree");
		}

		if (ConfigMixins.ladderTrapdoors) {
			mixins.add("laddertrapdoors.MixinBlockLadder");
			mixins.add("laddertrapdoors.MixinBlockTrapdoor");
		}

		if (ConfigMixins.betterPistons) {
			mixins.add("backinslime.MixinBlockPistonBase");
		}

		if (ConfigMixins.soulFire) {
			mixins.add("soulfire.MixinBlockFire");
			if(loadedCoreMods.stream().anyMatch(name -> name.contains("rple"))) { //RPLE compat
				mixins.add("soulfire.MixinBlockFireRPLE");
			}
			mixins.add("soulfire.MixinEntity");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("soulfire.MixinRenderBlocks");
			}
		}

		if (ConfigMixins.hideSingleLevelEnchants) {
			mixins.add("singlelevel.MixinEnchantment");
		}

		if (ConfigMixins.fireproofItems) {
			mixins.add("uninflammableitem.MixinEntityItem");
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

		if (ConfigMixins.modernMobSpawnLight) {
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

		if (ConfigEntities.enableFoxes) {
			mixins.add("foxes.MixinEntityLivingBase");
			mixins.add("foxes.MixinEntityWolf");
		}

        if (ConfigEntities.enableModernWither) {
            mixins.add("wither.MixinBlockSkull_OnPlacingWitherSkull");
        }

		return mixins;
	}

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
