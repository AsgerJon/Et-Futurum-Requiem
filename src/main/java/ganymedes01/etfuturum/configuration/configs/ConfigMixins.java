package ganymedes01.etfuturum.configuration.configs;

import ganymedes01.etfuturum.mixinplugin.EtFuturumEarlyMixins;
import ganymedes01.etfuturum.configuration.ConfigBase;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;

public class ConfigMixins extends ConfigBase {

	public static boolean endPortalFix;
	public static boolean fenceWallConnectFix;
	public static boolean furnaceCrackle;
	public static boolean stepHeightFix;
	public static boolean bouncyBeds;
	public static boolean floorCeilingButtons;
	public static boolean boundedBlockBreakingParticles;
	public static boolean newHurtSounds;
	public static boolean newMobSounds;
	public static boolean newEnderEyeSounds;
	public static boolean newEnchantingSounds;
	public static boolean newFishingRodSounds;
	public static boolean newBeaconSounds;
	public static boolean hoeTilling;
	public static boolean blockHopperInteraction;
	public static boolean avoidDroppingItemsWhenClosing;
	public static boolean enableSpectatorMode;
	public static boolean enableElytra;
	public static boolean enableNewElytraTakeoffLogic;
	public static boolean enableModernSneaking;
	public static boolean enableModernSwimming;
	public static boolean enableModernBreathing;
	public static boolean enableFloatingItems;
	public static boolean enableDoWeatherCycle;
	public static boolean enableRandomTickSpeed;
	public static float creativeFlightSpeedModifier;
	public static boolean enableObservers;
	public static boolean arrowFallingFix;
	public static boolean adjustedAttenuation;
	public static boolean dustUnderFallingBlocks;
	public static boolean collidedThrowableFix;
	public static boolean connectionAcceptFix;
	public static boolean offlineSkins;
	public static boolean fovEffects;
	public static boolean guiScaleWheel;
	public static boolean commandBlockCreativeOnly;
	public static boolean portalShatters;
	public static boolean shiftClickEquipsElytra;
	public static boolean giveAbove64;
	public static boolean postTreeGenEvent;
	public static boolean ladderTrapdoors;
	public static boolean rightClickArmorSwap;
	public static boolean pickBlockFromInventory;
	public static boolean effectPanelAlphaFix;
	public static boolean placementFacingByLook;
	public static boolean modernPistonEvents;
	public static boolean modernBlockUsePackets;
	public static boolean modernExplosionOrder;
	public static boolean modernMobSpawnLight;
	public static boolean arrowsInheritMotion;
	public static boolean modernPlacementShape;
	public static boolean modernCreativeInventoryTab;
	public static boolean modernHunger;
	public static boolean beaconBeamThroughTransparentBlocks;
	public static boolean skinCustomization;
	public static boolean shiftClickIntoCraftingGrid;
	public static boolean itemsBackOnClose;
	public static boolean setblockAtLookedBlock;
	public static boolean chatHistoryBetweenWorlds;
	public static boolean chatLogBetweenWorlds;
	public static boolean chatHistoryAcrossRestarts;
	public static boolean modernKeyRepeat;
	public static boolean enableOffhand;
	public static boolean noPotionGlint;
	public static boolean betterPistons;
	public static boolean flowerPotFixes;
	public static boolean soulFire;
	public static boolean hideSingleLevelEnchants;
	public static boolean fireproofItems;
	public static boolean thinPanes;
	public static boolean colorGrassBlockItemSides;
	public static boolean enablePlayersSleepingPecentageGamerule;
  public static boolean enableJumpClimbing;
	public static boolean worldSaveThumbnails;
	public static boolean modernLoadingScreen;
	public static boolean adjustedLiquidPhysics;
	public static boolean liquidItemFloat;
	public static boolean riddenHorsesInWater;


	static final String catBackport = "backported features";
	static final String catOptimization = "optimizations";
	static final String catFixes = "fixes";
	static final String catMisc = "misc";

	public ConfigMixins(File file) {
		super(file);
		setCategoryComment(catBackport, "Backports that can typically only have a clean implementation with mixins.");
		setCategoryComment(catOptimization, "Better implementations of existing features.\nThis is generally used when doing something through the Forge API would be slower or less practical than using a Mixin.");
		setCategoryComment(catFixes, "Fixes to vanilla issues which are necessary for backports.");
		setCategoryComment(catMisc, "Mixins that don't fit in any other category.");

		configCats.add(getCategory(catBackport));
		configCats.add(getCategory(catOptimization));
		configCats.add(getCategory(catFixes));
		configCats.add(getCategory(catMisc));
	}

	@Override
	protected void syncConfigOptions() {
		if (EtFuturumEarlyMixins.side == MixinEnvironment.Side.CLIENT) {
			furnaceCrackle = getBoolean("furnaceCrackle", catBackport, true, "Allows vanilla furnaces to have crackling sounds.\nModified Client Classes: net.minecraft.block.BlockFurnace");
			boundedBlockBreakingParticles = getBoolean("boundedBlockBreakingParticles", catBackport, true, "In 1.14+, when breaking a block, the block break particles stay within the outline, instead of always occupying the whole block space.\nMofified Client Classes: net.minecraft.client.particle.EffectRenderer");
			adjustedAttenuation = getBoolean("adjustedAttenuation", catBackport, true, "Adjusts the attenuation distance of certain sounds. This needs to be a separate mixin due to the way it works.\nCurrently changes portal abience, and beacon ambience to have an attenuation distance of 8 blocks away, instead of 16.\nModified Client Classes: net.minecraft.client.audio.SoundManager");

			flowerPotFixes = getBoolean("flowerPotFixes", catFixes, true, "Fixes flower pots having several restrictions limiting what they'll render inside of them. Required for crimson roots or azalea to render correctly in the flower pot, among other custom blocks.\nModified Client Classes: net.minecraft.client.renderer.RenderBlock");
			worldSaveThumbnails = getBoolean("worldSaveThumbnails", catBackport, true, "Saves and displays icon.png thumbnails in the world selection screen, like 1.14+.\nModified Client Classes: net.minecraft.client.gui.GuiSelectWorld");
			modernLoadingScreen = getBoolean("modernLoadingScreen", catBackport, true, "Modern loading screen with progress bar and chunk grid when entering a world, like 1.14+.\nModified Client Classes: net.minecraft.client.gui.GuiDownloadTerrain");
		}

		endPortalFix = getBoolean("endPortalFix", catBackport, true, "Makes the End Portal block (the actual portal, not the frame) have an item icon, proper hitbox and will not instantly destroy itself in other dimensions.\nModified classes: net.minecraft.block.BlockEndPortal");
		fenceWallConnectFix = getBoolean("fenceWallConnectFix", catBackport, true, "Makes vanilla fences connect to modded ones of the same material. Might have connection issue with mods that don't reference BlockFence super code.\nModified classes: net.minecraft.block.BlockFence net.minecraft.block.BlockWall");
		avoidDroppingItemsWhenClosing = getBoolean("avoidDroppingItemsWhenClosing", catBackport, false, "Experimental: avoid dropping items when closing an inventory, like in modern versions.\nModified Classes: net.minecraft.entity.player.EntityPlayerMP");
		enableSpectatorMode = getBoolean("enableSpectatorMode", catBackport, true, "VERY EXPERIMENTAL!\nModified Classes: net.minecraft.world.WorldSettings.GameType net.minecraft.entity.Entity net.minecraft.world.World net.minecraft.entity.player.EntityPlayer net.minecraft.network.NetHandlerPlayServer net.minecraft.entity.player.InventoryPlayer net.minecraft.inventory.ContainerChest\nModified Client Classes: net.minecraft.client.renderer.EntityRenderer net.minecraft.entity.player.EntityPlayer net.minecraft.client.renderer.WorldRenderer");
		enableObservers = getBoolean("enableObservers", catBackport, true, "Modified Classes: net.minecraft.world.World net.minecraft.world.WorldServer");
		blockHopperInteraction = getBoolean("blockHopperInteraction", catBackport, true, "Allows some blocks without tile entities (e.g. composters) to interact with hoppers. May still not interact with modded pipes.\nModified Classes: net.minecraft.tileentity.TileEntityHopper");

		enableNewElytraTakeoffLogic = getBoolean("enableNewElytraTakeoffLogic", catBackport, true, "When enabled, the 1.15+ elytra takeoff logic is used, when disabled, the 1.9-1.14 elytra takeoff logic is used.");
		enableModernSneaking = getBoolean("enableModernSneaking", catBackport, true, "Modern (1.14+) sneaking: a 1.5 block tall hitbox and lower eyes while crouching, so you fit under slabs, a smooth camera when the eye height changes, and automatic crouching where you can't stand up. Sneaking stops you at drops deeper than 0.6 blocks instead of a full block.\nModified Classes: net.minecraft.entity.Entity net.minecraft.entity.player.EntityPlayer\nModified Client Classes: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.renderer.EntityRenderer net.minecraft.client.renderer.entity.RenderManager net.minecraft.util.MovementInputFromOptions net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.model.ModelBiped net.minecraft.client.renderer.ItemRenderer");
		enableModernSwimming = getBoolean("enableModernSwimming", catBackport, true, "Modern (1.13+) swimming and crawling: sprint in water to swim with a 0.6 block tall hitbox, and crawl through 1 block tall gaps. Includes the 1.13+ water physics for players: water detection that works with the small hitbox, slower sinking, the modern sprint rules, and modern underwater vision (your eyes adjust to the water over 30 seconds; Respiration no longer brightens the fog).\nModified Classes: net.minecraft.entity.Entity net.minecraft.entity.EntityLivingBase net.minecraft.entity.player.EntityPlayer\nModified Client Classes: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.renderer.EntityRenderer net.minecraft.client.renderer.entity.RenderManager net.minecraft.util.MovementInputFromOptions net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.model.ModelBiped net.minecraft.client.renderer.ItemRenderer");
		enableModernBreathing = getBoolean("enableModernBreathing", catBackport, true, "Modern air bubbles: they burst one by one with a pop that gets louder as you run out, empty outlines show the air you've used, and air comes back gradually (4 per tick) instead of all at once when you surface.\nModified Classes: net.minecraft.entity.EntityLivingBase");
		enableFloatingItems = getBoolean("enableFloatingItems", catBackport, true, "Items and experience orbs float in water, and items in lava, as since 1.13: no gravity while more than 0.1 blocks deep, a slight lift up to 0.06 blocks per tick, extra horizontal drag. This is what lets bubble columns throw items like modern. While on, it replaces the item floating of adjustedLiquidPhysics, which would hold items to 0.05 blocks per tick.");
		enableDoWeatherCycle = getBoolean("enableDoWeatherCycle", catBackport, true, "Add the doWeatherCycle game rule from 1.11+");
		enableRandomTickSpeed = getBoolean("enableRandomTickSpeed", catBackport, true, "Add the randomTickSpeed game rule from 1.8+");
		creativeFlightSpeedModifier = getFloat("creativeFlightSpeedModifier", catBackport, 2, 1, 5, "When greater than 1, boosts creative flight speed when sprinting, like in newer versions");
		commandBlockCreativeOnly = getBoolean("commandBlockCreativeOnly", catBackport, true, "A command block opens only in creative, as in modern; outside creative a right click uses the held item instead. Saving still needs operator rights." +
				"\nModified Classes: net.minecraft.block.BlockCommandBlock");
		portalShatters = getBoolean("portalShatters", catBackport, true, "A nether portal whose frame breaks shatters with the glass breaking sound and particles, as in modern, instead of vanishing silently." +
				"\nModified Classes: net.minecraft.block.BlockPortal");
		shiftClickEquipsElytra = getBoolean("shiftClickEquipsElytra", catBackport, true, "Shift-clicking an elytra in the inventory puts it on when no chestplate or elytra is worn, as for armour." +
				"\nModified Classes: net.minecraft.inventory.ContainerPlayer");
		giveAbove64 = getBoolean("giveAbove64", catBackport, true, "/give accepts counts above 64, as in modern: up to 100 stacks of the item, handed over as full stacks." +
				"\nModified Classes: net.minecraft.command.CommandGive");
		fovEffects = getBoolean("fovEffects", catBackport, true, "Modern's FOV Effects setting (1.16+): a slider in Video Settings for how much the field of view follows speed, flying, a drawn bow and water, from off to vanilla's full effect. The value is fovEffectScale in functions.cfg." +
				"\nModified Classes: Client: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.renderer.EntityRenderer net.minecraft.client.gui.GuiVideoSettings net.minecraft.client.gui.GuiOptionsRowList");
		bouncyBeds = getBoolean("bouncyBeds", catBackport, true, "Makes beds bouncy. Should work with most modded beds. For continuity disabling this also disables EFR beds being bouncy.\nModified Classes: net.minecraft.block.BlockBed");
		floorCeilingButtons = getBoolean("floorCeilingButtons", catBackport, true, "Allows ability to place buttons on the floor and ceiling. Note: Due to metadata limits, they won't rotate to face the player like how they were made to in more modern versions.\nModified Classes: net.minecraft.block.BlockButton");
		newHurtSounds = getBoolean("newHurtSounds", catBackport, true, "Damage sounds for walking into a berry bush, drowning or burning\nModified Classes: net.minecraft.entity.player.EntityPlayer net.minecraft.client.entity.EntityClientPlayerMP");
		newEnderEyeSounds = getBoolean("newEnderEyeSounds", catBackport, true, "New sounds for throwing an eye of ender, and for them breaking or dropping.\nModified Classes: net.minecraft.entity.item.EntityEnderEye net.minecraft.item.ItemEnderEye");
		newMobSounds = getBoolean("newMobSounds", catBackport, true, "New sounds for the witch, snow golem, squid and wither skeleton.");
		newEnchantingSounds = getBoolean("newEnchantingSounds", catBackport, true, "Allows an enchantment sound to play on the vanilla enchantment table, and possibly a few modded ones...? This option does nothing to the Et Futurum Requiem enchantment tables.\nModified Classes: net.minecraft.inventory.ContainerEnchantment");
		newFishingRodSounds = getBoolean("newFishingRodSounds", catBackport, true, "New sounds for casting and reeling in fishing rods.\nModified Classes: net.minecraft.item.ItemFishingRod");
		newBeaconSounds = getBoolean("newBeaconSounds", catBackport, true, "Allows beacon ambience to play for beacons. Should include most modded beacons.\nModified Classes: net.minecraft.tileentity.TileEntityBeacon");
		hoeTilling = getBoolean("hoeTilling", catBackport, true, "Sounds for hoes tilling farmland.\nModified Classes: net.minecraft.item.ItemHoe");
		dustUnderFallingBlocks = getBoolean("dustUnderFallingBlocks", catBackport, true, "Spawns a particle under falling blocks like sand or gravel that are suspended mid-air.\nModified Classes: net.minecraft.block.BlockFalling");
		postTreeGenEvent = getBoolean("postTreeGenEvent", catBackport, true, "Fires an event after a tree generates, mainly for beehives to accurately know where most trees are. For now this option is disabled if bees are disabled.\nModified Classes: net.minecraft.world.gen.feature.WorldGenAbstractTree");
		ladderTrapdoors = getBoolean("ladderTrapdoors", catBackport, true, "Trapdoors that are clapped open against a block with a ladder below them can be climbed up as if they themselves were a ladder. Also expands the ladder hitbox to match the width of trapdoors.\nModified Classes: net.minecraft.block.BlockLadder net.minecraft.block.BlockTrapdoor");
		rightClickArmorSwap = getBoolean("rightClickArmorSwap", catBackport, true, "Right-clicking with a piece of armor in hand puts it on and takes the worn piece into the hand, as since 1.19.4. 1.7.10 only puts it on when the slot is empty. Armor with a right-click action of its own keeps that action.");
		pickBlockFromInventory = getBoolean("pickBlockFromInventory", catBackport, true, "Pick block (middle click) finds the block anywhere in your inventory and swaps it into the hotbar, as since 1.8. 1.7.10 only looks at the hotbar.");
		effectPanelAlphaFix = getBoolean("effectPanelAlphaFix", catBackport, true, "Turns the alpha test on before the inventory's potion effect panel is drawn. Vanilla assumes it is on; when earlier rendering leaves it off, the first effect shows a black box and coloured corner marks (the hidden pixels of its sprites).");
		placementFacingByLook = getBoolean("placementFacingByLook", catBackport, true, "Pistons, dispensers, droppers and observers face by where you look when placed, vertical when you look mostly up or down, as since 1.13. 1.7.10 decides the vertical cases from where you stand relative to the block." +
				"\nModified Classes: net.minecraft.block.BlockPistonBase");
		modernPistonEvents = getBoolean("modernPistonEvents", catBackport, true, "Piston block events act on the piston as it stands when they run, as modern: a piston placed into the spot of one that was just blown up inherits its pending retraction and clears the block in front of its own face, which is the exploding-piston way of breaking bedrock. 1.7.10 used the facing from when the event was queued. With betterPistons on, its piston code applies this rule itself." +
				"\nModified Classes: net.minecraft.block.BlockPistonBase");
		modernBlockUsePackets = getBoolean("modernBlockUsePackets", catBackport, true, "The client sends every right-click on a block to the server before trying it itself, as since 1.13. 1.7.10 sent nothing when it thought the held block could not be placed there, so a click could never reach the server in the tick that freed the spot. The server decides either way and corrects the client when it placed something the client did not expect. Needed for the exploding-piston bedrock method. The server's placement check then counts the placing player's own body (1.7.10 left that to the client; since 1.8 the server checks it), so a block can't be placed where you stand." +
				"\nModified Classes: net.minecraft.world.World" +
				"\nModified Client Classes: net.minecraft.client.multiplayer.PlayerControllerMP");
		modernMobSpawnLight = getBoolean("modernMobSpawnLight", catBackport, true, "Hostile mobs need block light 0 to spawn, as since 1.18: a lit cave stays empty however dim the light. The sky-light rule is unchanged, so the surface still spawns at night, and so is the thundering rule. Not applied in the Nether, as in modern." +
				"\nModified Classes: net.minecraft.entity.monster.EntityMob");
		arrowsInheritMotion = getBoolean("arrowsInheritMotion", catBackport, true, "An arrow shot by a player adds the player's own movement to its launch, as since 1.9 (the vertical part only while airborne): shooting while sprinting, riding or gliding carries the arrow further. The server learns the movement from the position packets, as modern's does." +
				"\nModified Classes: net.minecraft.entity.player.EntityPlayer net.minecraft.network.NetHandlerPlayServer net.minecraft.entity.projectile.EntityArrow");
		shiftClickIntoCraftingGrid = getBoolean("shiftClickIntoCraftingGrid", catBackport, true, "Shift-clicking a stack from the inventory or the hotbar in the crafting table puts it into the crafting grid first, onto matching stacks or the first empty cell, as since 1.12; only when the grid takes nothing does it move between the inventory and the hotbar as before." +
				"\nModified Classes: net.minecraft.inventory.ContainerWorkbench");
		itemsBackOnClose = getBoolean("itemsBackOnClose", catBackport, true, "Closing a crafting table, the inventory's crafting grid, an anvil, an enchanting table or a smithing table puts what was in it, and the stack on the cursor, back into the inventory, as modern does; only what doesn't fit drops, and everything drops if the player is dead or disconnected. The client no longer spawns a copy of each dropped item of its own either." +
				"\nModified Classes: net.minecraft.inventory.Container net.minecraft.inventory.ContainerWorkbench net.minecraft.inventory.ContainerPlayer net.minecraft.inventory.ContainerRepair net.minecraft.inventory.ContainerEnchantment");
		setblockAtLookedBlock = getBoolean("setblockAtLookedBlock", catBackport, true, "/setblock with no coordinates, from a player, sets the block they look at within reach: /setblock stone. A convenience of this mod, not a modern feature." +
				"\nModified Classes: net.minecraft.command.server.CommandSetBlock");
		chatHistoryBetweenWorlds = getBoolean("chatHistoryBetweenWorlds", catBackport, true, "The messages you typed stay in the chat's history (the up arrow) when you leave a world, as in modern; 1.7.10 forgot them." +
				"\nModified Client Classes: net.minecraft.client.gui.GuiNewChat");
		chatLogBetweenWorlds = getBoolean("chatLogBetweenWorlds", catBackport, true, "The chat log itself stays when you leave a world, so what was said in the last world or server is still there in the next. Modern clears it." +
				"\nModified Client Classes: net.minecraft.client.gui.GuiNewChat");
		skinCustomization = getBoolean("skinCustomization", catBackport, true, "The Skin Customization screen (1.8+), a button in Options: show or hide your cape, jacket, sleeves, pants legs and hat, and pick your main hand; the choices reach every player. The second-layer parts themselves need enablePlayerSkinOverlay in functions.cfg (client)." +
				"\nModified Classes: net.minecraft.entity.player.EntityPlayer" +
				"\nModified Client Classes: net.minecraft.client.gui.GuiOptions net.minecraft.client.renderer.entity.RenderPlayer");
		beaconBeamThroughTransparentBlocks = getBoolean("beaconBeamThroughTransparentBlocks", catBackport, true, "A beacon is on, and its beam shows, through water, ice, glass, leaves, slabs and anything else that is not a fully opaque block, and through bedrock, as since 1.8; 1.7.10 asked the sky map, which any water above the beacon breaks, and so beacons also work under the Nether's roof now. The same rule decides where the coloured beam stops." +
				"\nModified Classes: net.minecraft.tileentity.TileEntityBeacon");
		modernHunger = getBoolean("modernHunger", catBackport, true, "Hunger and healing as in modern (1.9 to 1.11): with saturation left and a full food bar, health comes back every half second from saturation; the slow healing below that costs 6 exhaustion instead of 3. Exhaustion costs are modern's, 1.7.10's in brackets: sprinting 0.1 per metre (same), walking and sneaking nothing (0.01), swimming 0.01 (0.015), a jump 0.05 and a sprint jump 0.2 (0.2 and 0.8), a hit landed 0.1 (0.3), damage taken 0.1 (0.3) and none from starving, breaking a block 0.005 (0.025), the Hunger effect 0.005 per level per tick (0.025)." +
				"\nModified Classes: net.minecraft.util.FoodStats net.minecraft.entity.player.EntityPlayer net.minecraft.util.DamageSource net.minecraft.potion.Potion net.minecraft.block.Block");
		chatHistoryAcrossRestarts = getBoolean("chatHistoryAcrossRestarts", catBackport, true, "The messages you typed also survive restarting the game: the last hundred are kept in etfuturum_chat_history.txt next to options.txt. Neither 1.7.10 nor modern does this." +
				"\nModified Client Classes: net.minecraft.client.gui.GuiNewChat");
		modernCreativeInventoryTab = getBoolean("modernCreativeInventoryTab", catBackport, true, "The creative screen's survival inventory tab laid out as modern's (1.9+): the armour slots in two columns with the player drawn between them, following the mouse, the off-hand slot at their left and modern's background for the tab." +
				"\nModified Client Classes: net.minecraft.client.gui.inventory.GuiContainerCreative");
		modernPlacementShape = getBoolean("modernPlacementShape", catBackport, true, "Placing a ladder or a trapdoor checks the player against the shape the block will have once placed (its orientation, and for a trapdoor its half and whether power opens it), as since 1.13, instead of the block's last computed bounds, often a full block: standing a little way from a wall no longer stops a ladder going onto it, and a trapdoor against a powered block, which is placed open as in modern, only needs its upright strip clear." +
				"\nModified Classes: net.minecraft.item.ItemBlock net.minecraft.block.BlockTrapDoor" +
				"\nModified Client Classes: net.minecraft.item.ItemBlock");
		modernExplosionOrder = getBoolean("modernExplosionOrder", catBackport, true, "Explosions destroy their blocks in random order, as since 1.9. 1.7.10 used a fixed order for a given layout, so whether a block notices a neighbour going before it goes itself (a piston losing its lever, say) was decided by the layout rather than by chance. Needed for the exploding-piston bedrock method to be a matter of tries, as in modern." +
				"\nModified Classes: net.minecraft.world.Explosion");
		modernKeyRepeat = getBoolean("modernKeyRepeat", catBackport, true, "A held keyboard key bound to Use fires at the keyboard's repeat rate, as since 1.13; a held mouse button keeps firing every four ticks, as in both. 1.7.10 counted one press per key-down. Screens keep vanilla's key repeat state, and repeats of other keys are dropped. Needed for the exploding-piston bedrock method, where the placing click has to land in one tick." +
				"\nModified Client Classes: net.minecraft.client.Minecraft");
		enableOffhand = getBoolean("enableOffhand", catBackport, true, "The off-hand (1.9+): a second hand slot, swapped with the main hand by a key (F by default), used when the main hand has nothing to do, shown beside the hotbar. Turns itself off when Backhand or Battlegear 2 is loaded, since a player can only have one off-hand." +
				"\nModified Classes: net.minecraft.entity.player.InventoryPlayer net.minecraft.entity.player.EntityPlayer net.minecraft.inventory.ContainerPlayer net.minecraft.network.NetHandlerPlayServer net.minecraft.server.management.ItemInWorldManager net.minecraft.item.ItemBow net.minecraft.entity.EntityLivingBase net.minecraft.inventory.Container" +
				"\nModified Client Classes: net.minecraft.client.gui.inventory.GuiContainer net.minecraft.client.gui.inventory.GuiInventory net.minecraft.client.gui.inventory.GuiContainerCreative net.minecraft.client.Minecraft net.minecraft.client.multiplayer.PlayerControllerMP net.minecraft.client.entity.EntityClientPlayerMP net.minecraft.client.network.NetHandlerPlayClient net.minecraft.client.entity.EntityOtherPlayerMP net.minecraft.client.renderer.ItemRenderer net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.model.ModelBiped");
		noPotionGlint = getBoolean("noPotionGlint", catBackport, true, "Potion bottles have no enchantment glint, as since 1.9. 1.7.10 gave every potion with an effect the glint." +
				"\nModified Client Classes: net.minecraft.item.ItemPotion");
		enableElytra = getBoolean("enableElytra", catBackport, true, "The elytra, based on a port of Backlytra (original author unascribed: https://legacy.curseforge.com/minecraft/mc-mods/backlytra), with modern (1.21) gliding: a 0.6 block tall hitbox and low eyes while gliding, modern takeoff rules, flight, wings, camera and firework boosting.\nIf you're getting crash related to the DataWatcher, try changing \"elytraDataWatcherFlag\" or \"poseDataWatcherId\" in functions.cfg and don't open an issue if changing that value fixes it." +
				"\nModified Classes: net.minecraft.entity.EntityLivingBase net.minecraft.entity.player.EntityPlayer net.minecraft.entity.EntityTrackerEntry net.minecraft.network.NetHandlerPlayServer net.minecraft.item.ItemFirework" +
				"\nModified Client Classes: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.model.ModelBiped net.minecraft.client.renderer.EntityRenderer net.minecraft.client.renderer.entity.RenderManager net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.renderer.ItemRenderer net.minecraft.util.MovementInputFromOptions" +
				"\nModified Mod Classes: thaumcraft.common.lib.events.EventHandlerEntity");
		betterPistons = getBoolean("betterPistons", catBackport, true, "A port of Back in Slime, similar to how the elytra is a port of Backlytra. Allows pistons to interact with slime blocks. The original author of this is DonBruce64: https://legacy.curseforge.com/minecraft/mc-mods/back-in-slime-slime-blocks-for-1-7.\nModified Classes: net.minecraft.block.BlockPistonBase");
		soulFire = getBoolean("soulFire", catBackport, true, "Is not a new block, but rather a mixin for fire. Allows fire to stay ignited on soul soil. Does double damage when standing in it, and does not spread to other blocks.\nEven if this is off fire can still stay ignited on soul soil, but do be mindful that fire will spread from soul soil if this option is disabled.\nModified Classes: net.minecraft.block.BlockFire net.minecraft.entity.Entity\nModified Client Classes: net.minecraft.client.renderer.RenderBlocks");
		fireproofItems = getBoolean("fireproofItems", catBackport, true, "Some items such as Netherite will not burn in fire and will float to the surface of lava.");
		thinPanes = getBoolean("thinPanes", catBackport, true, "Panes [Iron Bars, Glass Panes, etc] are changed to a single post if they are not connected to any other blocks" +
				"\nModified Classes: net.minecraft.block.BlockPane" +
				"\nModified Client Classes: net.minecraft.client.renderer.RenderBlocks");
		colorGrassBlockItemSides = getBoolean("colorGrassBlockItemSides", catBackport, true, "Grass block sides are colored in the player's inventory" +
				"\nModified Client Classes: net.minecraft.client.renderer.RenderBlocks");
		adjustedLiquidPhysics = getBoolean("adjustedLiquidPhysics", catBackport, true, "Moves entities in lava, speeds up items in all liquids, floats items in liquids, changes some other liquid to entity interactions.\nModified Classes: net.minecraft.world.World net.minecraft.entity.Entity net.minecraft.block.BlockLiquid");
		liquidItemFloat = getBoolean("liquidItemFloat", catBackport, true, "Floats items upwards in liquids.\nModified Classes: net.minecraft.entity.item.EntityItem");
		riddenHorsesInWater = getBoolean("riddenHorsesInWater", catBackport, true, "Allows ridden horses to float in water like in modern Minecraft versions.");

		stepHeightFix = getBoolean("stepHeightFix", catFixes, true, "Makes the player able to step up even if a block would be above their head at the destination.\nModified classes: net.minecraft.entity.Entity");
		arrowFallingFix = getBoolean("arrowFallingFix", catFixes, true, "Prevents arrows from falling off of blocks too easily\nModified classes: net.minecraft.entity.EntityArrow");
		connectionAcceptFix = getBoolean("connectionAcceptFix", catFixes, true, "Fixes the rare hang on the loading screen when entering a world, and a dedicated server never finishing a player's login: the server's network tick could drop a new connection in the instant between it being listed and its channel becoming active, after which it was never read. 1.8 fixed the same race." +
				"\nModified Classes: net.minecraft.network.NetworkSystem net.minecraft.network.NetworkManager");
		guiScaleWheel = getBoolean("guiScaleWheel", catMisc, true, "In Video Settings, after the first click on GUI Scale, the mouse wheel cycles the GUI scale (up: larger) instead of scrolling the list, until the screen is closed. A click lays the screen out again for the new scale and the button moves away from the cursor; the wheel does not." +
				"\nModified Classes: Client: net.minecraft.client.gui.GuiVideoSettings net.minecraft.client.gui.GuiSlot");
		offlineSkins = getBoolean("offlineSkins", catMisc, false, "For test servers. Players a server does not authenticate (offline mode, or a development client's single-player) get the skin of the Mojang account of the typed name, looked up on Mojang's public API when they log in, so every client, their own included, draws it; anyone can wear any name's skin. Set it on the server and on the clients. Answers are kept for a day in etfuturum_offline_skins.json next to the server's world (delete it to look a skin up again)." +
				"\nModified Classes: net.minecraft.server.network.NetHandlerLoginServer\nClient: net.minecraft.util.Session");
		collidedThrowableFix = getBoolean("collidedThrowableFix", catFixes, true, "Fixes EntityThrowable entities not calling onEntityCollidedWithBlock, causing them to not trigger target blocks or chime amethyst.\nModified classes: net.minecraft.entity.projectile.EntityThrowable");
		hideSingleLevelEnchants = getBoolean("hideSingleLevelEnchants", catFixes, true, "Fixes enchantments with only one possible level displaying a level in their name. E.G. \"Silk Touch I\" becomes \"Silk Touch\".\nModified Classes: net.minecraft.enchantment.Enchantment");
		enablePlayersSleepingPecentageGamerule = getBoolean("enablePlayersSleepingPecentageGamerule", catBackport, true, "You nappa, you get slappa");
        enableJumpClimbing = getBoolean("enableClimbingPlants", catBackport, true, "Allows players to simply hold jump to climb up climable blocks instead of having to collide with something horizontally.");
	}
}
