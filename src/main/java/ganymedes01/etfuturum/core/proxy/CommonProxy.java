package ganymedes01.etfuturum.core.proxy;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;

import ganymedes01.etfuturum.client.gui.inventory.GuiAnvil;
import ganymedes01.etfuturum.client.gui.inventory.GuiEnchantment;
import ganymedes01.etfuturum.client.gui.inventory.GuiNewBrewingStand;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigTweaks;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.handlers.EntityEventHandler;
import ganymedes01.etfuturum.core.handlers.ServerEventHandler;
import ganymedes01.etfuturum.core.handlers.WorldEventHandler;
import ganymedes01.etfuturum.core.utils.Logger;
import ganymedes01.etfuturum.core.utils.Utils;

import ganymedes01.etfuturum.entities.EntityArmourStand;
import ganymedes01.etfuturum.entities.EntityBee;
import ganymedes01.etfuturum.entities.EntityBrownMooshroom;
import ganymedes01.etfuturum.entities.EntityEndermite;
import ganymedes01.etfuturum.entities.EntityFox;
import ganymedes01.etfuturum.entities.EntityHusk;
import ganymedes01.etfuturum.entities.EntityItemUninflammable;
import ganymedes01.etfuturum.entities.EntityNewSnowGolem;
import ganymedes01.etfuturum.entities.EntityRabbit;
import ganymedes01.etfuturum.entities.EntityRespawnedDragon;
import ganymedes01.etfuturum.entities.EntityShulker;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.EntityZombieVillager;
import ganymedes01.etfuturum.entities.ModEntityList;
import io.github.asgerjon.mcfly.registry.McFlyEntities;
import ganymedes01.etfuturum.inventory.ContainerAnvil;
import ganymedes01.etfuturum.inventory.ContainerEnchantment;
import ganymedes01.etfuturum.inventory.ContainerNewBrewingStand;
import ganymedes01.etfuturum.lib.GUIIDs;

import ganymedes01.etfuturum.tileentities.TileEntityBeeHive;
import ganymedes01.etfuturum.tileentities.TileEntityCauldronPotion;
import ganymedes01.etfuturum.tileentities.TileEntityNewBeacon;
import ganymedes01.etfuturum.tileentities.TileEntityNewBrewingStand;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonProxy implements IGuiHandler {

	public void registerEvents() {
		MinecraftForge.EVENT_BUS.register(EntityEventHandler.INSTANCE);

		FMLCommonHandler.instance().bus().register(ServerEventHandler.INSTANCE);
		MinecraftForge.EVENT_BUS.register(ServerEventHandler.INSTANCE);

		if (ganymedes01.etfuturum.core.handlers.PowderSnowEvents.isEnabled()) {
			MinecraftForge.EVENT_BUS.register(ganymedes01.etfuturum.core.handlers.PowderSnowEvents.INSTANCE);
		}

		FMLCommonHandler.instance().bus().register(WorldEventHandler.INSTANCE);
		MinecraftForge.TERRAIN_GEN_BUS.register(WorldEventHandler.INSTANCE);



		if (ModItems.DEBUGGING_TOOL.isEnabled()) {
			MinecraftForge.EVENT_BUS.register(ModItems.DEBUGGING_TOOL.get());
		}
	}

	public void registerEntities() {
		if (ModBlocks.BREWING_STAND.isEnabled()) {
			GameRegistry.registerTileEntity(TileEntityNewBrewingStand.class, Utils.getUnlocalisedName("brewing_stand"));
		}
		if (ModBlocks.BEACON.isEnabled()) {
			GameRegistry.registerTileEntity(TileEntityNewBeacon.class, Utils.getUnlocalisedName("beacon"));
		}
		// the smoker's, blast furnace's (spec 92.3) and barrel's (92.4) are McFly's
		if (ModBlocks.POTION_CAULDRON.isEnabled()) {
			GameRegistry.registerTileEntity(TileEntityCauldronPotion.class, Utils.getUnlocalisedName("potion_cauldron"));
		}
		if (ModBlocks.BEEHIVE.isEnabled() || ModBlocks.BEE_NEST.isEnabled()) {
			GameRegistry.registerTileEntity(TileEntityBeeHive.class, Utils.getUnlocalisedName("hive"));
		}
		
		if (ConfigEntities.enableRabbit) {
			ModEntityList.registerEntity(EntityRabbit.class, "rabbit", 3, EtFuturum.instance, 80, 3, true, 0x995F40, 0x734831);

			EntityRegistry.addSpawn(EntityRabbit.class, 3, 1, 3, EnumCreatureType.creature, BiomeDictionary.getBiomesForType(Type.SANDY));
			BiomeGenBase[] array = BiomeDictionary.getBiomesForType(Type.SNOWY);
			array = ArrayUtils.addAll(array, BiomeDictionary.getBiomesForType(Type.PLAINS));
			array = ArrayUtils.addAll(array, BiomeDictionary.getBiomesForType(Type.FOREST));
			for (BiomeGenBase biome : ArrayUtils.clone(array)) {
				if (biome.getClass().getName().toLowerCase().contains("divinerpg")) {
					ArrayUtils.removeElement(array, biome);
				}
			}
			EntityRegistry.addSpawn(EntityRabbit.class, 6, 1, 3, EnumCreatureType.creature, array);
		}

		if (ConfigBlocksItems.enableArmourStand) {
			ModEntityList.registerEntity(EntityArmourStand.class, "wooden_armorstand", 0, EtFuturum.instance, 64, 1, true);
		}

		if (ConfigEntities.enableEndermite) {
			ModEntityList.registerEntity(EntityEndermite.class, "endermite", 1, EtFuturum.instance, 64, 1, true, 0x161616, 0x6E6E6E);
		}


		if (FMLCommonHandler.instance().getSide() == Side.CLIENT && FMLClientHandler.instance().hasOptifine()) {
			if (!ConfigWorld.oldHuskSpawning) {
				Logger.warn("OptiFine detected, old husk spawn logic will be enabled since OptiFine is stupid and breaks the default behavior.");
				ConfigWorld.oldHuskSpawning = true;
			}
			if (!ConfigWorld.oldStraySpawning) {
				Logger.warn("OptiFine detected, old stray spawn logic will be enabled since OptiFine is stupid and breaks the default behavior.");
				ConfigWorld.oldStraySpawning = true;
			}
		}

		if (ConfigEntities.enableHusk) {
			ModEntityList.registerEntity(EntityHusk.class, "husk", 4, EtFuturum.instance, 80, 3, true, 0x777561, 0xE0D991);

			if (ConfigWorld.oldHuskSpawning) {
				List<BiomeGenBase> biomes = new ArrayList<>(Arrays.asList(BiomeDictionary.getBiomesForType(Type.SANDY)));
				biomes.retainAll(Arrays.asList(BiomeDictionary.getBiomesForType(Type.DRY)));
				biomes.retainAll(Arrays.asList(BiomeDictionary.getBiomesForType(Type.HOT)));

				BiomeGenBase[] biomeArray = (BiomeGenBase[]) ArrayUtils.addAll(new BiomeGenBase[]{}, biomes.toArray());
				//change spawn weights
				EntityRegistry.removeSpawn(EntityZombie.class, EnumCreatureType.monster, biomeArray);

				EntityRegistry.addSpawn(EntityZombie.class, 20, 4, 4, EnumCreatureType.monster, biomeArray);
				EntityRegistry.addSpawn(EntityHusk.class, 80, 4, 4, EnumCreatureType.monster, biomeArray);
			}
		}

		if (ConfigEntities.enableStray) {
			ModEntityList.registerEntity(EntityStray.class, "stray", 5, EtFuturum.instance, 80, 3, true, 0x617778, 0xE6EAEA);

			if (ConfigWorld.oldStraySpawning) {
				//change spawn weights
				EntityRegistry.removeSpawn(EntitySkeleton.class, EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.SNOWY));

				EntityRegistry.addSpawn(EntitySkeleton.class, 20, 4, 4, EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.SNOWY));
				EntityRegistry.addSpawn(EntityStray.class, 80, 4, 4, EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.SNOWY));
			} else {
				// 26.3's lists (spec §63.2): strays 80 and skeletons 20 in snowy plains and ice spikes only; where they
				// may spawn is EntityStray.getCanSpawnHere's sky rule
				List<BiomeGenBase> snowyPlains = new ArrayList<>();
				for (BiomeGenBase biome : new BiomeGenBase[]{BiomeGenBase.icePlains, BiomeGenBase.getBiome(BiomeGenBase.icePlains.biomeID + 128)}) {
					if (biome != null) {
						snowyPlains.add(biome);
					}
				}
				BiomeGenBase[] strayBiomes = snowyPlains.toArray(new BiomeGenBase[0]);
				EntityRegistry.removeSpawn(EntitySkeleton.class, EnumCreatureType.monster, strayBiomes);
				EntityRegistry.addSpawn(EntitySkeleton.class, 20, 4, 4, EnumCreatureType.monster, strayBiomes);
				EntityRegistry.addSpawn(EntityStray.class, 80, 4, 4, EnumCreatureType.monster, strayBiomes);
			}
		}

		if (ConfigEntities.enableNetherEndermen) {
			EntityRegistry.addSpawn(EntityEnderman.class, 1, 4, 4, EnumCreatureType.monster, BiomeGenBase.hell);
//			if (io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.isEnabled()) {
//				EntityEnderman.setCarriable(io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), true);
//			}
		}


		if (ConfigEntities.enableVillagerZombies)
			ModEntityList.registerEntity(EntityZombieVillager.class, "villager_zombie", 8, EtFuturum.instance, 80, 3, true, 0x00AFAF, 0x799C65);

		if (ConfigEntities.enableDragonRespawn) {
			ModEntityList.registerEntity(EntityRespawnedDragon.class, "ender_dragon", 10, EtFuturum.instance, 160, 3, true);
		}

		if (ConfigEntities.enableShearableSnowGolems)
			ModEntityList.registerEntity(EntityNewSnowGolem.class, "snow_golem", 11, EtFuturum.instance, 80, 3, true);

		if (ConfigEntities.enableBrownMooshroom)
			ModEntityList.registerEntity(EntityBrownMooshroom.class, "brown_mooshroom", 12, EtFuturum.instance, 80, 3, true);

		ModEntityList.registerEntity(EntityItemUninflammable.class, "fireproof_item", 15, EtFuturum.instance, 64, 1, true);

		if (ConfigEntities.enableShulker) {
			// ours (McFly's list, spec §79 B6), at their old place among EFR's so the spawn eggs keep their ids
			McFlyEntities.SHULKER.register(EtFuturum.instance);
			McFlyEntities.SHULKER_BULLET.register(EtFuturum.instance);

			if (ConfigTweaks.shulkersSpawnAnywhere) {
				EntityRegistry.addSpawn(EntityShulker.class, 1, 1, 2, EnumCreatureType.monster, BiomeDictionary.getBiomesForType(Type.END));
			}
		}

//      {
//      }

		// the boats (13, 19, 14) are McFly's now (McFlyEntities, spec 91.12)


		if (ConfigEntities.enableBees) {
			ModEntityList.registerEntity(EntityBee.class, "bee", 21, EtFuturum.instance, 80, 1, true, 0xEDC343, 0x43241B);
		}

		if (ConfigEntities.enableFoxes) {
			ModEntityList.registerEntity(EntityFox.class, "fox", 22, EtFuturum.instance, 64, 1, true, 0xD5B69F, 0xCC6920);
			EntityRegistry.addSpawn(EntityFox.class, 8, 2, 4, EnumCreatureType.creature, BiomeDictionary.getBiomesForType(Type.CONIFEROUS));
		}

		// the rest of ours (McFly's list, spec §79 B6), after EFR's own, and where they spawn
		McFlyEntities.registerAll(EtFuturum.instance);

		//make magmas slightly more common, hopefully.
		EntityRegistry.removeSpawn(EntityMagmaCube.class, EnumCreatureType.monster, BiomeGenBase.hell);
		EntityRegistry.addSpawn(EntityMagmaCube.class, 2, 4, 4, EnumCreatureType.monster, BiomeGenBase.hell);
	}


	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		// McFly's own, from 100 (spec 92.3)
		if (ID >= io.github.asgerjon.mcfly.base.McFlyGuis.FIRST) {
			return io.github.asgerjon.mcfly.base.McFlyGuis.server(ID, player, world, x, y, z);
		}
		return switch (ID) {
			case GUIIDs.ENCHANTING_TABLE -> new ContainerEnchantment(player.inventory, world, x, y, z);
			case GUIIDs.ANVIL -> new ContainerAnvil(player, world, x, y, z);
			case GUIIDs.BREWING_STAND ->
					new ContainerNewBrewingStand(player.inventory, (TileEntityNewBrewingStand) world.getTileEntity(x, y, z));
			case GUIIDs.LOOM -> new ganymedes01.etfuturum.inventory.ContainerLoom(player.inventory, world, x, y, z);
			default -> null;
		};
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID >= io.github.asgerjon.mcfly.base.McFlyGuis.FIRST) {
			return io.github.asgerjon.mcfly.base.McFlyGuis.client(ID, player, world, x, y, z);
		}
		return switch (ID) {
			case GUIIDs.ENCHANTING_TABLE -> new GuiEnchantment(player.inventory, world, null);
			case GUIIDs.ANVIL -> new GuiAnvil(player, world, x, y, z);
			case GUIIDs.BREWING_STAND ->
					new GuiNewBrewingStand(player.inventory, (TileEntityNewBrewingStand) world.getTileEntity(x, y, z));
			case GUIIDs.LOOM -> new ganymedes01.etfuturum.client.gui.inventory.GuiLoom(new ganymedes01.etfuturum.inventory.ContainerLoom(player.inventory, world, x, y, z));
			default -> null;
		};
	}

	public void registerRenderers() {
	}
}
