package io.github.asgerjon.mcfly.end;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.world.end.modern.EndCrystals;
import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import io.github.asgerjon.mcfly.base.McFlyItem;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * 26.3's end crystal item (`EndCrystalItem`, its glint; SPEC-modern-movement.md §76.9, §85): on any face of obsidian or
 * bedrock whose block above is empty with no entity in the 1 by 2 above it, a crystal stands there without its base,
 * one used up (not in creative), and the End's fight tries a respawn. 26.3's recipe: glass round an eye of ender over a
 * ghast tear.
 */
public class EndCrystalItem extends McFlyItem {

	public EndCrystalItem() {
		super("end_crystal");
	}

	public static void registerRecipe(ItemStack crystal) {
		GameRegistry.addShapedRecipe(crystal, "GGG", "GEG", "GTG", 'G', Blocks.glass, 'E', Items.ender_eye, 'T', Items.ghast_tear);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		Block block = world.getBlock(x, y, z);
		if (block != Blocks.obsidian && block != Blocks.bedrock || !world.isAirBlock(x, y + 1, z)) {
			return false;
		}
		if (!world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(x, y + 1, z, x + 1, y + 3, z + 1)).isEmpty()) {
			return false;
		}
		if (!world.isRemote) {
			EntityEnderCrystal crystal = new EntityEnderCrystal(world);
			// 1.7.10 keeps an entity's y above its feet by its yOffset: set from the feet, as 26.3 places it
			crystal.setLocationAndAngles(x + 0.5, y + 1, z + 0.5, 0.0F, 0.0F);
			EndCrystals.setShowBottom(crystal, false);
			world.spawnEntityInWorld(crystal);
			// 26.3's `entity_place` above the block, by the player (§90.9, step H1)
			io.github.asgerjon.mcfly.gameevent.GameEventHooks.atBlock(world, "entity_place", x, y + 1, z, player);
			EnderDragonFight fight = EnderDragonFight.ifThere(world);
			if (fight != null) {
				fight.tryRespawn();
			}
		}
		if (!player.capabilities.isCreativeMode) {
			stack.stackSize--;
		}
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasEffect(ItemStack stack, int pass) {
		return true;
	}
}
