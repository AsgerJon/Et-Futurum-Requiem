package ganymedes01.etfuturum.core.handlers;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.FilledResult;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.entities.Freezing;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;

/** Powder snow's events (SPEC-modern-movement.md §62.3, §62.6): the bucket picking it up, its game rule, tracking. */
public class PowderSnowEvents {

	public static final PowderSnowEvents INSTANCE = new PowderSnowEvents();

	/** 26.3 {@code PowderSnowBlock.pickupBlock}: air (with the break particles), a powder snow bucket, its fill sound. */
	@SubscribeEvent
	public void onFillBucket(FillBucketEvent event) {
		MovingObjectPosition target = event.target;
		if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || event.current == null
				|| event.current.getItem() != Items.bucket || !McFlyItems.POWDER_SNOW_BUCKET.isEnabled()) {
			return;
		}
		World world = event.world;
		int x = target.blockX, y = target.blockY, z = target.blockZ;
		Block block = world.getBlock(x, y, z);
		if (!Freezing.isPowderSnow(block) || !world.canMineBlock(event.entityPlayer, x, y, z)
				|| !event.entityPlayer.canPlayerEdit(x, y, z, target.sideHit, event.current)) {
			return;
		}
		world.setBlockToAir(x, y, z);
		if (!world.isRemote) {
			world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(block));
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.fill_powder_snow", 1.0F, 1.0F);
		}
		event.result = new ItemStack(McFlyItems.POWDER_SNOW_BUCKET.get());
		event.setResult(Event.Result.ALLOW);
		// 1.7.10's bucket gives nothing in creative; 26.3's createFilledResult adds one if there's none (spec §63.1)
		if (McFlyConfig.modernCreativeFilling && event.entityPlayer.capabilities.isCreativeMode && !world.isRemote
				&& !FilledResult.holds(event.entityPlayer, event.result)) {
			event.entityPlayer.inventory.addItemStackToInventory(event.result.copy());
		}
	}

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (!event.world.isRemote && !event.world.getGameRules().hasRule(Freezing.GAMERULE)) {
			event.world.getGameRules().addGameRule(Freezing.GAMERULE, "true");
		}
	}

	@SubscribeEvent
	public void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.entityPlayer instanceof EntityPlayerMP) {
			Freezing.startTracking(event.target, (EntityPlayerMP) event.entityPlayer);
		}
	}

	public static boolean isEnabled() {
		return McFlyBlocks.POWDER_SNOW.isEnabled();
	}

	/**
	 * 26.3's dispensers (spec §62.8): the powder snow bucket places powder snow into an empty block in front (an empty
	 * bucket back), else drops as any item; an empty bucket takes powder snow in front (a powder snow bucket back, into
	 * the dispenser, or dropped when there's no room), else does what 1.7.10's bucket does.
	 */
	public static void registerDispensers() {
		final Item bucket = McFlyItems.POWDER_SNOW_BUCKET.get();
		final Block snow = McFlyBlocks.POWDER_SNOW.get();
		BlockDispenser.dispenseBehaviorRegistry.putObject(bucket, new BehaviorDefaultDispenseItem() {
			@Override
			protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
				EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
				World world = source.getWorld();
				int x = source.getXInt() + facing.getFrontOffsetX();
				int y = source.getYInt() + facing.getFrontOffsetY();
				int z = source.getZInt() + facing.getFrontOffsetZ();
				if (y >= 0 && y < 256 && world.isAirBlock(x, y, z)) {
					world.setBlock(x, y, z, snow, 0, 3);
					world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.empty_powder_snow", 1.0F, 1.0F);
					stack.func_150996_a(Items.bucket);
					stack.stackSize = 1;
					return stack;
				}
				return super.dispenseStack(source, stack);
			}
		});
		final IBehaviorDispenseItem emptyBucket = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(Items.bucket);
		BlockDispenser.dispenseBehaviorRegistry.putObject(Items.bucket, new IBehaviorDispenseItem() {
			private final BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();

			@Override
			public ItemStack dispense(IBlockSource source, ItemStack stack) {
				EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
				World world = source.getWorld();
				int x = source.getXInt() + facing.getFrontOffsetX();
				int y = source.getYInt() + facing.getFrontOffsetY();
				int z = source.getZInt() + facing.getFrontOffsetZ();
				if (world.getBlock(x, y, z) != snow) {
					return emptyBucket.dispense(source, stack);
				}
				world.setBlockToAir(x, y, z);
				world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(snow));
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.fill_powder_snow", 1.0F, 1.0F);
				// 1.7.10's dispenser sound and smoke, as its own behaviours make them
				world.playAuxSFX(1000, source.getXInt(), source.getYInt(), source.getZInt(), 0);
				world.playAuxSFX(2000, source.getXInt(), source.getYInt(), source.getZInt(), facing.getFrontOffsetX() + 1 + (facing.getFrontOffsetZ() + 1) * 3);
				if (--stack.stackSize <= 0) {
					stack.func_150996_a(bucket);
					stack.stackSize = 1;
				} else if (((TileEntityDispenser) source.getBlockTileEntity()).func_146019_a(new ItemStack(bucket)) < 0) {
					drop.dispense(source, new ItemStack(bucket));
				}
				return stack;
			}
		});
	}
}
