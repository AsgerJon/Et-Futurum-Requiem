package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * 26.3's recipes for the chain and the lights (SPEC-modern-movement.md §88.5; `netherlights_recipes_26_3.txt` in the
 * tests is its jar's list): the chain from two nuggets and an ingot, a lantern from eight nuggets around a torch (a soul
 * torch for the soul lantern), four soul torches from coal or charcoal over a stick over soul sand or soul soil (26.3's
 * `#soul_fire_base_blocks`; soul soil EFR's till step E). 1.7.10 has no iron nugget: other mods' `nuggetIron`.
 */
public final class NetherLightRecipes {

	private NetherLightRecipes() {
	}

	public static void register() {
		if (McFlyBlocks.CHAIN.isEnabled()) {
			GameRegistry.addRecipe(new ShapedOreRecipe(McFlyBlocks.CHAIN.newItemStack(), "N", "I", "N", 'N', "nuggetIron", 'I', Items.iron_ingot));
		}
		if (McFlyBlocks.LANTERN.isEnabled()) {
			GameRegistry.addRecipe(new ShapedOreRecipe(McFlyBlocks.LANTERN.newItemStack(), "XXX", "X#X", "XXX", 'X', "nuggetIron", '#', Blocks.torch));
		}
		if (McFlyBlocks.SOUL_TORCH.isEnabled()) {
			Block soulSoil = io.github.asgerjon.mcfly.registry.McFlyBlocks.SOUL_SOIL.isEnabled() ? io.github.asgerjon.mcfly.registry.McFlyBlocks.SOUL_SOIL.get() : null;
			for (ItemStack coal : new ItemStack[]{new ItemStack(Items.coal, 1, 0), new ItemStack(Items.coal, 1, 1)}) {
				for (Block base : new Block[]{Blocks.soul_sand, soulSoil}) {
					if (base != null) {
						GameRegistry.addRecipe(McFlyBlocks.SOUL_TORCH.newItemStack(4), "X", "#", "S", 'X', coal, '#', Items.stick, 'S', new ItemStack(base));
					}
				}
			}
			if (McFlyBlocks.SOUL_LANTERN.isEnabled()) {
				GameRegistry.addRecipe(new ShapedOreRecipe(McFlyBlocks.SOUL_LANTERN.newItemStack(), "XXX", "X#X", "XXX", 'X', "nuggetIron", '#',
					McFlyBlocks.SOUL_TORCH.get()));
			}
		}
	}
}
