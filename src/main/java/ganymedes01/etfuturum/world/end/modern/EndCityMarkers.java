package ganymedes01.etfuturum.world.end.modern;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityShulker;
import ganymedes01.etfuturum.loot.LootTableHolder;
import ganymedes01.etfuturum.world.structure.modern.ModernBox;
import ganymedes01.etfuturum.world.structure.modern.ModernTemplate;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Random;

/**
 * An end city piece's data markers as 26.3's `EndCityPiece.handleDataMarker` (SPEC-modern-movement.md §76.12, 7c):
 * `Chest` gives the chest below it the `end_city_treasure` table and a seed; `Sentry` a shulker on the spot;
 * `Elytra` an item frame there, facing the piece's turn of south, an elytra in it. Each only in the chunk being made.
 */
public final class EndCityMarkers {

	public static final String TREASURE = "minecraft:chests/end_city_treasure";

	private EndCityMarkers() {
	}

	static void handle(World world, EndCities.Piece piece, ModernTemplate.Marker marker, ModernBox chunkBox, Random random) {
		if (marker.metadata.startsWith("Chest")) {
			if (chunkBox.isInside(marker.x, marker.y - 1, marker.z)) {
				TileEntity chest = world.getTileEntity(marker.x, marker.y - 1, marker.z);
				if (chest instanceof LootTableHolder holder) {
					holder.etfu$setLootTable(TREASURE, random.nextLong());
					chest.markDirty();
				}
			}
		} else if (chunkBox.isInside(marker.x, marker.y, marker.z) && marker.y >= -20000000 && marker.y < 20000000) {
			if (marker.metadata.startsWith("Sentry")) {
				if (ConfigEntities.enableShulker) {
					EntityShulker sentry = new EntityShulker(world);
					sentry.setPosition(marker.x + 0.5, marker.y, marker.z + 0.5);
					world.spawnEntityInWorld(sentry);
				}
			} else if (marker.metadata.startsWith("Elytra")) {
				String facing = piece.rotation().rotate("south");
				int direction = hangingDirection(facing);
				// 1.7.10's frame is made from the block it hangs on, behind it
				int[] step = step(facing);
				EntityItemFrame frame = new EntityItemFrame(world, marker.x - step[0], marker.y, marker.z - step[1], direction);
				if (io.github.asgerjon.mcfly.registry.McFlyItems.ELYTRA.isEnabled()) {
					frame.setDisplayedItem(new ItemStack(io.github.asgerjon.mcfly.registry.McFlyItems.ELYTRA.get()));
				}
				world.spawnEntityInWorld(frame);
			}
		}
	}

	/** 1.7.10's hanging directions (Direction): south 0, west 1, north 2, east 3. */
	private static int hangingDirection(String facing) {
		return switch (facing) {
			case "west" -> 1;
			case "north" -> 2;
			case "east" -> 3;
			default -> 0;
		};
	}

	private static int[] step(String facing) {
		return switch (facing) {
			case "west" -> new int[] {-1, 0};
			case "north" -> new int[] {0, -1};
			case "east" -> new int[] {1, 0};
			default -> new int[] {0, 1};
		};
	}
}
