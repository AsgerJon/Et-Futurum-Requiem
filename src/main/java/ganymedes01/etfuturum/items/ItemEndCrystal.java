package ganymedes01.etfuturum.items;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.entities.EntityPlacedEndCrystal;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSimpleFoiled;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemEndCrystal extends ItemSimpleFoiled {

	public ItemEndCrystal() {
		setTextureName("end_crystal");
		setUnlocalizedName(Utils.getUnlocalisedName("end_crystal"));
		setCreativeTab(EtFuturum.creativeTabItems);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		if (ganymedes01.etfuturum.world.end.modern.ModernEnd.isActive()) {
			return useAsModern(stack, player, world, x, y, z);
		}
		if (side != 1)
			return false;

		Block block = world.getBlock(x, y, z);
		if (block == Blocks.obsidian || block == Blocks.bedrock)
			if (world.isAirBlock(x, y + 1, z)) {
				if (!world.isRemote) {
					EntityPlacedEndCrystal endCrystal = new EntityPlacedEndCrystal(world);
					endCrystal.setPosition(x + 0.5, y, z + 0.5);
					endCrystal.setBlockPos(x, y, z);

					world.spawnEntityInWorld(endCrystal);
					if (!player.capabilities.isCreativeMode)
						stack.stackSize--;
				}
				return true;
			}

		return false;
	}
	/**
	 * 26.3's (SPEC-modern-movement.md §76.9, `EndCrystalItem`): on any face of obsidian or bedrock whose block above is
	 * empty with no entity in the 1 by 2 above it, a crystal stands there without its base, one used (not in creative),
	 * and the fight tries a respawn.
	 */
	private static boolean useAsModern(ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block != Blocks.obsidian && block != Blocks.bedrock || !world.isAirBlock(x, y + 1, z)) {
			return false;
		}
		if (!world.getEntitiesWithinAABBExcludingEntity(null, net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y + 1, z, x + 1, y + 3, z + 1)).isEmpty()) {
			return false;
		}
		if (!world.isRemote) {
			net.minecraft.entity.item.EntityEnderCrystal crystal = new net.minecraft.entity.item.EntityEnderCrystal(world);
			// 1.7.10 keeps an entity's y above its feet by its yOffset: set from the feet, as 26.3 places it
			crystal.setLocationAndAngles(x + 0.5, y + 1, z + 0.5, 0.0F, 0.0F);
			ganymedes01.etfuturum.world.end.modern.EndCrystals.setShowBottom(crystal, false);
			world.spawnEntityInWorld(crystal);
			ganymedes01.etfuturum.world.end.modern.EnderDragonFight fight = ganymedes01.etfuturum.world.end.modern.EnderDragonFight.ifThere(world);
			if (fight != null) {
				fight.tryRespawn();
			}
		}
		if (!player.capabilities.isCreativeMode) {
			stack.stackSize--;
		}
		return true;
	}
}
