package io.github.asgerjon.mcfly.caves;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.RandomTicking;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Cave vines and glow berries as 26.3's (SPEC-modern-movement.md §90.3), EFR's two ids: `cave_vine` the head (26.3's
 * `cave_vines`), `cave_vine_plant` the body; the berries in meta bit 0 on both (light 14 with them), the head's age (26.3's
 * 0 to 25) in a tile entity under EFR's id `etfuturum.cave_vines`, an EFR head's `MaxLength` and `TipSheared` read into it
 * on its first random tick. Instabreak, no collision, climbable, its own sound, fire 15 and 60, popped by pistons. A head
 * under 25 grows one down at 0.1 a random tick, a year older, berries at 0.11; bonemeal gives berries; shears on the head
 * stop it; an unheld vine is destroyed with its drops on its next tick; berries picked by hand pop out at the block.
 */
public final class CaveVines {

	public static final int MAX_AGE = 25;

	private CaveVines() {
	}

	public static boolean berries(int meta) {
		return (meta & 1) != 0;
	}

	public static boolean isVine(Block block) {
		return block instanceof Vine;
	}

	/** What's the same in both. */
	public abstract static class Vine extends Block implements ModelledBlock, IGrowable {

		private final String model;
		@SideOnly(Side.CLIENT)
		private IIcon lit;

		protected Vine(String name, String model) {
			super(Material.plants);
			this.model = model;
			setBlockName(McFly.name(name));
			setBlockTextureName(model);
			setHardness(0.0F);
			setStepSound(McFlySoundType.CAVE_VINES);
			// 26.3's `column(14, 0, 16)`
			setBlockBounds(1.0F / 16.0F, 0.0F, 1.0F / 16.0F, 15.0F / 16.0F, 1.0F, 15.0F / 16.0F);
		}

		/** The head of this vine's kind and its body. */
		protected abstract Head head();

		protected abstract Body body();

		@Override
		public int getLightValue(IBlockAccess world, int x, int y, int z) {
			return world.getBlock(x, y, z) == this && berries(world.getBlockMetadata(x, y, z)) ? 14 : 0;
		}

		@Override
		public MapColor getMapColor(int meta) {
			return MapColor.foliageColor;
		}

		@Override
		public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return true;
		}

		@Override
		public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 60;
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
			return 15;
		}

		@Override
		public int getMobilityFlag() {
			return 1;
		}

		@Override
		public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
			return true;
		}

		@Override
		public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
			return null;
		}

		@Override
		public boolean isOpaqueCube() {
			return false;
		}

		@Override
		public boolean renderAsNormalBlock() {
			return false;
		}

		@Override
		public int getRenderType() {
			return McFlyRenderIds.MODEL;
		}

		// ---------------------------------------------------------------- held: a vine above, or a solid face

		public static boolean held(IBlockAccess world, int x, int y, int z) {
			Block above = world.getBlock(x, y + 1, z);
			return isVine(above) || above.isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN);
		}

		@Override
		public boolean canPlaceBlockAt(World world, int x, int y, int z) {
			return super.canPlaceBlockAt(world, x, y, z) && held(world, x, y, z);
		}

		@Override
		public boolean canBlockStay(World world, int x, int y, int z) {
			return held(world, x, y, z);
		}

		/** Unheld: destroyed on the next tick (26.3's scheduled tick); else the head or body it now is. */
		@Override
		public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
			if (world.isRemote) {
				return;
			}
			if (!held(world, x, y, z)) {
				world.scheduleBlockUpdate(x, y, z, this, 1);
				return;
			}
			reshape(world, x, y, z);
		}

		/** A head with a vine below becomes body, a body with none becomes a head (26.3's `updateShape`), the berries kept. */
		protected abstract void reshape(World world, int x, int y, int z);

		@Override
		public void updateTick(World world, int x, int y, int z, Random random) {
			if (!world.isRemote && !held(world, x, y, z)) {
				world.func_147480_a(x, y, z, true);
			}
		}

		// ---------------------------------------------------------------- the berries

		/** Picking: the berries pop out at the block, its sound at 0.8 to 1.2, the berries gone (26.3's `CaveVines.use`). */
		protected boolean pick(World world, int x, int y, int z) {
			int meta = world.getBlockMetadata(x, y, z);
			if (!berries(meta)) {
				return false;
			}
			if (!world.isRemote) {
				double px = x + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5, py = y + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5 - 0.125;
				double pz = z + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
				EntityItem item = new EntityItem(world, px, py, pz, McFlyItems.GLOW_BERRIES_ITEM.newItemStack());
				item.delayBeforeCanPickup = 10;
				world.spawnEntityInWorld(item);
				world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":block.cave_vines.pick_berries", 1.0F,
					0.8F + world.rand.nextFloat() * 0.4F);
				world.setBlockMetadataWithNotify(x, y, z, meta & ~1, 2);
				// 1.7.10 relights on a block change only when the block's own light differs: the meta's does here
				world.func_147451_t(x, y, z);
			}
			return true;
		}

		@Override
		public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
			return pick(world, x, y, z);
		}

		@Override
		public Item getItemDropped(int meta, Random random, int fortune) {
			return berries(meta) ? McFlyItems.GLOW_BERRIES_ITEM.get() : null;
		}

		@Override
		public int damageDropped(int meta) {
			return 0;
		}

		@Override
		public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
			return McFlyItems.GLOW_BERRIES_ITEM.newItemStack();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public Item getItem(World world, int x, int y, int z) {
			return McFlyItems.GLOW_BERRIES_ITEM.get();
		}

		// ---------------------------------------------------------------- bonemeal: the berries (26.3's `CaveVinesBlock`)

		@Override
		public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
			return !berries(world.getBlockMetadata(x, y, z));
		}

		@Override
		public boolean func_149852_a(World world, Random random, int x, int y, int z) {
			return true;
		}

		@Override
		public void func_149853_b(World world, Random random, int x, int y, int z) {
			world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) | 1, 2);
			world.func_147451_t(x, y, z);
		}

		// ---------------------------------------------------------------- 26.3's models (cross), lit with the berries

		@Override
		public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
			return Collections.singletonList(new ModelPart(berries(world.getBlockMetadata(x, y, z)) ? model + "_lit" : model));
		}

		@Override
		public List<ModelPart> itemParts(int meta) {
			return Collections.singletonList(new ModelPart(model));
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void registerBlockIcons(IIconRegister register) {
			super.registerBlockIcons(register);
			lit = register.registerIcon(model + "_lit");
		}

		@Override
		@SideOnly(Side.CLIENT)
		public IIcon modelIcon(String texture) {
			return texture.endsWith("_lit") ? lit : blockIcon;
		}
	}

	/** The head (`etfuturum:cave_vine`): its age in its tile entity, random ticks. */
	public static final class Head extends Vine implements ITileEntityProvider, RandomTicking {

		private Body body;

		public Head() {
			super("cave_vines", "cave_vines");
			this.isBlockContainer = true;
			setTickRandomly(true);
			setCreativeTab(null);
		}

		public void setBody(Body body) {
			this.body = body;
		}

		@Override
		protected Head head() {
			return this;
		}

		@Override
		protected Body body() {
			return body;
		}

		@Override
		public TileEntity createNewTileEntity(World world, int meta) {
			return new Tile();
		}

		@Override
		public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
			super.breakBlock(world, x, y, z, block, meta);
			world.removeTileEntity(x, y, z);
		}

		/** The age there, an EFR head's read from its length first (sheared 25, else 25 less what it had left to grow). */
		public int age(World world, int x, int y, int z) {
			TileEntity found = world.getTileEntity(x, y, z);
			if (!(found instanceof Tile tile)) {
				return MAX_AGE;
			}
			if (tile.age < 0) {
				if (tile.efrMaxLength >= 0) {
					int length = 1;
					while (world.getBlock(x, y + length, z) == body) {
						length++;
					}
					tile.setAge(tile.efrSheared ? MAX_AGE : Math.max(0, MAX_AGE - Math.max(0, tile.efrMaxLength - length)));
				} else {
					tile.setAge(world.rand.nextInt(MAX_AGE));
				}
			}
			return tile.age;
		}

		public void setAge(World world, int x, int y, int z, int age) {
			if (world.getTileEntity(x, y, z) instanceof Tile tile) {
				tile.setAge(age);
			}
		}

		@Override
		public void randomTick(World world, int x, int y, int z, Random random) {
			if (age(world, x, y, z) < MAX_AGE && random.nextDouble() < 0.1D) {
				growOn(world, x, y, z, random);
			}
		}

		/** One more below, into air: the new head a year older, berries at 0.11; this one becomes body by its neighbour's change. */
		public boolean growOn(World world, int x, int y, int z, Random random) {
			if (y <= 0 || !world.isAirBlock(x, y - 1, z)) {
				return false;
			}
			int age = Math.min(MAX_AGE, age(world, x, y, z) + 1);
			boolean berries = random.nextFloat() < 0.11F;
			world.setBlock(x, y - 1, z, this, berries ? 1 : 0, 3);
			setAge(world, x, y - 1, z, age);
			if (berries) {
				world.func_147451_t(x, y - 1, z);
			}
			return true;
		}

		@Override
		protected void reshape(World world, int x, int y, int z) {
			if (isVine(world.getBlock(x, y - 1, z))) {
				world.setBlock(x, y, z, body, world.getBlockMetadata(x, y, z) & 1, 2);
			}
		}

		/** Berries first (26.3's block use), then shears stop a head under 25 (`block.growing_plant.crop`). */
		@Override
		public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
			if (pick(world, x, y, z)) {
				return true;
			}
			ItemStack held = player.getHeldItem();
			if (held == null || !(held.getItem() instanceof ItemShears) || age(world, x, y, z) >= MAX_AGE) {
				return false;
			}
			if (!world.isRemote) {
				setAge(world, x, y, z, MAX_AGE);
				world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":block.growing_plant.crop", 1.0F, 1.0F);
				held.damageItem(1, player);
			}
			return true;
		}

		@Override
		public boolean onBlockEventReceived(World world, int x, int y, int z, int id, int data) {
			super.onBlockEventReceived(world, x, y, z, id, data);
			TileEntity tile = world.getTileEntity(x, y, z);
			return tile != null && tile.receiveClientEvent(id, data);
		}
	}

	/** The body (`etfuturum:cave_vine_plant`). */
	public static final class Body extends Vine {

		private final Head head;

		public Body(Head head) {
			super("cave_vines_plant", "cave_vines_plant");
			this.head = head;
			head.setBody(this);
			setCreativeTab(null);
		}

		@Override
		protected Head head() {
			return head;
		}

		@Override
		protected Body body() {
			return this;
		}

		@Override
		protected void reshape(World world, int x, int y, int z) {
			if (!isVine(world.getBlock(x, y - 1, z))) {
				world.setBlock(x, y, z, head, world.getBlockMetadata(x, y, z) & 1, 2);
				head.setAge(world, x, y, z, world.rand.nextInt(MAX_AGE));
			}
		}
	}

	/** The head's age, under EFR's tile entity id; an EFR head's `MaxLength` and `TipSheared` kept till read. */
	public static final class Tile extends TileEntity {

		int age = -1;
		int efrMaxLength = -1;
		boolean efrSheared;

		void setAge(int age) {
			this.age = age;
			markDirty();
		}

		@Override
		public void writeToNBT(NBTTagCompound tag) {
			super.writeToNBT(tag);
			if (age >= 0) {
				tag.setInteger("Age", age);
			} else if (efrMaxLength >= 0) {
				tag.setInteger("MaxLength", efrMaxLength);
				tag.setBoolean("TipSheared", efrSheared);
			}
		}

		@Override
		public void readFromNBT(NBTTagCompound tag) {
			super.readFromNBT(tag);
			if (tag.hasKey("Age")) {
				age = tag.getInteger("Age");
			} else if (tag.hasKey("MaxLength")) {
				efrMaxLength = tag.getInteger("MaxLength");
				efrSheared = tag.getBoolean("TipSheared");
			}
		}

		@Override
		public boolean canUpdate() {
			return false;
		}
	}

	/**
	 * Glow berries (`etfuturum:glow_berries_item`, EFR's id): 26.3's food 2 with 0.1; placing a vine (26.3's block item of
	 * `cave_vines`): a body under a vine, else a head of age 0 to 24, where a vine is held.
	 */
	public static final class Berries extends ItemFood {

		private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		private final Head head;
		private final Body body;

		public Berries(Head head, Body body) {
			super(2, 0.1F, false);
			this.head = head;
			this.body = body;
			setUnlocalizedName(McFly.name("glow_berries"));
			setTextureName("glow_berries");
			setCreativeTab(McFlyTab.TAB);
		}

		@Override
		public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
			Block clicked = world.getBlock(x, y, z);
			if (!clicked.isReplaceable(world, x, y, z)) {
				x += OFFSET[side][0];
				y += OFFSET[side][1];
				z += OFFSET[side][2];
			}
			if (stack.stackSize <= 0 || !player.canPlayerEdit(x, y, z, side, stack) || !world.getBlock(x, y, z).isReplaceable(world, x, y, z)
				|| !Vine.held(world, x, y, z)) {
				return false;
			}
			if (!world.isRemote) {
				boolean under = isVine(world.getBlock(x, y - 1, z));
				world.setBlock(x, y, z, under ? body : head, 0, 3);
				if (!under) {
					head.setAge(world, x, y, z, world.rand.nextInt(MAX_AGE));
				}
				world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, head.stepSound.func_150496_b(), (head.stepSound.getVolume() + 1.0F) / 2.0F,
					head.stepSound.getPitch() * 0.8F);
				stack.stackSize--;
			}
			return true;
		}
	}
}
