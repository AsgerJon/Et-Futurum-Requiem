package ganymedes01.etfuturum.compat;

import io.github.asgerjon.mcfly.banner.BannerBlockEntity;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import cpw.mods.fml.common.event.FMLInterModComms;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockPotionCauldron;
import ganymedes01.etfuturum.tileentities.TileEntityCauldronPotion;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class CompatWaila {

	public static void wailaCallback(IWailaRegistrar registrar) {
		registrar.registerStackProvider(new ShulkerDataProvider(), io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxBlock.class);
		registrar.registerStackProvider(new BannerDataProvider(), io.github.asgerjon.mcfly.banner.BannerBlock.class);
		registrar.registerBodyProvider(new BannerDataProvider(), io.github.asgerjon.mcfly.banner.BannerBlock.class);
		registrar.registerStackProvider(new PotionCauldronDataProvider(), BlockPotionCauldron.class);
		registrar.registerBodyProvider(new PotionCauldronDataProvider(), BlockPotionCauldron.class);
	}

	public static void register() {
		FMLInterModComms.sendMessage("Waila", "register", CompatWaila.class.getName() + ".wailaCallback");
	}

	public static class BannerDataProvider implements IWailaDataProvider {

		@Override
		public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
			BannerBlockEntity banner = (BannerBlockEntity) accessor.getTileEntity();
			return banner.createStack();
		}

		@Override
		public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
										 IWailaConfigHandler config) {
			return null;
		}

		@Override
		public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
										 IWailaConfigHandler config) {
			List<String> strings = new ArrayList<>();
			Item.getItemFromBlock(McFlyBlocks.BANNER.get()).addInformation(itemStack, null, strings, false);
			return strings;
		}

		@Override
		public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
										 IWailaConfigHandler config) {
			return null;
		}

		@Override
		public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
			return tag;
		}

	}

	public static class PotionCauldronDataProvider implements IWailaDataProvider {

		@Override
		public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
			TileEntityCauldronPotion cauldron = (TileEntityCauldronPotion) accessor.getTileEntity();
			if (cauldron.potion != null)
				return cauldron.potion;
			return new ItemStack(Blocks.cauldron);
		}

		@Override
		public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
										 IWailaConfigHandler config) {
			return null;
		}

		@Override
		public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
										 IWailaConfigHandler config) {
			List<String> strings = new ArrayList<>();
			Items.potionitem.addInformation(itemStack, null, strings, false);
			return strings;
		}

		@Override
		public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
										 IWailaConfigHandler config) {
			return null;
		}

		@Override
		public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
			return tag;
		}

	}

	public static class ShulkerDataProvider implements IWailaDataProvider {
		@Override
		public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
			ItemStack stack = accessor.getStack();
			if (accessor.getTileEntity() instanceof io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxBlockEntity box && box.colour() > 0) {
				if (!stack.hasTagCompound()) {
					stack.setTagCompound(new NBTTagCompound());
				}
				stack.getTagCompound().setByte("Color", (byte) box.colour());
			}
			return stack;
		}

		@Override
		public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
			return currenttip;
		}

		@Override
		public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
			return currenttip;
		}

		@Override
		public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
			return currenttip;
		}

		@Override
		public NBTTagCompound getNBTData(EntityPlayerMP arg0, TileEntity arg1, NBTTagCompound arg2, World arg3, int arg4, int arg5, int arg6) {
			return arg2;
		}
	}
}
