package io.github.asgerjon.mcfly.mixins.early.signs;

import ganymedes01.etfuturum.signs.ModernSigns;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockSign;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Using an item on a sign (SPEC-modern-movement.md §67.2, 26.3's `SignBlock.useItemOn`): 1.7.10's sign does nothing
 * when used, so this is the whole use; EFR's wood signs (`BlockWoodSign extends BlockSign`) get it too. Not taken, the
 * item's own use goes on.
 */
@Mixin(BlockSign.class)
public abstract class MixinBlockSign extends BlockContainer {

	protected MixinBlockSign(Material material) {
		super(material);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return ModernSigns.use(world, x, y, z, player) || super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
	}
}
