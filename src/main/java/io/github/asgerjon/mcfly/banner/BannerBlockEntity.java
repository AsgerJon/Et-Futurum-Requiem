package io.github.asgerjon.mcfly.banner;

import io.github.asgerjon.mcfly.dye.DyeColor;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A placed banner (SPEC-modern-movement.md §80; 26.3's `BannerBlockEntity`), stored in 1.8's form, as old saves hold it
 * (`etfuturum.banner`): `Base` its base colour, `IsStanding` (the block's meta is a standing banner's rotation or the
 * wall it hangs on, which can't tell 2 to 5 apart), `Patterns` its layers (each a short `Pattern` id and a `Color`).
 * Its layers are written back as they were read, a short id no pattern has included.
 */
public class BannerBlockEntity extends TileEntity {

	private int base;
	private boolean standing;
	private NBTTagList patterns;
	private List<Banners.Layer> layers;

	/** Its colour and layers from a banner item (placing one, or drawing one as an item). */
	public void setItemValues(ItemStack stack) {
		NBTTagCompound tag = Banners.entityTag(stack, false);
		patterns = tag != null && tag.hasKey("Patterns", 9) ? (NBTTagList) tag.getTagList("Patterns", 10).copy() : null;
		base = Banners.base(stack);
		layers = null;
	}

	public void setStanding(boolean standing) {
		this.standing = standing;
	}

	public boolean isStanding() {
		return standing;
	}

	public DyeColor baseColour() {
		return DyeColor.byId(base);
	}

	/** Its layers, in order (the base colour not among them). */
	public List<Banners.Layer> layers() {
		if (layers == null) {
			layers = Banners.layers(patterns);
		}
		return layers;
	}

	/** Its base and its layers as one key ("b14bs0glb11"), for the canvas made from them. */
	public String canvasKey() {
		StringBuilder key = new StringBuilder("b").append(base);
		for (Banners.Layer layer : layers()) {
			key.append(layer.pattern().shortId()).append(layer.colour().id());
		}
		return key.toString();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("Base", base);
		nbt.setBoolean("IsStanding", standing);
		if (patterns != null) {
			nbt.setTag("Patterns", patterns);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		base = nbt.getInteger("Base");
		standing = nbt.getBoolean("IsStanding");
		patterns = nbt.hasKey("Patterns", 9) ? nbt.getTagList("Patterns", 10) : null;
		layers = null;
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		if (packet.func_148853_f() == 0) {
			readFromNBT(packet.func_148857_g());
		}
	}

	/**
	 * The banner as an item (broken, or picked in creative): its colour as its damage, its layers (if any) in its tag,
	 * so a plain one stacks with a crafted one, as in 26.3.
	 */
	public ItemStack createStack() {
		ItemStack stack = McFlyBlocks.BANNER.newItemStack(1, base);
		if (patterns != null && patterns.tagCount() > 0) {
			Banners.entityTag(stack, true).setTag("Patterns", patterns.copy());
		}
		return stack;
	}

	/** Its layers as a list another banner can take (the loom's preview, the item drawn as a banner). */
	public static List<Banners.Layer> layersOf(ItemStack stack) {
		return new ArrayList<>(Banners.layers(stack));
	}
}
