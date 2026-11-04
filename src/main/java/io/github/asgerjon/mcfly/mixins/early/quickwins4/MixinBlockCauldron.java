package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.FilledResult;
import net.minecraft.block.BlockCauldron;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 1.7.10's cauldron as 26.3's (SPEC-modern-movement.md §63.1, §63.5): an empty bucket takes a full water cauldron,
 * a lava bucket replaces water at any level, a bottle filled in creative keeps the bottle; and for every cauldron (EFR's
 * lava and powder snow ones are cauldrons too) 26.3's collision ({@code AbstractCauldronBlock.SHAPE}).
 */
@Mixin(BlockCauldron.class)
public abstract class MixinBlockCauldron {

	/** 26.3's shape in sixteenths: the floor, four walls, eight pieces of leg. */
	@Unique
	private static final double[][] etfu$SHAPE = {
			{0, 3, 0, 16, 4, 16},
			{0, 4, 0, 2, 16, 16}, {14, 4, 0, 16, 16, 16}, {2, 4, 0, 14, 16, 2}, {2, 4, 14, 14, 16, 16},
			{0, 0, 0, 4, 3, 2}, {0, 0, 2, 2, 3, 4}, {12, 0, 0, 16, 3, 2}, {14, 0, 2, 16, 3, 4},
			{0, 0, 14, 4, 3, 16}, {0, 0, 12, 2, 3, 14}, {12, 0, 14, 16, 3, 16}, {14, 0, 12, 16, 3, 14}};

	@Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
	private void etfu$modernBucketsAndBottles(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ,
											  CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this != Blocks.cauldron) {
			return;
		}
		ItemStack held = player.getCurrentEquippedItem();
		if (held == null) {
			return;
		}
		int level = world.getBlockMetadata(x, y, z);
		if (McFlyConfig.modernCauldrons && held.getItem() == Items.bucket) {
			if (level < 3) {
				cir.setReturnValue(false);
				return;
			}
			if (!world.isRemote) {
				world.setBlockMetadataWithNotify(x, y, z, 0, 2);
				world.func_147453_f(x, y, z, Blocks.cauldron);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":item.bucket.fill", 1.0F, 1.0F);
				FilledResult.fill(player, held, new ItemStack(Items.water_bucket));
			}
			cir.setReturnValue(true);
		} else if (McFlyConfig.modernCauldrons && held.getItem() == Items.lava_bucket && level > 0 && ModBlocks.LAVA_CAULDRON.isEnabled()) {
			if (!world.isRemote) {
				world.setBlock(x, y, z, ModBlocks.LAVA_CAULDRON.get(), 0, 3);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, McFly.MC_ASSETS + ":item.bucket.empty_lava", 1.0F, 1.0F);
				if (!player.capabilities.isCreativeMode) {
					held.func_150996_a(Items.bucket);
				}
			}
			cir.setReturnValue(true);
		} else if (McFlyConfig.modernCreativeFilling && player.capabilities.isCreativeMode && held.getItem() == Items.glass_bottle && level > 0) {
			// 1.7.10 used the bottle up in creative too
			if (!world.isRemote) {
				FilledResult.fill(player, held, new ItemStack(Items.potionitem, 1, 0));
				world.setBlockMetadataWithNotify(x, y, z, level - 1, 2);
				world.func_147453_f(x, y, z, Blocks.cauldron);
			}
			cir.setReturnValue(true);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Inject(method = "addCollisionBoxesToList", at = @At("HEAD"), cancellable = true)
	private void etfu$modernShape(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity, CallbackInfo ci) {
		if (!McFlyConfig.modernCauldrons) {
			return;
		}
		for (double[] part : etfu$SHAPE) {
			AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + part[0] / 16.0D, y + part[1] / 16.0D, z + part[2] / 16.0D,
					x + part[3] / 16.0D, y + part[4] / 16.0D, z + part[5] / 16.0D);
			if (mask.intersectsWith(box)) {
				list.add(box);
			}
		}
		ci.cancel();
	}
}
